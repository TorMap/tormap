import {BeforeInstallPromptEvent, getPwaState, setPwaState} from "./pwa-state";

/**
 * Keeps the browser's install prompt for the install button in the settings. Only Chromium fires the event, other
 * browsers offer installing in their own menus. Must run before the app renders, the event can fire early.
 */
export function listenForInstallPrompt(target: Window = window) {
    target.addEventListener("beforeinstallprompt", event => {
        // Prevents Chrome's own install banner on mobile, the app only offers installing in the settings
        event.preventDefault()
        setPwaState({installPrompt: event as BeforeInstallPromptEvent})
    })
    target.addEventListener("appinstalled", () => setPwaState({installPrompt: undefined}))
}

/**
 * Shows the browser's install dialog. A prompt can only be shown once, Chromium fires a new event if the user declines.
 */
export async function promptInstall() {
    const installPrompt = getPwaState().installPrompt
    if (!installPrompt) return
    setPwaState({installPrompt: undefined})
    await installPrompt.prompt()
}
