import {afterEach, describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {fakeServiceWorker} from "../../test/fake-service-worker";
import {renderWithProviders} from "../../test/render";
import {AppSettings} from "./AppSettings";

describe("OfflineDataSettings", () => {
    afterEach(() => {
        vi.restoreAllMocks()
    })

    it("is hidden without a service worker", async () => {
        await renderWithProviders(<AppSettings elevation={0}/>)
        await expect.element(page.getByText("Relays must have flag")).toBeVisible()
        expect(page.getByRole("button", {name: "Offline data"}).query()).toBeNull()
    })

    it("is the last section and deletes the saved days on request", async () => {
        const {requests} = fakeServiceWorker({enabled: true, dates: ["2024-01-01", "2024-01-02"]})
        await renderWithProviders(<AppSettings elevation={0}/>)

        await page.getByRole("button", {name: "Offline data"}).click()
        const headings = Array.from(document.querySelectorAll(".MuiAccordionSummary-root"), heading => heading.textContent)
        expect(headings.at(-1)).toBe("Offline data")

        await page.getByRole("button", {name: "Delete 2 saved days"}).click()
        await expect.element(page.getByRole("button", {name: "No days saved"})).toBeDisabled()
        expect(requests).toContainEqual({type: "CLEAR_DATA_CACHE"})
    })

    it("turns keeping viewed days off and on", async () => {
        const {requests} = fakeServiceWorker({enabled: true, dates: ["2024-01-01"]})
        await renderWithProviders(<AppSettings elevation={0}/>)
        await page.getByRole("button", {name: "Offline data"}).click()

        await page.getByRole("switch", {name: "Keep viewed days"}).click()
        await expect.element(page.getByRole("switch", {name: "Keep viewed days"})).not.toBeChecked()
        expect(page.getByRole("button", {name: /saved/}).query()).toBeNull()

        await page.getByRole("switch", {name: "Keep viewed days"}).click()
        await expect.element(page.getByRole("switch", {name: "Keep viewed days"})).toBeChecked()
        expect(requests).toContainEqual({type: "SET_DATA_CACHING", enabled: false})
        expect(requests).toContainEqual({type: "SET_DATA_CACHING", enabled: true})
    })
})
