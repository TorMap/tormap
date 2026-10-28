/**
 * Whether the browser knows it has no network. `navigator.onLine` is only reliable when false: true can also mean a
 * network without internet access, then requests fail as before.
 */
export const isOffline = () => !navigator.onLine

/**
 * Identifies the offline snackbar, so the app and the map show it once and close it together
 */
export const offlineSnackbarKey = "offline"

/**
 * Calls back once the browser is online again, instead of retrying in a loop. Returns a function that cancels it.
 */
export function onceOnline(callback: () => void): () => void {
    window.addEventListener("online", callback, {once: true})
    return () => window.removeEventListener("online", callback)
}
