-- consensus_weight: the "w Bandwidth=" value of the relay's consensus entry (what clients use for path selection)
-- guard_weight: the share of it that clients use for choosing the relay as entry guard (NULL if the consensus has no bandwidth weights)
alter table if exists relay_location add column if not exists consensus_weight bigint;
alter table if exists relay_location add column if not exists guard_weight bigint;

-- Existing relay locations have no weights yet. Marking all consensus files (ARCHIVE_RELAY_CONSENSUS = 0,
-- RECENT_RELAY_CONSENSUS = 2) as unprocessed makes the backend read them once more and fill in the missing weights.
-- This only runs once with this migration and takes about as long as the initial import.
delete from processed_file where type in (0, 2);
