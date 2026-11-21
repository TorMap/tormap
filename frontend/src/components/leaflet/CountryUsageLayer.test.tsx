import {useAtom} from "jotai";
import {FunctionComponent, useEffect} from "react";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {CountryUsageDto} from "../../dto/relay";
import {mockBackend, networkError, reply} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {SnackbarMessage} from "../../types/ui";
import {countryUsageAtom} from "./country-usage-atoms";
import {CountryUsageLayer} from "./CountryUsageLayer";
import {MapContainer} from "./map";

const Day: FunctionComponent<{ day: string }> = ({day}) => {
    const {setAvailableDays} = useDate()
    useEffect(() => setAvailableDays([day]), [day, setAvailableDays])
    return null
}

const State = () => {
    const [countryUsage] = useAtom(countryUsageAtom)
    return <output aria-label="usage">{countryUsage ? `${countryUsage.day}:${countryUsage.usage.length}` : "none"}</output>
}

const renderLayer = (enabled: boolean, day = "2024-03-05") => renderWithProviders(
    <>
        <Day day={day}/>
        <MapContainer center={[30, 0]} zoom={3} style={{width: 600, height: 400}}>
            <CountryUsageLayer/>
        </MapContainer>
        <State/>
    </>,
    {settings: {showCountryUsage: enabled}}
)

const usage: CountryUsageDto[] = [
    {country: "DE", users: 1000, lower: 900, upper: 1100, frac: 70},
    {country: "FR", users: 500, lower: null, upper: null, frac: 70},
]

describe("CountryUsageLayer", () => {
    it("does not request anything while disabled", async () => {
        await renderLayer(false)
        await expect.element(page.getByRole("status", {name: "usage"})).toHaveTextContent("none")
        expect(mockBackend.requested).toEqual([])
    })

    it("loads the estimates of the selected day", async () => {
        mockBackend.onGet("/relay/country-usage/day/2024-03-05", usage)
        await renderLayer(true)
        await expect.element(page.getByRole("status", {name: "usage"})).toHaveTextContent("2024-03-05:2")
        expect(mockBackend.requested).toEqual(["GET /relay/country-usage/day/2024-03-05"])
    })

    it("keeps an empty list for days without estimates", async () => {
        mockBackend.onGet("/relay/country-usage/day/2008-01-01", [])
        await renderLayer(true, "2008-01-01")
        await expect.element(page.getByRole("status", {name: "usage"})).toHaveTextContent("2008-01-01:0")
    })

    it.each([
        ["a backend that does not know the endpoint", () => reply(404, {error: "Not Found"})],
        ["a lost connection", () => networkError()],
    ])("tells the user about %s", async (_name, answer) => {
        mockBackend.onGet("/relay/country-usage/day/2024-03-05", answer)
        await renderLayer(true)
        await expect.element(page.getByText(SnackbarMessage.CountryUsageUnavailable)).toBeVisible()
        await expect.element(page.getByRole("status", {name: "usage"})).toHaveTextContent("none")
    })

    it("ignores a response that is not a list", async () => {
        mockBackend.onGet("/relay/country-usage/day/2024-03-05", "<html>not the api</html>")
        await renderLayer(true)
        await expect.poll(() => mockBackend.requested.length).toBe(1)
        await expect.element(page.getByRole("status", {name: "usage"})).toHaveTextContent("none")
    })

    it("colors the countries with an estimate on the map", async () => {
        mockBackend.onGet("/relay/country-usage/day/2024-03-05", usage)
        await renderLayer(true)
        await expect.element(page.getByRole("status", {name: "usage"})).toHaveTextContent("2024-03-05:2")
        // Only the two countries with an estimate are drawn, in their own pane below the markers
        await expect.poll(() => document.querySelectorAll(".leaflet-countryUsage-pane path").length).toBe(2)
        const pane = document.querySelector<HTMLElement>(".leaflet-countryUsage-pane")!
        expect(pane.style.zIndex).toBe("350")
    })

    it("removes the colors when the estimates are switched off", async () => {
        mockBackend.onGet("/relay/country-usage/day/2024-03-05", usage)
        const screen = await renderLayer(true)
        await expect.poll(() => document.querySelectorAll(".leaflet-countryUsage-pane path").length).toBe(2)
        await screen.unmount()
        expect(document.querySelectorAll(".leaflet-countryUsage-pane path").length).toBe(0)
    })
})
