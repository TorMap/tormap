import {FunctionComponent, useEffect, useRef} from "react";
import {describe, expect, it} from "vitest";
import {page, userEvent} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {renderWithProviders} from "../../test/render";
import {DateSlider} from "./DateSlider";

const days = ["2024-02-01", "2024-02-02", "2024-02-03"]

const Seed: FunctionComponent = () => {
    const {setAvailableDays, selectedDate} = useDate()
    const seeded = useRef(false)
    useEffect(() => {
        if (seeded.current) return
        seeded.current = true
        setAvailableDays(days)
    }, [setAvailableDays])
    return <output data-testid="selected-date">{selectedDate}</output>
}

describe("DateSlider", () => {
    it("has an accessible name and announces the selected day instead of its index", async () => {
        await renderWithProviders(<><Seed/><DateSlider/></>)

        const slider = page.getByRole("slider", {name: "Selected day"})
        await expect.element(slider).toBeVisible()
        // The last available day is selected by default
        await expect.element(slider).toHaveAttribute("aria-valuetext", "2024-02-03")
    })

    it("selects another day with the keyboard", async () => {
        await renderWithProviders(<><Seed/><DateSlider/></>)
        const slider = page.getByRole("slider", {name: "Selected day"})
        await expect.element(slider).toHaveAttribute("aria-valuetext", "2024-02-03")

        // The range input is visually hidden, so it is focused instead of clicked
        const input = slider.element() as HTMLInputElement
        input.focus()
        await userEvent.keyboard("{ArrowLeft}")
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent("2024-02-02")
        await userEvent.keyboard("{Home}")
        await expect.element(page.getByTestId("selected-date")).toHaveTextContent("2024-02-01")
        await expect.element(slider).toHaveAttribute("aria-valuetext", "2024-02-01")
    })

    it("is disabled until days are available", async () => {
        await renderWithProviders(<DateSlider/>)
        await expect.element(page.getByRole("slider", {name: "Selected day"})).toBeDisabled()
    })
})
