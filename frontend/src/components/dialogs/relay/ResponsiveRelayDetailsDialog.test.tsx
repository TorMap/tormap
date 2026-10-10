import {useSetAtom} from "jotai";
import {useEffect} from "react";
import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {useSettings} from "../../../context/settings-context";
import {RelayLocationDto} from "../../../dto/relay";
import {makeRelay, makeRelayDetails, makeRelayIdentifier, makeSettings} from "../../../test/fixtures";
import {mockBackend, networkError} from "../../../test/mock-backend";
import {renderWithProviders} from "../../../test/render";
import {RelayFlag, RelayType} from "../../../types/relay";
import {SnackbarMessage} from "../../../types/ui";
import {relaysForDetailsDialogAtom, ResponsiveRelayDetailsDialog, showRelayDetailsDialogAtom} from "./ResponsiveRelayDetailsDialog";

/**
 * Opens the dialog for the given relays, like a marker click on the map does, and exposes the settings it changes.
 */
const Harness = ({relays}: { relays: RelayLocationDto[] }) => {
    const setRelays = useSetAtom(relaysForDetailsDialogAtom)
    const setShow = useSetAtom(showRelayDetailsDialogAtom)
    const {settings} = useSettings()
    useEffect(() => {
        setRelays(relays)
        setShow(true)
    }, [relays, setRelays, setShow])
    return (
        <>
            <ResponsiveRelayDetailsDialog/>
            <output aria-label="settings">{JSON.stringify({
                selectedFamily: settings.selectedFamily ?? null,
                sortFamily: settings.sortFamily,
                showRelayTypes: settings.showRelayTypes,
            })}</output>
        </>
    )
}

const alpha = makeRelay({detailsId: 21, nickname: "alpha", flags: [RelayFlag.Exit], familyId: 7})
const bravo = makeRelay({detailsId: 22, nickname: "bravo", flags: [RelayFlag.Guard]})
const charlie = makeRelay({detailsId: 23, nickname: "charlie"})

const mockLocationWithThreeRelays = () => {
    mockBackend.onPost("/relay/details/relay/identifiers", [
        makeRelayIdentifier({id: 21, nickname: "alpha", fingerprint: "AAAA"}),
        makeRelayIdentifier({id: 22, nickname: "bravo", fingerprint: "BBBB"}),
        makeRelayIdentifier({id: 23, nickname: "charlie", fingerprint: "CCCC"}),
    ])
    mockBackend.onGet(/^\/relay\/details\/relay\/(\d+)$/, ({match}) => {
        const id = Number(match![1])
        const nickname = {21: "alpha", 22: "bravo", 23: "charlie"}[id]
        return makeRelayDetails({id, nickname, fingerprint: `FP${id}`, familyId: id === 21 ? 7 : null})
    })
}

const settingsOutput = () => page.getByRole("status", {name: "settings"})

describe.each([
    ["large", 1400, 900],
    ["small", 500, 800],
])("ResponsiveRelayDetailsDialog on a %s screen", (_name, width, height) => {
    const open = async (relays: RelayLocationDto[]) => {
        await page.viewport(width, height)
        return renderWithProviders(<Harness relays={relays}/>, {settings: makeSettings()})
    }

    it("shows the details of a single relay without a list", async () => {
        mockBackend.onGet("/relay/details/relay/21", makeRelayDetails({
            id: 21,
            nickname: "alpha",
            fingerprint: "AAAA1111",
            address: "198.51.100.7",
            familyId: 7,
            confirmedFamilyMembers: [makeRelayIdentifier({id: 5, nickname: "sibling", fingerprint: "SIB1"})],
        }))
        await open([alpha])

        await expect.element(page.getByRole("heading", {name: "Relay's nickname"})).toHaveTextContent("alpha")
        await expect.element(page.getByRole("link", {name: "AAAA1111"})).toBeVisible()
        await expect.element(page.getByRole("link", {name: "198.51.100.7"})).toBeVisible()
        await expect.element(page.getByRole("link", {name: "sibling (SIB1)"})).toBeVisible()
        await expect.element(page.getByLabelText("Nickname or fingerprint")).not.toBeInTheDocument()
        expect(mockBackend.requested).toEqual(["GET /relay/details/relay/21"])
    })

    it("formats the relay's numbers and flags", async () => {
        mockBackend.onGet("/relay/details/relay/21", makeRelayDetails({
            id: 21,
            nickname: "alpha",
            uptime: 7200,
            bandwidthRate: 2_000_000,
            isHibernating: true,
        }))
        await open([makeRelay({detailsId: 21, nickname: "alpha", flags: [RelayFlag.Exit, RelayFlag.Fast]})])

        await expect.element(page.getByText("2.00 hours")).toBeVisible()
        await expect.element(page.getByText("2.00 MB/s")).toBeVisible()
        await expect.element(page.getByText("Exit, Fast")).toBeVisible()
        await expect.element(page.getByText("Example AS (64500)")).toBeVisible()
    })

    it("lists several relays at one location and opens the chosen one", async () => {
        mockLocationWithThreeRelays()
        await open([charlie, alpha, bravo])

        await expect.element(page.getByRole("button", {name: "alpha"})).toBeVisible()
        await expect.element(page.getByRole("button", {name: "bravo"})).toBeVisible()
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
        expect(mockBackend.requests.find(r => r.url.endsWith("/identifiers"))?.data).toEqual([23, 21, 22])

        await page.getByRole("button", {name: "bravo"}).click()
        await expect.element(page.getByRole("link", {name: "FP22"})).toBeVisible()
    })

    it("narrows the list by search and opens a single match", async () => {
        mockLocationWithThreeRelays()
        await open([alpha, bravo, charlie])
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()

        const search = page.getByLabelText("Nickname or fingerprint")
        if (width < 1200) {
            // On small screens the search lives in the list view, which is the first thing shown
            await expect.element(search).toBeVisible()
        }
        await search.fill("char")

        await expect.element(page.getByRole("button", {name: "alpha"})).not.toBeInTheDocument()
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
    })

    it("shows a message when nothing matches the search", async () => {
        mockLocationWithThreeRelays()
        await open([alpha, bravo, charlie])
        await page.getByLabelText("Nickname or fingerprint").fill("zzz")

        await expect.element(page.getByText("No results found")).toBeVisible()
    })

    it("restricts the map to the relay's type", async () => {
        mockBackend.onGet("/relay/details/relay/21", makeRelayDetails({id: 21, nickname: "alpha"}))
        await open([alpha])
        await page.getByRole("button", {name: "select relay type"}).click()

        await expect.element(settingsOutput()).toHaveTextContent(
            JSON.stringify({
                selectedFamily: null,
                sortFamily: false,
                showRelayTypes: {[RelayType.Exit]: true, [RelayType.Guard]: false, [RelayType.Other]: false},
            })
        )
    })

    it("selects the relay's family on the map", async () => {
        mockBackend.onGet("/relay/details/relay/21", makeRelayDetails({id: 21, nickname: "alpha", familyId: 7}))
        await open([alpha])
        await page.getByRole("button", {name: "select family"}).first().click()

        await expect.poll(() => settingsOutput().element().textContent).toMatch(/"selectedFamily":7,"sortFamily":true/)
    })

    it("warns when the relays have no details", async () => {
        await open([makeRelay({detailsId: null}), makeRelay({detailsId: undefined})])

        await expect.element(page.getByText(SnackbarMessage.NoRelayDetails)).toBeVisible()
        expect(mockBackend.requested).toEqual([])
    })

    it("reports a failed list lookup and closes", async () => {
        mockBackend.onPost("/relay/details/relay/identifiers", () => networkError())
        await open([alpha, bravo])

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).toBeVisible()
        await expect.element(page.getByRole("dialog")).not.toBeInTheDocument()
    })

    it("reports failed details", async () => {
        mockBackend.onGet("/relay/details/relay/21", () => networkError())
        await open([alpha])

        await expect.element(page.getByText(SnackbarMessage.ConnectionFailed)).toBeVisible()
    })
})

describe("ResponsiveRelayDetailsDialog on a small screen", () => {
    it("goes back from the details to the list", async () => {
        await page.viewport(500, 800)
        mockLocationWithThreeRelays()
        await renderWithProviders(<Harness relays={[alpha, bravo, charlie]}/>)

        await page.getByRole("button", {name: "bravo"}).click()
        await expect.element(page.getByRole("link", {name: "FP22"})).toBeVisible()

        await page.getByRole("button", {name: "Back"}).click()
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
        await expect.element(page.getByRole("link", {name: "FP22"})).not.toBeInTheDocument()
    })

    it("closes the dialog with Back from the list", async () => {
        await page.viewport(500, 800)
        mockLocationWithThreeRelays()
        await renderWithProviders(<Harness relays={[alpha, bravo]}/>)
        await expect.element(page.getByRole("button", {name: "alpha"})).toBeVisible()

        await page.getByRole("button", {name: "Back"}).click()
        await expect.element(page.getByRole("dialog")).not.toBeInTheDocument()
    })
})

describe("RelayList paging", () => {
    it("renders long lists in batches until every relay is shown", async () => {
        await page.viewport(1400, 900)
        const relays = Array.from({length: 120}, (_, index) =>
            makeRelay({detailsId: 1000 + index, nickname: `relay${String(index).padStart(3, "0")}`}))
        mockBackend.onPost("/relay/details/relay/identifiers", relays.map(relay =>
            makeRelayIdentifier({id: relay.detailsId!, nickname: relay.nickname, fingerprint: `FP${relay.detailsId}`})))
        mockBackend.onGet(/^\/relay\/details\/relay\/(\d+)$/, ({match}) =>
            makeRelayDetails({id: Number(match![1])}))
        await renderWithProviders(<Harness relays={relays}/>)

        await expect.element(page.getByRole("button", {name: "relay000"})).toBeVisible()
        // The list only renders a batch at first and appends the next ones as its loader comes into view
        await vi.waitFor(() => {
            if (document.body.textContent?.includes("relay119")) return
            const loaders = document.querySelectorAll('[role="progressbar"]')
            loaders[loaders.length - 1]?.scrollIntoView()
            throw new Error("not every relay is rendered yet")
        }, {timeout: 5000})
        await expect.element(page.getByRole("progressbar")).not.toBeInTheDocument()
    })
})
