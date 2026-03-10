-- V1: Initial schema
-- is_deleted = 1 means soft-deleted (never physically removed)

CREATE TABLE IF NOT EXISTS team_members (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT    NOT NULL,
    surname    TEXT    NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tasks (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    task_name  TEXT    NOT NULL,
    comment    TEXT    NOT NULL DEFAULT '',
    status     TEXT    NOT NULL DEFAULT 'PENDING',
    member_id  INTEGER NOT NULL,
    is_deleted INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS skills (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    skill_name TEXT    NOT NULL,
    member_id  INTEGER NOT NULL,
    FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS grades (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    grade     INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE
);
