ALTER TABLE user_trace
    ALTER COLUMN method TYPE VARCHAR(32) USING (CASE method
                                                    WHEN 0 THEN 'GET'
                                                    WHEN 1 THEN 'HEAD'
                                                    WHEN 2 THEN 'POST'
                                                    WHEN 3 THEN 'PUT'
                                                    WHEN 4 THEN 'DELETE'
                                                    WHEN 5 THEN 'CONNECT'
                                                    WHEN 6 THEN 'OPTIONS'
                                                    WHEN 7 THEN 'TRACE'
                                                    ELSE 'PATCH'
        END);

ALTER TABLE processed_file
    ALTER COLUMN type TYPE VARCHAR(32) USING (CASE type
                                                  WHEN 0 THEN 'ARCHIVE_RELAY_CONSENSUS'
                                                  WHEN 1 THEN 'ARCHIVE_RELAY_SERVER'
                                                  WHEN 2 THEN 'RECENT_RELAY_CONSENSUS'
                                                  ELSE 'RECENT_RELAY_SERVER'
        END);

CREATE OR REPLACE FUNCTION relay_location_type_to_numeric_string(flags VARCHAR)
    RETURNS VARCHAR
    IMMUTABLE AS
$$
SELECT array_agg(
           CASE flag
               WHEN 'Valid' THEN 0
               WHEN 'Named' THEN 1
               WHEN 'Unnamed' THEN 2
               WHEN 'Running' THEN 3
               WHEN 'Stable' THEN 4
               WHEN 'Exit' THEN 5
               WHEN 'Fast' THEN 6
               WHEN 'Guard' THEN 7
               WHEN 'Authority' THEN 8
               WHEN 'V2Dir' THEN 9
               WHEN 'HSDir' THEN 10
               WHEN 'NoEdConsensus' THEN 11
               WHEN 'StaleDesc' THEN 12
               WHEN 'Sybil' THEN 13
               ELSE 14
               END
           )
FROM unnest(string_to_array(flags, ',')) AS t(flag)
$$ LANGUAGE SQL;

ALTER TABLE relay_location
    ADD COLUMN flags_numeric VARCHAR(32) GENERATED ALWAYS AS (relay_location_type_to_numeric_string(flags)) STORED;
