# TorMap

This project visualizes current and past public Tor relays on a world map. The backend regularly downloads descriptors
from [TorProject Archive](https://metrics.torproject.org/collector/archive/) and saves a processed version in a
PostgreSQL database. The frontend displays the data on a world map and allows to filter and search for relays.

## Development

The project is split into a `backend` and `frontend` part. The backend is written in Kotlin and the frontend in TypeScript.
You can work on either part independently:
- [Backend](backend/README.md)
- [Frontend](frontend/README.md)

## Releases

We use [Semantic Versioning](https://semver.org/) and try to keep the frontend,
backend, GitHub and Docker tags consistent. Releases can be found at:

- https://github.com/TorMap/tormap/releases
- https://hub.docker.com/u/tormap
