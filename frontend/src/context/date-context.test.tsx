import {eachDayOfInterval, format} from "date-fns";
import {SnackbarProvider} from "notistack";
import {expect, describe, it} from "vitest";
import {page} from "vitest/browser";
import {renderHook} from "vitest-browser-react";

import {SnackbarMessage} from "../types/ui";
import {DateProvider, useDate} from "./date-context";

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

    it("does nothing for an empty list of days", async () => {
        const {result, act} = await render()
        await act(() => result.current.setAvailableDays([]))
        expect(result.current.selectedDate).toBeUndefined()
        await expect.element(historicMessage()).not.toBeInTheDocument()
    })
})
