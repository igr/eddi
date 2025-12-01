---
--- Student table.
---
CREATE TABLE IF NOT EXISTS student
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seq           BIGINT      NOT NULL REFERENCES events (seq),
    first_name    TEXT        NOT NULL,
    last_name     TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

---
--- Add index on student.seq for faster event sequence lookups.
---
CREATE INDEX IF NOT EXISTS idx_student_seq ON student (seq);

---
--- Add index on student.email for faster email lookups.
---
CREATE INDEX IF NOT EXISTS idx_student_email ON student (email);