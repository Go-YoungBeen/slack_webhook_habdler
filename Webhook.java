import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    public static void main(String[] args) {
        try {
            String prompt = System.getenv("LLM_PROMPT");
            String llmResult = useLLM(prompt);
            System.out.println("llmResult = " + llmResult);
            
            String template = System.getenv("LLM2_IMAGE_TEMPLATE");
            // Escape any special characters in the LLM result
            String escapedResult = llmResult.replace("\"", "\\\"")
                                         .replace("\n", "\\n")
                                         .replace("\r", "\\r");
            String imagePrompt = String.format("%s: %s", template, escapedResult);
            System.out.println("imagePrompt = " + imagePrompt);
            
            String llmImageResult = useLLMForImage(imagePrompt);
            System.out.println("llmImageResult = " + llmImageResult);
            
            String title = System.getenv("SLACK_WEBHOOK_TITLE");
            sendSlackMessage(title, llmResult, llmImageResult);
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String useLLMForImage(String prompt) {
        String apiUrl = System.getenv("LLM2_API_URL");
        String apiKey = System.getenv("LLM2_API_KEY");
        String model = System.getenv("LLM2_MODEL");
        
        // Properly escape the prompt for JSON
        String escapedPrompt = prompt.replace("\"", "\\\"")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r");
        
        String payload = String.format("""
                {
                  "prompt": "%s",
                  "model": "%s",
                  "width": 1440,
                  "height": 1440,
                  "steps": 4,
                  "n": 1
                }
                """, escapedPrompt, model);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("API request failed with status " + response.statusCode() + ": " + response.body());
            }
            
            // More robust response parsing
            String body = response.body();
            if (!body.contains("\"url\": \"")) {
                throw new RuntimeException("Unexpected response format: " + body);
            }
            
            return body.split("\"url\": \"")[1].split("\"")[0];
        } catch (Exception e) {
            throw new RuntimeException("Error in useLLMForImage: " + e.getMessage(), e);
        }
    }

    public static String useLLM(String prompt) {
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");
        
        // Properly escape the prompt for JSON
        String escapedPrompt = prompt.replace("\"", "\\\"")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r");
        
        String payload = String.format("""
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "%s"
                }
                """, escapedPrompt, model);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("API request failed with status " + response.statusCode() + ": " + response.body());
            }
            
            // More robust response parsing
            String body = response.body();
            if (!body.contains("\"content\":\"")) {
                throw new RuntimeException("Unexpected response format: " + body);
            }
            
            return body.split("\"content\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            throw new RuntimeException("Error in useLLM: " + e.getMessage(), e);
        }
    }

    public static void sendSlackMessage(String title, String text, String imageUrl) {
        String slackUrl = System.getenv("SLACK_WEBHOOK_URL");
        
        // Escape special characters for JSON
        String escapedTitle = title.replace("\"", "\\\"")
                                 .replace("\n", "\\n")
                                 .replace("\r", "\\r");
        String escapedText = text.replace("\"", "\\\"")
                               .replace("\n", "\\n")
                               .replace("\r", "\\r");
        String escapedImageUrl = imageUrl.replace("\"", "\\\"");
        
        String payload = String.format("""
                {
                    "attachments": [{
                        "title": "%s",
                        "text": "%s",
                        "image_url": "%s"
                    }]
                }
                """, escapedTitle, escapedText, escapedImageUrl);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Slack response status code: " + response.statusCode());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Slack webhook failed with status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in sendSlackMessage: " + e.getMessage(), e);
        }
    }
}