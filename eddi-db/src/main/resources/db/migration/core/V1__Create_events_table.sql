---
--- Events table for storing event data.
---
CREATE TABLE IF NOT EXISTS events
(
    seq  BIGSERIAL PRIMARY KEY,
    cid  BIGINT      NOT NULL,
    name TEXT        NOT NULL,
    data JSONB       NOT NULL,
    tags JSONB       NOT NULL DEFAULT '[]'::JSONB,
    ts   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

---
--- Add index on events.name for faster event name lookups.
---
CREATE INDEX IF NOT EXISTS idx_events_name ON events (name);

---
--- Add GIN index on events.tags for efficient tag queries.
---
CREATE INDEX IF NOT EXISTS idx_events_tags ON events USING gin(tags);

---
--- Offset table for reliable event publishing.
---
CREATE TABLE IF NOT EXISTS events_offsets
(
    id       BIGINT PRIMARY KEY,
    last_seq BIGINT      NOT NULL REFERENCES events (seq),
    ts       TIMESTAMPTZ NOT NULL
);