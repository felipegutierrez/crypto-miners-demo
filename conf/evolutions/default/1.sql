# --- !Ups

CREATE TABLE "rack"
(
  id           VARCHAR(255) NOT NULL UNIQUE,
  produced     FLOAT NOT NULL,
  currentHour  LONG NOT NULL
);

CREATE TABLE "gpu"
(
  id           VARCHAR(255) NOT NULL UNIQUE,
  rackId       VARCHAR(255) NOT NULL,
  produced     FLOAT NOT NULL,
  installedAt  LONG NOT NULL
);

# --- !Downs

DROP TABLE "rack";

DROP TABLE "gpu";
