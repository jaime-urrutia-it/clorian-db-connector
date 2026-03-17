package com.clorian.db.util; //Clases de apoyo reutilizables, separadas para mantener limpieza.

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil { //Lectura de archivos SQL

    public static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public static List<String> listFiles(String dirPath) {
        try {
            return Files.list(Paths.get(dirPath))
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error listando archivos: " + e.getMessage());
            return List.of();
        }
    }
}