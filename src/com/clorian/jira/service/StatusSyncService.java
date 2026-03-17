// archivo: src/main/java/com/clorian/jira/service/StatusSyncService.java

package com.clorian.jira.service;

import com.clorian.db.DatabaseConnection;
import com.clorian.jira.client.JiraApiClient;

import java.sql.*;

public class StatusSyncService {

    private final JiraApiClient jiraClient;

    public StatusSyncService(JiraApiClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    /**
     * Revisa periódicamente si hay cambios de estado y los sincroniza con Jira.
     */
    public void pollAndSync() {
        String sql = """
            SELECT support_ticket_id, status, jira_issue_key
            FROM SupportTickets
            WHERE jira_issue_key IS NOT NULL
              AND (
                (last_sync_status IS NULL AND status IS NOT NULL) OR
                (last_sync_status IS NOT NULL AND status IS NULL) OR
                (last_sync_status <> status)
              )
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            boolean hasChanges = false;

            while (rs.next()) {
                int ticketId = rs.getInt("support_ticket_id");
                String jiraKey = rs.getString("jira_issue_key");
                String status = rs.getString("status");

                System.out.printf("🔍 Procesando ticket #%d (%s): estado='%s'%n", ticketId, jiraKey, status);

                String transitionId = getTransitionIdForStatus(status);

                if (transitionId == null) {
                    System.err.printf("⚠️  Estado no mapeado para %s: '%s'%n", jiraKey, status);
                    continue;
                }

                System.out.printf("🔄 Actualizando estado de %s a '%s' (transición %s)%n", jiraKey, status, transitionId);
                boolean success = jiraClient.transitionIssue(jiraKey, transitionId);

                if (success) {
                    markAsSynced(conn, ticketId, status);
                    System.out.printf("✅ Estado de Soporte #%d (%s) sincronizado a Jira%n", ticketId, jiraKey);
                    hasChanges = true;
                } else {
                    System.err.printf("❌ Fallo al sincronizar estado de %s%n", jiraKey);
                }
            }

            if (!hasChanges) {
                System.out.println("🔍 No hay cambios de estado para sincronizar.");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al sincronizar estados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtiene el ID de transición en Jira basado en el estado de MySQL.
     */
    private String getTransitionIdForStatus(String status) {
        if (status == null) return null;

        return switch (status.trim().toLowerCase()) {
            case "open", "to do", "por hacer" -> "11";
            case "in progress", "en curso" -> "21";
            case "resolved", "resuelta" -> "41";
            case "waiting for customer", "esperando por el cliente" -> "31";
            default -> {
                System.err.printf("❌ Estado no reconocido: '%s'%n", status);
                yield null;
            }
        };
    }

    /**
     * Marca el ticket como sincronizado con el estado actual.
     */
    private void markAsSynced(Connection conn, int ticketId, String status) {
        String sql = "UPDATE SupportTickets SET last_sync_status = ? WHERE support_ticket_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, ticketId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                System.err.println("❌ No se encontró el ticket con ID: " + ticketId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar last_sync_status: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Sincronización manual para pruebas, depuración o después de cambios masivos
    public void syncNow() {
        System.out.println("🔄 Sincronización manual iniciada...");
        pollAndSync();
        System.out.println("✅ Sincronización manual completada.");
    }
}