/**
 * Messages from the app to the service worker. Only the type is shared: a value imported by both would become a chunk
 * of its own, which the worker cannot load.
 */
export type ServiceWorkerMessage =
    // Activates a waiting worker after the user agreed to reload
    "SKIP_WAITING"
