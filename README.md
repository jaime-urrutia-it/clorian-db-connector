Aquí tienes el README completamente rediseñado para presentar **Clorian DB Connector** como parte de una arquitectura de sincronización bidireccional:

---

```markdown
# 🔗 Clorian DB Connector

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-green)](https://spring.io)
[![Arquitectura](https://img.shields.io/badge/Arquitectura-Bidireccional-orange)]()

**Módulo de integración Java para orquestación de datos entre MySQL y Jira Cloud**. Este proyecto funciona como **componente emisor** en una arquitectura de sincronización híbrida, permitiendo tanto operación standalone (sincronización unidireccional programada) como integración en tiempo real con su complemento [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver).

> 🏗️ **Parte de un Ecosistema**: Este es el **EMISOR** (MySQL → Jira) del sistema completo. Para sincronización **bidireccional en tiempo real**, despliégalo junto con el **RECEPTOR** ([Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver)), que escucha cambios de Jira hacia MySQL vía webhooks.

---

## 🎯 Propósito y Arquitectura

### Modo Standalone (Unidireccional)
Opera de forma independiente realizando **polling periódico** (cada 30s) para:
- Detectar nuevos tickets de soporte en MySQL y crearlos automáticamente en Jira
- Sincronizar estados de Jira hacia MySQL mediante consulta periódica a la API REST

### Modo Integrado (Bidireccional - Recomendado)
En conjunto con **Jira Webhook Receiver**, forma un sistema de sincronización completo:
- **Este proyecto (Emisor)**: Envía tickets nuevos de MySQL a Jira + Polling de estado cada 30s
- **Webhook Receiver (Receptor)**: Recibe actualizaciones instantáneas de Jira vía HTTP webhooks

```
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
  │  CLORIAN DB      │  ● Emisor de tickets         │  JIRA WEBHOOK      │
  │   CONNECTOR      │  ● Polling cada 30s          │    RECEIVER        │
  │   (Este proyecto)│  ● Creación de issues        │  (Proyecto         │
  │                  │  ● Sync MySQL → Jira         │   complementario)  │
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

## ✨ Características Principales

### Módulo de Base de Datos (`com.clorian.db`)
- **🔌 Conexión JDBC Robusta**: Gestión thread-safe de conexiones MySQL con validación de estado (`isConnectionValid()`)
- **⚡ Ejecución de Scripts Dinámicos**: Carga y ejecución de archivos `.sql` externos con clasificación automática de criticidad (scripts "critical", "refund" o "payment" detienen ejecución ante fallos)
- **🛡️ Seguridad SQL**: Uso de `PreparedStatement` para prevenir inyección SQL
- **📊 Procesamiento de Resultados**: Manejo tipado de ResultSets con exportación a consola y archivos TXT
- **🎛️ Orquestación**: Servicio `QueryAutomationService` que gestiona flujos de trabajo críticos con manejo de dependencias entre scripts

### Módulo de Integración Jira (`com.clorian.jira`)
- **☁️ API REST V3**: Cliente HTTP nativo (Java 11+) para Jira Cloud con autenticación Basic Auth
- **📝 Creación de Issues**: Generación automática de tickets con formato ADF (Atlassian Document Format), campos personalizados (`customfield_10058` para ID externo) y mapeo de prioridades (High/Medium/Low)
- **🔄 Sincronización por Polling**: 
  - `SupportTicketSyncService`: Detecta tickets `status='Open'` sin `jira_issue_key` y los crea en Jira
  - `StatusSyncService`: Sincroniza estados cada 30s mediante consulta a API de Jira y actualiza MySQL
- **🎯 Gestión de Estados**: Mapeo inteligente bidireccional entre sistemas

### Funcionalidades Específicas por Modo

| Función | Modo Standalone | Modo Integrado (con Webhook Receiver) |
|---------|----------------|--------------------------------------|
| Crear tickets en Jira | ✅ Sí | ✅ Sí (vía este proyecto) |
| Detectar cambios de estado Jira→MySQL | ⚠️ Cada 30s (polling) | ✅ Tiempo real (webhook) |
| Detectar cambios de estado MySQL→Jira | ✅ Sí (polling) | ✅ Sí (polling) |
| Latencia de sincronización | ~30 segundos | < 1 segundo |
| Carga sobre Jira API | Alta (polling constante) | Baja (solo creación) |

---

## 🛠️ Stack Tecnológico

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Lenguaje** | Java 17+ (Compatible JDK 8+) | Lógica de negocio |
| **Framework** | Spring Boot 3.3.3 | IoC y ejecución standalone |
| **Base de Datos** | MySQL 5.7+ | Persistencia local |
| **Driver JDBC** | MySQL Connector/J 8.x | Conectividad BD |
| **API Externa** | Jira REST API v3 | Integración cloud |
| **JSON** | org.json 20231013 | Parseo de payloads |
| **HTTP Client** | `java.net.http.HttpClient` | Comunicación con Jira |
| **Build** | Maven / Eclipse IDE | Empaquetado JAR ejecutable |

---

## 📂 Estructura del Proyecto

```
clorian-db-connector/
├── 📁 lib/
│   └── json-20231013.jar                 # Dependencia JSON externa
├── 📁 src/
│   ├── module-info.java                  # Configuración Java Modules
│   └── 📁 com/clorian/
│       ├── 📁 db/                        # 🔌 CAPA DE PERSISTENCIA
│       │   ├── DatabaseConnection.java   # Gestión de conexiones JDBC
│       │   ├── DatabaseQueries.java      # Consultas genéricas (genérico)
│       │   ├── MainTest.java            # 🚀 PUNTO DE ENTRADA PRINCIPAL
│       │   ├── TestConnection.java       # Test unitario básico
│       │   ├── 📁 automation/           # Núcleo de automatización SQL
│       │   │   ├── QueryAutomationService.java  # Orquestador de scripts
│       │   │   ├── QueryExecutor.java           # Ejecutor seguro (PreparedStatement)
│       │   │   ├── QueryResultHandler.java      # Manejo de resultados y logs
│       │   │   └── ScriptLoader.java            # Carga dinámica de .sql
│       │   ├── 📁 model/                # Modelos de datos
│       │   │   ├── QueryResult.java     # POJO resultado de consultas
│       │   │   └── QueryScript.java     # Metadatos de scripts SQL
│       │   └── 📁 util/                 # Utilidades
│       │       ├── FileUtil.java        # I/O de archivos (NIO.2)
│       │       └── SQLParser.java       # Validador sintaxis SQL básico
│       │
│       └── 📁 jira/                      # ☁️ CAPA DE INTEGRACIÓN
│           ├── 📁 client/
│           │   └── JiraApiClient.java   # Cliente HTTP REST (Basic Auth)
│           └── 📁 service/
│               ├── IssueCreator.java           # Creación de issues con ADF
│               ├── StatusSyncService.java      # Sincronización por polling
│               └── SupportTicketSyncService.java # Sync inicial MySQL→Jira
└── README.md
```

---

## 🚀 Instalación y Configuración

### 1. Prerrequisitos
- Java JDK 17 o superior (compatible con JDK 8+)
- MySQL Server 5.7+ con esquema `clorian_db`
- Cuenta en Jira Cloud con token de API generado
- (Opcional) [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) para modo bidireccional

### 2. Esquema de Base de Datos Requerido

```sql
-- Tabla principal de tickets (debe existir para ambos proyectos)
CREATE TABLE SupportTickets (
    support_ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    priority ENUM('High', 'Medium', 'Low') DEFAULT 'Medium',
    status ENUM('Open', 'In Progress', 'Waiting for Customer', 'Resolved') DEFAULT 'Open',
    jira_issue_key VARCHAR(20) UNIQUE,        -- Enlace bidireccional crucial
    last_sync_status VARCHAR(50),             -- Para evitar ciclos de sync
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES Customers(customer_id)
);

-- Tabla de clientes (referenciada)
CREATE TABLE Customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255)
);

-- Índices recomendados para performance
CREATE INDEX idx_jira_key ON SupportTickets(jira_issue_key);
CREATE INDEX idx_status_sync ON SupportTickets(status, last_sync_status);
CREATE INDEX idx_open_tickets ON SupportTickets(status) WHERE status = 'Open';
```

### 3. Configuración de Credenciales

Edita `src/com/clorian/db/MainTest.java`:

```java
// Configuración Jira Cloud (obligatoria)
private static final String JIRA_URL = "https://tu-dominio.atlassian.net";
private static final String JIRA_EMAIL = "tu-email@ejemplo.com";
private static final String JIRA_API_TOKEN = "tu-token-de-api"; // Generar en Configuración de Jira → Seguridad → Tokens de API
private static final String JIRA_PROJECT_KEY = "KAN"; // Clave de tu proyecto (ej: KAN, PROJ, SUP)
private static final String JIRA_ISSUE_TYPE_ID = "10004"; // ID del tipo de issue (10004=Story/Task, ver en Jira)

// Configuración MySQL (en DatabaseConnection.java)
private static final String URL = "jdbc:mysql://localhost:3306/clorian_db?useSSL=false&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "tu_password_segura";
```

**🔐 Seguridad**: En producción, externaliza estas credenciales a variables de entorno o archivos de propiedades externas.

### 4. Compilación y Ejecución

```bash
# Compilar (genera clases en bin/ o out/)
javac -cp "lib/*:." -d out src/module-info.java $(find src -name "*.java")

# Ejecutar modo standalone
java -cp "lib/*:out" com.clorian.db.MainTest
```

---

## 🔄 Modos de Operación

### Modo 1: Standalone (Sincronización Unidireccional)

En este modo, el sistema opera de forma independiente sincronizando datos cada 30 segundos.

**Flujo de ejecución (MainTest.java)**:
1. **Validación de conexiones**: Verifica MySQL y Jira
2. **Sincronización inicial**: Ejecuta `SupportTicketSyncService.syncOpenTickets()`
   - Busca tickets con `status='Open'` y `jira_issue_key IS NULL`
   - Crea issues en Jira vía REST API
   - Actualiza `jira_issue_key` en MySQL
3. **Monitoreo continuo**: Inicia `StatusSyncService` en hilo separado
   - Cada 30s consulta estados de Jira
   - Actualiza MySQL si detecta diferencias (`last_sync_status <> status`)
4. **Menú interactivo**: Permite sincronización manual o salida

**Ideal para**: Entornos donde la latencia de 30s es aceptable y no se requiere infraestructura adicional de webhooks.

### Modo 2: Integrado (Arquitectura Bidireccional - Recomendado)

Combina este proyecto con **Jira Webhook Receiver** para sincronización en tiempo real.

**Arquitectura de despliegue**:
```
Servidor A (Este proyecto - Emisor):
  - Clorian DB Connector ejecutándose
  - Polling cada 30s (solo para crear nuevos tickets)
  - NO sincroniza estados por polling (desactivar o aumentar intervalo)

Servidor B (Receptor):
  - Jira Webhook Receiver ejecutándose
  - Expuesto a Internet (o VPN) para recibir webhooks de Jira
  - Actualiza MySQL inmediatamente al recibir cambio de estado
```

**Ventajas**:
- ✅ Sincronización casi instantánea (subsegundo)
- ✅ Menor carga en API de Jira (menos polling)
- ✅ Mayor escalabilidad

**Configuración para modo integrado**:

En `MainTest.java`, modifica el intervalo de polling para que solo cree tickets, no sincronice estados (dejando esto al webhook):

```java
// En StatusSyncService, ajusta el intervalo si es necesario
// o comenta la línea de inicio de pollingThread si todo el sync de estados
// lo manejará el Webhook Receiver

// Opción: Aumentar a 5 minutos (300000ms) solo para verificación de sanity
Thread.sleep(300000); 
```

---

## 🔗 Guía de Integración con Jira Webhook Receiver

Para lograr la sincronización bidireccional completa:

### Paso 1: Desplegar este proyecto (Emisor)
Sigue las instrucciones de instalación anteriores. Asegúrate de que pueda:
- Conectarse a MySQL
- Crear issues en Jira (prueba con `MainTest.java`)

### Paso 2: Desplegar Jira Webhook Receiver (Receptor)
En el mismo servidor o diferente (recomendado mismo servidor para compartir MySQL localmente):

```bash
# En directorio separado
git clone https://github.com/jaime-urrutia-it/jira-webhook-receiver.git
cd jira-webhook-receiver
mvn clean package
java -jar target/JiraWebhookReceiver-1.0.0.jar
```

Verifica que responda en `http://localhost:8080/api/jira-webhook`

### Paso 3: Configurar Webhook en Jira

1. Ve a **Configuración del Sistema** → **WebHooks** (requiere admin)
2. Crea nuevo webhook:
   - **URL**: `http://<ip-servidor>:8080/api/jira-webhook`
   - **Eventos**: Issue → updated
3. Guarda

### Paso 4: Prevenir Ciclos Infinitos

El sistema está diseñado para evitar bucles:

```java
// En este proyecto (Emisor):
// Actualiza last_sync_status después de sincronizar
UPDATE SupportTickets SET status=?, last_sync_status=? WHERE ...

// En Webhook Receiver (Receptor):
// Al recibir webhook, actualiza status y last_sync_status al mismo valor
// Esto evita que el Emisor detecte cambio pendiente
```

**Flujo seguro**:
1. Usuario cambia estado en Jira a "En curso"
2. Jira → Webhook Receiver → MySQL (status='In Progress', last_sync='In Progress')
3. Emisor hace polling: compara status='In Progress' vs last_sync='In Progress' → **No hay diferencia, no hace nada** ✅
4. Si por error Emisor actualiza Jira, Jira dispara webhook, pero Webhook Receiver actualiza MySQL con el mismo valor → **Sin efecto secundario** ✅

---

## 🗺️ Mapeo de Estados Bidireccional

Sistema de traducción entre estados de Jira (Español) y MySQL:

| Estado Jira (UI) | Estado MySQL | ID Transición (Jira) |
|------------------|--------------|---------------------|
| **Tareas por hacer** | `Open` | 11 |
| **En curso** | `In Progress` | 21 |
| **Esperando por el cliente** | `Waiting for Customer` | 31 |
| **Resuelta** | `Resolved` | 41 |

**Nota**: Si personalizas los nombres de estado en Jira, actualiza:
- En **este proyecto**: `StatusSyncService.getTransitionIdForStatus()`
- En **Webhook Receiver**: `WebhookController.mapJiraStatusToMySQL()`

---

## 📊 Monitoreo y Troubleshooting

### Logs y Trazabilidad

Este proyecto utiliza logging por consola (configurable a SLF4J en futuras versiones). 

**Indicadores de funcionamiento correcto**:
```
✅ MySQL: Conexión exitosa.
☁️ Conectando a Jira Cloud...
✅ Conexión a Jira exitosa
🔄 Sincronizando nuevos tickets de soporte a Jira...
✅ Sincronizado: Soporte #123 → KAN-45
🔄 Iniciando monitoreo continuo de estados (cada 30 segundos)...
```

### Problemas Comunes y Soluciones

| Síntoma | Causa probable | Solución |
|---------|---------------|----------|
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Falta Connector/J en classpath | Descargar mysql-connector-java-8.x.jar y agregar a `lib/` |
| Error 401 al conectar Jira | Token inválido o email incorrecto | Verificar token en [id.atlassian.com](https://id.atlassian.com) |
| "No se encontró ticket con jira_issue_key" | Issue fue eliminado en Jira | Verificar integridad referencial o limpiar campo manualmente |
| Doble actualización de estados | Ambos proyectos están sincronizando estados | Dejar solo Webhook Receiver para sync de estados, este proyecto solo para creación |
| Campos personalizados no aparecen | ID de campo incorrecto | Verificar ID en Jira → Configuración → Campos personalizados (ej: customfield_10058) |

### Testing de la Integración Completa

1. **Crear ticket en MySQL**:
```sql
INSERT INTO SupportTickets (customer_id, subject, description, status) 
VALUES (1, 'Prueba Integración', 'Test bidireccional', 'Open');
```

2. **Verificar creación en Jira** (Emisor):
   - Esperar hasta 30s o usar opción manual en menú
   - Verificar que aparezca issue tipo "KAN-XX"

3. **Verificar webhook inverso** (Receptor):
   - Mover el issue a "En curso" en Jira
   - Verificar en MySQL: `SELECT status FROM SupportTickets WHERE jira_issue_key='KAN-XX';` → Debe ser 'In Progress' inmediatamente (< 1s)

---

## 🏛️ Arquitectura Interna y Patrones

### Patrones de Diseño Implementados

| Patrón | Implementación | Ubicación |
|--------|---------------|-----------|
| **Singleton** | Carga estática del driver JDBC | `DatabaseConnection` |
| **Factory** | Creación de objetos `QueryResult` | `QueryExecutor.execute()` |
| **Strategy** | Diferentes estrategias de salida (consola vs archivo) | `QueryResultHandler` |
| **Service Layer** | Lógica de negocio separada de controllers | `*SyncService`, `IssueCreator` |
| **DTO** | Transporte de datos entre capas | `QueryResult`, `QueryScript` |
| **Client API** | Encapsulación de HTTP/REST | `JiraApiClient` |

### Consideraciones de Concurrencia

- **Thread-safe**: `DatabaseConnection` usa bloques estáticos sincronizados
- **Hilos separados**: El polling de estados corre en `Polling-Thread` (daemon=false para mantener vivo al JVM)
- **Recursos**: Uso exhaustivo de try-with-resources para cerrar Connections, Statements y ResultSets

---

## 🚧 Roadmap y Evolución

### Próximas Mejoras (Backlog)
- [ ] **Externalización de configuración**: Mover credenciales a `application.properties` y variables de entorno
- [ ] **Logging profesional**: Migrar a SLF4J + Logback con appenders diferenciados
- [ ] **Base de datos**: Soporte para PostgreSQL además de MySQL
- [ ] **Dockerización**: Dockerfile oficial para despliegue en contenedores
- [ ] **Microservicios**: Separar módulo DB y Jira en servicios independientes comunicados por mensajería
- [ ] **Seguridad**: Implementar encriptación de credenciales con JKS (Java KeyStore)
- [ ] **API REST propia**: Exponer endpoints para gestionar la sincronización vía HTTP (start/stop/status)

### Versión 2.0 (Planeada)
- Migración completa a **Spring Boot** (ya parcialmente en Webhook Receiver)
- Soporte para **múltiples instancias de Jira** simultáneas
- **Cola de mensajes** (RabbitMQ/ActiveMQ) para desacoplar recepción de procesamiento
- **Dashboard web** (Spring Boot + Thymeleaf) para monitoreo visual

---

## 🤝 Contribución y Ecosistema

Este proyecto forma parte de un conjunto de herramientas de integración:

| Proyecto | Rol | Dirección | Latencia |
|----------|-----|-----------|----------|
| **Clorian DB Connector** | Emisor | MySQL → Jira | ~30s (polling) |
| **Jira Webhook Receiver** | Receptor | Jira → MySQL | <1s (webhook) |

**Repositorios relacionados**:
- 🔄 [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) - Componente receptor obligatorio para arquitectura bidireccional

**Cómo contribuir**:
1. Reporta issues en el repositorio correspondiente
2. Para mejoras de arquitectura, considera el impacto en ambos proyectos
3. Mantén compatibilidad del esquema de base de datos `clorian_db`

---

## 📝 Licencia y Autoría

Desarrollado por **Jaime Urrutia**  
GitHub: [@jaime-urrutia-it](https://github.com/jaime-urrutia-it)

**Versión actual**: 1.0.0  
**Compatibilidad**: Jira Cloud, Jira Server 8.x+, Jira Data Center  
**Requiere**: Java 17+, MySQL 5.7+

---

## 📞 Soporte

Para dudas sobre:
- **Este proyecto (Emisor)**: Abre un issue en este repositorio
- **Arquitectura bidireccional**: Consulta ambos READMEs y verifica la sección "Integración"
- **Configuración de webhooks**: Revisa el README de [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver)

---

**¿Te resulta útil este proyecto?** Dale ⭐ al repositorio y considera desplegar la arquitectura completa para una experiencia de sincronización en tiempo real.
```

---

Este README completamente rediseñado logra:

1. **Contextualización inmediata**: Desde el primer párrafo se entiende que es parte de un ecosistema
2. **Claridad de roles**: Distingue claramente "Emisor" vs "Receptor"
3. **Flexibilidad**: Explica cómo usarlo solo (standalone) o en conjunto
4. **Diagramas actualizados**: Muestra la arquitectura completa con ambos proyectos
5. **Guía de integración específica**: Paso a paso para combinar ambos
6. **Prevención de ciclos**: Explica la lógica anti-bucle entre ambos sistemas
7. **Mantenimiento de información técnica**: Conserva todos los detalles del análisis original pero reorganizados

¿Necesitas que ajuste alguna sección específica o agregue más detalles técnicos de algún componente particular?
