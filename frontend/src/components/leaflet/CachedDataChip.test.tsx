import {afterEach, describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {renderWithProviders} from "../../test/render";
import {CachedDataChip} from "./CachedDataChip";

describe("CachedDataChip", () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it("is hidden for data from the network", async () => {
        const screen = await renderWithProviders(<CachedDataChip/>)
        expect(screen.container.textContent).toBe("")
    })

    it("tells when the shown copy was downloaded", async () => {
        const cachedAt = new Date(2026, 9, 28, 20, 31).toISOString()
        await renderWithProviders(<CachedDataChip cachedAt={cachedAt}/>)
        await expect.element(page.getByText("Saved 2026-10-28 20:31")).toBeVisible()
    })

    it("tells that the browser is offline", async () => {
        vi.spyOn(Navigator.prototype, "onLine", "get").mockReturnValue(false)
        await renderWithProviders(<CachedDataChip cachedAt={new Date(2026, 9, 28, 20, 31).toISOString()}/>)
        await expect.element(page.getByText("Offline · saved 2026-10-28 20:31")).toBeVisible()
    })
})
