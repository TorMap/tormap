import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {useSettings} from "../../../context/settings-context";
import {makeRelay} from "../../../test/fixtures";
import {mockBackend, networkError} from "../../../test/mock-backend";
import {renderWithProviders} from "../../../test/render";
import {RelayFlag} from "../../../types/relay";
import {buildRelayAutonomousSystemMap} from "../../../util/aggregate-relays";
import {AutonomousSystemSelectionDialog} from "./AutonomousSystemSelectionDialog";

const SelectedAutonomousSystem = () => {
    const {settings} = useSettings()
    return <output aria-label="selected autonomous system">{String(settings.selectedAutonomousSystem ?? "none")}</output>
}

const relays = [
    makeRelay({asNumber: 24940, country: "DE", consensusWeight: 10, flags: [RelayFlag.Exit]}),
    makeRelay({asNumber: 24940, country: "FI", consensusWeight: 10}),
    makeRelay({asNumber: 16276, country: "FR", consensusWeight: 60}),
    makeRelay({asNumber: 3320, country: "DE", consensusWeight: 20}),
]
const asNumbers = [24940, 16276, 3320]
const names = [
    {number: 24940, name: "Hetzner Online GmbH"},
    {number: 16276, name: "OVH SAS"},
    {number: 3320, name: "Deutsche Telekom AG"},
]

describe.each([
    ["large", 1400, 900, "Select an autonomous system"],
    ["small", 500, 800, "Autonomous systems"],
])("AutonomousSystemSelectionDialog on a %s screen", (_name, width, height, title) => {
    const open = async () => {
        await page.viewport(width, height)
        const closeDialog = vi.fn()
        await renderWithProviders(
            <>
                <AutonomousSystemSelectionDialog
                    shouldShowDialog={true}
                    closeDialog={closeDialog}
                    asNumbers={asNumbers}
                    relayAutonomousSystemMap={buildRelayAutonomousSystemMap(relays)}
                    totalConsensusWeight={100}
                    month="2024-01"
                />
                <SelectedAutonomousSystem/>
            </>,
            {settings: {sortAutonomousSystem: true}}
        )
        return {closeDialog}
    }
    const firstCells = () => Array.from(document.querySelectorAll("tbody tr"))
        .map(row => row.querySelector("td")?.textContent)

    it("lists the autonomous systems with relays and consensus weight, heaviest first", async () => {
        mockBackend.onGet("/relay/details/autonomous-systems/2024-01", names)
        await open()

        await expect.element(page.getByRole("dialog", {name: new RegExp(title)})).toBeVisible()
        await expect.poll(firstCells).toEqual(["OVH SAS", "Hetzner Online GmbH", "Deutsche Telekom AG"])
        const hetzner = Array.from(document.querySelectorAll("tbody tr"))[1]
        expect(Array.from(hetzner.querySelectorAll("td")).map(cell => cell.textContent))
            .toEqual(["Hetzner Online GmbH", "AS24940", "2", "1", "20.00%", "2"])
    })

    it("sorts by the clicked column", async () => {
        mockBackend.onGet("/relay/details/autonomous-systems/2024-01", names)
        await open()
        await expect.poll(firstCells).toHaveLength(3)

        await page.getByRole("button", {name: "Relays"}).click()
        await expect.poll(firstCells).toEqual(["Hetzner Online GmbH", "OVH SAS", "Deutsche Telekom AG"])
        await page.getByRole("button", {name: "Name"}).click()
        await expect.poll(firstCells).toEqual(["Deutsche Telekom AG", "Hetzner Online GmbH", "OVH SAS"])
    })

    it("filters by name or number", async () => {
        mockBackend.onGet("/relay/details/autonomous-systems/2024-01", names)
        await open()
        await expect.poll(firstCells).toHaveLength(3)

        await page.getByLabelText("Search by name or number").fill("ovh")
        await expect.poll(firstCells).toEqual(["OVH SAS"])
        await page.getByLabelText("Search by name or number").fill("AS3320")
        await expect.poll(firstCells).toEqual(["Deutsche Telekom AG"])
    })

    it("selects the autonomous system of the clicked row and closes", async () => {
        mockBackend.onGet("/relay/details/autonomous-systems/2024-01", names)
        const {closeDialog} = await open()
        await page.getByText("Deutsche Telekom AG").click()

        // The modal dialog hides the rest of the page from role queries, so read the output from the DOM
        await expect.poll(() => document.querySelector('output[aria-label="selected autonomous system"]')?.textContent).toBe("3320")
        expect(closeDialog).toHaveBeenCalledOnce()
    })

    it("still lists the autonomous systems when the names can not be loaded", async () => {
        mockBackend.onGet("/relay/details/autonomous-systems/2024-01", () => networkError())
        await open()

        await expect.poll(firstCells).toEqual(["Unknown name", "Unknown name", "Unknown name"])
        await expect.element(page.getByText("AS16276")).toBeVisible()
    })
})
