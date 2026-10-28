import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {renderWithProviders} from "../../test/render";
import {AboutInformation} from "./AboutInformation";

describe("AboutInformation", () => {
    it("gives the icon links an accessible name", async () => {
        await page.viewport(1400, 900)
        const screen = await renderWithProviders(<AboutInformation/>)

        await screen.getByRole("button", {name: "About TorMap"}).click()

        await expect.element(page.getByRole("link", {name: "TorMap on GitHub"})).toHaveAttribute("href", "https://github.com/TorMap/tormap")
        await expect.element(page.getByRole("link", {name: "TorMap on Twitter"})).toBeVisible()
        await expect.element(page.getByRole("link", {name: "Email TorMap"})).toHaveAttribute("href", "mailto:hi@tormap.org")
    })

    it("links to GitHub Sponsors instead of crypto donations", async () => {
        await page.viewport(1400, 900)
        const screen = await renderWithProviders(<AboutInformation/>)

        await screen.getByRole("button", {name: "About TorMap"}).click()

        await expect.element(page.getByRole("link", {name: "Sponsor on GitHub"})).toHaveAttribute("href", "https://github.com/sponsors/JuliusHenke")
        await expect.element(page.getByText("Active maintainer")).toBeVisible()
        expect(page.getByText("Bitcoin").elements()).toHaveLength(0)
    })
})
