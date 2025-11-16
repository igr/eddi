---
--- Events table for storing event data.
---
CREATE TABLE events
(
    seq  BIGSERIAL PRIMARY KEY,
    cid  BIGINT      NOT NULL,
    name TEXT        NOT NULL,
    data JSONB       NOT NULL,
    tags JSONB       NOT NULL DEFAULT '[]'::JSONB,
    ts   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_seq ON events (seq);


---
--- Offset table for reliable event publishing.
---
CREATE TABLE events_offset
(
    last_seq BIGSERIAL PRIMARY KEY,
    ts       TIMESTAMPTZ NOT NULL
);

-- Initialize the offset table with a starting value.
INSERT INTO events_offset (last_seq, ts)
VALUES (0, CURRENT_TIMESTAMP);