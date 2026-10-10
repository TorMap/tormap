import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {makeRelays} from "../test/fixtures";
import {deferred, mockBackend, networkError} from "../test/mock-backend";
import {renderWithProviders} from "../test/render";
import {SnackbarMessage} from "../types/ui";
import {App} from "./App";

// Tiles are plain image requests to a CDN, which tests must not hit
vi.mock("react-leaflet", async (importOriginal) => ({
    ...(await importOriginal<typeof import("react-leaflet")>()),
    TileLayer: () => null,
}))

const days = ["2024-01-01", "2024-01-02"]

describe("App", () => {
    it("loads the available days and then the latest one", async () => {
        mockBackend.onGet("/relay/location/days", days)
        mockBackend.onGet("/relay/location/day/2024-01-02", makeRelays(2))
        await renderWithProviders(<App/>)

        await vi.waitFor(() => expect(mockBackend.requested).toEqual([
            "GET /relay/location/days",
            "GET /relay/location/day/2024-01-02",
        ]))
    })

    it("shows a loading animation until the days are available", async () => {
        const answer = deferred<string[]>()
        mockBackend.onGet("/relay/location/days", () => answer.promise)
        mockBackend.onGet("/relay/location/day/2024-01-02", [])
        await renderWithProviders(<App/>)
        await expect.element(page.getByRole("progressbar")).toBeVisible()

        answer.resolve(days)
        await expect.element(page.getByRole("progressbar")).not.toBeInTheDocument()
    })

    it("offers a retry when the days can not be loaded", async () => {
        let attempts = 0
        mockBackend.onGet("/relay/location/days", () => ++attempts === 1 ? networkError() : days)
        mockBackend.onGet("/relay/location/day/2024-01-02", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).toBeVisible()
        await page.getByRole("button", {name: "Retry"}).click()

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).not.toBeInTheDocument()
        await vi.waitFor(() => expect(mockBackend.requested).toContain("GET /relay/location/day/2024-01-02"))
        expect(attempts).toBe(2)
    })

    it("credits the map sources", async () => {
        mockBackend.onGet("/relay/location/days", [])
        await renderWithProviders(<App/>)

        for (const name of ["Leaflet", "OpenStreetMap", "CARTO"]) {
            await expect.element(page.getByRole("link", {name})).toBeVisible()
        }
    })

    it("shows the settings button on small screens", async () => {
        await page.viewport(500, 800)
        mockBackend.onGet("/relay/location/days", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByLabelText("more settings")).toBeVisible()
    })

    it("shows the full overlay on large screens", async () => {
        await page.viewport(1400, 900)
        mockBackend.onGet("/relay/location/days", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByRole("slider")).toBeVisible()
        await expect.element(page.getByLabelText("more settings")).not.toBeInTheDocument()
    })
})
