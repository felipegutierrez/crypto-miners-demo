# --- !Ups

CREATE TABLE "rack"
(
  id           VARCHAR(255) NOT NULL UNIQUE,
  produced     FLOAT NOT NULL,
  currentHour  TIMESTAMP NOT NULL
);

# --- !Downs

DROP TABLE "rack";


