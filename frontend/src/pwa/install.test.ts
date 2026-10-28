import {afterEach, describe, expect, it, vi} from "vitest";

import {listenForInstallPrompt, promptInstall} from "./install";
import {getPwaState, setPwaState} from "./pwa-state";

function installPromptEvent() {
    const event = new Event("beforeinstallprompt", {cancelable: true})
    return Object.assign(event, {prompt: vi.fn(async () => ({outcome: "accepted" as const}))})
}

describe("install prompt", () => {
    afterEach(() => setPwaState({installPrompt: undefined}))

    it("keeps the browser's prompt instead of showing the browser's banner", () => {
        const target = new EventTarget()
        listenForInstallPrompt(target as Window)
        const event = installPromptEvent()
        target.dispatchEvent(event)

        expect(event.defaultPrevented).toBe(true)
        expect(getPwaState().installPrompt).toBe(event)
    })

    it("forgets the prompt once the app is installed", () => {
        const target = new EventTarget()
        listenForInstallPrompt(target as Window)
        target.dispatchEvent(installPromptEvent())
        target.dispatchEvent(new Event("appinstalled"))

        expect(getPwaState().installPrompt).toBeUndefined()
    })

    it("shows the prompt once", async () => {
        const event = installPromptEvent()
        setPwaState({installPrompt: event})

        await promptInstall()
        await promptInstall()

        expect(event.prompt).toHaveBeenCalledTimes(1)
        expect(getPwaState().installPrompt).toBeUndefined()
    })
})
