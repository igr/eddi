---
--- Events table for storing event data.
---
CREATE TABLE events
(
    seq  BIGSERIAL PRIMARY KEY,
    cid  BIGINT      NOT NULL,
    name TEXT        NOT NULL,
    data JSONB       NOT NULL,
    ts   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

---
--- Offset table for reliable event publishing.
---
CREATE TABLE events_offsets
(
    id       BIGINT PRIMARY KEY,
    last_seq BIGINT      NOT NULL REFERENCES events (seq),
    ts       TIMESTAMPTZ NOT NULL
);