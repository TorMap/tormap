import {afterEach, describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {BeforeInstallPromptEvent, setPwaState} from "../../pwa/pwa-state";
import {renderWithProviders} from "../../test/render";
import {InstallAppButton} from "./InstallAppButton";

describe("InstallAppButton", () => {
    afterEach(() => setPwaState({installPrompt: undefined}))

    it("is hidden while the browser does not offer installing", async () => {
        await renderWithProviders(<InstallAppButton/>)
        expect(page.getByRole("button", {name: "Install TorMap as an app"}).query()).toBeNull()
    })

    it("installs the app on request and disappears", async () => {
        const prompt = vi.fn(async () => ({outcome: "accepted" as const}))
        setPwaState({installPrompt: Object.assign(new Event("beforeinstallprompt"), {prompt}) as BeforeInstallPromptEvent})
        await renderWithProviders(<InstallAppButton/>)

        await page.getByRole("button", {name: "Install TorMap as an app"}).click()

        expect(prompt).toHaveBeenCalledTimes(1)
        await expect.element(page.getByRole("button", {name: "Install TorMap as an app"})).not.toBeInTheDocument()
    })
})
