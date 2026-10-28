import {useSetAtom} from "jotai";
import {FunctionComponent, useEffect, useRef} from "react";
import {describe, expect, it} from "vitest";
import {page, userEvent} from "vitest/browser";

import {useDate} from "../../context/date-context";
import {makeRelay, makeRelayDetails, makeRelayIdentifier} from "../../test/fixtures";
import {mockBackend} from "../../test/mock-backend";
import {renderWithProviders} from "../../test/render";
import {ResponsiveRelayDetailsDialog} from "../dialogs/relay/ResponsiveRelayDetailsDialog";
import {filteredRelaysAtom} from "../leaflet/LeafletLayers";
import {KeyboardShortcuts} from "./KeyboardShortcuts";

const days = ["2024-02-01", "2024-02-02", "2024-02-03"]
const relays = [
    makeRelay({detailsId: 31, nickname: "alpha"}),
    makeRelay({detailsId: 32, nickname: "beta"}),
]

/**
 * Provides the days and relays which the map would load, and the UI elements the shortcuts work with
 */
const Harness: FunctionComponent = () => {
    const {setAvailableDays, selectedDate} = useDate()
    const setFilteredRelays = useSetAtom(filteredRelaysAtom)
    const seeded = useRef(false)
    useEffect(() => {
        if (seeded.current) return
        seeded.current = true
        setAvailableDays(days)
        setFilteredRelays(relays)
    }, [setAvailableDays, setFilteredRelays])
    return (
        <>
            <KeyboardShortcuts/>
            <ResponsiveRelayDetailsDialog/>
            <output data-testid="selected-date">{selectedDate}</output>
            <input aria-label="other field"/>
        </>
    )
}

const selectedDate = () => page.getByTestId("selected-date")

describe("KeyboardShortcuts", () => {
    it("selects the previous and next day with the arrow keys while nothing has the focus", async () => {
        await renderWithProviders(<Harness/>)
        await expect.element(selectedDate()).toHaveTextContent("2024-02-03")

        await userEvent.keyboard("{ArrowLeft}")
        await expect.element(selectedDate()).toHaveTextContent("2024-02-02")
        await userEvent.keyboard("{ArrowLeft}{ArrowLeft}")
        // Stays on the first day
        await expect.element(selectedDate()).toHaveTextContent("2024-02-01")
        await userEvent.keyboard("{ArrowRight}")
        await expect.element(selectedDate()).toHaveTextContent("2024-02-02")
    })

    it("leaves the arrow keys to a focused element and to modifier shortcuts", async () => {
        await renderWithProviders(<Harness/>)
        await expect.element(selectedDate()).toHaveTextContent("2024-02-03")

        await userEvent.keyboard("{Alt>}{ArrowLeft}{/Alt}")
        await page.getByRole("textbox", {name: "other field"}).click()
        await userEvent.keyboard("{ArrowLeft}")
        await expect.element(selectedDate()).toHaveTextContent("2024-02-03")
    })

    it("opens the relay search with Ctrl+K and focuses its search field", async () => {
        await page.viewport(1400, 900)
        mockBackend.onPost("/relay/details/relay/identifiers", relays.map(relay =>
            makeRelayIdentifier({id: relay.detailsId!, nickname: relay.nickname, fingerprint: `FP${relay.detailsId}`})))
        mockBackend.onGet(/^\/relay\/details\/relay\/(\d+)$/, ({match}) => makeRelayDetails({id: Number(match![1])}))
        await renderWithProviders(<Harness/>)
        await expect.element(selectedDate()).toHaveTextContent("2024-02-03")

        // Works from a text field too, like in other web apps
        await page.getByRole("textbox", {name: "other field"}).click()
        await userEvent.keyboard("{Control>}k{/Control}")

        const search = page.getByRole("textbox", {name: "Nickname or fingerprint"})
        await expect.element(search).toHaveFocus()
        await userEvent.keyboard("bet")
        await expect.element(page.getByRole("button", {name: "beta"})).toBeVisible()
        await expect.element(page.getByRole("button", {name: "alpha"})).not.toBeInTheDocument()
    })
})
