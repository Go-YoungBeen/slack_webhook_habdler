import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    public static void main(String[] args) {
        String prompt = System.getenv("LLM_PROMPT");
//        String llmResult = useLLM("자바 알고리즘 공부를 위한 자료구조 중 랜덤으로 하나를 추천하고 설명해주는 내용을 200자 이내로 작성. 별도의 앞뒤 내용 없이 해당 내용만 출력. nutshell, for slack message, in korean.");
        String llmResult = useLLM(prompt); // 환경변수화
        System.out.println("llmResult = " + llmResult);
//        sendSlackMessage("안녕 안녕 나는 자바야 헬륨 가스 마시고 요로케 됐지");
//        String llmImageResult = useLLMForImage(prompt);
//        String llmImageResult = useLLMForImage(
//                llmResult + "를 바탕으로 해당 개념을 이해할 수 있는 상징적 과정을 표현한 비유적 이미지를 만들어줘.");
        String template = System.getenv("LLM2_IMAGE_TEMPLATE");
        // %s를 바탕으로 해당 개념을 이해할 수 있는 상징적 과정을 표현한 비유적 이미지를 만들어줘.
        String imagePrompt = template.formatted(llmResult);
        System.out.println("imagePrompt = " + imagePrompt);
        String llmImageResult = useLLMForImage(imagePrompt);
        System.out.println("llmImageResult = " + llmImageResult); // 발송은 안함
//        sendSlackMessage(llmResult);
        String title = System.getenv("SLACK_WEBHOOK_TITLE");
        sendSlackMessage(title, llmResult, llmImageResult);
    }

    public static String useLLMForImage(String prompt) {
        // https://api.together.xyz/
        // https://api.together.xyz/settings/api-keys
        // https://api.together.xyz/models
        // https://api.together.xyz/models/black-forest-labs/FLUX.1-schnell-Free
        // https://api.together.xyz/playground/image/black-forest-labs/FLUX.1-schnell-Free

        String apiUrl = System.getenv("LLM2_API_URL"); // 환경변수로 관리
        String apiKey = System.getenv("LLM2_API_KEY"); // 환경변수로 관리
        String model = System.getenv("LLM2_MODEL"); // 환경변수로 관리
        String payload = """
                {
                  "prompt": "%s",
                  "model": "%s",
                  "width": 1440,
                  "height": 1440,
                  "steps": 4,
                  "n": 1
                }
                """.formatted(prompt, model); // 대부분 JSON 파싱 문제는 , 문제!
        HttpClient client = HttpClient.newHttpClient(); // 새롭게 요청할 클라이언트 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl)) // URL을 통해서 어디로 요청을 보내는지 결정
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build(); // 핵심
        String result = null; // return을 하려면 일단은 할당이 되긴 해야함
        try { // try
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            /*
            {
                  "id": "...",
                    ...
                  "data": [
                    {
                      "index": 0,
                      "url": "https://api.together.ai/imgproxy/hzSDBVCxqVOStdqwFYa7jerhI2ky9aIIu9RN3-yhdAQ/format:jpeg/aHR0cHM6Ly90b2dldGhlci1haS1iZmwtaW1hZ2VzLXByb2QuczMudXMtd2VzdC0yLmFtYXpvbmF3cy5jb20vaW1hZ2VzLzBlY2U1MzQ4OTNiNTk2YTA0NTBkY2NkNTRkNjExNjc4OTBiMDkwZTRjZjI3NDdkOGUwZjI3NDZlM2M1MWRkYjg_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ29udGVudC1TaGEyNTY9VU5TSUdORUQtUEFZTE9BRCZYLUFtei1DcmVkZW50aWFsPUFTSUFZV1pXNEhWQ0JRT0I2VjVKJTJGMjAyNTAyMjElMkZ1cy13ZXN0LTIlMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUwMjIxVDA2MzMxNFomWC1BbXotRXhwaXJlcz0zNjAwJlgtQW16LVNlY3VyaXR5LVRva2VuPUlRb0piM0pwWjJsdVgyVmpFS2YlMkYlMkYlMkYlMkYlMkYlMkYlMkYlMkYlMkYlMkZ3RWFDWFZ6TFhkbGMzUXRNaUpITUVVQ0lFV2x1OXQlMkZSVUllZko5OXVWM0p3ZjdYb1lyNjR0c3VUcmZ1cnRFTmduc1lBaUVBeWs2QktQYVk1eWFRSUo5bEJEUENUMjVmYkRGclclMkZ0ekglMkZvR1RLZGVFaFFxbVFVSTBQJTJGJTJGJTJGJTJGJTJGJTJGJTJGJTJGJTJGJTJGQVJBQUdndzFPVGczTWpZeE5qTTNPREFpRE53cVQ4T29hVU1RSGI1THl5cnRCUCUyRkhBdkhUODN6VUFoUjRJZVR6MGRycFNibkdKJTJCYkw4WlJRaVIxRDZyNTd6ZldLcnRuelM4am9Ca0dxakk1WTMlMkZmUFRucjNnbCUyRkdUbkt0WFl0ZCUyQjUlMkJvYmxFcDBHdnZyS1pZbzgxbmZST3dMTGRHNEJkbTNFMmVKd0FSZEEyU2JDb2luSCUyQnd3RlljcDd6NzdNekdabE9sNmVYcXBCJTJCOHh3aDR4cEY1VWlGRVJEVWklMkZNTXhtTjZadUUlMkI4UHJzdk5IVGxyMXlKUnhsaHRzZlolMkJtUk1qZVZjMDdIbDZiZGxtaktyUHZJMUUlMkJnMHdITzFhZlE1SFhsRmhaRSUyQmFQcnVZSmFSZUI1QVl0YVBBY2Z4c3RlJTJGUDNjRTJFZ2FibEtGczVGMFpoUlpsJTJCZHNieE9OeDJoNXRXSGNWcWpRRjJoUUpOWmlaNTEwZDlka3lBUGJWUEczRmV2dnVlaGljQnBQYXBCaURSa3B2UHk3Z09IUUFJblNodWRQUEElMkIlMkZKWDd2ZTA5UEVZTzVWNUY4JTJGWGdhZndMbGdjbURXRFlMMyUyQmZ6cFU0TVBjYTVVNWFEJTJCeWgxTHB3MzM2dGVoQzR6SDdqOGladUxyd3BrVmt0QmFTRzFTb1l0UUtNZnRQc01DdUl3cEVLR09yWnpmJTJCYUtMMUlDR29TTmdYcTI1Y0hMckQlMkZIYm0wOXNEenR0dmtCaFQ4Y2tnVnJRamxNdTJTV0l4RWUwVU5UaGVxaFVHUkpUTUswUDJmdEhJVjVVelJ1OFFnbWIyWktBUTQ1bWlrUm9OMUIzM1dtZFVack9zb3Q2MlJkVmZSUE1wRGw1NnZFJTJCNDFZQWVYZ2tlaE9IVVJscVcxSTdtOHRZeUk0a2YlMkJMS0l1SEltVTB3QkExNjAwRUdteHFuUUYzTkZBbGUlMkZkNFM2TkZNZ0hVWFRtdUduTVdXYzRsYllXOGNHZ0RIc1U3UzBVTndjdjc1UnJHVFlUbUt5ZGlLMWdNd3ZwNHptYk9JbkVqcGZZQiUyRklKdWpyR1llcVBHMkxnUFQzdyUyQmV6Z0hXdnNTTGlYeHVZeFF5VkQlMkZOTHQ5M3lVWnlCVjNCJTJGTzlYQU5jS1AlMkZLbEExcHdnJTJCTkl6Q3B2T0M5QmpxYkFkR1ZEQ3YlMkJLdnZtYzBiblYzVFpwSjNSWHR0QUxBUmg3JTJGVG1uYjczTW1zdjRiS3A1bnJ4dTBYVkpFbWxXVENiN0c2dEJKdHVla1lXUSUyQlZqdUd6bXFrbzF3WWhZVElHSFliQklrTGFWRmREd1ptUSUyQnFyczQzTFJvJTJCJTJGZzVaTCUyQm11bEFyTENCd25lN3pKVUhPcnBzeDNSYkdVYlhaSHB3aWlCTVhEenp2YzdPaDJFSVgxWjRGNFhqTHlKRlBESVM2VUdiTHQ3bUFubHlaWTZVcyZYLUFtei1TaWduYXR1cmU9NTM5ZWRmZDhiNTMxODE5ZTQ5ZmRhYTI0ZTM4YzM5ZTVhODc1MzMwNGY3MmFmZjU0ZDQxYTdkY2FjNmE0MTM4YyZYLUFtei1TaWduZWRIZWFkZXJzPWhvc3QmeC1pZD1HZXRPYmplY3Q",
                      "timings": ...
                    }
                  ]
            }
             */
            result = response.body()
                    .split("url\": \"")[1]
                    .split("\",")[0];
        } catch (Exception e) { // catch exception e
            throw new RuntimeException(e);
        }
        return result; // 앞뒤를 자르고 우리에게 필요한 내용만 리턴
    }

    public static String useLLM(String prompt) {
    String apiUrl = System.getenv("LLM_API_URL");
    String apiKey = System.getenv("LLM_API_KEY");
    
    // API URL에 key 파라미터 추가
    apiUrl = apiUrl + "?key=" + apiKey;
    
    // Gemini API의 요청 형식에 맞게 수정
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
            """, prompt);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            // Gemini API는 Authorization 헤더가 필요하지 않음
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

    try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("response.statusCode() = " + response.statusCode());
        System.out.println("response.body() = " + response.body());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API call failed: " + response.body());
        }

        // Gemini API 응답 형식에 맞게 파싱
        if (response.body().contains("\"text\":")) {
            return response.body()
                    .split("\"text\": \"")[1]
                    .split("\"")[0];
        }
        throw new RuntimeException("Failed to parse response: " + response.body());
    } catch (Exception e) {
        throw new RuntimeException("Error calling LLM API", e);
    }
}

    //    public static void sendSlackMessage(String text) {
    public static void sendSlackMessage(String title, String text, String imageUrl) {
        // 다시 시작된 슬랙 침공
//        String slackUrl = "https://hooks.slack.com/services/";
        String slackUrl = System.getenv("SLACK_WEBHOOK_URL"); // 환경변수로 관리
//        String payload = "{\"text\": \"채널에 있는 한 줄의 텍스트입니다.\\n또 다른 한 줄의 텍스트입니다.\"}";
//        String payload = "{\"text\": \"" + text + "\"}";
        // slack webhook attachments -> 검색 혹은 LLM
        String payload = """
                    {"attachments": [{
                        "title": "%s",
                        "text": "%s",
                        "image_url": "%s"
                    }]}
                """.formatted(title, text, imageUrl);

        HttpClient client = HttpClient.newHttpClient(); // 새롭게 요청할 클라이언트 생성
        // 요청을 만들어보자! (fetch)
        HttpRequest request = HttpRequest.newBuilder()
                // 어디로? URI(URL) -> Uniform Resource Identifier(Link)
                .uri(URI.create(slackUrl)) // URL을 통해서 어디로 요청을 보내는지 결정
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build(); // 핵심

        // 네트워크 과정에서 오류가 있을 수 있기에 선제적 예외처리
        try { // try
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
      
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
        } catch (Exception e) { // catch exception e
            throw new RuntimeException(e);
        }
    }
}
