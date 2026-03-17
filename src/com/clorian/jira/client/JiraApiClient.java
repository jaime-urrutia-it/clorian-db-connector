// archivo: src/main/java/com/clorian/jira/client/JiraApiClient.java

package com.clorian.jira.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;

public class JiraApiClient {

    private final String baseUrl;
    private final String authHeader;
    private final HttpClient client;
    private final String email;
    private final String apiToken;

    public JiraApiClient(String instanceUrl, String email, String apiToken) {
        // Normalizamos la URL base
        String cleanedUrl = instanceUrl.trim();
        if (!cleanedUrl.endsWith("/")) {
            cleanedUrl = cleanedUrl + "/";
        }
        this.baseUrl = cleanedUrl;

        // Guardamos credenciales para uso futuro
        this.email = email;
        this.apiToken = apiToken;

        // Preparamos el header de autenticación
        String credentials = email + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        // Cliente HTTP
        this.client = HttpClient.newHttpClient();

        System.out.println("[JIRA] Cliente inicializado para: " + this.baseUrl);
    }

    /**
     * Cambia el estado de un issue en Jira.
     *
     * @param issueKey      Clave del issue (ej: KAN-7)
     * @param transitionId  ID de la transición (ej: "21" para "En curso")
     * @return true si tuvo éxito
     */
    public boolean transitionIssue(String issueKey, String transitionId) {
        try {
            JSONObject json = new JSONObject();
            json.put("transition", new JSONObject().put("id", transitionId));

            String body = json.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "rest/api/3/issue/" + issueKey + "/transitions"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204 || response.statusCode() == 200) {
                System.out.println("[JIRA] ✅ Estado del issue " + issueKey + " actualizado (transición " + transitionId + ")");
                return true;
            } else {
                System.err.println("[JIRA] ❌ Error al transicionar: " + response.statusCode() + " - " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("[JIRA] ❌ Excepción al transicionar: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Prueba la conexión con Jira haciendo una llamada a /rest/api/2/myself
     *
     * @return true si la conexión es exitosa
     */
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "rest/api/2/myself"))
                    .header("Authorization", authHeader)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Conexión a Jira exitosa");
                return true;
            } else {
                System.err.println("❌ Error en Jira: Código " + response.statusCode());
                System.err.println("❌ Respuesta: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Fallo de conexión: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Getters
    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAuthHeader() {
        return authHeader;
    }

    public HttpClient getClient() {
        return client;
    }

    public String getEmail() {
        return email;
    }

    public String getApiToken() {
        return apiToken;
    }
}