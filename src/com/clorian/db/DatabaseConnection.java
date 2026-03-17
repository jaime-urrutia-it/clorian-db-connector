// archivo: src/com/clorian/db/DatabaseConnection.java

package com.clorian.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase responsable de gestionar la conexión a la base de datos MySQL.
 * Utiliza JDBC con el driver de MySQL Connector/J.
 * 
 * Configuración actual:
 *   - URL: jdbc:mysql://localhost:3306/clorian_db
 *   - Usuario: root
 *   - Contraseña: vacía
 *   - Driver: com.mysql.cj.jdbc.Driver
 * 
 * Esta clase es thread-safe gracias al uso estático y sincronizado del driver.
 */

public class DatabaseConnection {

    // 🔧 Configuración de conexión
    private static final String URL = "jdbc:mysql://localhost:3306/clorian_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    // ✅ Bloque estático para cargar el driver JDBC
    static {
        try {
            Class.forName(DRIVER_CLASS);
            System.out.println("[INFO] Driver JDBC cargado correctamente: " + DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                """
                [ERROR] No se encontró el driver JDBC: %s
                Asegúrate de tener mysql-connector-java en el classpath.
                Descárgalo desde: https://dev.mysql.com/downloads/connector/j/
                """.formatted(DRIVER_CLASS), e);
        }
    }

    /**
     * Obtiene una nueva conexión a la base de datos.
     *
     * @return Connection objeto de conexión a MySQL
     * @throws SQLException si ocurre un error al conectar
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[INFO] Conexión a la base de datos establecida.");
            return connection;
        } catch (SQLException e) {
            System.err.println("[ERROR] Fallo al conectar con la base de datos: " + e.getMessage());
            throw new SQLException("No se pudo conectar a la base de datos.", e);
        }
    }

    // 🛠️ Método opcional futuro: validar conexión
    public static boolean isConnectionValid() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("[VALIDATION] Conexión no válida: " + e.getMessage());
            return false;
        }
    }
}
