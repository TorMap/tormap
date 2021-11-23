drop table API_TRACE;

create table API_TRACE
(
    ID              BIGINT  not null
        primary key,
    COUNTRY_CODE    CHAR(2),
    METHOD          INTEGER,
    RESPONSE_STATUS INTEGER not null,
    TIME_TAKEN      BIGINT  not null,
    TIMESTAMP       TIMESTAMP,
    URI             VARCHAR(255),
    USER_AGENT      VARCHAR(255)
);

create index TIMESTAMP_INDEX
    on API_TRACE (TIMESTAMP);

create index METHOD_INDEX
    on API_TRACE (METHOD);

create index RESPONSESTATUS_INDEX
    on API_TRACE (RESPONSE_STATUS);
