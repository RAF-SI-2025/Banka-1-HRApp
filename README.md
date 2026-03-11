# HR App

A desktop HR management application built with JavaFX and SQLite. Allows managing team members, assigning tasks, tracking skills, and recording performance grades.

## Tech Stack

- **Java 21**
- **JavaFX 21** — UI framework
- **SQLite** — local database (`hrapp.db` in working directory)
- **Flyway** — database migrations
- **SLF4J + Logback** — logging (logs stored in `~/hrapp-logs/`)
- **JUnit 5 + Mockito** — unit testing
- **Maven** — build tool
- **JaCoCo** — test coverage
## Project Structure

```
src/
├── main/java/com/example/
│   ├── Main.java                  # Entry point, manual DI wiring
│   ├── config/AppConfig.java      # Loads configuration from app.properties
│   ├── model/                     # Domain models (TeamMember, Task, TaskStatus)
│   ├── dto/                       # Data transfer objects
│   ├── repository/                # SQLite data access layer
│   ├── service/                   # Business logic layer
│   ├── exception/                 # Custom exception hierarchy
│   └── view/                      # JavaFX UI (MainStage + dialogs)
└── main/resources/
    ├── db/migration/              # Flyway SQL migrations
    └── logback.xml                # Logging configuration
└── test/java/com/example/
```

## Database Schema

| Table          | Description                                      |
|----------------|--------------------------------------------------|
| `team_members` | Members with soft-delete support (`is_deleted`)  |
| `tasks`        | Tasks linked to members, with status and comment |
| `skills`       | Skills per member (unique, case-insensitive)     |
| `grades`       | Append-only performance grades (1–10)            |

## Getting Started

### Prerequisites

- JDK 21+
- Maven 3.8+

### Run

```bash
mvn javafx:run
```

The database file `hrapp.db` will be created automatically in the working directory on first run. Flyway applies migrations automatically at startup.

### Test

```bash
mvn test
```
```bash
mvn verify
```

## Configuration

All constants are in app.properties (in the working directory) and accessed via `AppConfig.java`.

| Constant          | Default       | Description                   |
|-------------------|---------------|-------------------------------|
| `GRADE_MIN`       | 1             | Minimum grade value           |
| `GRADE_MAX`       | 10            | Maximum grade value           |
| `MAX_NAME_LENGTH` | 100           | Max characters for names      |
| `MAX_SKILL_LENGTH`| 100           | Max characters for skill name |
| `APP_WIDTH`       | 1100          | Main window width (px)        |
| `APP_HEIGHT`      | 700           | Main window height (px)       |

## Architecture

The app follows a strict layered architecture with constructor injection:

```
View → Service → Repository → DatabaseManager
```

No layer instantiates a dependency directly — all wiring happens in `Main.java`.
