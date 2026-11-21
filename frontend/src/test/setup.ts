// Registers `page.render` and its types for browser-mode component tests
import "vitest-browser-react";

import {afterEach, beforeEach, expect} from "vitest";

import {clearRelayMonthsCache} from "../components/dialogs/relay/RelayMonths";
import {mockBackend} from "./mock-backend";

beforeEach(() => {
    // Settings are persisted, so they would leak between tests
    localStorage.clear()
    // The map position and day are kept in the URL hash, so it would leak between tests too
    window.history.replaceState(null, "", window.location.pathname + window.location.search)
    mockBackend.reset()
    mockBackend.install()
    // The relay details show the months of the relay. Without a test's own mock, a relay has no history.
    mockBackend.onGet(/^\/v1\/relays\/[0-9A-F]{40}\/months$/, [])
    clearRelayMonthsCache()
})

afterEach(() => {
    // An unmocked request usually means a test forgot a route, and the app would swallow the error
    const unhandled = [...mockBackend.unhandled]
    mockBackend.reset()
    expect(unhandled, "requests without a mock").toEqual([])
})
