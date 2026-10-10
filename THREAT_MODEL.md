# TorMap Threat Model

Scope: this repository (`backend/`, `frontend/`, `.github/`) and the reference deployment (`backend/docker-compose.yml`,
Docker Hub images, Firebase Hosting). Review this file when changing a trust boundary, adding an endpoint, an outbound
call, a dependency source or a deployment default. Report vulnerabilities as described in [SECURITY.md](SECURITY.md).

## System overview

TorMap is a public, read-only data platform. The Spring Boot backend periodically downloads Tor relay descriptors from
Tor Project Collector, parses them with a vendored copy of metrics-lib, enriches them with offline GeoIP/ASN databases,
stores them in PostgreSQL and serves them as JSON. The React frontend (static files on Firebase Hosting or nginx) renders
the data on a map. There are no user accounts. The only privileged role is the actuator admin.

```
Browser ──HTTPS──> Frontend (static)            Tor Collector (HTTPS, index.json + tarballs)
   │                                                    │
   └──HTTP(S) /relay/** ──> Backend (Spring Boot) <─────┘ scheduled pulls (hourly / 12h)
   Operator ── Basic auth ─> /actuator/**  │  ├──> PostgreSQL
                                           │  ├──> DNS resolver (PTR + forward lookups, per request)
                                           │  └──> New Relic OTLP (optional, outbound only)
```

## Assets

| Asset                                | Why it matters                                                                  |
|--------------------------------------|---------------------------------------------------------------------------------|
| Service availability                 | The main security property: public API, scheduler, DB. Low-effort DoS matters.  |
| Data integrity                       | Correct relay, family and location data. Users draw conclusions about Tor.      |
| Actuator admin plane                 | Log file, thread dumps, recent HTTP exchanges, metrics, request mappings, cache eviction.             |
| Secrets                              | `TORMAP_ADMIN_PASSWORD(_BCRYPT)`, DB password, `NEW_RELIC_INGEST_KEY`, CI secrets (Docker Hub, Firebase). |
| Host and volume                      | `/tormap-data` (tens of GB of descriptors), log directory.                       |
| Release artifacts                    | `tormap/backend` and `tormap/frontend` images, tormap.org frontend.             |

Relay data is public by design, so confidentiality of relay data is not an asset.

## Trust boundaries and entry points

| # | Boundary                         | Input (attacker control)                                                                 | Main threats                                                   | Code |
|---|----------------------------------|------------------------------------------------------------------------------------------|----------------------------------------------------------------|------|
| 1 | Public API `/relay/**`, `/`      | Path params `id`, `day`; JSON arrays of IDs (≤ 50 000 / 5 000); headers (direct)         | Memory/CPU/DB exhaustion, cache thrash, error leakage          | `adapter/controller/`, `config/AppConfig.kt`, `config/CacheConfig.kt` |
| 2 | HTTP Basic authentication        | `Authorization` header (direct)                                                           | Brute force, password-hashing CPU cost, cleartext creds without TLS | `config/SecurityConfig.kt` |
| 3 | Admin plane `/actuator/**`       | Requests from whoever holds the admin password                                            | Info disclosure (log file, thread dumps, exchanges), cache eviction | `application.yml` `management.*` |
| 4 | Reverse proxy headers            | `Forwarded`, `X-Forwarded-*` (direct if the backend port is exposed)                      | Spoofed client IP/scheme/host in logs, redirects, exchanges    | `ForwardedHeaderFilter` in `SecurityConfig.kt` |
| 5 | Relay-published descriptor data  | Nickname, contact, platform, family entries, address (indirect: any relay operator)       | Parser edge cases, expensive family computation, stored XSS    | `service/Descriptor*`, `util/RelayFamilyUtil.kt`, vendored `org.torproject.descriptor` |
| 6 | Collector download                | `index.json` (`path`, file names, sizes, mtimes), tar/xz/bz2 archives (trusted, HTTPS)    | SSRF via index URLs, path traversal, decompression or disk bombs, hung downloads | `descriptor/index/*`, `impl/DescriptorReaderImpl.java` |
| 7 | DNS                               | PTR records and forward lookups for relay IPs (indirect: whoever controls the IP's reverse zone) | Slow lookups that tie up request threads, spoofed host names   | `service/ReverseDnsLookupService.kt` |
| 8 | Frontend rendering                | API JSON (backend-controlled, relay-derived strings)                                     | XSS through HTML sinks (map tooltips/popups, raw HTML APIs)     | `frontend/src/util/layer-construction.ts`, `components/dialogs/relay/` |
| 9 | Deployment configuration          | Env vars, Spring profile, compose port mappings (operator)                               | Exposed DB, Swagger on, no TLS, default credentials            | `docker-compose*.yml`, `application*.yml`, `build.gradle.kts` (Jib) |
| 10| CI/CD and supply chain            | Dependencies, actions, Renovate PRs, AI agent instructions (developer / upstream)         | Malicious package, secret theft, prompt injection via changelogs | `.github/`, `renovate.json`, `frontend/.yarnrc.yml`, `AGENTS.md` |

## Assumptions

- The backend runs as a single instance behind a TLS-terminating reverse proxy or CDN. The app itself serves plain HTTP
  on 8080 and does not enforce HTTPS or rate limits; both are expected from the edge.
- The `prod` Spring profile is active in production. It is what disables Swagger, enables the CSP header and sets the
  production CORS origins and cache headers. Without it the default (development) profile applies.
- Tor Collector and its TLS are trusted in normal operation. Collector compromise is in scope only for "bounded damage"
  (no code execution, no writes outside `/tormap-data`, no unbounded resource use).
- Relay operators are untrusted. Running a relay is cheap and its descriptors end up in the DB and permanently in the
  public API. Signatures on descriptors are not verified by TorMap.
- The DB is reachable only by the backend. Anyone with DB write access fully controls API output.
- Admin password holders are trusted. The actuator does not expose `env`, `heapdump`, `shutdown` or `configprops`.

## Existing controls

- `SecurityConfig`: only `/`, `/error`, `/relay/**` and static files are public. Actuator requires role `ADMIN`. With no
  admin password configured, no user exists and the actuator is unreachable. Swagger is public only when enabled.
  Stateless sessions, no form login. CSRF (double-submit cookie) for unsafe actuator methods. `SecurityRulesTest` pins
  these rules.
- Admin password is read only from the environment, plaintext or BCrypt. Never stored in config files.
- Security headers: `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, CSP `default-src 'none'` when
  Swagger is off.
- Input validation: `@Size` limits on POST ID lists, typed `Long` path variables, strict `LocalDate.parse`.
- Persistence uses Spring Data derived queries and bound parameters only, no string-built SQL/JPQL. Flyway migrations
  with `ddl-auto: validate`.
- Caches with bounded entry counts (Ehcache heap tiers). Shallow ETags only on small endpoints. `CoalesceService`
  bounds concurrent background recomputation per key.
- Ingestion: remote file names must not contain `/`, `\`, `.` or `..`, and target directories must resolve inside the
  download directory (real-path check). Downloads are written to a temp file and kept only if the size matches the
  index. Free-text descriptor fields (contact, platform, protocols) are truncated to 255 characters; family
  entries are stored untruncated.
- Reverse DNS: at most 10 PTR names verified per IP, forward results filtered to public addresses, results cached for
  6 hours with `sync = true`.
- Container runs as non-root `1000:1000` from a digest-pinned base image.
- Supply chain: Maven Central and npm registry only, Yarn install scripts disabled, 3-day npm age gate, Renovate with
  release age, actions pinned by SHA, Gradle wrapper validation, least-privilege workflow `permissions`, no
  `pull_request_target`, CodeQL weekly, Snyk PR checks, GitHub secret scanning with push protection.

## Threat scenarios

1. **API flooder (unauthenticated, low effort).** Oversized request bodies, cache-busting parameters, authentication
   attempts and requests that trigger expensive back-end work (DB queries, DNS lookups). Goal: exhaust heap, Tomcat
   threads, CPU or DB.
2. **Malicious relay operator.** Publishes descriptors with large or many family entries, odd nicknames, or a PTR zone
   with slow or many answers, to slow down family computation, tie up request threads, or inject content that the
   frontend renders.
3. **Compromised or spoofed Collector.** Serves a manipulated index (URLs, file names, sizes), oversized or endless
   files, or decompression bombs. Goal: SSRF, local file read into the pipeline, disk/heap exhaustion, poisoned
   data.
4. **Admin credential attacker.** Brute-forces HTTP Basic, sniffs it on a deployment without TLS, or finds it in
   shell history or compose files. Goal: log file, thread dumps, HTTP exchanges, cache eviction.
5. **Misconfiguration opportunist.** Scans for deployments with a reachable PostgreSQL port, the backend on plain
   HTTP, or the `prod` profile not active (Swagger and docs exposed).
6. **Supply-chain attacker.** Publishes a malicious version of a dependency or action, or plants instructions in a
   changelog that an AI reviewer acting on Renovate PRs follows.

## Out of scope or low priority

- Horizontal privilege escalation: there are no user accounts or tenants.
- Disclosure of relay data: it is public. (Disclosure of secrets, logs or internal hosts is in scope.)
- SQL injection: only bound parameters today. Becomes relevant if string-built queries are introduced.
- Volumetric network DDoS: handled at the edge, not by the application.
- Bugs in Tor itself or in Collector: report to the Tor Project.

## Severity calibration

| Severity     | Meaning in this project                                                                                                         |
|--------------|---------------------------------------------------------------------------------------------------------------------------------|
| **Critical** | Remote code execution, admin plane or DB takeover, or file writes outside `/tormap-data` without special preconditions.          |
| **High**     | Unauthenticated single-client outage of the API or scheduler, persistent data poisoning, or stored XSS on tormap.org.            |
| **Medium**   | DoS that needs sustained traffic, a relay, or a non-default but plausible deployment; secret exposure that needs another flaw.    |
| **Low**      | Defense in depth, hardening gaps, issues that need Collector compromise or admin access, or have only cosmetic impact.          |

## Review checklist for changes

- New endpoint: add it to `SecurityConfig` explicitly, extend `SecurityRulesTest`, bound input sizes (including the raw
  request body), and keep heavy work behind a cache with a bounded key space.
- New outbound call: set connect and read timeouts, cap response size, restrict schemes and hosts.
- New relay-derived field: truncate before storing, render as text only in the frontend (no Leaflet string tooltips or
  popups, no `dangerouslySetInnerHTML`).
- Deployment default change: keep the DB unexposed, the `prod` profile active and the actuator off without a password.
