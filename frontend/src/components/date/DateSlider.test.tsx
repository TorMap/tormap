import {FunctionComponent, useEffect, useRef} from "react";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {renderWithProviders} from "../../test/render";
import {DateSlider} from "./DateSlider";

const days = ["2024-02-01", "2024-02-02", "2024-02-03"]

const Seed: FunctionComponent = () => {
    const {setAvailableDays} = useDate()
    const seeded = useRef(false)
    useEffect(() => {
        if (seeded.current) return
        seeded.current = true
        setAvailableDays(days)
    }, [setAvailableDays])
    return null
}

describe("DateSlider", () => {
    it("has an accessible name and announces the selected day instead of its index", async () => {
        await renderWithProviders(<><Seed/><DateSlider/></>)

        const slider = page.getByRole("slider", {name: "Selected day"})
        await expect.element(slider).toBeVisible()
        // The last available day is selected by default
        await expect.element(slider).toHaveAttribute("aria-valuetext", "2024-02-03")
    })
})
