create sequence if not exists HIBERNATE_SEQUENCE;

create table if not exists DESCRIPTORS_FILE
(
    FILENAME      VARCHAR(255) not null,
    TYPE          INTEGER      not null,
    LAST_MODIFIED BIGINT       not null,
    PROCESSED_AT  TIMESTAMP,
    primary key (TYPE, FILENAME)
);

create table if not exists GEO_RELAY
(
    ID           BIGINT not null
        primary key,
    COUNTRY_CODE CHAR(2),
    DAY          DATE,
    FINGERPRINT  CHAR(40),
    FLAGS        VARCHAR(255),
    LATITUDE     DECIMAL(6, 4),
    LONGITUDE    DECIMAL(7, 4),
    constraint DAY_FINGERPRINT_INDEX
        unique (DAY, FINGERPRINT)
);

create table if not exists NODE_DETAILS
(
    ID                        BIGINT  not null
        primary key,
    ADDRESS                   VARCHAR(15),
    ADDRESS_NUMBER            BIGINT,
    ALLOW_SINGLE_HOP_EXITS    BOOLEAN not null,
    AUTONOMOUS_SYSTEM_NAME    VARCHAR(255),
    AUTONOMOUS_SYSTEM_NUMBER  VARCHAR(10),
    BANDWIDTH_BURST           INTEGER not null,
    BANDWIDTH_OBSERVED        INTEGER not null,
    BANDWIDTH_RATE            INTEGER not null,
    CACHES_EXTRA_INFO         BOOLEAN not null,
    CIRCUIT_PROTOCOL_VERSIONS VARCHAR(255),
    CONTACT                   VARCHAR(255),
    DAY                       DATE,
    FAMILY_ENTRIES            CLOB,
    FAMILY_ID                 BIGINT,
    FINGERPRINT               CHAR(40),
    IS_HIBERNATING            BOOLEAN not null,
    IS_HIDDEN_SERVICE_DIR     BOOLEAN not null,
    LINK_PROTOCOL_VERSIONS    VARCHAR(255),
    MONTH                     CHAR(7),
    NICKNAME                  VARCHAR(19),
    PLATFORM                  VARCHAR(255),
    PROTOCOLS                 VARCHAR(255),
    TUNNELLED_DIR_SERVER      BOOLEAN not null,
    UPTIME                    BIGINT,
    constraint MONTH_FINGERPRINT_INDEX
        unique (MONTH, FINGERPRINT)
);

create index if not exists FAMILYID_INDEX
    on NODE_DETAILS (FAMILY_ID);

create table if not exists AUTONOMOUS_SYSTEM
(
    IP_FROM                  BIGINT       not null,
    IP_TO                    BIGINT       not null,
    CIDR                     VARCHAR(43)  not null,
    AUTONOMOUS_SYSTEM_NUMBER VARCHAR(10)  not null,
    AUTONOMOUS_SYSTEM_NAME   VARCHAR(255) not null,
    primary key (IP_FROM, IP_TO)
);
