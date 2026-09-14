# Clorian DB Connector
![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Jira](https://img.shields.io/badge/Jira-Cloud%2FServer-0052CC.svg)
![MySQL](https://img.shields.io/badge/MySQL-5.7+-4479A1.svg)

Módulo de integración Java para orquestación de datos entre MySQL y Jira Cloud. Este proyecto funciona como componente **emisor** en una arquitectura de sincronización bidireccional, permitiendo tanto operación standalone como integración en tiempo real con su complemento [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver).

**Parte de un Ecosistema:** Este es el EMISOR (MySQL → Jira). Para sincronización bidireccional en tiempo real, despliégalo junto con el RECEPTOR ([Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver)).

⚠️ **Estado Actual (Agosto 2026):**
- Este proyecto es un MVP funcional para demostración técnica.
- La integración con Jira requiere token válido (renovado periódicamente).
- Para ver el código real, consulta los archivos `.java` en `src/`.

## Propósito y Arquitectura

### Contexto de Negocio
Este proyecto demuestra cómo la automatización de flujos operativos entre sistemas desconectados (una base de datos de operaciones y una plataforma de gestión de proyectos) reduce la latencia de respuesta, elimina errores manuales y proporciona trazabilidad completa. Es aplicable a entornos de **SSC, Business Operations y gestión de servicios compartidos** donde la coordinación entre equipos operativos y técnicos es crítica.

### Modo Standalone (Unidireccional)
Opera de forma independiente realizando polling periódico (cada 30s) para:
- Detectar nuevos tickets de soporte en MySQL y crearlos automáticamente en Jira.
- Sincronizar estados de Jira hacia MySQL mediante consulta periódica a la API REST.

### Modo Integrado (Bidireccional - Recomendado)
En conjunto con Jira Webhook Receiver, forma un sistema de sincronización completo:
- **Este proyecto (Emisor):** Envía tickets nuevos de MySQL a Jira + Polling de estado cada 30s.
- **Webhook Receiver (Receptor):** Recibe actualizaciones instantáneas de Jira vía HTTP webhooks.

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    ARQUITECTURA COMPLETA                            │
└─────────────────────────────────────────────────────────────────────┘
  ┌─────────────────┐         Opción A: Polling (30s)          ┌──────┐
  │   JIRA CLOUD    │ ◄─────────────────────────────────────── │      │
  │                 │                                          │      │
  │  • Issues       │         Opción B: Webhook (Tiempo real)  │      │
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
  │   (Este proyecto)│  • Creación de issues        │  (Proyecto         │
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

## Características Principales

### Módulo de Base de Datos (`com.clorian.db`)
- **Conexión JDBC Robusta:** Gestión thread-safe de conexiones MySQL con validación de estado.
- **Ejecución de Scripts Dinámicos:** Carga y ejecución de archivos `.sql` externos con clasificación automática de criticidad.
- **Seguridad SQL:** Uso de `PreparedStatement` para prevenir inyección SQL.
- **Procesamiento de Resultados:** Manejo tipado de `ResultSet` con exportación a consola y archivos TXT.
- **Orquestación:** Servicio `QueryAutomationService` que gestiona flujos de trabajo con manejo de dependencias.

### Módulo de Integración Jira (`com.clorian.jira`)
- **API REST V3:** Cliente HTTP nativo (Java 11+) para Jira Cloud con autenticación Basic Auth.
- **Creación de Issues:** Generación automática de tickets con formato ADF (Atlassian Document Format), campos personalizados y mapeo de prioridades.
- **Sincronización por Polling:**
  - `SupportTicketSyncService`: Detecta tickets `status='Open'` sin `jira_issue_key` y los crea en Jira.
  - `StatusSyncService`: Sincroniza estados cada 30s mediante consulta a API de Jira.
- **Gestión de Estados:** Mapeo bidireccional (ver [tabla unificada del ecosistema](https://github.com/jaime-urrutia-it/clorian-ecosystem#mapeo-de-estados-referencia-unica)).

## Stack Tecnológico

| Tecnología | Versión | Propósito |
|---|---|---|
| Lenguaje | Java 17+ (usa text blocks, switch expressions y `java.net.http.HttpClient`) | Lógica de negocio |
| Base de Datos | MySQL 5.7+ | Persistencia local |
| Driver JDBC | MySQL Connector/J 8.x | Conectividad BD |
| API Externa | Jira REST API v3 | Integración con Jira Cloud/Server |
| JSON | org.json 20231013 | Parseo ligero de payloads REST |
| HTTP Client | `java.net.http.HttpClient` | Comunicación con Jira |
| Build | `javac` (compilación manual) | Empaquetado |

## Estructura del Proyecto

> ℹ️ **Nota sobre dependencias en `lib/`:** este proyecto usa compilación manual con `javac` (no Maven/Gradle), por lo que las dependencias externas necesarias para compilar se versionan directamente en `lib/`. El archivo `json-20231013.jar` (74 KB, org.json) es requerido para el parseo de payloads REST. La regla `*.jar` en `.gitignore` previene la adición futura de artefactos de build o JARs innecesarios.

```text
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
 │       │       └── FileUtil.java
 │       └── jira/                      # CAPA DE INTEGRACIÓN
 │           ├── client/
 │           │   └── JiraApiClient.java
 │           └── service/
 │               ├── IssueCreator.java
 │               ├── StatusSyncService.java
 │               └── SupportTicketSyncService.java
 └── README.md
```

## Instalación y Configuración

### 1. Requisitos previos
- Java JDK 17 o superior (requerido: usa text blocks, switch expressions y HttpClient).
- MySQL Server 5.7+ con esquema `clorian_db`.
- Cuenta en Jira Cloud con token de API generado.
- (Opcional) [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) para modo bidireccional.

### 2. Esquema de Base de Datos
```sql
CREATE TABLE Customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255)
);

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

CREATE INDEX idx_jira_key ON SupportTickets(jira_issue_key);
CREATE INDEX idx_status_sync ON SupportTickets(status, last_sync_status);
```
*Nota: `jira_issue_key` usa `VARCHAR(50)` para acomodar claves de Jira con prefijos de proyecto largos, y el `ENUM` incluye `'Closed'` para alineación total con el receptor.*

### 3. Configuración de Credenciales
Edita `src/com/clorian/db/MainTest.java` (o usa variables de entorno en tu sistema):
```java
private static final String JIRA_URL = System.getenv().getOrDefault("JIRA_URL", "https://tu-dominio.atlassian.net");
private static final String JIRA_EMAIL = System.getenv().getOrDefault("JIRA_EMAIL", "tu-email@ejemplo.com");
private static final String JIRA_API_TOKEN = System.getenv().getOrDefault("JIRA_API_TOKEN", "tu-token-de-api");
private static final String JIRA_PROJECT_KEY = "KAN";
private static final String JIRA_ISSUE_TYPE_ID = "10004";
```
*En producción: Externaliza estas credenciales a variables de entorno o archivos de propiedades externas.*

### 4. Compilación y Ejecución
```bash
# Asegúrate de tener mysql-connector-java-8.x.jar en lib/ (descárgalo si no está)
javac -cp "lib/*:." -d out src/module-info.java $(find src -name "*.java")
java -cp "lib/*:out" com.clorian.db.MainTest
```

## Modos de Operación

### Modo Standalone
- Validación de conexiones (MySQL y Jira).
- Sincronización inicial: `SupportTicketSyncService.syncOpenTickets()`.
- Monitoreo continuo: `StatusSyncService` cada 30s en hilo separado.
- Menú interactivo para sincronización manual.

### Modo Integrado (Recomendado)
Combina este proyecto con [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) para sincronización en tiempo real. En modo integrado, el polling de estados de este proyecto se puede desactivar o reducir, dejando la sincronización Jira → MySQL al webhook receptor.

## Seguridad

Para el estado actual y mejoras recomendadas, consultar la [sección de seguridad del ecosistema](https://github.com/jaime-urrutia-it/clorian-ecosystem#seguridad).

**Mejoras específicas de este componente:**
- [ ] Externalización completa de credenciales (variables de entorno / vault).
- [ ] Validación de respuestas de la API de Jira.
- [ ] Configuración de timeout y reintentos en `HttpClient`.

### ⚠️ Limitaciones Conocidas del MVP (Agosto 2026)
Este proyecto es un MVP de demostración, no un sistema de producción. Las siguientes limitaciones están documentadas intencionalmente como parte del roadmap de maduración:

| Limitación | Impacto | Plan de mitigación |
|---|---|---|
| Polling cada 30s en modo standalone | Carga innecesaria sobre API de Jira | Aumentar intervalo o migrar a webhook-only |
| Logging por consola (`System.out`) en algunos puntos | Sin rotación ni niveles estructurados | Migrar completamente a SLF4J + Logback |
| Compilación manual con `javac` | Requiere gestión manual de dependencias en `lib/` | Migración futura a Maven/Gradle (ver Roadmap) |

**Nota sobre el alcance:** Estas limitaciones están documentadas porque un entorno SSC/Business Operations valora tanto el control de un sistema como la honestidad sobre su estado. La decisión de abordarlas (o aceptarlas como riesgo controlado en un entorno de bajo volumen) corresponde al equipo de operaciones que adopte el proyecto.

## Roadmap

### Pista de Negocio
- [ ] Módulo de conciliación O2C (implementado en el ecosistema Clorian 2.0)
- [ ] Dashboard de KPIs de servicio
- [ ] Reportes operativos exportables
- [ ] Integración con ERPs

### Pista Técnica
- [ ] Migración a Spring Boot
- [ ] Dockerización oficial
- [ ] Logging profesional (SLF4J + Logback)
- [ ] Soporte para PostgreSQL
- [ ] API REST propia para gestión de sincronización

## Licencia y Autoría

Desarrollado por Jaime Urrutia  
[GitHub](https://github.com/jaime-urrutia-it) | [Portfolio](https://yagourrutia.com) | [LinkedIn](https://www.linkedin.com/in/jaime-urrutia-multilingue/?locale=es-ES)

**Versión:** 1.0.0 | **Última actualización:** Agosto 2026
```
