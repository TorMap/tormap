import {afterEach, describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {BeforeInstallPromptEvent, setPwaState} from "../../pwa/pwa-state";
import {renderWithProviders} from "../../test/render";
import {AppSettings} from "./AppSettings";

describe("AppInstallSettings", () => {
    afterEach(() => setPwaState({installPrompt: undefined}))

    it("is hidden while the browser does not offer installing", async () => {
        await renderWithProviders(<AppSettings elevation={0}/>)
        await expect.element(page.getByText("Relays must have flag")).toBeVisible()
        expect(page.getByRole("button", {name: "App"}).query()).toBeNull()
    })

    it("is the last section and installs the app on request", async () => {
        const prompt = vi.fn(async () => ({outcome: "accepted" as const}))
        setPwaState({installPrompt: Object.assign(new Event("beforeinstallprompt"), {prompt}) as BeforeInstallPromptEvent})
        await renderWithProviders(<AppSettings elevation={0}/>)

        const headings = Array.from(document.querySelectorAll(".MuiAccordionSummary-root"), heading => heading.textContent)
        expect(headings.at(-1)).toBe("App")

        await page.getByRole("button", {name: "App"}).click()
        await page.getByRole("button", {name: "Install app"}).click()
        expect(prompt).toHaveBeenCalledTimes(1)
        await expect.element(page.getByRole("button", {name: "App"})).not.toBeInTheDocument()
    })
})
