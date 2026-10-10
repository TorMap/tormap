import {describe, expect, it} from "vitest";

import {renderWithProviders} from "../../test/render";
import {AppSettings} from "./AppSettings";

describe("AppSettings", () => {
    const restoreButton = {name: "Restore default settings"}

    it("hides the restore button while the settings are default", async () => {
        const screen = await renderWithProviders(<AppSettings elevation={0}/>)

        await expect.element(screen.getByLabelText("Density heatmap")).toBeVisible()
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()
    })

    it("shows the restore button after a change and restores the defaults", async () => {
        const screen = await renderWithProviders(<AppSettings elevation={0}/>)
        const heatMap = screen.getByLabelText("Density heatmap")

        await heatMap.click()
        await expect.element(heatMap).toBeChecked()
        await expect.element(screen.getByRole("button", restoreButton)).toBeVisible()

        await screen.getByRole("button", restoreButton).click()
        await expect.element(heatMap).not.toBeChecked()
        await expect.element(screen.getByRole("button", restoreButton)).not.toBeInTheDocument()
    })
})
