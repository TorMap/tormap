import {useSyncExternalStore} from "react";

/**
 * Chromium's install prompt, see https://developer.mozilla.org/en-US/docs/Web/API/BeforeInstallPromptEvent
 */
export interface BeforeInstallPromptEvent extends Event {
    prompt(): Promise<{ outcome: "accepted" | "dismissed" }>
}

export interface PwaState {
    /** A new version of the app that waits until the user agrees to reload */
    waitingWorker?: ServiceWorker
    /** Set while the browser offers to install the app */
    installPrompt?: BeforeInstallPromptEvent
}

let state: PwaState = {}
const listeners = new Set<() => void>()

/**
 * Service worker and install events arrive outside React, so their state lives in this small store.
 */
export function setPwaState(update: Partial<PwaState>) {
    state = {...state, ...update}
    listeners.forEach(listener => listener())
}

export function getPwaState(): PwaState {
    return state
}

function subscribe(listener: () => void) {
    listeners.add(listener)
    return () => {
        listeners.delete(listener)
    }
}

export function usePwaState(): PwaState {
    return useSyncExternalStore(subscribe, getPwaState)
}
