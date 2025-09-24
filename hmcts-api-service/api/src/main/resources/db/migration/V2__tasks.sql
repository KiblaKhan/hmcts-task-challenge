CREATE TABLE IF NOT EXISTS tasks (
  id_key      VARCHAR(50)  PRIMARY KEY,
  title       VARCHAR(255) NOT NULL,
  description VARCHAR(2000),
  status      VARCHAR(32)  NOT NULL,
  due_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tasks_status  ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_due_at  ON tasks(due_at);
