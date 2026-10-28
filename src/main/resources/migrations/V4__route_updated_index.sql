-- Lets TakeEventManager find the latest updated timestamp at startup without a full table scan
ALTER TABLE route
    ADD KEY updated (updated);
