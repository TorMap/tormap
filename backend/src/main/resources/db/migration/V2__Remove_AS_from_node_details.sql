alter table NODE_DETAILS
    drop column if exists AUTONOMOUS_SYSTEM_NAME;
alter table NODE_DETAILS
    drop column if exists AUTONOMOUS_SYSTEM_NUMBER;
alter table NODE_DETAILS add column if not exists ADDRESS_NUMBER BIGINT after ADDRESS;
drop index if exists AUTONOMOUSSYSTEMNUMBER_NUMBER_INDEX;

delete from DESCRIPTORS_FILE where TYPE = 1;
truncate table NODE_DETAILS;
