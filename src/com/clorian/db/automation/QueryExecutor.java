package com.clorian.db.automation; 
//Contiene toda la lógica de automatización. Es el núcleo de la nueva funcionalidad.

import com.clorian.db.DatabaseConnection;
import com.clorian.db.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutor { // Esta clase ejecuta consultas genéricas con seguridad (PreparedStatement) y manejo de recursos.
  
    
    
 // ✅ Método para ejecutar cualquier SQL (SELECT, INSERT, CREATE, etc.)
    public static QueryResult execute(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            // Usa execute() → maneja cualquier tipo de sentencia
            boolean hasResultSet = stmt.execute();

            if (hasResultSet) {
                // Es un SELECT
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    List<String[]> rows = new ArrayList<>();

                    // Encabezados
                    String[] headers = new String[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        headers[i - 1] = meta.getColumnName(i);
                    }
                    rows.add(headers);

                    // Filas
                    while (rs.next()) {
                        String[] row = new String[columnCount];
                        for (int i = 1; i <= columnCount; i++) {
                            Object value = rs.getObject(i);
                            row[i - 1] = (value != null) ? value.toString() : "NULL";
                        }
                        rows.add(row);
                    }
                    return new QueryResult(true, "Consulta ejecutada con éxito", rows, null);
                }
            } else {
                // No hay ResultSet → es INSERT, UPDATE, DELETE, CREATE, etc.
                int rowsAffected = stmt.getUpdateCount();
                return new QueryResult(true, "Operación exitosa. Filas afectadas: " + rowsAffected, null, null);
            }

        } catch (SQLException e) {
            return new QueryResult(false, "Error SQL: " + e.getMessage(), null, e);
        }
    }
    // ✅ Método estático para establecer parámetros
    private static void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}