package com.clorian.db.automation; 
// Contiene toda la lógica de automatización. Es el núcleo de la nueva funcionalidad.


import com.clorian.db.model.QueryResult;
import com.clorian.db.model.QueryScript;

import java.util.List;

public class QueryAutomationService { // Orquesta la ejecución automática

    public void runScripts(List<QueryScript> scripts) {
        for (QueryScript script : scripts) {
            System.out.println("\n--- Ejecutando script: " + script.getName() + " ---");
            QueryResult result = QueryExecutor.execute(script.getSql());

            if (script.isCritical() && !result.isSuccess()) {
                System.err.println("🚨 Script crítico fallido. Deteniendo ejecución.");
                QueryResultHandler.logToConsole(result);
                break;
            } else {
                QueryResultHandler.logToConsole(result);
            }
        }
    }
}