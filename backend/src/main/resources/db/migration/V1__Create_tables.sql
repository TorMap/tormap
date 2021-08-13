create sequence if not exists HIBERNATE_SEQUENCE;

create table if not exists descriptors_file
(
    filename      VARCHAR(255) not null,
    type          INTEGER      not null,
    last_modified BIGINT       not null,
    processed_at  TIMESTAMP,
    primary key (filename, type)
);

create table if not exists geo_relay
(
    id           BIGINT not null
        primary key,
    country_code VARCHAR(2),
    day          DATE,
    fingerprint  VARCHAR(40),
    flags        VARCHAR(255),
    latitude     DECIMAL(6, 4),
    longitude    DECIMAL(7, 4),
    constraint fingerprint_day_index
        unique (fingerprint, day)
);

create index if not exists day_index
    on geo_relay (day);

create table if not exists node_details
(
    id                        BIGINT  not null
        primary key,
    address                   VARCHAR(15),
    address_number            BIGINT,
    allow_single_hop_exits    BOOLEAN not null,
    autonomous_system_name    varchar(255),
    autonomous_system_number  varchar(10),
    bandwidth_burst           INTEGER not null,
    bandwidth_observed        INTEGER not null,
    bandwidth_rate            INTEGER not null,
    caches_extra_info         BOOLEAN not null,
    circuit_protocol_versions VARCHAR(255),
    contact                   VARCHAR(255),
    day                       DATE,
    family_entries            CLOB,
    family_id                 BIGINT,
    fingerprint               VARCHAR(40),
    is_hibernating            BOOLEAN not null,
    is_hidden_service_dir     BOOLEAN not null,
    link_protocol_versions    VARCHAR(255),
    month                     VARCHAR(255),
    nickname                  VARCHAR(19),
    platform                  VARCHAR(255),
    protocols                 VARCHAR(255),
    tunnelled_dir_server      BOOLEAN not null,
    uptime                    BIGINT,
    constraint fingerprint_month_index
        unique (fingerprint, month)
);

create index if not exists nickname_month_index
    on node_details (nickname, month);

create index if not exists family_index
    on node_details (family_id);

CREATE TABLE IF NOT EXISTS autonomous_system
(
    ip_from bigint       NOT NULL,
    ip_to   bigint       NOT NULL,
    cidr    varchar(43)  NOT NULL,
    autonomous_system_number     varchar(10)  NOT NULL,
    autonomous_system_name      varchar(255) NOT NULL,
    PRIMARY KEY (ip_from, ip_to)
);
