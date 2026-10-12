import {useSetAtom} from "jotai";
import {describe, expect, it} from "vitest";

import {makeRelay} from "../../test/fixtures";
import {renderWithProviders} from "../../test/render";
import {relaySearchOnMapAtom} from "../dialogs/relay/relay-details-atoms";
import {OverlaySmall} from "./OverlaySmall";

// Stands in for the relay search dialog, which shows its results on the map
const ShowSearchOnMap = () => {
    const setRelaySearchOnMap = useSetAtom(relaySearchOnMapAtom)
    return <button onClick={() => setRelaySearchOnMap({search: "relay", relays: [makeRelay()]})}>Show search</button>
}

describe("OverlaySmall", () => {
    it("closes the settings when search results are shown on the map", async () => {
        const screen = await renderWithProviders(<><OverlaySmall/><ShowSearchOnMap/></>)

        await screen.getByRole("button", {name: "more settings"}).click()
        await expect.element(screen.getByRole("dialog")).toBeVisible()

        // The full screen dialog hides the rest of the page, so the button is clicked directly in the DOM
        const showSearch = screen.getByRole("button", {name: "Show search", includeHidden: true}).element() as HTMLButtonElement
        showSearch.click()

        await expect.element(screen.getByRole("dialog")).not.toBeInTheDocument()
    })
})
