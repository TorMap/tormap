# Roadmap

What TorMap plans for the next year. Priorities can change; open issues show the current state.

- **Keep the data current:** import new relay descriptors and user estimates hourly, and monthly IP lookup data from
  [TorMap/ip-lookup-data](https://github.com/TorMap/ip-lookup-data).
- **More accurate history:** look up each relay's location and autonomous system with the IP data of its own month
  ([#684](https://github.com/TorMap/tormap/issues/684)).
- **Stable public API:** keep API v1 (`api/openapi-v1.json`) backward compatible and extend the `tormap` CLI on top of it.
- **Security and supply chain:** keep dependencies, base images and scanners up to date, and keep the practices of the
  [OpenSSF Best Practices badge](https://www.bestpractices.dev/projects/17152).

Not planned: user accounts, tracking or ads.
