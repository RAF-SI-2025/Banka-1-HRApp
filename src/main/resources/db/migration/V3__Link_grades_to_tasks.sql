-- V3: Link grades to tasks (one grade per completed task)
-- Drop old grades table and recreate with task_id FK + unique constraint

DROP TABLE IF EXISTS grades;

CREATE TABLE IF NOT EXISTS grades (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    grade     INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    task_id   INTEGER NOT NULL UNIQUE,
    FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id)   REFERENCES tasks(id)        ON DELETE CASCADE
);

