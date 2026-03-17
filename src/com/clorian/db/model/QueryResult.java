package com.clorian.db.model; //Modelos de datos usados en la automatización (ideal si usas POJOs).

import java.util.List;

public class QueryResult { //Representa el resultado de una consulta
    private boolean success;
    private String message;
    private List<String[]> rows;
    private Exception exception;

    public QueryResult(boolean success, String message, List<String[]> rows, Exception exception) {
        this.success = success;
        this.message = message;
        this.rows = rows;
        this.exception = exception;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<String[]> getRows() { return rows; }
    public Exception getException() { return exception; }
}