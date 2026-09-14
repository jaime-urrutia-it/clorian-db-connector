# Clorian DB Connector

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Jira](https://img.shields.io/badge/Jira-Cloud%2FServer-0052CC.svg)
![MySQL](https://img.shields.io/badge/MySQL-5.7+-4479A1.svg)

Java integration module for data orchestration between MySQL and Jira Cloud. This project functions as the **EMITTER** component in a bidirectional synchronization architecture, enabling both standalone operation and real-time integration with its complement, the [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver).

> **Part of an Ecosystem:** This is the EMITTER (MySQL → Jira). For real-time bidirectional synchronization, deploy it alongside the RECEIVER ([Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver)).

---

## ⚠️ Current State (August 2026)

This project is a **functional MVP** for technical demonstration.
- Jira integration requires a valid API token (periodically renewed).
- To view the actual implementation, consult the `.java` files in `src/`.

---

## 🎯 Purpose and Architecture

### Business Context
This project demonstrates how automating operational workflows between disconnected systems (an operations database and a project management platform) reduces response latency, eliminates manual errors, and provides complete traceability. **It is highly applicable to SSC, Business Operations, and shared services environments where coordination between operational and technical teams is critical.**

### Standalone Mode (Unidirectional)
Operates independently, performing periodic polling (every 30s) to:
- Detect new support tickets in MySQL and automatically create them in Jira.
- Synchronize states from Jira back to MySQL via periodic REST API queries.

### Integrated Mode (Bidirectional - Recommended)
When combined with the [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver), it forms a complete synchronization system:
- **This project (Emitter):** Sends new tickets from MySQL to Jira + State polling every 30s.
- **Webhook Receiver (Receiver):** Receives instant state updates from Jira via HTTP webhooks.

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    COMPLETE ARCHITECTURE                            │
└─────────────────────────────────────────────────────────────────────┘

   ┌─────────────────┐         Option A: Polling (30s)          ┌──────┐
   │   JIRA CLOUD    │ ◄─────────────────────────────────────── │      │
   │                 │                                          │      │
   │  • Issues       │         Option B: Webhook (Real-time)    │      │
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
   │  CLORIAN DB      │  • Ticket emitter            │  JIRA WEBHOOK      │
   │   CONNECTOR      │  • Polling every 30s         │    RECEIVER        │
   │   (This project) │  • Issue creation            │  (Complementary    │
   │                  │  • Sync MySQL → Jira         │   project)         │
   └────────┬─────────┘                               └────────────────────┘
            │                                                    ▲
            │ JDBC                                               │
            ▼                                                    │
   ┌──────────────────┐                                          │
   │   MYSQL SERVER   │                                          │
   │  (clorian_db)    │ ◄────────────────────────────────────────┘
   │                  │         State updates
   │ • SupportTickets │         (Real-time via webhook)
   │ • Customers      │
   │ • Sync status    │
   └──────────────────┘
```

---

## ✅ Main Features

### Database Module (`com.clorian.db`)
- **Robust JDBC Connection:** Thread-safe MySQL connection management with state validation.
- **Dynamic Script Execution:** Loading and execution of external `.sql` files with automatic criticality classification.
- **SQL Security:** Use of `PreparedStatement` to prevent SQL injection.
- **Result Processing:** Typed `ResultSet` handling with export to console and TXT files.
- **Orchestration:** `QueryAutomationService` manages workflows with dependency handling.

### Jira Integration Module (`com.clorian.jira`)
- **REST API V3:** Native HTTP client (Java 11+) for Jira Cloud with Basic Auth.
- **Issue Creation:** Automatic ticket generation with ADF (Atlassian Document Format), custom fields, and priority mapping.
- **Polling Synchronization:**
  - `SupportTicketSyncService`: Detects tickets with `status='Open'` and no `jira_issue_key`, creating them in Jira.
  - `StatusSyncService`: Synchronizes states every 30s via Jira API queries.
- **State Management:** Bidirectional mapping (see [unified ecosystem table](https://github.com/jaime-urrutia-it/clorian-ecosystem#mapeo-de-estados-referencia-unica)).

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Language | Java 17+ (uses text blocks, switch expressions, and `java.net.http.HttpClient`) | Business logic |
| Database | MySQL 5.7+ | Local persistence |
| JDBC Driver | MySQL Connector/J 8.x | DB connectivity |
| External API | Jira REST API v3 | Integration with Jira Cloud/Server |
| JSON | org.json 20231013 | Lightweight REST payload parsing |
| HTTP Client | `java.net.http.HttpClient` | Communication with Jira |
| Build | `javac` (manual compilation) | Packaging |

---

## 📂 Project Structure

> ℹ️ **Note on dependencies in `lib/`:** This project uses manual compilation with `javac` (not Maven/Gradle), so external dependencies required for compilation are versioned directly in `lib/`. The file `json-20231013.jar` (74 KB, org.json) is required for REST payload parsing. The `*.jar` rule in `.gitignore` prevents the future addition of build artifacts or unnecessary JARs.

```text
clorian-db-connector/
 ├── lib/
 │   └── json-20231013.jar
 ├── src/
 │   ├── module-info.java
 │   └── com/clorian/
 │       ├── db/                        # PERSISTENCE LAYER
 │       │   ├── DatabaseConnection.java
 │       │   ├── DatabaseQueries.java
 │       │   ├── MainTest.java           # ENTRY POINT
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
 │       └── jira/                      # INTEGRATION LAYER
 │           ├── client/
 │           │   └── JiraApiClient.java
 │           └── service/
 │               ├── IssueCreator.java
 │               ├── StatusSyncService.java
 │               └── SupportTicketSyncService.java
 └── README.md
```

---

## 🚀 Installation and Configuration

### 1. Prerequisites
- Java JDK 17 or higher (required: uses text blocks, switch expressions, and `HttpClient`).
- MySQL Server 5.7+ with the `clorian_db` schema.
- Jira Cloud account with a generated API token.
- (Optional) [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) for bidirectional mode.

### 2. Database Schema
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
> **Note:** The complete ecosystem DDL (including the business layer tables `bookings`, `payments`, `tickets`, `refunds` for Clorian 2.0) is available in the [ecosystem README](https://github.com/jaime-urrutia-it/clorian-ecosystem).

### 3. Credentials Configuration
Edit `src/com/clorian/db/MainTest.java`:
```java
private static final String JIRA_URL = System.getenv().getOrDefault("JIRA_URL", "https://your-domain.atlassian.net");
private static final String JIRA_EMAIL = System.getenv().getOrDefault("JIRA_EMAIL", "");
private static final String JIRA_API_TOKEN = System.getenv().getOrDefault("JIRA_API_TOKEN", "");
private static final String JIRA_PROJECT_KEY = "KAN";
private static final String JIRA_ISSUE_TYPE_ID = "10004";
```
**In production:** Externalize these credentials to environment variables or external properties files.

### 4. Compilation and Execution
```bash
# Ensure mysql-connector-java-8.x.jar is placed in lib/
javac -cp "lib/*:." -d out src/module-info.java $(find src -name "*.java")
java -cp "lib/*:out" com.clorian.db.MainTest
```

---

## 🔄 Operation Modes

### Standalone Mode
- Connection validation (MySQL and Jira).
- Initial synchronization: `SupportTicketSyncService.syncOpenTickets()`.
- Continuous monitoring: `StatusSyncService` every 30s in a separate thread.
- Interactive menu for manual synchronization.

### Integrated Mode (Recommended)
Combines this project with the [Jira Webhook Receiver](https://github.com/jaime-urrutia-it/jira-webhook-receiver) for real-time synchronization. In integrated mode, the state polling of this project can be disabled or reduced, leaving the Jira → MySQL synchronization to the webhook receiver.

---

## 🔐 Security

For the current state and recommended improvements, consult the [ecosystem security section](https://github.com/jaime-urrutia-it/clorian-ecosystem#seguridad).

**Specific improvements for this component:**
- [ ] Complete credential externalization (environment variables / vault).
- [ ] Validation of Jira API responses.
- [ ] Timeout and retry configuration in `HttpClient`.

---

### ⚠️ Known MVP Limitations (August 2026)
This project is a demonstration MVP, not a production system. The following limitations are intentionally documented as part of the maturation roadmap:

| Limitation | Impact | Mitigation Plan |
|---|---|---|
| Polling every 30s in standalone mode | Unnecessary load on Jira API | Increase interval or migrate to webhook-only |
| Console logging (`System.out`) in some points | No rotation or structured levels | Fully migrate to SLF4J + Logback |
| Manual compilation with `javac` | Requires manual dependency management in `lib/` | Future migration to Maven/Gradle (see Roadmap) |

**Note on scope:** These limitations are documented because an SSC/Business Operations environment values both the control of a system and honesty about its state. The decision to address them (or accept them as a controlled risk in a low-volume environment) corresponds to the operations team that adopts the project.

---

## 📈 Roadmap

### Business Track
- [ ] O2C reconciliation module (implemented in [Clorian 2.0](https://github.com/jaime-urrutia-it/clorian-ecosystem#clorian-20--capa-de-business-operations-control-o2c))
- [ ] Service KPIs dashboard
- [ ] Exportable operational reports
- [ ] ERP integration

### Technical Track
- [ ] Migration to Spring Boot
- [ ] Official Dockerization
- [ ] Credential externalization
- [ ] Professional logging (SLF4J + Logback)
- [ ] PostgreSQL support
- [ ] Own REST API for synchronization management

---

## 📄 License and Authorship

Developed by Jaime Urrutia  
[GitHub](https://github.com/jaime-urrutia-it) | [Portfolio](https://yagourrutia.com) | [LinkedIn](https://www.linkedin.com/in/jaime-urrutia-multilingue/?locale=en-US)

**Version:** 1.0.0 | **Last update:** August 2026
