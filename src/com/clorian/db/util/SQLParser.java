package com.clorian.db.util; //Clases de apoyo reutilizables, separadas para mantener limpieza.

// Utilidades para parsear SQL
// Por ahora, puedes usarlo para futuras funcionalidades
// Ej: extraer tablas de un SQL, validar sintaxis básica, etc.
public class SQLParser {
 // Ejemplo futuro: detectar si es SELECT, INSERT, etc.
 public static boolean isSelect(String sql) {
     return sql.trim().toUpperCase().startsWith("SELECT");
 }

 public static boolean isUpdateOrDelete(String sql) {
     String upper = sql.trim().toUpperCase();
     return upper.startsWith("UPDATE") || upper.startsWith("DELETE");
 }
}