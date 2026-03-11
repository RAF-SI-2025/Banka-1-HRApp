-- V3: Link grades to tasks instead of team_members
-- Grades now represent an evaluation of a specific task (COMPLETED tasks).

CREATE TABLE grades_new (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    grade   INTEGER NOT NULL,
    task_id INTEGER NOT NULL UNIQUE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

DROP TABLE grades;
ALTER TABLE grades_new RENAME TO grades;

CREATE INDEX idx_grades_task_id ON grades(task_id);
