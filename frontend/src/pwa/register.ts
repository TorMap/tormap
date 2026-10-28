import type {ServiceWorkerMessage} from "../service-worker/messages";
import {setPwaState} from "./pwa-state";

let registered = false

/**
 * Registers the service worker of a production build. Called once the first map data has loaded, so the worker does
 * not compete with it. Browsers without service workers, like Tor Browser, skip this.
 */
export function startServiceWorker() {
    if (registered || !import.meta.env.PROD || !("serviceWorker" in navigator)) return
    registered = true
    registerServiceWorker(navigator.serviceWorker, () => window.location.reload()).catch(() => {
        // The app works without the worker, e.g. when the browser blocks it in private windows
    })
}

/**
 * Registers /sw.js and reports a new version through the PWA state instead of activating it, so a reload never
 * interrupts the user. Reloads once another worker takes over this page, as the old page may need files that the new
 * version no longer has.
 */
export async function registerServiceWorker(container: ServiceWorkerContainer, reload: () => void) {
    // The first worker takes over a page that runs without one, that needs no reload
    let controlled = container.controller !== null
    let reloading = false
    container.addEventListener("controllerchange", () => {
        if (controlled && !reloading) {
            reloading = true
            reload()
        }
        controlled = true
    })

    const registration = await container.register("/sw.js", {scope: "/"})
    const offerUpdate = (worker: ServiceWorker) => {
        // Without a controller, this is the first install, which does not replace a running version
        if (container.controller !== null) setPwaState({waitingWorker: worker})
    }
    if (registration.waiting) offerUpdate(registration.waiting)
    registration.addEventListener("updatefound", () => {
        const installing = registration.installing
        installing?.addEventListener("statechange", () => {
            if (installing.state === "installed") offerUpdate(installing)
        })
    })
}

/**
 * Activates the waiting version. The page reloads when it takes over.
 */
export function applyUpdate(worker: ServiceWorker) {
    worker.postMessage("SKIP_WAITING" satisfies ServiceWorkerMessage)
}
