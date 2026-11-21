import {FunctionComponent, useEffect, useRef} from "react";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {DailyRelayCountDto} from "../../dto/relay";
import {mockBackend, networkError, reply} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {SnackbarMessage} from "../../types/ui";
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

    it("stays quiet when the backend does not know the endpoint yet", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", reply(404, {error: "Not Found"}))
        await renderTimeline()
        await expect.element(page.getByRole("img", {name: /Relays per day/})).not.toBeInTheDocument()
        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).not.toBeInTheDocument()
    })

    it("ignores an unexpected response body", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", "<html>not the api</html>")
        await renderTimeline()
        await expect.element(page.getByRole("img", {name: /Relays per day/})).not.toBeInTheDocument()
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[39])
    })

    it("stays quiet when the request fails", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", () => networkError())
        await renderTimeline()
        await expect.element(page.getByRole("img", {name: /Relays per day/})).not.toBeInTheDocument()
    })

    it("selects an outlier day with the keyboard", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", counts)
        await renderTimeline()

        const outlier = page.getByTestId("relay-count-outlier")
        await expect.element(outlier).toBeVisible()
        outlier.element().dispatchEvent(new KeyboardEvent("keydown", {key: "Tab", bubbles: true}))
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[39])

        const space = new KeyboardEvent("keydown", {key: " ", bubbles: true, cancelable: true})
        outlier.element().dispatchEvent(space)
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[20])
        expect(space.defaultPrevented).toBe(true)

    })

    it("selects an outlier day with Enter", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", counts)
        await renderTimeline()

        const outlier = page.getByTestId("relay-count-outlier")
        await expect.element(outlier).toBeVisible()
        outlier.element().dispatchEvent(new KeyboardEvent("keydown", {key: "Enter", bubbles: true}))
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[20])
    })

    it("describes the outlier with its change and marks the selected day", async () => {
        const spike = counts.map((count, i) => i === 10 ? {...count, total: 9000} : count)
        mockBackend.onGet("/relay/statistics/daily-counts", spike)
        await renderTimeline()

        await expect.element(page.getByRole("button", {name: new RegExp(`Select ${days[10]}: 9000 relays \\(\\+`)})).toBeVisible()
        await expect.element(page.getByRole("button", {name: new RegExp(`Select ${days[20]}: 2500 relays \\(-`)})).toBeVisible()
        // The selected (latest) day is marked with a vertical line at the right end
        expect(document.querySelector("svg line")?.getAttribute("x1")).toBe("1000")
    })

    it("breaks the line at days without counts", async () => {
        const withGap = counts.filter((_, i) => i < 15 || i > 17)
        mockBackend.onGet("/relay/statistics/daily-counts", withGap)
        await renderTimeline()

        await expect.element(page.getByRole("img", {name: /Relays per day/})).toBeVisible()
        expect(document.querySelectorAll("svg polyline")).toHaveLength(2)
    })

    it("renders nothing if no count matches an available day", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", [{day: "2001-01-01", total: 1, exit: 0, guard: 0, other: 1}])
        await renderTimeline()
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent(days[39])
        await expect.element(page.getByRole("img", {name: /Relays per day/})).not.toBeInTheDocument()
    })
})
