-- Estimated number of Tor relay users per country and day, as published by Tor Metrics
-- (https://metrics.torproject.org/userstats-relay-country.csv).
-- lower and upper are the bounds of the estimate (not available for early days),
-- frac is the percentage of the network that contributed to the estimate of the day.
create table if not exists country_usage
(
    day          date     not null,
    country_code char(2)  not null,
    users        integer  not null,
    lower        integer,
    upper        integer,
    frac         smallint not null,
    primary key (day, country_code)
);
