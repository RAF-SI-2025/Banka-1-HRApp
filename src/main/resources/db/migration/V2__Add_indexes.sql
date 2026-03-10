-- V2: Performance indexes on frequently filtered columns

CREATE INDEX IF NOT EXISTS idx_tasks_member_id  ON tasks(member_id);
CREATE INDEX IF NOT EXISTS idx_skills_member_id ON skills(member_id);
CREATE INDEX IF NOT EXISTS idx_grades_member_id ON grades(member_id);
CREATE INDEX IF NOT EXISTS idx_members_surname  ON team_members(surname);
CREATE INDEX IF NOT EXISTS idx_members_deleted  ON team_members(is_deleted);
CREATE INDEX IF NOT EXISTS idx_tasks_deleted    ON tasks(is_deleted);
