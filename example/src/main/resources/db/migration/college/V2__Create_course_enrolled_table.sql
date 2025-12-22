
---
--- Course enrollment table (many-to-many relationship between students and courses).
---
CREATE TABLE IF NOT EXISTS college.course_enrolled
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL REFERENCES college.student (id),
    course_id   UUID        NOT NULL REFERENCES college.course (id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, course_id)
);

---
--- Add index on course_enrolled.student_id for faster student lookups.
---
CREATE INDEX IF NOT EXISTS idx_course_enrolled_student_id ON college.course_enrolled (student_id);

---
--- Add index on course_enrolled.course_id for faster course lookups.
---
CREATE INDEX IF NOT EXISTS idx_course_enrolled_course_id ON college.course_enrolled (course_id);