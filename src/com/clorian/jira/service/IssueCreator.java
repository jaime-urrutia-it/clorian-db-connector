// archivo: src/com/clorian/jira/service/IssueCreator.java

package com.clorian.jira.service;

import com.clorian.jira.client.JiraApiClient;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;

/**
 * Servicio para crear issues en Jira a partir de datos externos (ej: MySQL).
 * No incluye el campo 'priority' temporalmente para evitar errores de pantalla.
 */
public class IssueCreator {

    private final JiraApiClient jiraClient;
    private final String projectKey;
    private final String issueTypeId;

    public IssueCreator(JiraApiClient jiraClient, String projectKey, String issueTypeId) {
        this.jiraClient = jiraClient;
        this.projectKey = projectKey;
        this.issueTypeId = issueTypeId;
    }

    /**
     * Crea un issue en Jira.
     *
     * @param summary     Título del issue
     * @param description Descripción (texto plano)
     * @return el key del issue creado (ej: "KAN-123") o null si falla
     */
    /**
     * Crea un issue en Jira usando un objeto description en formato ADF.
     *
     * @param summary        Título del issue
     * @param adfDescription Descripción en formato ADF (JSONObject)
     * @return el key del issue creado (ej: "KAN-123") o null si falla
     */
    /**
     * Crea un issue con descripción ADF, campo personalizado y prioridad.
     */
    public String createIssueWithDescriptionAndCustomField(
            String summary,
            JSONObject adfDescription,
            String externalId,
            String priority) {

        try {
            JSONObject fields = new JSONObject();
            fields.put("project", new JSONObject().put("key", projectKey));
            fields.put("summary", summary);
            fields.put("issuetype", new JSONObject().put("id", issueTypeId));
            fields.put("description", adfDescription);

            // ✅ Campo personalizado: ID externo
            if (externalId != null && !externalId.isBlank()) {
                fields.put("customfield_10058", externalId); // ← Cambia por tu ID real
            }

            // ✅ Prioridad (si está permitida)
            if (priority != null && !priority.isBlank()) {
                JSONObject prio = new JSONObject();
                prio.put("name", priority);
                fields.put("priority", prio);
            }

            JSONObject json = new JSONObject();
            json.put("fields", fields);
            String body = json.toString(2);

            System.out.println("[JIRA] JSON enviado: " + body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jiraClient.getBaseUrl() + "rest/api/3/issue"))
                    .header("Authorization", jiraClient.getAuthHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = jiraClient.getClient().send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                JSONObject resp = new JSONObject(response.body());
                String issueKey = resp.getString("key");
                System.out.println("[JIRA] ✅ Issue creado: " + issueKey);
                return issueKey;
            } else {
                System.err.println("[JIRA] ❌ Error: " + response.statusCode());
                System.err.println("[JIRA] Respuesta: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("[JIRA] ❌ Excepción: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}