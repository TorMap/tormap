create table if not exists API_TRACE
(
    TIMESTAMP       TIMESTAMP not null
        primary key,
    METHOD          INTEGER,
    RESPONSE_STATUS INTEGER   not null,
    URI             VARCHAR(255),
    USER_AGENT      VARCHAR(255)
);