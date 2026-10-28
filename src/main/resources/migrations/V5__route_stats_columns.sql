-- Precomputed stats so map queries don't need to read and parse times.
-- Nullable: existing rows are filled in by a background backfill at startup (RouteStatsBackfill).
ALTER TABLE route
    ADD COLUMN timesRun    INT NULL,
    ADD COLUMN avgTime     INT NULL,
    ADD COLUMN medTime     INT NULL,
    ADD COLUMN fastestTime INT NULL;
