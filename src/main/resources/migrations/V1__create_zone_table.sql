CREATE TABLE zone
(
    id        int PRIMARY KEY,
    name      VARCHAR(32) NOT NULL,
    latitude  DOUBLE      NOT NULL,
    longitude DOUBLE      NOT NULL,
    region    VARCHAR(32) NOT NULL,
    country   VARCHAR(32),
    UNIQUE KEY nameKey (name)
) engine = InnoDB
  default charset utf8;
