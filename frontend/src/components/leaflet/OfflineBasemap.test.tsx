import {describe, expect, it} from "vitest";

import {renderWithProviders} from "../../test/render";
import {MapContainer} from "./map";
import {OfflineBasemap} from "./OfflineBasemap";

const borders = () => document.querySelectorAll(".leaflet-offlineBasemap-pane path")

describe("OfflineBasemap", () => {
    it("draws the bundled country borders below the relays", async () => {
        await renderWithProviders(
            <MapContainer center={[30, 0]} zoom={2} style={{width: 600, height: 400}}>
                <OfflineBasemap/>
            </MapContainer>
        )
        await expect.poll(() => borders().length, {timeout: 5000}).toBeGreaterThan(100)
        const pane = document.querySelector<HTMLElement>(".leaflet-offlineBasemap-pane")!
        expect(Number(pane.style.zIndex)).toBeLessThan(400)
        // Purely decorative, clicks reach the map and the relays
        expect(borders()[0].classList.contains("leaflet-interactive")).toBe(false)
    })

    it("removes the borders when unmounted", async () => {
        const screen = await renderWithProviders(
            <MapContainer center={[30, 0]} zoom={2} style={{width: 600, height: 400}}>
                <OfflineBasemap/>
            </MapContainer>
        )
        await expect.poll(() => borders().length, {timeout: 5000}).toBeGreaterThan(100)
        await screen.rerender(<MapContainer center={[30, 0]} zoom={2} style={{width: 600, height: 400}}/>)
        await expect.poll(() => borders().length).toBe(0)
    })
})
