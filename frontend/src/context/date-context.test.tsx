import {eachDayOfInterval, format} from "date-fns";
import {expect, describe, it} from "vitest";
import {page} from "vitest/browser";
import {renderHook} from "vitest-browser-react";

import {SnackbarMessage} from "../types/ui";
import {DateProvider, hasMissingHistoricMonth, useDate} from "./date-context";
import {SnackbarProvider} from "./snackbar-context";

const render = () =>
    renderHook(() => useDate(), {
        wrapper: ({children}) => (
            <SnackbarProvider>
                <DateProvider>{children}</DateProvider>
            </SnackbarProvider>
        ),
    })

const historicMessage = () => page.getByText(SnackbarMessage.HistoricDataProcessing)

// All days since the first available consensus, which is what the backend returns once everything is processed
const allDaysSince2007 = () =>
    eachDayOfInterval({start: new Date("2007-10-27"), end: new Date()}).map(day => format(day, "yyyy-MM-dd"))

describe("DateProvider", () => {
    it("starts without days and without a selected date", async () => {
        const {result} = await render()
        expect(result.current.availableDays).toEqual([])
        expect(result.current.selectedDate).toBeUndefined()
    })

    it("selects the latest day once days are available", async () => {
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays(["2024-01-01", "2024-01-02", "2024-01-03"]))
        expect(result.current.availableDays).toHaveLength(3)
        expect(result.current.selectedDate).toBe("2024-01-03")
    })

    it("selects the day from the URL if it is available", async () => {
        window.location.hash = "#day=2024-01-02"
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays(["2024-01-01", "2024-01-02", "2024-01-03"]))
        expect(result.current.selectedDate).toBe("2024-01-02")
    })

    it("falls back to the latest day if the day from the URL is not available", async () => {
        window.location.hash = "#day=2023-05-05"
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays(["2024-01-01", "2024-01-02"]))
        expect(result.current.selectedDate).toBe("2024-01-02")
    })

    it("writes the selected day to the URL", async () => {
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays(["2024-01-01", "2024-01-02"]))
        expect(window.location.hash).toContain("day=2024-01-02")
    })

    it("lets the user select another day", async () => {
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays(["2024-01-01", "2024-01-02"]))
        await act(() => result.current.setSelectedDate("2024-01-01"))
        expect(result.current.selectedDate).toBe("2024-01-01")
    })

    it("tells the user when historic data is still processing", async () => {
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays(["2024-01-01", "2024-01-02"]))
        await expect.element(historicMessage()).toBeVisible()
    })

    it("stays quiet when the history is complete", async () => {
        const {result, act} = await render()
        const days = allDaysSince2007()
        await act(() => result.current.setAvailableDays(days))
        expect(result.current.selectedDate).toBe(days[days.length - 1])
        await expect.element(historicMessage()).not.toBeInTheDocument()
    })

    it("stays quiet when the history only has gaps shorter than a month", async () => {
        const {result, act} = await render()
        // Like the live data: the archive misses a few days in 2007 and 2008 and 18 days in April 2026
        const days = allDaysSince2007().filter(day =>
            !(day >= "2007-11-09" && day <= "2007-11-11") && !(day >= "2026-04-03" && day <= "2026-04-20"))
        await act(() => result.current.setAvailableDays(days))
        await expect.element(historicMessage()).not.toBeInTheDocument()
    })

    it("does nothing for an empty list of days", async () => {
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays([]))
        expect(result.current.selectedDate).toBeUndefined()
        await expect.element(historicMessage()).not.toBeInTheDocument()
    })
})

describe("hasMissingHistoricMonth", () => {
    it("finds a gap of more than a month in the middle of the history", () => {
        const days = allDaysSince2007().filter(day => !(day >= "2015-01-01" && day <= "2015-02-15"))
        expect(hasMissingHistoricMonth(days)).toBe(true)
    })

    it("finds missing months at the start of the history", () => {
        expect(hasMissingHistoricMonth(["2008-01-01", "2008-01-02"])).toBe(true)
    })

    it("accepts gaps of up to a month", () => {
        const days = allDaysSince2007().filter(day => !(day >= "2015-01-01" && day <= "2015-01-30"))
        expect(hasMissingHistoricMonth(days)).toBe(false)
    })
})
