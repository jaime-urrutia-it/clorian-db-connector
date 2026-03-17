package com.clorian.db.automation;
//Contiene toda la lógica de automatización. Es el núcleo de la nueva funcionalidad.

import com.clorian.db.model.QueryResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QueryResultHandler {  //Procesa resultados (logs, exportaciones, etc.)

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Imprime resultado en consola
    public static void logToConsole(QueryResult result) {
        if (result.isSuccess()) {
            System.out.println("[INFO] " + result.getMessage());
            if (result.getRows() != null) {
                for (String[] row : result.getRows()) {
                    System.out.println(String.join(" | ", row));
                }
            }
        } else {
            System.err.println("[ERROR] " + result.getMessage());
        }
    }

    // Guarda resultado en archivo de texto
    public static void exportToTxt(QueryResult result, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Resultado de consulta - " + LocalDateTime.now().format(formatter) + "\n");
            writer.write("Mensaje: " + result.getMessage() + "\n\n");

            if (result.getRows() != null) {
                for (String[] row : result.getRows()) {
                    writer.write(String.join(" | ", row) + "\n");
                }
            }
            System.out.println("✅ Resultado exportado a: " + filename);
        } catch (IOException e) {
            System.err.println("Error al guardar archivo: " + e.getMessage());
        }
    }
}