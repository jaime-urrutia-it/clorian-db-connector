package com.clorian.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseQueries {

    public static void selectAllFromTable(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            System.out.println("\nDatos de la tabla '" + tableName + "':");
            while (resultSet.next()) {
                // Ejemplo genérico (ajusta según tus columnas)
                int id = resultSet.getInt("id"); // Cambia "id" por tu columna real
                String nombre = resultSet.getString("nombre"); // Ejemplo
                System.out.println("ID: " + id + ", Nombre: " + nombre);
            }
            
        } catch (SQLException e) {
            System.err.println("Error en la consulta SELECT:");
            e.printStackTrace();
        }
    }
}