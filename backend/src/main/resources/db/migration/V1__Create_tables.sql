CREATE SEQUENCE public.hibernate_sequence INCREMENT 1 START 1 MINVALUE 1;

create table if not exists processed_file
(
    filename      varchar(255) not null,
    type          integer      not null,
    error         varchar(255),
    last_modified bigint       not null,
    processed_at  timestamp,
    primary key (filename, type)
);

create table if not exists relay_details
(
    id                        bigint  not null
        primary key,
    address                   varchar(15),
    allow_single_hop_exits    boolean not null,
    autonomous_system_name    varchar(255),
    autonomous_system_number  integer,
    bandwidth_burst           integer not null,
    bandwidth_observed        integer not null,
    bandwidth_rate            integer not null,
    caches_extra_info         boolean not null,
    circuit_protocol_versions varchar(255),
    contact                   varchar(255),
    day                       date,
    family_entries            text,
    family_id                 bigint,
    fingerprint               char(40),
    is_hibernating            boolean not null,
    is_hidden_service_dir     boolean not null,
    link_protocol_versions    varchar(255),
    month                     char(7),
    nickname                  varchar(19),
    platform                  varchar(255),
    protocols                 varchar(255),
    tunnelled_dir_server      boolean not null,
    uptime                    bigint,
    constraint ukdj8s83cn1fwmfuojb7gmdqjpb
        unique (month, fingerprint)
);

create index if not exists idxcq3lhp78mbmub50no34jx9yud
    on relay_details (family_id);

create table if not exists relay_location
(
    id           bigint not null
        primary key,
    country_code char(2),
    day          date,
    fingerprint  char(40),
    flags        varchar(255),
    latitude     decimal(19, 2),
    longitude    decimal(19, 2),
    constraint uks31o09joxqgoqoolnuj89fkta
        unique (day, fingerprint)
);

create index if not exists idx7afgo3a18qk33vq1e11cfnv81
    on relay_location (day);

create table if not exists user_trace
(
    id                  bigint  not null
        primary key,
    agent_major_version varchar(255),
    country_code        char(2),
    device_class        varchar(255),
    method              integer,
    operating_system    varchar(255),
    response_status     integer not null,
    time_taken          bigint  not null,
    timestamp           timestamp,
    uri                 varchar(255)
);

create index if not exists idx24noi93pummsw06gwgfjaa8ha
    on user_trace (timestamp);

create index if not exists idxpas47vyvu0o6hkfnt39w1ofds
    on user_trace (method);

create index if not exists idx7kd9lcf6beywa50yyykkxvlfh
    on user_trace (response_status);
