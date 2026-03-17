// archivo: src/main/java/com/clorian/db/MainTest.java

package com.clorian.db;

import com.clorian.db.automation.QueryAutomationService;
import com.clorian.db.automation.ScriptLoader;
import com.clorian.db.model.QueryScript;
import com.clorian.jira.client.JiraApiClient;
import com.clorian.jira.service.IssueCreator;
import com.clorian.jira.service.StatusSyncService;
import com.clorian.jira.service.SupportTicketSyncService;

import java.util.List;
import java.util.Scanner;

public class MainTest {
    // 🔐 Credenciales Jira
    private static final String JIRA_URL = "https://yurrutiavila.atlassian.net";
    private static final String JIRA_EMAIL = "yurrutiavila@gmail.com";
    private static final String JIRA_API_TOKEN = "TU_TOKEN_AQUI";

    // 🎯 Proyecto y tipo de issue en Jira
    private static final String JIRA_PROJECT_KEY = "KAN";
    private static final String JIRA_ISSUE_TYPE_ID = "10004";

    public static void main(String[] args) {
        System.out.println("🚀 Iniciando sistema ClorianDBConnector... (Modo Producción)");

        // 1. ✅ Conexión a MySQL
        System.out.println("🔍 Probando conexión a MySQL...");
        if (!DatabaseConnection.isConnectionValid()) {
            System.err.println("❌ Fallo en conexión a MySQL. Abortando.");
            return;
        }
        System.out.println("✅ MySQL: Conexión exitosa.");

        // 2. ✅ Conexión a Jira
        System.out.println("☁️  Conectando a Jira Cloud...");
        JiraApiClient jiraClient = new JiraApiClient(JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN);
        if (!jiraClient.testConnection()) {
            System.err.println("❌ Fallo en conexión a Jira. Abortando.");
            return;
        }

        // 3. ✅ Crear servicio de creación de issues
        IssueCreator issueCreator = new IssueCreator(jiraClient, JIRA_PROJECT_KEY, JIRA_ISSUE_TYPE_ID);

        // 4. ✅ Sincronizar tickets de soporte pendientes
        System.out.println("🔄 Sincronizando nuevos tickets de soporte a Jira...");
        SupportTicketSyncService syncService = new SupportTicketSyncService(issueCreator, "customfield_10058");
        syncService.syncOpenTickets();

        // 5. ✅ Iniciar monitoreo continuo de estados (Jira → MySQL)
        System.out.println("🔄 Iniciando monitoreo continuo de estados (cada 30 segundos)...");

        StatusSyncService statusSyncService = new StatusSyncService(jiraClient);

        Runnable pollingTask = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    statusSyncService.pollAndSync();
                    Thread.sleep(30_000); // Espera 30 segundos
                } catch (InterruptedException e) {
                    System.out.println("🛑 Monitoreo detenido.");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("❌ Error en el monitoreo: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        Thread pollingThread = new Thread(pollingTask, "Polling-Thread");
        pollingThread.setDaemon(false);
        pollingThread.start();

        // ✅ Menú interactivo en el hilo principal
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== MENÚ DE SOPORTE ===");
            System.out.println("1. Sincronizar ahora (manual)");
            System.out.println("2. Salir");
            System.out.print("Elige una opción: ");

            try {
                int choice = scanner.nextInt();
                switch (choice) {
                    case 1 -> {
                        System.out.println("🔄 Iniciando sincronización manual...");
                        statusSyncService.pollAndSync();
                        System.out.println("✅ Sincronización manual completada.");
                    }
                    case 2 -> {
                        System.out.println("🛑 Cerrando sistema...");
                        System.exit(0);
                    }
                    default -> System.out.println("❌ Opción no válida");
                }
            } catch (Exception e) {
                System.err.println("❌ Entrada inválida. Por favor, introduce un número.");
                scanner.nextLine(); // Limpia el buffer
            }
        }
        // No se llega aquí, el programa termina con System.exit(0)
    }
}