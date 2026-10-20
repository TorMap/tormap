import {FunctionComponent, useEffect, useRef} from "react";
import {describe, expect, it} from "vitest";
import {page, userEvent} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {renderWithProviders} from "../../test/render";
import {ResponsiveDatePicker} from "./ResponsiveDatePicker";

// 2024-02-03 is missing, e.g. because no consensus was archived that day
const days = ["2024-02-01", "2024-02-02", "2024-02-04", "2024-02-05"]

const Seed: FunctionComponent = () => {
    const {setAvailableDays, selectedDate, setSelectedDate} = useDate()
    const seeded = useRef(false)
    useEffect(() => {
        if (seeded.current) return
        seeded.current = true
        setAvailableDays(days)
    }, [setAvailableDays])
    return (
        <>
            <output data-testid="selected-date">{selectedDate}</output>
            <button onClick={() => setSelectedDate(days[0])}>select first day</button>
        </>
    )
}

const renderPicker = (largeScreen = true) => renderWithProviders(<><Seed/><ResponsiveDatePicker largeScreen={largeScreen}/></>)

const field = () => document.querySelector<HTMLInputElement>("input.MuiPickersInputBase-input")!

describe("ResponsiveDatePicker", () => {
    it("shows the selected day", async () => {
        await renderPicker()
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent("2024-02-05")
        await expect.poll(() => field().value).toBe("2024-02-05")
    })

    it("selects an available day from the calendar", async () => {
        await renderPicker()
        await page.getByRole("button", {name: /Choose date/}).click()
        await page.getByRole("gridcell", {name: "2", exact: true}).click()
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent("2024-02-02")
        await expect.poll(() => field().value).toBe("2024-02-02")
    })

    it("disables days without data", async () => {
        await renderPicker(false)
        await page.getByRole("button", {name: /Choose date/}).click()
        await expect.element(page.getByRole("gridcell", {name: "3", exact: true})).toBeDisabled()
        await expect.element(page.getByRole("gridcell", {name: "4", exact: true})).toBeEnabled()
        // Days outside the available range are disabled as well
        await expect.element(page.getByRole("gridcell", {name: "6", exact: true})).toBeDisabled()
    })

    it("follows a day selected elsewhere, e.g. on the slider", async () => {
        await renderPicker()
        await page.getByRole("button", {name: "select first day"}).click()
        await expect.poll(() => field().value).toBe("2024-02-01")
    })

    it("keeps a typed day that is not available out of the selection and resets the field on blur", async () => {
        await renderPicker()
        await expect.poll(() => field().value).toBe("2024-02-05")
        await page.getByRole("spinbutton", {name: "Day"}).click()
        await userEvent.keyboard("03")
        await expect.poll(() => field().value).toBe("2024-02-03")
        // 2024-02-03 has no data, so the loaded day stays selected
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent("2024-02-05")

        await page.getByRole("button", {name: "select first day"}).click()
        await expect.poll(() => field().value).toBe("2024-02-01")
        await page.getByRole("spinbutton", {name: "Day"}).click()
        await userEvent.keyboard("03")
        await userEvent.click(page.getByTestId("selected-date"))
        await expect.poll(() => field().value).toBe("2024-02-01")
    })

    it("selects a typed day that is available", async () => {
        await renderPicker()
        await page.getByRole("spinbutton", {name: "Day"}).click()
        await userEvent.keyboard("04")
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent("2024-02-04")
    })
})
