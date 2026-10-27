ALTER TABLE zone
    ADD KEY location (latitude, longitude);

UPDATE route
SET fastestUser = ''
WHERE fastestUser IS NULL;

UPDATE route
SET fastestTimestamp = coalesce(updated, current_timestamp())
WHERE fastestTimestamp IS NULL;

UPDATE route
SET updated = current_timestamp()
WHERE updated IS NULL;

ALTER TABLE route
    MODIFY fastestUser VARCHAR(255) NOT NULL,
    MODIFY fastestTimestamp DATETIME NOT NULL,
    MODIFY updated TIMESTAMP NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp();
