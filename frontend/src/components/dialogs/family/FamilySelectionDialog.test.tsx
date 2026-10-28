import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {useSettings} from "../../../context/settings-context";
import {makeFamilyIdentifier} from "../../../test/fixtures";
import {mockBackend, networkError} from "../../../test/mock-backend";
import {renderWithProviders} from "../../../test/render";
import {SnackbarMessage} from "../../../types/ui";
import {FamilySelectionDialog} from "./FamilySelectionDialog";

const SelectedFamily = () => {
    const {settings} = useSettings()
    return <output aria-label="selected family">{String(settings.selectedFamily ?? "none")}</output>
}

const familyIds = [3, 4]

describe.each([
    ["large", 1400, 900, "Select a family"],
    ["small", 500, 800, "Families"],
])("FamilySelectionDialog on a %s screen", (_name, width, height, title) => {
    const open = async (props: { familyIds?: number[] } = {}) => {
        await page.viewport(width, height)
        const closeDialog = vi.fn()
        const reloadSelectedDay = vi.fn()
        await renderWithProviders(
            <>
                <FamilySelectionDialog
                    shouldShowDialog={true}
                    closeDialog={closeDialog}
                    reloadSelectedDay={reloadSelectedDay}
                    familyIds={props.familyIds ?? familyIds}
                />
                <SelectedFamily/>
            </>,
            {settings: {sortFamily: true}}
        )
        return {closeDialog, reloadSelectedDay}
    }

    it("lists the families that were looked up", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", [
            makeFamilyIdentifier({id: 3, nicknames: "alpha, bravo", memberCount: 2, autonomousSystems: "AS1"}),
            makeFamilyIdentifier({id: 4, nicknames: "charlie", memberCount: 1, autonomousSystems: undefined}),
        ])
        await open()

        await expect.element(page.getByText(title, {exact: true})).toBeVisible()
        await expect.element(page.getByText("alpha, bravo")).toBeVisible()
        await expect.element(page.getByText("AS1")).toBeVisible()
        await expect.element(page.getByText("data not yet available")).toBeVisible()
        expect(mockBackend.requests[0].data).toEqual(familyIds)
    })

    it("has an accessible name for the dialog", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", [makeFamilyIdentifier({id: 3, nicknames: "alpha"})])
        await open()

        await expect.element(page.getByRole("dialog", {name: new RegExp(title)})).toBeVisible()
    })

    it("selects the family of the clicked row and closes", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", [
            makeFamilyIdentifier({id: 3, nicknames: "alpha, bravo"}),
            makeFamilyIdentifier({id: 4, nicknames: "charlie"}),
        ])
        const {closeDialog} = await open()
        await page.getByText("charlie").click()

        // The modal dialog hides the rest of the page from role queries, so read the output from the DOM
        await expect.poll(() => document.querySelector('output[aria-label="selected family"]')?.textContent).toBe("4")
        expect(closeDialog).toHaveBeenCalledOnce()
    })

    it("sorts the families by the clicked column", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", [
            makeFamilyIdentifier({id: 3, nicknames: "bravo", memberCount: 2, autonomousSystems: "AS10"}),
            makeFamilyIdentifier({id: 4, nicknames: "alpha", memberCount: 9, autonomousSystems: undefined}),
            makeFamilyIdentifier({id: 5, nicknames: "charlie", memberCount: 5, autonomousSystems: "AS9"}),
        ])
        await open()
        const nicknames = () => Array.from(document.querySelectorAll("tbody tr"))
            .map(row => row.querySelector("td")?.textContent)

        // Largest families first by default
        await expect.poll(nicknames).toEqual(["alpha", "charlie", "bravo"])

        await page.getByRole("button", {name: "Relay nicknames"}).click()
        await expect.poll(nicknames).toEqual(["alpha", "bravo", "charlie"])
        await page.getByRole("button", {name: "Relay nicknames"}).click()
        await expect.poll(nicknames).toEqual(["charlie", "bravo", "alpha"])

        // Numeric order for AS numbers, families without data stay last
        await page.getByRole("button", {name: "Autonomous Systems"}).click()
        await expect.poll(nicknames).toEqual(["charlie", "bravo", "alpha"])
        await page.getByRole("button", {name: "Autonomous Systems"}).click()
        await expect.poll(nicknames).toEqual(["bravo", "charlie", "alpha"])

        await page.getByRole("button", {name: "Count"}).click()
        await expect.poll(nicknames).toEqual(["alpha", "charlie", "bravo"])
    })

    it("filters a long list by nickname or autonomous system", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", [
            makeFamilyIdentifier({id: 3, nicknames: "alpha", autonomousSystems: "Hetzner Online GmbH"}),
            makeFamilyIdentifier({id: 4, nicknames: "bravo", autonomousSystems: "OVH SAS"}),
            makeFamilyIdentifier({id: 5, nicknames: "charlie"}),
            makeFamilyIdentifier({id: 6, nicknames: "delta"}),
            makeFamilyIdentifier({id: 7, nicknames: "echo"}),
            makeFamilyIdentifier({id: 8, nicknames: "foxtrot"}),
        ])
        await open({familyIds: [3, 4, 5, 6, 7, 8]})
        const nicknames = () => Array.from(document.querySelectorAll("tbody tr"))
            .map(row => row.querySelector("td")?.textContent)
        await expect.poll(nicknames).toHaveLength(6)

        await page.getByLabelText("Search by nickname or autonomous system").fill("ovh")
        await expect.poll(nicknames).toEqual(["bravo"])
        await page.getByLabelText("Search by nickname or autonomous system").fill("ECHO")
        await expect.poll(nicknames).toEqual(["echo"])
    })

    it("reloads the day when the families are outdated", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", [])
        const {closeDialog, reloadSelectedDay} = await open()

        await expect.element(page.getByText(SnackbarMessage.UpdatedData)).toBeVisible()
        expect(closeDialog).toHaveBeenCalled()
        expect(reloadSelectedDay).toHaveBeenCalledOnce()
    })

    it("reports a failed lookup", async () => {
        mockBackend.onPost("/relay/details/family/identifiers", () => networkError())
        const {closeDialog, reloadSelectedDay} = await open()

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).toBeVisible()
        expect(closeDialog).not.toHaveBeenCalled()
        expect(reloadSelectedDay).not.toHaveBeenCalled()
    })

    it("does not look anything up without families", async () => {
        await open({familyIds: []})

        await expect.element(page.getByText(title, {exact: true})).toBeVisible()
        expect(mockBackend.requested).toEqual([])
    })
})
