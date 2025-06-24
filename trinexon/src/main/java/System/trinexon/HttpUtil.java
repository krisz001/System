package System.trinexon;

import java.net.URI;
import java.net.http.*;

public class HttpUtil {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static String post(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body(); // 🔁 Mindig visszaadjuk a válasz törzsét

        } catch (Exception e) {
            return "{\"error\": \"Hiba POST: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    public static String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body(); // 🔁 Akkor is visszaadjuk, ha 404 vagy más kód

        } catch (Exception e) {
            return "{\"error\": \"Hiba GET: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // Teljes válasz ha kell
    public static HttpResponse<String> sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> sendPost(String url, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}