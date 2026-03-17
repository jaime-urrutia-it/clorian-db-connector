package com.clorian.db.model; //Modelos de datos usados en la automatización (ideal si usas POJOs).



public class QueryScript { //Representa un script SQL con metadatos
    private String name;
    private String sql;
    private boolean critical;

    public QueryScript(String name, String sql, boolean critical) {
        this.name = name;
        this.sql = sql;
        this.critical = critical;
    }

    // Getters
    public String getName() { return name; }
    public String getSql() { return sql; }
    public boolean isCritical() { return critical; }
    
    static {
        System.out.println("✅ Clase QueryScript cargada correctamente");
    }
}