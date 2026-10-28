import {useSyncExternalStore} from "react";

/**
 * Whether the browser lets the page go full screen. iPhone Safari does not support the Fullscreen API for pages.
 */
export const isFullscreenSupported = (): boolean => document.fullscreenEnabled === true

/**
 * Shows the whole page in full screen, or leaves full screen if it is already shown that way
 */
export const toggleFullscreen = async (): Promise<void> => {
    try {
        if (document.fullscreenElement) await document.exitFullscreen()
        else await document.documentElement.requestFullscreen()
    } catch (error) {
        // The browser may refuse, e.g. without a user gesture or when a permission policy forbids it
        console.warn("Could not toggle full screen", error)
    }
}

const subscribe = (onChange: () => void) => {
    document.addEventListener("fullscreenchange", onChange)
    return () => document.removeEventListener("fullscreenchange", onChange)
}

/**
 * Whether the page is currently shown in full screen, also after leaving it with Escape
 */
export const useIsFullscreen = (): boolean => useSyncExternalStore(subscribe, () => document.fullscreenElement !== null)
