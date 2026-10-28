import {atom} from "jotai";

/*
 State of the dialogs for selecting a family or an autonomous system. Kept apart from the dialog components, so that
 importing it does not pull the lazily loaded dialogs into the main bundle.
 */

export interface GroupSelection {
    /**
     * Whether the dialog is shown. The IDs are kept while it closes, so its content does not change during the transition.
     */
    show: boolean

    /**
     * The groups to choose from
     */
    ids: number[]
}

export const familySelectionAtom = atom<GroupSelection>({show: false, ids: []})
export const autonomousSystemSelectionAtom = atom<GroupSelection>({show: false, ids: []})

/**
 * The families and autonomous systems of the relays on the map, regardless of a selected one
 */
export const groupsOnMapAtom = atom<{ familyIds: number[], asNumbers: number[] }>({familyIds: [], asNumbers: []})
