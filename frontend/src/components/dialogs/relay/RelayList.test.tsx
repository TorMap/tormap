import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {makeRelay, makeRelayIdentifier} from "../../../test/fixtures";
import {renderWithProviders} from "../../../test/render";
import {RelayIdentifierMatch, RelayType} from "../../../types/relay";
import {RelayList} from "./RelayList";

const makeMatches = (prefix: string, count: number): RelayIdentifierMatch[] =>
    Array.from({length: count}, (_, index) => {
        const id = 1000 + index
        const nickname = `${prefix}${String(index).padStart(3, "0")}`
        return {
            ...makeRelay({detailsId: id, nickname}),
            ...makeRelayIdentifier({id, nickname, fingerprint: `FP${id}`}),
            relayType: RelayType.Other,
        }
    })

const renderedNicknames = () =>
    Array.from(document.querySelectorAll('[role="button"]')).map(button => button.textContent)

/**
 * Scrolls the loader into view until the given relay is rendered.
 */
const scrollUntilRendered = (nickname: string) => vi.waitFor(() => {
    if (renderedNicknames().includes(nickname)) return
    document.querySelector('[role="progressbar"]')?.scrollIntoView()
    throw new Error(`${nickname} is not rendered yet`)
}, {timeout: 5000})

describe("RelayList", () => {
    it("renders exactly one batch of 50 relays at first", async () => {
        await page.viewport(1400, 900)
        await renderWithProviders(<RelayList relayMatches={makeMatches("relay", 120)} setSelectedRelayId={() => undefined}/>)

        await expect.element(page.getByRole("button", {name: "relay049"})).toBeInTheDocument()
        expect(renderedNicknames()).toHaveLength(50)
        expect(renderedNicknames()).not.toContain("relay050")
        await expect.element(page.getByRole("progressbar")).toBeInTheDocument()
    })

    it("starts again with the first batch when the matches change", async () => {
        await page.viewport(1400, 900)
        const setSelectedRelayId = () => undefined
        const screen = await renderWithProviders(<RelayList relayMatches={makeMatches("relay", 120)} setSelectedRelayId={setSelectedRelayId}/>)
        await scrollUntilRendered("relay119")

        // Scroll back up so the loader of the new list is out of view and does not load the next batch right away
        window.scrollTo(0, 0)
        await screen.rerender(<RelayList relayMatches={makeMatches("match", 120)} setSelectedRelayId={setSelectedRelayId}/>)

        await expect.element(page.getByRole("button", {name: "match049"})).toBeInTheDocument()
        expect(renderedNicknames()).toHaveLength(50)
        expect(renderedNicknames()).not.toContain("match050")
    })
})
