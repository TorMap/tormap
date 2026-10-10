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
