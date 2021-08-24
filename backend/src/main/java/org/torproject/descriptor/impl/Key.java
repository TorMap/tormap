package org.torproject.descriptor.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * Define keys from descriptor document files
 *
 * <p>The extra-info document format is defined in the dir-spec
 * document.</p>
 *
 * @see <a href="https://github.com/torproject/torspec/blob/main/dir-spec.txt#L893">dir-spec.txt</a>
 *
 * <p>Occurence of each key is checked in the respective
 * implementation class.</p>
 */

public enum Key {

  EMPTY("the-empty-key"),
  INVALID("the-invalid-key"),

  /* crypto keys */
  CRYPTO_BEGIN("-----BEGIN"),
  CRYPTO_END("-----END"),

  /* descriptor keys (in alphabetic order) */
  A("a"),
  ACCEPT("accept"),
  ALLOW_SINGLE_HOP_EXITS("allow-single-hop-exits"),
  BANDWIDTH("bandwidth"),
  BANDWIDTH_FILE_DIGEST("bandwidth-file-digest"),
  BANDWIDTH_FILE_HEADERS("bandwidth-file-headers"),
  BANDWIDTH_WEIGHTS("bandwidth-weights"),
  BRIDGEDB_METRICS_END("bridgedb-metrics-end"),
  BRIDGEDB_METRICS_VERSION("bridgedb-metrics-version"),
  BRIDGEDB_METRIC_COUNT("bridgedb-metric-count"),
  BRIDGESTRAP_STATS_END("bridgestrap-stats-end"),
  BRIDGESTRAP_CACHED_REQUESTS("bridgestrap-cached-requests"),
  BRIDGESTRAP_TEST("bridgestrap-test"),
  BRIDGE_DISTRIBUTION_REQUEST("bridge-distribution-request"),
  BRIDGE_IPS("bridge-ips"),
  BRIDGE_IP_TRANSPORTS("bridge-ip-transports"),
  BRIDGE_IP_VERSIONS("bridge-ip-versions"),
  BRIDGE_POOL_ASSIGNMENT("bridge-pool-assignment"),
  BRIDGE_STATS_END("bridge-stats-end"),
  CACHES_EXTRA_INFO("caches-extra-info"),
  CELL_CIRCUITS_PER_DECILE("cell-circuits-per-decile"),
  CELL_PROCESSED_CELLS("cell-processed-cells"),
  CELL_QUEUED_CELLS("cell-queued-cells"),
  CELL_STATS_END("cell-stats-end"),
  CELL_TIME_IN_QUEUE("cell-time-in-queue"),
  CLIENT_DENIED_COUNT("client-denied-count"),
  CLIENT_RESTRICTED_DENIED_COUNT("client-restricted-denied-count"),
  CLIENT_SNOWFLAKE_MATCH_COUNT("client-snowflake-match-count"),
  CLIENT_UNRESTRICTED_DENIED_COUNT("client-unrestricted-denied-count"),
  CLIENT_VERSIONS("client-versions"),
  CONN_BI_DIRECT("conn-bi-direct"),
  CONSENSUS_METHOD("consensus-method"),
  CONSENSUS_METHODS("consensus-methods"),
  CONTACT("contact"),
  DIRCACHEPORT("dircacheport"),
  DIRECTORY_FOOTER("directory-footer"),
  DIRECTORY_SIGNATURE("directory-signature"),
  DIRREQ_READ_HISTORY("dirreq-read-history"),
  DIRREQ_STATS_END("dirreq-stats-end"),
  DIRREQ_V2_DIRECT_DL("dirreq-v2-direct-dl"),
  DIRREQ_V2_IPS("dirreq-v2-ips"),
  DIRREQ_V2_REQS("dirreq-v2-reqs"),
  DIRREQ_V2_RESP("dirreq-v2-resp"),
  DIRREQ_V2_SHARE("dirreq-v2-share"),
  DIRREQ_V2_TUNNELED_DL("dirreq-v2-tunneled-dl"),
  DIRREQ_V3_DIRECT_DL("dirreq-v3-direct-dl"),
  DIRREQ_V3_IPS("dirreq-v3-ips"),
  DIRREQ_V3_REQS("dirreq-v3-reqs"),
  DIRREQ_V3_RESP("dirreq-v3-resp"),
  DIRREQ_V3_SHARE("dirreq-v3-share"),
  DIRREQ_V3_TUNNELED_DL("dirreq-v3-tunneled-dl"),
  DIRREQ_WRITE_HISTORY("dirreq-write-history"),
  DIR_ADDRESS("dir-address"),
  DIR_IDENTITY_KEY("dir-identity-key"),
  DIR_KEY_CERTIFICATE_VERSION("dir-key-certificate-version"),
  DIR_KEY_CERTIFICATION("dir-key-certification"),
  DIR_KEY_CROSSCERT("dir-key-crosscert"),
  DIR_KEY_EXPIRES("dir-key-expires"),
  DIR_KEY_PUBLISHED("dir-key-published"),
  DIR_OPTIONS("dir-options"),
  DIR_SIGNING_KEY("dir-signing-key"),
  DIR_SOURCE("dir-source"),
  ENTRY_IPS("entry-ips"),
  ENTRY_STATS_END("entry-stats-end"),
  EVENTDNS("eventdns"),
  EXIT_KIBIBYTES_READ("exit-kibibytes-read"),
  EXIT_KIBIBYTES_WRITTEN("exit-kibibytes-written"),
  EXIT_STATS_END("exit-stats-end"),
  EXIT_STREAMS_OPENED("exit-streams-opened"),
  EXTRA_INFO("extra-info"),
  EXTRA_INFO_DIGEST("extra-info-digest"),
  FAMILY("family"),
  FINGERPRINT("fingerprint"),
  FLAG_THRESHOLDS("flag-thresholds"),
  FRESH_UNTIL("fresh-until"),
  GEOIP6_DB_DIGEST("geoip6-db-digest"),
  GEOIP_CLIENT_ORIGINS("geoip-client-origins"),
  GEOIP_DB_DIGEST("geoip-db-digest"),
  GEOIP_START_TIME("geoip-start-time"),
  HIBERNATING("hibernating"),
  HIDDEN_SERVICE_DIR("hidden-service-dir"),
  HIDSERV_DIR_ONIONS_SEEN("hidserv-dir-onions-seen"),
  HIDSERV_DIR_V3_ONIONS_SEEN("hidserv-dir-v3-onions-seen"),
  HIDSERV_REND_RELAYED_CELLS("hidserv-rend-relayed-cells"),
  HIDSERV_REND_V3_RELAYED_CELLS("hidserv-rend-v3-relayed-cells"),
  HIDSERV_STATS_END("hidserv-stats-end"),
  HIDSERV_V3_STATS_END("hidserv-v3-stats-end"),
  ID("id"),
  IDENTITY_ED25519("identity-ed25519"),
  IPV6_CONN_BI_DIRECT("ipv6-conn-bi-direct"),
  IPV6_POLICY("ipv6-policy"),
  IPV6_READ_HISTORY("ipv6-read-history"),
  IPV6_WRITE_HISTORY("ipv6-write-history"),
  KNOWN_FLAGS("known-flags"),
  LEGACY_DIR_KEY("legacy-dir-key"),
  LEGACY_KEY("legacy-key"),
  M("m"),
  MASTER_KEY_ED25519("master-key-ed25519"),
  NETWORK_STATUS_VERSION("network-status-version"),
  NTOR_ONION_KEY("ntor-onion-key"),
  NTOR_ONION_KEY_CROSSCERT("ntor-onion-key-crosscert"),
  ONION_KEY("onion-key"),
  ONION_KEY_CROSSCERT("onion-key-crosscert"),
  OPT("opt"),
  OR_ADDRESS("or-address"),
  OVERLOAD_FD_EXHAUSTED("overload-fd-exhausted"),
  OVERLOAD_GENERAL("overload-general"),
  OVERLOAD_RATELIMITS("overload-ratelimits"),
  P("p"),
  P6("p6"),
  PACKAGE("package"),
  PADDING_COUNTS("padding-counts"),
  PARAMS("params"),
  PLATFORM("platform"),
  PR("pr"),
  PROTO("proto"),
  PROTOCOLS("protocols"),
  PUBLISHED("published"),
  R("r"),
  READ_HISTORY("read-history"),
  RECOMMENDED_CLIENT_PROTOCOLS("recommended-client-protocols"),
  RECOMMENDED_RELAY_PROTOCOLS("recommended-relay-protocols"),
  RECOMMENDED_SOFTWARE("recommended-software"),
  REJECT("reject"),
  REQUIRED_CLIENT_PROTOCOLS("required-client-protocols"),
  REQUIRED_RELAY_PROTOCOLS("required-relay-protocols"),
  ROUTER("router"),
  ROUTER_DIGEST("router-digest"),
  ROUTER_DIGEST_SHA256("router-digest-sha256"),
  ROUTER_SIGNATURE("router-signature"),
  ROUTER_SIG_ED25519("router-sig-ed25519"),
  ROUTER_STATUS("router-status"),
  RUNNING_ROUTERS("running-routers"),
  S("s"),
  SERVER_VERSIONS("server-versions"),
  SHARED_RAND_COMMIT("shared-rand-commit"),
  SHARED_RAND_CURRENT_VALUE("shared-rand-current-value"),
  SHARED_RAND_PARTICIPATE("shared-rand-participate"),
  SHARED_RAND_PREVIOUS_VALUE("shared-rand-previous-value"),
  SIGNED_DIRECTORY("signed-directory"),
  SIGNING_KEY("signing-key"),
  SNOWFLAKE_IDLE_COUNT("snowflake-idle-count"),
  SNOWFLAKE_IPS("snowflake-ips"),
  SNOWFLAKE_IPS_BADGE("snowflake-ips-badge"),
  SNOWFLAKE_IPS_NAT_RESTRICTED("snowflake-ips-nat-restricted"),
  SNOWFLAKE_IPS_NAT_UNKNOWN("snowflake-ips-nat-unknown"),
  SNOWFLAKE_IPS_NAT_UNRESTRICTED("snowflake-ips-nat-unrestricted"),
  SNOWFLAKE_IPS_STANDALONE("snowflake-ips-standalone"),
  SNOWFLAKE_IPS_TOTAL("snowflake-ips-total"),
  SNOWFLAKE_IPS_WEBEXT("snowflake-ips-webext"),
  SNOWFLAKE_STATS_END("snowflake-stats-end"),
  TRANSPORT("transport"),
  TUNNELLED_DIR_SERVER("tunnelled-dir-server"),
  UPTIME("uptime"),
  V("v"),
  VALID_AFTER("valid-after"),
  VALID_UNTIL("valid-until"),
  VOTE_DIGEST("vote-digest"),
  VOTE_STATUS("vote-status"),
  VOTING_DELAY("voting-delay"),
  W("w"),
  WRITE_HISTORY("write-history");

  /** The keyword as it appears in descriptors. */
  public final String keyword;

  private static final Map<String, Key> keywordMap = new HashMap<>();
  static {
    for (Key key : values()) {
      keywordMap.put(key.keyword, key);
    }
    keywordMap.remove(INVALID.keyword);
    keywordMap.remove(EMPTY.keyword);
  }

  Key(String keyword) {
    this.keyword = keyword;
  }

  /** Retrieve a Key for a keyword.
   *  Returns Key.INVALID for non-existing keywords. */
  public static Key get(String keyword) {
    Key res;
    try {
      res = keywordMap.get(keyword);
    } catch (Throwable th) {
      res = INVALID;
    }
    if (null == res) {
      res = INVALID;
    }
    return res;
  }
}
