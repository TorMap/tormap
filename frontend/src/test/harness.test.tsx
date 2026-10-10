import {Dialog, DialogContent} from "@mui/material";
import {expect, it} from "vitest";
import {page} from "vitest/browser";
import {MapContainer} from "react-leaflet";

import {useDate} from "../context/date-context";
import {useSettings} from "../context/settings-context";
import {useStatistics} from "../context/statistics-context";
import {renderWithProviders} from "./render";

// Guards the browser-mode harness: Leaflet needs real layout and MUI portals need a real document.
it("renders a Leaflet map in a real browser", async () => {
    const screen = await page.render(
        <div style={{width: 400, height: 300}}>
            <MapContainer center={[30, 0]} zoom={3} style={{width: "100%", height: "100%"}}/>
        </div>
    )
    const container = screen.container.querySelector(".leaflet-container")
    expect(container).not.toBeNull()
    expect(container!.getBoundingClientRect().height).toBeGreaterThan(0)
})

it("renders with the app providers and settings overrides", async () => {
    const Probe = () => {
        const {settings} = useSettings()
        const {availableDays} = useDate()
        const {statistics} = useStatistics()
        return <span>{`heat=${settings.heatMap} days=${availableDays.length} families=${statistics.familyCount}`}</span>
    }
    const screen = await renderWithProviders(<Probe/>, {settings: {heatMap: true}})
    await expect.element(screen.getByText("heat=true days=0 families=0")).toBeVisible()
})

it("renders a MUI dialog through its portal", async () => {
    const screen = await page.render(
        <Dialog open><DialogContent>Hello relay</DialogContent></Dialog>
    )
    await expect.element(screen.getByText("Hello relay")).toBeVisible()
})
