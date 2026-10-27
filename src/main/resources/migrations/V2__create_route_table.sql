CREATE TABLE route
(
    fromZone         INT  NOT NULL,
    toZone           INT  NOT NULL,
    times            TEXT NOT NULL,
    fastestUser      VARCHAR(255),
    fastestTimestamp DATETIME,
    updated          TIMESTAMP,
    PRIMARY KEY (fromZone, toZone)
) engine = InnoDB
  default charset utf8;
