CREATE TABLE statusmelding_buffer
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    statusmelding    TEXT         NOT NULL,
    fagsystem_id     VARCHAR(255) NOT NULL,
    vedtak_id        VARCHAR(255) NOT NULL,
    status           VARCHAR(255) NOT NULL,
    lest             BOOLEAN      NOT NULL DEFAULT FALSE,
    mottatt          TIMESTAMP    NOT NULL DEFAULT NOW()
);
