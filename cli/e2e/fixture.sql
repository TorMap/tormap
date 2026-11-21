-- Relays for the end-to-end test of the CLI binary against a real backend (see run.sh and .github/workflows/cli-native.yml).
-- Flags are enum ordinals of TorRelayFlag: Valid 0, Running 3, Exit 5, Fast 6, Guard 7.
-- Addresses are loopback, so the reverse DNS lookups of the details stay local.

INSERT INTO relay_location (id, day, fingerprint, country_code, latitude, longitude, flags, consensus_weight, guard_weight) VALUES
    (900001, '2024-03-04', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'DE', 50.11, 8.68, '0, 3, 5, 6, 7', 3000, 0),
    (900002, '2024-03-04', 'BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB', 'NL', 52.37, 4.90, '0, 3, 7', 5000, 0),
    (900003, '2024-03-05', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'DE', 50.11, 8.68, '0, 3, 5, 6, 7', 3000, 0),
    (900004, '2024-03-05', 'BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB', 'NL', 52.37, 4.90, '0, 3, 7', 5000, 0),
    (900005, '2024-03-05', 'CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC', 'DE', 48.14, 11.58, '0, 3', 10, 0),
    (900006, '2024-04-10', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'DE', 50.11, 8.68, '0, 3, 5, 6, 7', 3100, 0);

INSERT INTO relay_details (id, month, day, fingerprint, nickname, address, autonomous_system_number, autonomous_system_name,
                           platform, contact, family_id, bandwidth_rate, bandwidth_burst, bandwidth_observed, uptime,
                           allow_single_hop_exits, caches_extra_info, is_hibernating, is_hidden_service_dir, tunnelled_dir_server) VALUES
    (900101, '2024-02', '2024-02-20', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'e2eAlphaOld', '127.0.0.1', 24940, 'Hetzner Online GmbH',
     'Tor 0.4.7.16 on Linux', 'old@example.org', NULL, 1000000, 2000000, 900000, 3600, false, false, false, false, true),
    (900102, '2024-03', '2024-03-05', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'e2eAlpha', '127.0.0.1', 24940, 'Hetzner Online GmbH',
     'Tor 0.4.8.10 on Linux', 'alpha@example.org', 7, 1000000, 2000000, 950000, 7200, false, false, false, false, true),
    (900103, '2024-03', '2024-03-05', 'BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB', 'e2eBravo', '127.0.0.2', 1101, 'SURFnet',
     'Tor 0.4.8.10 on Linux', NULL, 7, 1000000, 2000000, 800000, 7200, false, false, false, false, true),
    (900104, '2024-03', '2024-03-05', 'CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC', 'e2eCharlie', '127.0.0.3', NULL, NULL,
     'Tor 0.4.8.9 on FreeBSD', NULL, NULL, 100000, 200000, 50000, 60, false, false, false, false, false),
    (900105, '2024-04', '2024-04-10', 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'e2eAlpha', '127.0.0.1', 24940, 'Hetzner Online GmbH',
     'Tor 0.4.8.11 on Linux', 'alpha@example.org', NULL, 1000000, 2000000, 990000, 7200, false, false, false, false, true);

-- Tor Metrics publishes estimates 2 to 3 days late, so the latest relay day has none
INSERT INTO country_usage (day, country_code, users, lower, upper, frac) VALUES
    ('2024-03-05', 'DE', 250000, 200000, 300000, 90),
    ('2024-03-05', 'NL', 50000, NULL, NULL, 90),
    ('2024-04-08', 'DE', 260000, NULL, NULL, 88);
