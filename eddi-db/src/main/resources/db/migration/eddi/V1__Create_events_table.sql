

---
--- Events table for storing event data.
---
CREATE TABLE IF NOT EXISTS eddi.events
(
    seq  BIGSERIAL PRIMARY KEY,
    cid  BIGINT      NOT NULL,
    name TEXT        NOT NULL,
    data JSONB       NOT NULL,
    ids  JSONB       NOT NULL DEFAULT '[]'::JSONB,
    ts   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

---
--- Add index on events.name for faster event name lookups.
---
CREATE INDEX IF NOT EXISTS idx_events_name ON eddi.events (name);

---
--- Add GIN index on events.ids for efficient id queries.
---
CREATE INDEX IF NOT EXISTS idx_events_ids ON eddi.events USING gin(ids);

---
--- Offset table for reliable event publishing.
---
CREATE TABLE IF NOT EXISTS eddi.events_offsets
(
    id       BIGINT PRIMARY KEY,
    last_seq BIGINT      NOT NULL REFERENCES eddi.events (seq),
    ts       TIMESTAMPTZ NOT NULL
);