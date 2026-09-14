# Clorian DB Connector

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.java.com)
[![Jira](https://img.shields.io/badge/Jira-Cloud%2FServer-0052CC.svg)](https://www.atlassian.com/software/jira)
[![MySQL](https://img.shields.io/badge/MySQL-5.7+-4479A1.svg)](https://www.mysql.com)

**Modulo de integracion Java para orquestacion de datos entre MySQL y Jira Cloud**. Este proyecto funciona como **componente emisor** en una arquitectura de sincronizacion bidireccional, permitiendo tanto operacion standalone como integracion en tiempo real con su complemento [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver).

> **Parte de un Ecosistema**: Este es el **EMISOR** (MySQL → Jira). Para sincronizacion **bidireccional en tiempo real**, desplegalo junto con el **RECEPTOR** ([Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver)).

> ⚠️ **Estado Actual (Agosto 2026):**  
> Este proyecto es un MVP funcional para demostracion tecnica.
> - La integracion con Jira requiere token valido (renovado periodicamente).
> - Para ver el codigo real, consulta los archivos .java en src/.

---

## Proposito y Arquitectura

### Contexto de negocio

Este proyecto demuestra como la automatizacion de flujos operativos entre sistemas desconectados (una base de datos de operaciones y una plataforma de gestion de proyectos) reduce la latencia de respuesta, elimina errores manuales y proporciona trazabilidad completa. Es aplicable a entornos de SSC, Business Operations y gestion de servicios compartidos donde la coordinacion entre equipos operativos y tecnicos es critica.

### Modo Standalone (Unidireccional)

Opera de forma independiente realizando **polling periodico** (cada 30s) para:

- Detectar nuevos tickets de soporte en MySQL y crearlos automaticamente en Jira
- Sincronizar estados de Jira hacia MySQL mediante consulta periodica a la API REST

### Modo Integrado (Bidireccional - Recomendado)

En conjunto con **Jira Webhook Receiver**, forma un sistema de sincronizacion completo:

- **Este proyecto (Emisor)**: Envía tickets nuevos de MySQL a Jira + Polling de estado cada 30s
- **Webhook Receiver (Receptor)**: Recibe actualizaciones instantaneas de Jira vía HTTP webhooks

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ARQUITECTURA COMPLETA                            │
└─────────────────────────────────────────────────────────────────────┘

  ┌─────────────────┐         Opcion A: Polling (30s)          ┌──────┐
  │   JIRA CLOUD    │ ◄─────────────────────────────────────── │      │
  │                 │                                          │      │
  │  • Issues       │         Opcion B: Webhook (Tiempo real)  │      │
  │  • Workflows    │ ───────────────────────────────────────► │      │
  │  • Updates      │         HTTP POST /api/jira-webhook      │      │
  └─────────────────┘                                          │      │
           ▲                                                   │      │
           │                                                   │      │
    ┌──────┴──────┐                                            │      │
    │   REST API  │                                            │      │
    │   (v3)      │                                            │      │
    └──────┬──────┘                                            │      │
           │                                                    │      │
           ▼                                                    │      │
  ┌──────────────────┐                               ┌─────────┴──────┴───┐
  │  CLORIAN DB      │  • Emisor de tickets         │  JIRA WEBHOOK      │
  │   CONNECTOR      │  • Polling cada 30s          │    RECEIVER        │
  │   (Este proyecto)│  • Creacion de issues        │  (Proyecto         │
  │                  │  • Sync MySQL → Jira         │   complementario)  │
  └────────┬─────────┘                               └────────────────────┘
           │                                                    ▲
           │ JDBC                                               │
           ▼                                                    │
  ┌──────────────────┐                                          │
  │   MYSQL SERVER   │                                          │
  │  (clorian_db)    │ ◄────────────────────────────────────────┘
  │                  │         Actualización de estados
  │ • SupportTickets │         (Tiempo real vía webhook)
  │ • Customers      │
  │ • Sync status    │
  └──────────────────┘
```

---

## Caracteristicas Principales

### Modulo de Base de Datos (`com.clorian.db`)

- **Conexion JDBC Robusta**: Gestion thread-safe de conexiones MySQL con validacion de estado
- **Ejecucion de Scripts Dinamicos**: Carga y ejecucion de archivos `.sql` externos con clasificacion automatica de criticidad
- **Seguridad SQL**: Uso de `PreparedStatement` para prevenir inyeccion SQL
- **Procesamiento de Resultados**: Manejo tipado de ResultSets con exportacion a consola y archivos TXT
- **Orquestacion**: Servicio `QueryAutomationService` que gestiona flujos de trabajo con manejo de dependencias

### Modulo de Integracion Jira (`com.clorian.jira`)

- **API REST V3**: Cliente HTTP nativo (Java 11+) para Jira Cloud con autenticacion Basic Auth
- **Creacion de Issues**: Generacion automatica de tickets con formato ADF (Atlassian Document Format), campos personalizados y mapeo de prioridades
- **Sincronizacion por Polling**:
    - `SupportTicketSyncService`: Detecta tickets `status='Open'` sin `jira_issue_key` y los crea en Jira
    - `StatusSyncService`: Sincroniza estados cada 30s mediante consulta a API de Jira
- **Gestion de Estados**: Mapeo bidireccional (ver [tabla unificada del ecosistema](https://github.com/jaime-urrutia-it/clorian-ecosystem#mapeo-de-estados-referencia-unica))

---

## Stack Tecnologico

| Tecnologia | Version | Proposito |
|---|---|---|
| **Lenguaje** | Java 17+ (usa text blocks, switch expressions y java.net.http.HttpClient) | Logica de negocio |
| **Base de Datos** | MySQL 5.7+ | Persistencia local |
| **Driver JDBC** | MySQL Connector/J 8.x | Conectividad BD |
| **API Externa** | Jira REST API v3 | Integracion con Jira Cloud/Server |
| **JSON** | org.json 20231013 | Parseo ligero de payloads REST |
| **HTTP Client** | `java.net.http.HttpClient` | Comunicacion con Jira |
| **Build** | javac (compilacion manual) | Empaquetado |

---

## Estructura del Proyecto

> ℹ️ **Nota sobre dependencias en `lib/`:** este proyecto usa compilación manual con `javac` (no Maven/Gradle), por lo que las dependencias externas necesarias para compilar se versionan directamente en `lib/`. El archivo `json-20231013.jar` (74 KB, org.json) es requerido para el parseo de payloads REST. La regla `*.jar` en `.gitignore` previene la adición futura de artefactos de build o JARs innecesarios.

```
clorian-db-connector/
├── lib/
│   └── json-20231013.jar
├── src/
│   ├── module-info.java
│   └── com/clorian/
│       ├── db/                        # CAPA DE PERSISTENCIA
│       │   ├── DatabaseConnection.java
│       │   ├── DatabaseQueries.java
│       │   ├── MainTest.java           # PUNTO DE ENTRADA
│       │   ├── automation/
│       │   │   ├── QueryAutomationService.java
│       │   │   ├── QueryExecutor.java
│       │   │   ├── QueryResultHandler.java
│       │   │   └── ScriptLoader.java
│       │   ├── model/
│       │   │   ├── QueryResult.java
│       │   │   └── QueryScript.java
│       │   └── util/
│       │       ├── FileUtil.java
│       │       └── SQLParser.java
│       └── jira/                      # CAPA DE INTEGRACION
│           ├── client/
│           │   └── JiraApiClient.java
│           └── service/
│               ├── IssueCreator.java
│               ├── StatusSyncService.java
│               └── SupportTicketSyncService.java
└── README.md
```

---

## Instalacion y Configuracion

### 1. Requisitos previos

- Java JDK 17 o superior (requerido: usa text blocks, switch expressions y HttpClient)
- MySQL Server 5.7+ con esquema `clorian_db`
- Cuenta en Jira Cloud con token de API generado
- (Opcional) [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) para modo bidireccional

### 2. Esquema de Base de Datos

```sql
CREATE TABLE SupportTickets (
    support_ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    priority ENUM('High', 'Medium', 'Low') DEFAULT 'Medium',
    status ENUM('Open', 'In Progress', 'Waiting for Customer', 'Resolved', 'Closed') DEFAULT 'Open',
    jira_issue_key VARCHAR(50) UNIQUE,
    last_sync_status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES Customers(customer_id)
);

CREATE TABLE Customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255)
);

CREATE INDEX idx_jira_key ON SupportTickets(jira_issue_key);
CREATE INDEX idx_status_sync ON SupportTickets(status, last_sync_status);
CREATE INDEX idx_open_tickets ON SupportTickets(status);
```

> **Nota**: El DDL completo del ecosistema (incluyendo la tabla de mapeo de estados unificada) esta disponible en el [README del ecosistema](https://github.com/jaime-urrutia-it/clorian-ecosystem).

### 3. Configuracion de Credenciales

Edita `src/com/clorian/db/MainTest.java`:

```java
private static final String JIRA_URL = "https://tu-dominio.atlassian.net";
private static final String JIRA_EMAIL = "tu-email@ejemplo.com";
private static final String JIRA_API_TOKEN = "tu-token-de-api";
private static final String JIRA_PROJECT_KEY = "KAN";
private static final String JIRA_ISSUE_TYPE_ID = "10004";
```

**En produccion**: Externaliza estas credenciales a variables de entorno o archivos de propiedades externas.

### 4. Compilacion y Ejecucion

```bash
# Descargar mysql-connector-java-8.x.jar y colocarlo en lib/
javac -cp "lib/*:." -d out src/module-info.java $(find src -name "*.java")
java -cp "lib/*:out" com.clorian.db.MainTest
```

---

## Modos de Operacion

### Modo Standalone

1. Validacion de conexiones (MySQL y Jira)
2. Sincronizacion inicial: `SupportTicketSyncService.syncOpenTickets()`
3. Monitoreo continuo: `StatusSyncService` cada 30s en hilo separado
4. Menu interactivo para sincronizacion manual

### Modo Integrado (Recomendado)

Combina este proyecto con [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) para sincronizacion en tiempo real. En modo integrado, el polling de estados de este proyecto se puede desactivar o reducir, dejando la sincronizacion Jira → MySQL al webhook receptor.

---

## Seguridad

Para el estado actual y mejoras recomendadas, consultar la [seccion de seguridad del ecosistema](https://github.com/jaime-urrutia-it/clorian-ecosystem#seguridad).

**Mejoras especificas de este componente:**
- [ ] Externalizacion completa de credenciales (variables de entorno / vault)
- [ ] Validacion de respuestas de la API de Jira
- [ ] Configuracion de timeout y reintentos en HttpClient

---

## ⚠️ Limitaciones Conocidas del MVP (Agosto 2026)

Este proyecto es un MVP de demostración, no un sistema de producción. Las siguientes limitaciones están documentadas intencionalmente como parte del roadmap de maduración:

| Limitación | Impacto | Plan de mitigación |
|---|---|---|
| Endpoint webhook sin autenticación HMAC | Cualquiera podría enviar payloads falsos | Implementar validación HMAC-SHA256 (ver Roadmap) |
| Procesamiento asíncrono con `new Thread()` sin pool | Riesgo bajo carga alta | Migrar a `ExecutorService` con pool controlado |
| UPSERT recursivo en receptor | Posible `StackOverflowError` bajo condiciones extremas | Refactorizar a bucle iterativo |
| Polling cada 30s en modo standalone | Carga innecesaria sobre API de Jira | Aumentar intervalo o migrar a webhook-only |
| Logging por consola (`System.out`) | Sin rotación ni niveles | Migrar a SLF4J + Logback |

**Nota sobre el alcance:** Estas limitaciones están documentadas porque un entorno SSC/Business Operations valora tanto el control de un sistema como la honestidad sobre su estado. La decisión de abordarlas (o aceptarlas como riesgo controlado en un entorno de bajo volumen) corresponde al equipo de operaciones que adopte el proyecto.

---

## Roadmap

### Pista de Negocio
- [ ] Modulo de conciliacion O2C
- [ ] Dashboard de KPIs de servicio
- [ ] Reportes operativos exportables
- [ ] Integracion con ERPs

### Pista Tecnica
- [ ] Migracion a Spring Boot
- [ ] Dockerizacion oficial
- [ ] Externalizacion de credenciales
- [ ] Logging profesional (SLF4J + Logback)
- [ ] Soporte para PostgreSQL
- [ ] API REST propia para gestion de sincronizacion

---

## Licencia y Autoria

Desarrollado por **Jaime Urrutia**  
[GitHub](https://github.com/jaime-urrutia-it) | [Portfolio](https://yagourrutia.com) | [LinkedIn](https://www.linkedin.com/in/jaime-yago-urrutia-multilingue/)  

**Version**: 1.0.0 | **Ultima actualizacion**: Agosto 2026
