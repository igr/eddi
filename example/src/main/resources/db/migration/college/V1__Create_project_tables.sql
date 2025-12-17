
---
--- Student table.
---
CREATE TABLE IF NOT EXISTS college.student
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seq           BIGINT      NOT NULL REFERENCES eddi.events (seq),
    first_name    TEXT        NOT NULL,
    last_name     TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE,
    payed         BOOLEAN     NOT NULL DEFAULT false,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

---
--- Add index on student.seq for faster event sequence lookups.
---
CREATE INDEX IF NOT EXISTS idx_student_seq ON college.student (seq);

---
--- Add index on student.email for faster email lookups.
---
CREATE INDEX IF NOT EXISTS idx_student_email ON college.student (email);

---
--- Course table.
---
CREATE TABLE IF NOT EXISTS college.course
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seq         BIGINT      NOT NULL REFERENCES eddi.events (seq),
    name        TEXT        NOT NULL,
    instructor  TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

---
--- Add index on course.seq for faster event sequence lookups.
---
CREATE INDEX IF NOT EXISTS idx_course_seq ON college.course (seq);