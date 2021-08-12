CREATE TABLE IF NOT EXISTS IP_LOOKUP_AS
(
    ip_from bigint       NOT NULL,
    ip_to   bigint       NOT NULL,
    cidr    varchar(43)  NOT NULL,
    asn     varchar(10)  NOT NULL,
    as      varchar(256) NOT NULL,
    PRIMARY KEY (ip_from, ip_to)
) AS
SELECT *
FROM CSVREAD('backend/csv/IP2LOCATION-LITE-ASN/IP2LOCATION-LITE-ASN.CSV');
