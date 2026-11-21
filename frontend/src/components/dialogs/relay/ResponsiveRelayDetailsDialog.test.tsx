import {useAtom, useAtomValue, useSetAtom} from "jotai";
import {useEffect, useState} from "react";
import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {useDate} from "../../../context/date-context";
import {useSettings} from "../../../context/settings-context";
import {RelayLocationDto} from "../../../dto/relay";
import {fingerprintOf, makeRelay, makeRelayDetails, makeRelayIdentifier, makeSettings} from "../../../test/fixtures";
import {mockBackend, networkError} from "../../../test/mock-backend";
import {renderWithProviders} from "../../../test/render";
import {RelayFlag, RelayType} from "../../../types/relay";
import {SnackbarMessage} from "../../../types/ui";
import {relaysForDetailsDialogAtom, relaySearchOnMapAtom, showRelayDetailsDialogAtom} from "./relay-details-atoms";
import {ResponsiveRelayDetailsDialog} from "./ResponsiveRelayDetailsDialog";

/**
 * Opens the dialog for the given relays, like a marker click on the map does, and exposes the settings it changes.
 */
const Harness = ({relays}: { relays: RelayLocationDto[] }) => {
    const setRelays = useSetAtom(relaysForDetailsDialogAtom)
    const setShow = useSetAtom(showRelayDetailsDialogAtom)
    const {settings} = useSettings()
    const {setSelectedDate} = useDate()
    const searchOnMap = useAtomValue(relaySearchOnMapAtom)
    useEffect(() => {
        setSelectedDate(day)
        setRelays(relays)
        setShow(true)
    }, [relays, setRelays, setSelectedDate, setShow])
    return (
        <>
            <ResponsiveRelayDetailsDialog/>
            <output aria-label="settings">{JSON.stringify({
                selectedFamily: settings.selectedFamily ?? null,
                sortFamily: settings.sortFamily,
                showRelayTypes: settings.showRelayTypes,
            })}</output>
            <output aria-label="search on map">{JSON.stringify(searchOnMap ? {
                search: searchOnMap.search,
                fingerprints: searchOnMap.relays.map(relay => relay.fingerprint),
            } : null)}</output>
        </>
    )
}


/**
 * Mounts the dialog only once it is opened for the first time, like LeafletLayers does
 */
const LazyHarness = ({relays}: { relays: RelayLocationDto[] }) => {
    const setRelays = useSetAtom(relaysForDetailsDialogAtom)
    const [show, setShow] = useAtom(showRelayDetailsDialogAtom)
    const [hasOpened, setHasOpened] = useState(false)
    if (show && !hasOpened) setHasOpened(true)
    return (
        <>
            <button onClick={() => {
                setRelays(relays)
                setShow(true)
            }}>Open</button>
            {hasOpened ? <ResponsiveRelayDetailsDialog/> : null}
        </>
    )
}

// The details of a relay are requested for the month of the selected day
const day = "2024-01-02"
const detailsPath = (n: number) => `/v1/relays/${fingerprintOf(n)}?month=2024-01`
const detailsPathPattern = /^\/v1\/relays\/([0-9A-F]{40})\?month=2024-01$/

const alpha = makeRelay({fingerprint: fingerprintOf(21), nickname: "alpha", flags: [RelayFlag.Exit], familyId: 7})
const bravo = makeRelay({fingerprint: fingerprintOf(22), nickname: "bravo", flags: [RelayFlag.Guard]})
const charlie = makeRelay({fingerprint: fingerprintOf(23), nickname: "charlie"})

const mockLocationWithThreeRelays = () => {
    mockBackend.onGet(detailsPathPattern, ({match}) => {
        const n = parseInt(match![1], 16)
        const nickname = {21: "alpha", 22: "bravo", 23: "charlie"}[n]
        return makeRelayDetails({nickname, fingerprint: match![1], familyId: n === 21 ? 7 : null})
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
        mockBackend.onGet(detailsPath(21), makeRelayDetails({
            nickname: "alpha",
            fingerprint: fingerprintOf(21),
            address: "198.51.100.7",
            familyId: 7,
            confirmedFamilyMembers: [makeRelayIdentifier({nickname: "sibling", fingerprint: "SIB1"})],
        }))
        await open([alpha])

        await expect.element(page.getByRole("heading", {name: "Relay's nickname"})).toHaveTextContent("alpha")
        await expect.element(page.getByRole("link", {name: fingerprintOf(21)})).toBeVisible()
        await expect.element(page.getByRole("link", {name: "198.51.100.7"})).toBeVisible()
        await expect.element(page.getByRole("link", {name: "sibling (SIB1)"})).toBeVisible()
        await expect.element(page.getByLabelText("Nickname or fingerprint")).not.toBeInTheDocument()
        expect(mockBackend.requested).toEqual([`GET ${detailsPath(21)}`])
    })

    it("has an accessible name for the dialog", async () => {
        mockLocationWithThreeRelays()
        await open([charlie, alpha, bravo])

        await expect.element(page.getByRole("dialog", {name: /\S/})).toBeVisible()
    })

    it("formats the relay's numbers and flags", async () => {
        mockBackend.onGet(detailsPath(21), makeRelayDetails({
            nickname: "alpha",
            uptime: 7200,
            bandwidthRate: 2_000_000,
            hibernating: true,
        }))
        await open([makeRelay({fingerprint: fingerprintOf(21), nickname: "alpha", flags: [RelayFlag.Exit, RelayFlag.Fast]})])

        await expect.element(page.getByText("2.00 hours")).toBeVisible()
        await expect.element(page.getByText("2.00 MB/s")).toBeVisible()
        await expect.element(page.getByText("Exit, Fast")).toBeVisible()
        await expect.element(page.getByText("Example AS (64500)")).toBeVisible()
    })

    it("lists the families the relay certified", async () => {
        mockBackend.onGet(detailsPath(21), makeRelayDetails({
            nickname: "alpha",
            familyKeys: ["ed25519:one", "ed25519:two"],
        }))
        await open([makeRelay({fingerprint: fingerprintOf(21), nickname: "alpha"})])

        await expect.element(page.getByText("Certified families")).toBeVisible()
        await expect.element(page.getByText("ed25519:one")).toBeVisible()
        await expect.element(page.getByText("ed25519:two")).toBeVisible()
    })

    it("lists several relays at one location and opens the chosen one", async () => {
        mockLocationWithThreeRelays()
        await open([charlie, alpha, bravo])

        await expect.element(page.getByRole("button", {name: "alpha"})).toBeVisible()
        await expect.element(page.getByRole("button", {name: "bravo"})).toBeVisible()
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
        // The relays of the day carry fingerprint and nickname, so the list needs no request
        expect(mockBackend.requested.filter(request => !request.startsWith("GET /v1/relays/"))).toEqual([])

        await page.getByRole("button", {name: "bravo"}).click()
        await expect.element(page.getByRole("link", {name: fingerprintOf(22)})).toBeVisible()
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

    it("shows the search results on the map", async () => {
        mockLocationWithThreeRelays()
        await open([alpha, bravo, charlie])
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
        await expect.element(page.getByRole("button", {name: /on map/})).not.toBeInTheDocument()

        await page.getByLabelText("Nickname or fingerprint").fill("a")
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
        await page.getByRole("button", {name: "Show 3 results on map"}).click()

        await expect.element(page.getByRole("dialog")).not.toBeInTheDocument()
        await expect.element(page.getByRole("status", {name: "search on map"}))
            .toHaveTextContent(JSON.stringify({search: "a", fingerprints: [21, 22, 23].map(fingerprintOf)}))
    })

    it("shows a message when nothing matches the search", async () => {
        mockLocationWithThreeRelays()
        await open([alpha, bravo, charlie])
        await page.getByLabelText("Nickname or fingerprint").fill("zzz")

        await expect.element(page.getByText("No results found")).toBeVisible()
        await expect.element(page.getByRole("button", {name: /on map/})).not.toBeInTheDocument()
    })

    it("restricts the map to the relay's type", async () => {
        mockBackend.onGet(detailsPath(21), makeRelayDetails({nickname: "alpha"}))
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
        mockBackend.onGet(detailsPath(21), makeRelayDetails({nickname: "alpha", familyId: 7}))
        await open([alpha])
        await page.getByRole("button", {name: "select family"}).first().click()

        await expect.poll(() => settingsOutput().element().textContent).toMatch(/"selectedFamily":7,"sortFamily":true/)
    })

    it("warns when the relays have no details", async () => {
        await open([makeRelay({hasDetails: false}), makeRelay({hasDetails: false})])

        await expect.element(page.getByText(SnackbarMessage.NoRelayDetails)).toBeVisible()
        expect(mockBackend.requested).toEqual([])
    })

    it("does not block the page when the relays have no details", async () => {
        // The app mounts the dialog the first time it is opened (see LeafletLayers), so it is mounted already open
        await renderWithProviders(<LazyHarness relays={[makeRelay({hasDetails: false})]}/>)
        await page.getByRole("button", {name: "Open"}).click()
        await expect.element(page.getByText(SnackbarMessage.NoRelayDetails)).toBeVisible()

        // A dialog, which was opened and closed again, must not leave its backdrop or scroll lock behind
        await expect.poll(() => document.querySelector(".MuiModal-root:not(.MuiPopper-root)")).toBeNull()
        expect(document.body.style.overflow).not.toBe("hidden")
    })

    it("reports failed details", async () => {
        mockBackend.onGet(detailsPath(21), () => networkError())
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
        await expect.element(page.getByRole("link", {name: fingerprintOf(22)})).toBeVisible()

        await page.getByRole("button", {name: "Back"}).click()
        await expect.element(page.getByRole("button", {name: "charlie"})).toBeVisible()
        await expect.element(page.getByRole("link", {name: fingerprintOf(22)})).not.toBeInTheDocument()
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
            makeRelay({fingerprint: fingerprintOf(1000 + index), nickname: `relay${String(index).padStart(3, "0")}`}))
        mockBackend.onGet(detailsPathPattern, ({match}) => makeRelayDetails({fingerprint: match![1]}))
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
