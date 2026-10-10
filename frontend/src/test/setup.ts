// Registers `page.render` and its types for browser-mode component tests
import "vitest-browser-react";

import {afterEach, beforeEach, expect} from "vitest";

import {mockBackend} from "./mock-backend";

beforeEach(() => {
    mockBackend.reset()
    mockBackend.install()
})

afterEach(() => {
    // An unmocked request usually means a test forgot a route, and the app would swallow the error
    const unhandled = [...mockBackend.unhandled]
    mockBackend.reset()
    expect(unhandled, "requests without a mock").toEqual([])
})
