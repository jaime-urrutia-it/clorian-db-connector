# Clorian DB Connector

## 🎯 Descripción

**Clorian DB Connector** es una plataforma de integración híbrida desarrollada en **Java** que conecta bases de datos **MySQL** con **Jira Cloud**, permitiendo la automatización bidireccional de flujos de trabajo entre sistemas de datos y gestión de incidencias.

Este proyecto actúa como capa de abstracción que no solo facilita la ejecución de operaciones SQL, sino que también orquesta la sincronización inteligente entre tickets de soporte en bases de datos locales y proyectos en Jira, manteniendo estados actualizados en tiempo real.

---

## ✨ Características Principales

### Módulo de Base de Datos (`com.clorian.db`)

- **🔌 Conexión JDBC Robusta**: Gestión thread-safe de conexiones MySQL con validación de estado
- **⚡ Ejecución de Scripts Dinámicos**: Carga y ejecución de archivos `.sql` externos con clasificación automática de criticidad
- **🛡️ Seguridad SQL**: Uso de `PreparedStatement` para prevenir inyección SQL
- **📊 Procesamiento de Resultados**: Manejo tipado de ResultSets con exportación a consola y archivos TXT
- **🎛️ Orquestación**: Servicio de automatización que gestiona flujos de trabajo críticos (detiene ejecución ante fallos en scripts críticos)

### Módulo de Integración Jira (`com.clorian.jira`)

- **☁️ API REST V3**: Cliente HTTP nativo (Java 11+) para Jira Cloud con autenticación Basic Auth
- **📝 Creación de Issues**: Generación automática de tickets con formato ADF (Atlassian Document Format), campos personalizados y prioridades mapeadas
- **🔄 Sincronización Bidireccional**:
    - MySQL → Jira: Tickets de soporte pendientes se convierten en issues automáticamente
    - Jira → MySQL: Actualización de estados mediante polling continuo (30s)
- **🎯 Gestión de Estados**: Mapeo inteligente de estados entre sistemas (Open→Por Hacer, In Progress→En Curso, etc.)

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión/Detalle |
| --- | --- |
| **Lenguaje** | Java 11+ (Compatible con JDK 8+) |
| **Base de Datos** | MySQL (Connector/J 8.x) |
| **APIs** | Jira REST API v3 |
| **Librerías** | `org.json` (JSON 20231013) |
| **HTTP Client** | `java.net.http.HttpClient` (Java 11+) |
| **IDE** | Eclipse IDE / IntelliJ IDEA |
| **Build** | Módulos Java (module-info.java) |

---

## 📂 Estructura del Proyecto

```
clorian-db-connector/
├── 📁 lib/
│   └── json-20231013.jar                 # Dependencia JSON
│
└── 📁 src/
    ├── module-info.java                  # Configuración Java Modules
    └── 📁 com/clorian/
        ├── 📁 db/                        # 🔌 MÓDULO BASE DE DATOS
        │   ├── DatabaseConnection.java   # Gestión de conexiones JDBC
        │   ├── DatabaseQueries.java      # Consultas genéricas
        │   ├── MainTest.java            # 🚀 Punto de entrada
        │   ├── 📁 automation/           # Núcleo de automatización
        │   │   ├── QueryAutomationService.java  # Orquestador
        │   │   ├── QueryExecutor.java           # Ejecutor SQL
        │   │   ├── QueryResultHandler.java      # Manejo de resultados
        │   │   └── ScriptLoader.java            # Carga de scripts
        │   ├── 📁 model/                # Modelos de datos
        │   │   ├── QueryResult.java     # Resultado de consultas
        │   │   └── QueryScript.java     # Metadatos de scripts
        │   └── 📁 util/                 # Utilidades
        │       ├── FileUtil.java        # I/O de archivos
        │       └── SQLParser.java       # Validador SQL
        │
        └── 📁 jira/                      # ☁️ MÓDULO JIRA
            ├── 📁 client/
            │   └── JiraApiClient.java   # Cliente REST
            └── 📁 service/
                ├── IssueCreator.java           # Creación de issues
                ├── StatusSyncService.java      # Sync de estados (polling)
                └── SupportTicketSyncService.java # Sync tickets → Jira
```

---

## 🚀 Instalación y Configuración

### 1. Prerrequisitos

- Java JDK 11 o superior
- MySQL Server 5.7+ con base de datos `clorian_db`
- Cuenta en Jira Cloud con token de API
- MySQL Connector/J en el classpath

### 2. Configuración de Base de Datos

Asegúrate de tener la siguiente tabla en MySQL:

```sql
CREATE TABLE SupportTickets (
    support_ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT,
    subject VARCHAR(255),
    description TEXT,
    priority ENUM('High', 'Medium', 'Low'),
    status ENUM('Open', 'In Progress', 'Waiting for Customer', 'Resolved'),
    jira_issue_key VARCHAR(20),
    last_sync_status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES Customers(customer_id)
);

CREATE TABLE Customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255)
);
```

### 3. Configuración de Credenciales

Edita `src/com/clorian/db/MainTest.java` y actualiza las constantes:

```java
// Configuración MySQL (en DatabaseConnection.java)
private static final String URL = "jdbc:mysql://localhost:3306/clorian_db?useSSL=false&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "tu_password";

// Configuración Jira (en MainTest.java)
private static final String JIRA_URL = "<https://tu-dominio.atlassian.net>";
private static final String JIRA_EMAIL = "tu-email@ejemplo.com";
private static final String JIRA_API_TOKEN = "tu-token-de-api"; // Generar en Configuración de Jira
private static final String JIRA_PROJECT_KEY = "PROY"; // Clave de tu proyecto
private static final String JIRA_ISSUE_TYPE_ID = "10004"; // ID del tipo de issue (ej: Task)
```

**Nota de seguridad**: En producción, mueve estas credenciales a variables de entorno o archivos de configuración externos.

### 4. Compilación y Ejecución

```bash
# Compilar
javac -cp "lib/*:." -d out src/module-info.java $(find src -name "*.java")

# Ejecutar
java -cp "lib/*:out" com.clorian.db.MainTest
```

---

## 💡 Ejemplos de Uso

### Ejemplo 1: Ejecución de Scripts SQL Automatizada

```java
// Cargar scripts desde directorio
List<QueryScript> scripts = ScriptLoader.loadScriptsFromDir("./sql-scripts");

// Configurar y ejecutar
QueryAutomationService service = new QueryAutomationService();
service.runScripts(scripts);

// Los scripts que contengan "critical", "refund" o "payment" en el nombre
// detendrán la ejecución si fallan
```

### Ejemplo 2: Sincronización Manual de Tickets a Jira

```java
// Inicializar cliente Jira
JiraApiClient jiraClient = new JiraApiClient(JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN);
IssueCreator creator = new IssueCreator(jiraClient, "KAN", "10004");

// Sincronizar tickets abiertos
SupportTicketSyncService syncService = new SupportTicketSyncService(creator, "customfield_10058");
syncService.syncOpenTickets();
// Crea issues en Jira y actualiza jira_issue_key en MySQL
```

### Ejemplo 3: Monitoreo Continuo de Estados

```java
StatusSyncService statusSync = new StatusSyncService(jiraClient);

// Iniciar polling en segundo plano (cada 30 segundos)
Thread pollingThread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        statusSync.pollAndSync(); // Detecta cambios en MySQL y actualiza Jira
        Thread.sleep(30000);
    }
});
pollingThread.start();
```

---

## 🏗️ Arquitectura y Flujo de Datos

```
┌─────────────────┐         ┌──────────────────┐
│   MySQL Server  │         │   Jira Cloud     │
│  (clorian_db)   │◄───────►│   REST API v3    │
└────────┬────────┘  Sync   └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│      Clorian DB Connector (Java)            │
│  ┌──────────────┐    ┌──────────────────┐  │
│  │   Módulo DB  │    │   Módulo Jira    │  │
│  │  • JDBC      │◄──►│  • REST Client   │  │
│  │  • Scripts   │    │  • Sync Service  │  │
│  │  • Queries   │    │  • Issue Creator │  │
│  └──────────────┘    └──────────────────┘  │
│            │                                 │
│            ▼                                 │
│  ┌─────────────────────────────────────┐    │
│  │         MainTest.java               │    │
│  │  • Validación de conexiones         │    │
│  │  • Menú interactivo                 │    │
│  │  • Polling de sincronización        │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### Mapeo de Estados (MySQL ↔ Jira)

| Estado MySQL | Transición Jira | Estado Jira |
| --- | --- | --- |
| `Open` | ID: 11 | Por Hacer (To Do) |
| `In Progress` | ID: 21 | En Curso |
| `Waiting for Customer` | ID: 31 | Esperando por el Cliente |
| `Resolved` | ID: 41 | Resuelta (Done) |

---

## ⚙️ Configuración Avanzada

### Personalización de Campos Personalizados

El sistema utiliza campos personalizados de Jira para vincular registros:

```java
// En SupportTicketSyncService.java
fields.put("customfield_10058", externalId); // ID del ticket MySQL

// Ajusta estos IDs según tu configuración de Jira:
// 1. Ve a Configuración del Proyecto → Campos Personalizados
// 2. Busca el campo para ID externo
// 3. Reemplaza "customfield_10058" con tu ID real
```

### Ajuste de Intervalos de Polling

```java
// En MainTest.java, modifica el tiempo de espera:
Thread.sleep(30000); // 30 segundos (default)
// Cambiar a 60000 para 1 minuto, o 5000 para 5 segundos (testing)
```

---

## 🔒 Seguridad

- **Autenticación**: Basic Auth con token de API Jira (nunca uses password de cuenta)
- **SQL Injection**: Mitigado mediante `PreparedStatement` en todas las consultas parametrizadas
- **SSL**: Configurable (actualmente `useSSL=false` para desarrollo local, habilitar en producción)

---

## 🐛 Troubleshooting

### Problema: `ClassNotFoundException: com.mysql.cj.jdbc.Driver`

**Solución**: Asegúrate de que mysql-connector-java.jar esté en el classpath

### Problema: Error 401 al conectar con Jira

**Solución**:

1. Verifica que el token de API sea válido (generar en: Perfil Jira → Configuración de Cuenta → Seguridad → Tokens de API)
2. Confirma que el email coincida exactamente con el de la cuenta de Jira

### Problema: Campos personalizados no aparecen en Jira

**Solución**: Los campos personalizados deben estar en la pantalla de creación de issues del proyecto. Verifica en Configuración del Proyecto → Pantallas.

---

## 📝 Licencia

Proyecto desarrollado por **Jaime Urrutia**

GitHub: [@jaime-urrutia-it](https://github.com/jaime-urrutia-it)

---

## 🚧 Roadmap Futuro

- [ ]  Externalización de configuración a `application.properties`
- [ ]  Implementación de logging profesional (SLF4J/Log4j2)
- [ ]  Soporte para bases de datos PostgreSQL
- [ ]  Webhook bidireccional (Jira → MySQL en tiempo real)
- [ ]  Interfaz gráfica JavaFX para administración

---

**Versión**: 1.0

**Última actualización**: 2024
