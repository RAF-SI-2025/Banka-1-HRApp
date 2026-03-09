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

## Project Structure

```
src/
├── main/java/com/example/
│   ├── Main.java                  # Entry point, manual DI wiring
│   ├── config/AppConfig.java      # All configurable constants
│   ├── model/                     # Domain models (TeamMember, Task, TaskStatus)
│   ├── dto/                       # Data transfer objects
│   ├── repository/                # SQLite data access layer
│   ├── service/                   # Business logic layer
│   ├── exception/                 # Custom exception hierarchy
│   └── view/                      # JavaFX UI (MainStage + dialogs)
└── main/resources/
    ├── db/migration/              # Flyway SQL migrations
    └── logback.xml                # Logging configuration
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

## Configuration

All constants are in `AppConfig.java`:

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

## Future Work

TODO for future contributors:
- Consider adding pagination or filtering to member/task lists if data grows
- Consider adding a reporting/export feature (CSV or PDF)
- All UI changes are welcome
- Other functional improvements are welcome if justified
- Additional changes will likely be required after the meeting with the back lead/pm