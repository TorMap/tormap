import {afterEach, beforeEach, describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {makeApiRelays} from "../test/fixtures";
import {deferred, mockBackend, networkError} from "../test/mock-backend";
import {setPwaState} from "../pwa/pwa-state";
import {renderWithProviders} from "../test/render";
import {SnackbarMessage} from "../types/ui";
import {App} from "./App";

// Tiles are plain image requests to a CDN, which tests must not hit
vi.mock("./leaflet/map", async (importOriginal) => ({
    ...(await importOriginal<typeof import("./leaflet/map")>()),
    TileLayer: () => null,
}))

const days = ["2024-01-01", "2024-01-02"]

describe("App", () => {
    afterEach(async () => {
        if (document.fullscreenElement) await document.exitFullscreen()
        vi.restoreAllMocks()
        setPwaState({waitingWorker: undefined})
    })

    beforeEach(() => {
        // Requested by the timeline of the large overlay
        mockBackend.onGet("/v1/statistics/daily-counts", [])
    })

    it("loads the available days and then the latest one", async () => {
        mockBackend.onGet("/v1/days", days)
        mockBackend.onGet("/v1/days/2024-01-02/relays", makeApiRelays(2))
        await renderWithProviders(<App/>)

        await vi.waitFor(() => expect(mockBackend.requested).toEqual([
            "GET /v1/days",
            "GET /v1/days/2024-01-02/relays",
        ]))
    })

    it("shows only the map, the attribution and the full screen button in full screen", async () => {
        mockBackend.onGet("/v1/days", days)
        mockBackend.onGet("/v1/days/2024-01-02/relays", [])
        await renderWithProviders(<App/>)
        await expect.element(page.getByRole("button", {name: "About TorMap"})).toBeVisible()

        await page.getByRole("button", {name: "Full screen"}).click()
        // Hidden elements have no role, while the overlays stay mounted
        await expect.element(page.getByRole("button", {name: "About TorMap"})).not.toBeInTheDocument()
        await expect.element(page.getByRole("link", {name: "OpenStreetMap"})).toBeVisible()

        await page.getByRole("button", {name: "Exit full screen"}).click()
        await expect.element(page.getByRole("button", {name: "About TorMap"})).toBeVisible()
    })

    it("shows a loading animation until the days are available", async () => {
        const answer = deferred<string[]>()
        mockBackend.onGet("/v1/days", () => answer.promise)
        mockBackend.onGet("/v1/days/2024-01-02/relays", [])
        await renderWithProviders(<App/>)
        await expect.element(page.getByRole("progressbar")).toBeVisible()

        answer.resolve(days)
        await expect.element(page.getByRole("progressbar")).not.toBeInTheDocument()
    })

    it("offers a retry when the days can not be loaded", async () => {
        let attempts = 0
        mockBackend.onGet("/v1/days", () => ++attempts === 1 ? networkError() : days)
        mockBackend.onGet("/v1/days/2024-01-02/relays", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).toBeVisible()
        await page.getByRole("button", {name: "Retry"}).click()

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).not.toBeInTheDocument()
        await vi.waitFor(() => expect(mockBackend.requested).toContain("GET /v1/days/2024-01-02/relays"))
        expect(attempts).toBe(2)
    })

    it("waits until the browser is online again instead of offering a retry", async () => {
        const onLine = vi.spyOn(Navigator.prototype, "onLine", "get").mockReturnValue(false)
        let attempts = 0
        mockBackend.onGet("/v1/days", () => ++attempts === 1 ? networkError() : days)
        mockBackend.onGet("/v1/days/2024-01-02/relays", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByText(SnackbarMessage.Offline)).toBeVisible()
        expect(page.getByRole("button", {name: "Retry"}).query()).toBeNull()
        expect(attempts).toBe(1)

        onLine.mockReturnValue(true)
        window.dispatchEvent(new Event("online"))

        await expect.element(page.getByText(SnackbarMessage.Offline)).not.toBeInTheDocument()
        await vi.waitFor(() => expect(mockBackend.requested).toContain("GET /v1/days/2024-01-02/relays"))
        expect(attempts).toBe(2)
    })

    it("offers a new version until the user reloads", async () => {
        mockBackend.onGet("/v1/days", [])
        const worker = {postMessage: vi.fn()} as unknown as ServiceWorker
        await renderWithProviders(<App/>)

        setPwaState({waitingWorker: worker})
        await expect.element(page.getByText(SnackbarMessage.UpdateAvailable)).toBeVisible()
        await page.getByRole("button", {name: "Reload"}).click()

        expect(worker.postMessage).toHaveBeenCalledWith("SKIP_WAITING")
    })

    it("lets the user dismiss the new version notice", async () => {
        mockBackend.onGet("/v1/days", [])
        const worker = {postMessage: vi.fn()} as unknown as ServiceWorker
        await renderWithProviders(<App/>)

        setPwaState({waitingWorker: worker})
        await expect.element(page.getByText(SnackbarMessage.UpdateAvailable)).toBeVisible()
        await page.getByRole("button", {name: "Dismiss"}).click()

        await expect.element(page.getByText(SnackbarMessage.UpdateAvailable)).not.toBeInTheDocument()
        expect(worker.postMessage).not.toHaveBeenCalled()
    })

    it("credits the map sources", async () => {
        mockBackend.onGet("/v1/days", [])
        await renderWithProviders(<App/>)

        for (const name of ["Leaflet", "OpenStreetMap", "CARTO"]) {
            await expect.element(page.getByRole("link", {name})).toBeVisible()
        }
    })

    it("shows the settings button on small screens", async () => {
        await page.viewport(500, 800)
        mockBackend.onGet("/v1/days", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByLabelText("more settings")).toBeVisible()
    })

    it("shows the full overlay on large screens", async () => {
        await page.viewport(1400, 900)
        mockBackend.onGet("/v1/days", [])
        await renderWithProviders(<App/>)

        await expect.element(page.getByRole("slider")).toBeVisible()
        await expect.element(page.getByLabelText("more settings")).not.toBeInTheDocument()
    })
})
