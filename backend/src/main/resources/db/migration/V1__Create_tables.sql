create sequence HIBERNATE_SEQUENCE;

create table DESCRIPTORS_FILE
(
    FILENAME      VARCHAR(255) not null,
    TYPE          INTEGER      not null,
    LAST_MODIFIED BIGINT       not null,
    PROCESSED_AT  TIMESTAMP,
    primary key (FILENAME, TYPE)
);

create table GEO_RELAY
(
    ID               BIGINT not null
        primary key,
    COUNTRY_ISO_CODE VARCHAR(255),
    DAY              DATE,
    FINGERPRINT      VARCHAR(40),
    FLAGS            VARCHAR(255),
    LATITUDE         DECIMAL(19, 2),
    LONGITUDE        DECIMAL(19, 2),
    NODE_DETAILS_ID  BIGINT,
    NODE_FAMILY_ID   BIGINT,
    constraint FINGERPRINT_DAY_INDEX
        unique (FINGERPRINT, DAY)
);

create index DAY_INDEX
    on GEO_RELAY (DAY);

create table NODE_DETAILS
(
    ID                        BIGINT  not null
        primary key,
    ADDRESS                   VARCHAR(15),
    ALLOW_SINGLE_HOP_EXITS    BOOLEAN not null,
    BANDWIDTH_BURST           INTEGER not null,
    BANDWIDTH_OBSERVED        INTEGER not null,
    BANDWIDTH_RATE            INTEGER not null,
    CACHES_EXTRA_INFO         BOOLEAN not null,
    CIRCUIT_PROTOCOL_VERSIONS VARCHAR(255),
    CONTACT                   VARCHAR(255),
    DAY                       DATE,
    FAMILY_ENTRIES            CLOB,
    FAMILY_ID                 BIGINT,
    FINGERPRINT               VARCHAR(40),
    IS_HIBERNATING            BOOLEAN not null,
    IS_HIDDEN_SERVICE_DIR     BOOLEAN not null,
    LINK_PROTOCOL_VERSIONS    VARCHAR(255),
    MONTH                     VARCHAR(255),
    NICKNAME                  VARCHAR(19),
    PLATFORM                  VARCHAR(255),
    PROTOCOLS                 VARCHAR(255),
    TUNNELLED_DIR_SERVER      BOOLEAN not null,
    UPTIME                    BIGINT  not null,
    constraint FINGERPRINT_MONTH_INDEX
        unique (FINGERPRINT, MONTH)
);

create index NICKNAME_MONTH_INDEX
    on NODE_DETAILS (NICKNAME, MONTH);

create index FAMILYID_MONTH_INDEX
    on NODE_DETAILS (FAMILY_ID, MONTH);
