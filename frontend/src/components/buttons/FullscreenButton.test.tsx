import {afterEach, describe, expect, it} from "vitest";
import {page, userEvent} from "vitest/browser";

import {renderWithProviders} from "../../test/render";
import {KeyboardShortcuts} from "../keyboard/KeyboardShortcuts";
import {FullscreenButton} from "./FullscreenButton";

describe("FullscreenButton", () => {
    afterEach(async () => {
        if (document.fullscreenElement) await document.exitFullscreen()
    })

    it("enters and leaves full screen", async () => {
        await renderWithProviders(<FullscreenButton/>)

        await page.getByRole("button", {name: "Full screen"}).click()
        await expect.poll(() => document.fullscreenElement).toBe(document.documentElement)

        await page.getByRole("button", {name: "Exit full screen"}).click()
        await expect.poll(() => document.fullscreenElement).toBeNull()
        await expect.element(page.getByRole("button", {name: "Full screen"})).toBeInTheDocument()
    })

    it("toggles full screen with the F key, but not while typing", async () => {
        await renderWithProviders(<>
            <KeyboardShortcuts/>
            <FullscreenButton/>
            <input aria-label="other field"/>
        </>)

        await page.getByRole("textbox", {name: "other field"}).click()
        await userEvent.keyboard("f")
        expect(document.fullscreenElement).toBeNull()

        ;(document.activeElement as HTMLElement).blur()
        await userEvent.keyboard("f")
        await expect.element(page.getByRole("button", {name: "Exit full screen"})).toBeInTheDocument()
        await userEvent.keyboard("F")
        await expect.element(page.getByRole("button", {name: "Full screen"})).toBeInTheDocument()
    })
})
