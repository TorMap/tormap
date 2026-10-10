import {useAtomValue} from "jotai";
import {useEffect} from "react";
import {afterEach, describe, expect, it, vi} from "vitest";

import {useDate} from "../../context/date-context";
import {makeRelays} from "../../test/fixtures";
import {deferred, mockBackend, networkError} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {SnackbarMessage} from "../../types/ui";
import {filteredRelaysAtom} from "./LeafletLayers";
import {LeafletWorldMap} from "./LeafletWorldMap";

import {page} from "vitest/browser";

// Tiles are plain image requests to a CDN, which tests must not hit
vi.mock("react-leaflet", async (importOriginal) => ({
    ...(await importOriginal<typeof import("react-leaflet")>()),
    TileLayer: () => null,
}))

const days = ["2024-01-01", "2024-01-02"]

/**
 * Provides the available days like `App` does, lets the test pick another one, and shows what the map filtered.
 */
const Harness = ({availableDays, setIsLoading}: { availableDays: string[], setIsLoading: (b: boolean) => void }) => {
    const {setAvailableDays, setSelectedDate} = useDate()
    const filteredRelays = useAtomValue(filteredRelaysAtom)
    useEffect(() => setAvailableDays(availableDays), [availableDays, setAvailableDays])
    return (
        <>
            <LeafletWorldMap setIsLoading={setIsLoading}/>
            {/* The map covers the whole viewport, so the controls have to sit above it */}
            <div style={{position: "fixed", top: 0, left: 0, zIndex: 10000}}>
                <button onClick={() => setSelectedDate("2024-01-01")}>pick first day</button>
                <span>{`shown=${filteredRelays.length}`}</span>
            </div>
        </>
    )
}

describe("LeafletWorldMap", () => {
    afterEach(() => {
        vi.useRealTimers()
    })

    it("loads the latest available day and draws its relays", async () => {
        mockBackend.onGet("/relay/location/day/2024-01-02", makeRelays(3))
        const setIsLoading = vi.fn()
        const screen = await renderWithProviders(<Harness availableDays={days} setIsLoading={setIsLoading}/>)

        await expect.element(screen.getByText("shown=3")).toBeVisible()
        expect(mockBackend.requested).toEqual(["GET /relay/location/day/2024-01-02"])
        expect(setIsLoading.mock.calls.map(([loading]) => loading)).toEqual([true, false])
    })

    it("does not request anything without available days", async () => {
        const screen = await renderWithProviders(<Harness availableDays={[]} setIsLoading={vi.fn()}/>)

        await expect.element(screen.getByText("shown=0")).toBeVisible()
        expect(mockBackend.requested).toEqual([])
    })

    it("tells the user when the day can not be loaded", async () => {
        mockBackend.onGet("/relay/location/day/2024-01-02", () => networkError())
        const setIsLoading = vi.fn()
        const screen = await renderWithProviders(<Harness availableDays={days} setIsLoading={setIsLoading}/>)

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).toBeVisible()
        await expect.element(screen.getByText("shown=0")).toBeVisible()
        expect(setIsLoading).toHaveBeenLastCalledWith(false)
    })

    it("ignores the answer of an outdated request", async () => {
        // The map identifies requests by timestamp, so the clock must advance between selections
        vi.useFakeTimers({toFake: ["Date"]})
        vi.setSystemTime(new Date("2024-02-01T00:00:00Z"))
        const latest = deferred<unknown>()
        const first = deferred<unknown>()
        mockBackend.onGet("/relay/location/day/2024-01-02", () => latest.promise)
        mockBackend.onGet("/relay/location/day/2024-01-01", () => first.promise)
        const screen = await renderWithProviders(<Harness availableDays={days} setIsLoading={vi.fn()}/>)
        await vi.waitFor(() => expect(mockBackend.requested).toHaveLength(1))

        // The user picks another day while the first request is still running
        vi.setSystemTime(new Date("2024-02-01T00:00:01Z"))
        await screen.getByRole("button", {name: "pick first day"}).click()
        await vi.waitFor(() => expect(mockBackend.requested).toHaveLength(2))

        first.resolve(makeRelays(1))
        await expect.element(screen.getByText("shown=1")).toBeVisible()

        latest.resolve(makeRelays(5))
        await new Promise(resolve => setTimeout(resolve, 50))
        await expect.element(screen.getByText("shown=1")).toBeVisible()
    })
})
