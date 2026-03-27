# Clorian DB Connector

## 📖 Descripción
**Clorian DB Connector** es un módulo de integración desarrollado en **Java** diseñado para facilitar la conexión, consulta y automatización de operaciones con bases de datos **MySQL**. 

Este proyecto actúa como una capa de abstracción que permite cargar scripts SQL externos, ejecutarlos de forma dinámica y procesar los resultados estructuradamente, sirviendo como núcleo de sistemas más complejos de gestión de datos y automatización de incidentes.

## 🚀 Características Principales
- **Ejecución de Scripts Dinámicos:** Carga y ejecución de archivos `.sql` externos mediante la clase `ScriptLoader`.
- **Automatización de Consultas:** Servicio dedicado (`QueryAutomationService`) para orquestar flujos de trabajo de base de datos.
- **Gestión Eficiente de Resultados:** Procesamiento tipado de respuestas JDBC a través de `QueryResultHandler`.
- **Integración JSON:** Serialización y deserialización de datos utilizando la librería `org.json` (incluida en `/lib`).
- **Arquitectura Limpia:** Separación de responsabilidades entre ejecución, manejo de resultados y carga de recursos.

## 🛠️ Stack Tecnológico
| Tecnología | Versión / Detalle |
| :--- | :--- |
| **Lenguaje** | Java (Compatible con JDK 8+) |
| **Base de Datos** | MySQL |
| **Librerías** | `org.json` (json-20231013.jar) |
| **IDE** | Eclipse IDE |
| **Control de Versiones** | Git / GitHub |

## 📂 Estructura del Proyecto
El código fuente se organiza bajo el paquete `com.clorian.db.automation`:

```text
src/
└── com/
    └── clorian/
        └── db/
            └── automation/
                ├── QueryAutomationService.java   # Orquestador principal
                ├── QueryExecutor.java            # Ejecución directa de sentencias
                ├── QueryResultHandler.java       # Procesamiento de ResultSet
                └── ScriptLoader.java             # Carga de archivos .sql externos
lib/
└── json-20231013.jar                             # Dependencia externa
