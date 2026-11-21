-- Family IDs from verified family-cert entries of server descriptors ("happy families", Tor proposal 321)
alter table if exists relay_details add column if not exists family_ids text;

-- Relays publish family-cert entries since 2025-04. Process the server descriptors of these months again,
-- so the stored relays get their family IDs (see DescriptorProcessingService.processServerDescriptor).
-- 1 = DescriptorType.ARCHIVE_RELAY_SERVER, 3 = DescriptorType.RECENT_RELAY_SERVER
delete from processed_file where type = 1 and filename >= 'server-descriptors-2025-04';
delete from processed_file where type = 3;
