import {FunctionComponent, useEffect, useRef} from "react";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {DailyRelayCountDto} from "../../dto/relay";
import {mockBackend, networkError} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {RelayCountTimeline} from "./RelayCountTimeline";

// 40 days with ~6000 relays and a sudden drop on day 20
const days = Array.from({length: 40}, (_, i) => new Date(Date.UTC(2024, 1, 1 + i)).toISOString().slice(0, 10))
const counts: DailyRelayCountDto[] = days.map((day, i) => {
    const total = i === 20 ? 2500 : 6000 + (i % 4) * 10
    return {day, total, exit: 100, guard: 500, other: total - 600}
})

const SelectedDate: FunctionComponent<{ available: string[] }> = ({available}) => {
    const {setAvailableDays, selectedDate} = useDate()
    const seeded = useRef(false)
    // Seed the days only once, so that a later selection is not reset
    useEffect(() => {
        if (seeded.current) return
        seeded.current = true
        setAvailableDays(available)
    }, [available, setAvailableDays])
    return <output data-testid="selected-date">{selectedDate}</output>
}

const renderTimeline = () => renderWithProviders(
    <>
        <SelectedDate available={days}/>
        <RelayCountTimeline/>
    </>
)

describe("RelayCountTimeline", () => {
    it("renders a chart and marks days with an unusual relay count", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", counts)
        await renderTimeline()

        await expect.element(page.getByRole("img", {name: /Relays per day/})).toBeVisible()
        await expect.element(page.getByTestId("relay-count-outlier")).toBeVisible()
        expect(page.getByTestId("relay-count-outlier").elements()).toHaveLength(1)
    })

    it("selects an outlier day when it is clicked", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", counts)
        await renderTimeline()

        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[39])
        await page.getByTestId("relay-count-outlier").click()
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[20])
    })

    it("renders nothing without data", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", [])
        await renderTimeline()
        await expect.element(page.getByRole("img", {name: /Relays per day/})).not.toBeInTheDocument()
    })

    it("stays quiet when the request fails", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", () => networkError())
        await renderTimeline()
        await expect.element(page.getByRole("img", {name: /Relays per day/})).not.toBeInTheDocument()
    })
})
