import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    public static void main(String[] args) {
        String prompt = System.getenv("LLM_PROMPT");
        String llmResult = useLLM(prompt);
        System.out.println("llmResult = " + llmResult);
        
        String template = System.getenv("LLM2_IMAGE_TEMPLATE");
        String imagePrompt = template.formatted(llmResult);
        System.out.println("imagePrompt = " + imagePrompt);
        String llmImageResult = useLLMForImage(imagePrompt);
        System.out.println("llmImageResult = " + llmImageResult);
        
        String title = System.getenv("SLACK_WEBHOOK_TITLE");
        sendSlackMessage(title, llmResult, llmImageResult);
    }

    public static String useLLMForImage(String prompt) {
        String apiUrl = System.getenv("LLM2_API_URL");
        String apiKey = System.getenv("LLM2_API_KEY");
        String model = System.getenv("LLM2_MODEL");
        
        String payload = """
                {
                  "prompt": "%s",
                  "model": "%s",
                  "width": 1440,
                  "height": 1440,
                  "steps": 4,
                  "n": 1
                }
                """.formatted(prompt, model);
                
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
                
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("API call failed: " + response.body());
            }

            return response.body()
                    .split("\"url\": \"")[1]
                    .split("\",")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String useLLM(String prompt) {
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        
        // API URL에 generateContent 추가 및 key 파라미터 추가
        apiUrl = apiUrl + ":generateContent?key=" + apiKey;
        
        // 프롬프트 앞에 명확한 지시사항 추가
        String enhancedPrompt = prompt + "\n\n해당 내용을 이해하고 현재 시간의 분(minute)을 확인하여, 적절한 구간의 문장 중 하나만을 선택하여 출력하세요. 어떠한 설명이나 부가 내용 없이 선택된 문장만을 그대로 출력하세요.";
        
        String payload = String.format("""
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """, enhancedPrompt);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("API call failed: " + response.body());
            }

            // Gemini API 응답 파싱
            String responseBody = response.body();
            if (responseBody.contains("\"text\":")) {
                String text = responseBody.split("\"text\": \"")[1].split("\"")[0];
                // 출력된 텍스트에서 대괄호 안의 내용만 추출
                if (text.contains("[") && text.contains("]")) {
                    return text.substring(text.indexOf("[") + 1, text.lastIndexOf("]"));
                }
                return text;
            }
            throw new RuntimeException("Failed to parse response: " + responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Error calling LLM API", e);
        }
    }

    public static void sendSlackMessage(String title, String text, String imageUrl) {
        String slackUrl = System.getenv("SLACK_WEBHOOK_URL");
        
        String payload = """
                    {"attachments": [{
                        "title": "%s",
                        "text": "%s",
                        "image_url": "%s"
                    }]}
                """.formatted(title, text, imageUrl);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}