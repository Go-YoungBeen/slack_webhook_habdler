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
        String result = null;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            result = response.body().split("url\": \"")[1].split("\",")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static String useLLM(String prompt) {
    String apiUrl = System.getenv("LLM_API_URL");
    String apiKey = System.getenv("LLM_API_KEY");
    String model = System.getenv("LLM_MODEL");
    
    String payload = """
            {
              "messages": [
                {
                  "role": "user",
                  "content": "%s"
                }
              ],
              "model": "%s"
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

        String responseBody = response.body();
        // "content":" 다음에 오는 텍스트를 찾고
        int startIndex = responseBody.indexOf("\"content\":\"") + "\"content\":\"".length();
        // 그 다음에 오는 첫 번째 "\""까지의 내용만 추출
        int endIndex = responseBody.indexOf("\"", startIndex);
        
        if (startIndex >= 0 && endIndex > startIndex) {
            return responseBody.substring(startIndex, endIndex);
        }
        
        throw new RuntimeException("Failed to parse response");
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