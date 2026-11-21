import {useSetAtom} from "jotai";
import {FunctionComponent} from "react";
import {describe, expect, it} from "vitest";
import {page} from "vitest/browser";

import {makeRelay} from "../../test/fixtures";
import {mockBackend} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {relaySearchOnMapAtom} from "../dialogs/relay/relay-details-atoms";
import {OverlayLarge} from "./OverlayLarge";

const ShowSearchOnMap: FunctionComponent<{ search: string }> = ({search}) => {
    const setRelaySearchOnMap = useSetAtom(relaySearchOnMapAtom)
    return <button onClick={() => setRelaySearchOnMap({search, relays: [makeRelay()]})}>Show search</button>
}

describe("OverlayLarge", () => {
    it("keeps the width of the settings when a limit info with a long text appears", async () => {
        mockBackend.onGet("/relay/statistics/daily-counts", [])
        const search = "a very long search text that would widen the settings panel if it was allowed to grow"
        await renderWithProviders(<><OverlayLarge/><ShowSearchOnMap search={search}/></>)
        const panel = () => page.getByRole("button", {name: /^Search \d+ relays/}).element().parentElement as HTMLElement
        const before = panel().getBoundingClientRect().width

        await page.getByRole("button", {name: "Show search"}).click()
        await expect.element(page.getByText(/Matching/)).toBeVisible()

        expect(panel().getBoundingClientRect().width).toBe(before)
    })
})
