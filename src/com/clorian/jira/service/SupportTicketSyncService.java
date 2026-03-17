// archivo: src/com/clorian/jira/service/SupportTicketSyncService.java

package com.clorian.jira.service;

import com.clorian.db.DatabaseConnection;
import com.clorian.jira.client.JiraApiClient;
import org.json.JSONObject;
import org.json.JSONArray;

import java.sql.*;

public class SupportTicketSyncService {

    private final IssueCreator issueCreator;
    private final String customFieldId; // Ej: "customfield_10020"

    public SupportTicketSyncService(IssueCreator issueCreator, String customFieldId) {
        this.issueCreator = issueCreator;
        this.customFieldId = customFieldId;
    }

	 public void syncOpenTickets() {
	     String sql = """
	         SELECT 
	             st.support_ticket_id, st.subject, st.description, 
	             st.priority, st.created_at,
	             c.first_name, c.last_name, c.email
	         FROM SupportTickets st
	         JOIN Customers c ON st.customer_id = c.customer_id
	         WHERE st.status = 'Open'
	         AND st.jira_issue_key IS NULL  -- ✅ Solo los que no están sincronizados
	         """;
	
	     try (Connection conn = DatabaseConnection.getConnection();
	          PreparedStatement stmt = conn.prepareStatement(sql);
	          ResultSet rs = stmt.executeQuery()) {
	
	         int synced = 0;
	         while (rs.next()) {
	             int ticketId = rs.getInt("support_ticket_id");
	             String summary = String.format("[%d] %s", ticketId, rs.getString("subject"));
	             String descriptionText = String.format("""
	                 **Cliente:** %s %s
	                 **Email:** %s
	                 **Prioridad:** %s
	                 **Fecha de creación:** %s
	                 
	                 **Descripción original:**
	                 %s
	                 """,
	                 rs.getString("first_name"),
	                 rs.getString("last_name"),
	                 rs.getString("email"),
	                 rs.getString("priority"),
	                 rs.getTimestamp("created_at"),
	                 rs.getString("description"));
	
	             JSONObject adf = createAdfDocument(descriptionText);
	             String jiraPriority = mapPriority(rs.getString("priority"));
	
	             // ✅ Crear issue en Jira
	             String issueKey = issueCreator.createIssueWithDescriptionAndCustomField(
	                 summary,
	                 adf,
	                 String.valueOf(ticketId),
	                 jiraPriority
	             );
	
	             if (issueKey != null) {
	                 // ✅ Actualizar MySQL con el jira_issue_key
	                 updateJiraIssueKey(conn, ticketId, issueKey);
	                 System.out.printf("✅ Sincronizado: Soporte #%d → %s%n", ticketId, issueKey);
	                 synced++;
	             }
	         }
	         System.out.println("✅ Se sincronizaron %d tickets abiertos.".formatted(synced));
	
	     } catch (Exception e) {
	         System.err.println("❌ Error al sincronizar tickets: " + e.getMessage());
	         e.printStackTrace();
	     }
	 }
	
	 // ✅ Actualiza el campo jira_issue_key en MySQL
	 private void updateJiraIssueKey(Connection conn, int supportTicketId, String jiraIssueKey) {
	     String sql = "UPDATE SupportTickets SET jira_issue_key = ? WHERE support_ticket_id = ?";
	     try (PreparedStatement stmt = conn.prepareStatement(sql)) {
	         stmt.setString(1, jiraIssueKey);
	         stmt.setInt(2, supportTicketId);
	         int rows = stmt.executeUpdate();
	         if (rows == 0) {
	             System.err.println("❌ No se encontró el ticket con ID: " + supportTicketId);
	         }
	     } catch (SQLException e) {
	         System.err.println("❌ Error al actualizar jira_issue_key: " + e.getMessage());
	         e.printStackTrace();
	     }
	 }
	
    // Crea un JSON en formato ADF
    private JSONObject createAdfDocument(String text) {
        JSONObject doc = new JSONObject();
        doc.put("version", 1);
        doc.put("type", "doc");

        JSONArray content = new JSONArray();

        JSONObject paragraph = new JSONObject();
        paragraph.put("type", "paragraph");

        JSONArray textContent = new JSONArray();
        JSONObject textNode = new JSONObject();
        textNode.put("type", "text");
        textNode.put("text", text);
        textContent.put(textNode);

        paragraph.put("content", textContent);
        content.put(paragraph);

        doc.put("content", content);
        return doc;
    }

    // Mapea prioridad de MySQL a Jira
    private String mapPriority(String priority) {
        return switch (priority) {
            case "High", "Alta" -> "High";
            case "Low", "Baja" -> "Low";
            case "Medium", "Media" -> "Medium";
            default -> "Medium";
        };
    }

    // 🔍 Verifica si ya fue sincronizado (ej: con una tabla de control)
    private boolean isAlreadySynced(int supportTicketId) {
        String sql = "SELECT 1 FROM SyncedTickets WHERE support_ticket_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supportTicketId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al verificar sincronización: " + e.getMessage());
            return false; // Continuar por seguridad
        }
    }

    // ✅ Marca como sincronizado
    private void markAsSynced(int supportTicketId) {
        String sql = "INSERT IGNORE INTO SyncedTickets (support_ticket_id, jira_issue_key, synced_at) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supportTicketId);
            stmt.setString(2, "PENDIENTE"); // Puedes mejorar con el key real
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error al marcar como sincronizado: " + e.getMessage());
        }
    }
}