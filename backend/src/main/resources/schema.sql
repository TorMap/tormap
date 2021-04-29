create table public.relays
(
    nickname varchar(19),
    fingerprint char(40),
    dir_address char(15),
    last_seen timestamp,
    last_changed_address_or_port timestamp,
    first_seen timestamp,
    running bool,
    country char(2),
    country_name varchar(20),
    region_name varchar(20),
    city_name varchar(20),
    latitude double precision,
    longitude double precision,
    "as" varchar(6),
    as_name varchar(20),
    consensus_weight int,
    last_restarted timestamp,
    bandwidth_rate int,
    bandwidth_burst int,
    observed_bandwidth int,
    advertised_bandwidth int,
    platform varchar(20),
    version varchar(10),
    recommended_version varchar(10),
    measured bool,
    hibernating bool
);

comment on table data.relays is 'relay information from onionoo/details query';

