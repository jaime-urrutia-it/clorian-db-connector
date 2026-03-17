package com.clorian.db.automation; //Contiene toda la lógica de automatización. Es el núcleo de la nueva funcionalidad.

import com.clorian.db.model.QueryScript;
import com.clorian.db.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class ScriptLoader { // Carga y parsea scripts SQL

    public static List<QueryScript> loadScriptsFromDir(String dirPath) {
        List<QueryScript> scripts = new ArrayList<>();
        try {
            List<String> files = FileUtil.listFiles(dirPath);
            for (String file : files) {
                if (file.endsWith(".sql")) {
                    String fullPath = dirPath + "/" + file;
                    String sql = FileUtil.readFile(fullPath);
                    String name = file.replace(".sql", "");
                    boolean critical = name.contains("critical") || name.contains("refund") || name.contains("payment");
                    scripts.add(new QueryScript(name, sql, critical));
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando scripts: " + e.getMessage());
        }
        return scripts;
    }
}