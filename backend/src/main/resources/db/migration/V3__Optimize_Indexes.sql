alter table NODE_DETAILS alter column FINGERPRINT CHAR(40);
alter table NODE_DETAILS alter column MONTH CHAR(7);

drop index if exists NICKNAME_MONTH_INDEX;
drop index if exists FAMILY_INDEX;
drop index if exists AUTONOMOUSSYSTEMNUMBER_NUMBER_INDEX;

alter table NODE_DETAILS drop constraint if exists FINGERPRINT_MONTH_INDEX;
alter table NODE_DETAILS add constraint if not exists MONTH_FINGERPRINT_INDEX UNIQUE(MONTH, FINGERPRINT);

alter table GEO_RELAY alter column FINGERPRINT CHAR(40);
alter table GEO_RELAY alter column COUNTRY_CODE CHAR(2);

alter table GEO_RELAY drop constraint if exists FINGERPRINT_DAY_INDEX;
alter table GEO_RELAY add constraint if not exists DAY_FINGERPRINT_INDEX UNIQUE(DAY, FINGERPRINT);

drop index if exists DAY_INDEX;

alter table DESCRIPTORS_FILE drop primary key;
alter table DESCRIPTORS_FILE add primary key(TYPE, FILENAME);


