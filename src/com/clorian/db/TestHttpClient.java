package com.clorian.db;

//TestHttpClient.java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestHttpClient {
 public static void main(String[] args) {
     HttpClient client = HttpClient.newHttpClient();
     System.out.println("✅ HttpClient funcionando correctamente.");
 }
}