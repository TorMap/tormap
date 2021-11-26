create sequence if not exists HIBERNATE_SEQUENCE;

create table if not exists PROCESSED_FILE
(
    FILENAME      VARCHAR(255) not null,
    TYPE          INTEGER      not null,
    ERROR         VARCHAR(255),
    LAST_MODIFIED BIGINT       not null,
    PROCESSED_AT  TIMESTAMP,
    primary key (FILENAME, TYPE)
);

create table if not exists RELAY_DETAILS
(
    ID                        BIGINT  not null
        primary key,
    ADDRESS                   VARCHAR(15),
    ALLOW_SINGLE_HOP_EXITS    BOOLEAN not null,
    AUTONOMOUS_SYSTEM_NAME    VARCHAR(255),
    AUTONOMOUS_SYSTEM_NUMBER  INTEGER,
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
    constraint UKDJ8S83CN1FWMFUOJB7GMDQJPB
        unique (MONTH, FINGERPRINT)
);

create index if not exists IDXCQ3LHP78MBMUB50NO34JX9YUD
    on RELAY_DETAILS (FAMILY_ID);

create table if not exists RELAY_LOCATION
(
    ID           BIGINT not null
        primary key,
    COUNTRY_CODE CHAR(2),
    DAY          DATE,
    FINGERPRINT  CHAR(40),
    FLAGS        VARCHAR(255),
    LATITUDE     DECIMAL(19, 2),
    LONGITUDE    DECIMAL(19, 2),
    constraint UKS31O09JOXQGOQOOLNUJ89FKTA
        unique (DAY, FINGERPRINT)
);

create index if not exists IDX7AFGO3A18QK33VQ1E11CFNV81
    on RELAY_LOCATION (DAY);

create table if not exists USER_TRACE
(
    ID                  BIGINT  not null
        primary key,
    AGENT_MAJOR_VERSION VARCHAR(255),
    COUNTRY_CODE        CHAR(2),
    DEVICE_CLASS        VARCHAR(255),
    METHOD              INTEGER,
    OPERATING_SYSTEM    VARCHAR(255),
    RESPONSE_STATUS     INTEGER not null,
    TIME_TAKEN          BIGINT  not null,
    TIMESTAMP           TIMESTAMP,
    URI                 VARCHAR(255)
);

create index if not exists IDX24NOI93PUMMSW06GWGFJAA8HA
    on USER_TRACE (TIMESTAMP);

create index if not exists IDXPAS47VYVU0O6HKFNT39W1OFDS
    on USER_TRACE (METHOD);

create index if not exists IDX7KD9LCF6BEYWA50YYYKKXVLFH
    on USER_TRACE (RESPONSE_STATUS);
