import {Button} from "@mui/material";
import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";
import {renderHook} from "vitest-browser-react";

import {SnackbarProvider, useSnackbar} from "./snackbar-context";

type ProviderProps = Omit<React.ComponentProps<typeof SnackbarProvider>, "children">

const render = (props: ProviderProps = {}) => renderHook(() => useSnackbar(), {
    wrapper: ({children}) => (
        <SnackbarProvider maxSnack={3} autoHideDuration={60000} {...props}>{children}</SnackbarProvider>
    ),
})

const alerts = () => page.getByRole("alert").elements().map(element => element.textContent)

describe("SnackbarProvider", () => {
    it("shows a message with the variant and the default variant", async () => {
        const {result, act} = await render({variant: "info"})
        await act(() => {
            result.current.enqueueSnackbar("first")
            result.current.enqueueSnackbar("second", {variant: "error"})
        })
        await expect.element(page.getByRole("alert").filter({hasText: "first"})).toHaveClass(/MuiAlert-colorInfo/)
        await expect.element(page.getByRole("alert").filter({hasText: "second"})).toHaveClass(/MuiAlert-colorError/)
    })

    it("renders the action", async () => {
        const onRetry = vi.fn()
        const {result, act} = await render()
        await act(() => {
            result.current.enqueueSnackbar("failed", {action: <Button onClick={onRetry}>Retry</Button>})
        })
        await page.getByRole("button", {name: "Retry"}).click()
        expect(onRetry).toHaveBeenCalledOnce()
    })

    it("closes the oldest snackbar beyond maxSnack and shows the new one afterwards", async () => {
        const {result, act} = await render({maxSnack: 2})
        await act(() => {
            result.current.enqueueSnackbar("one")
            result.current.enqueueSnackbar("two")
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["one", "two"]))
        await act(() => {
            result.current.enqueueSnackbar("three")
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["two", "three"]))
    })

    it("keeps persisted snackbars when making room", async () => {
        const {result, act} = await render({maxSnack: 2})
        await act(() => {
            result.current.enqueueSnackbar("kept", {persist: true})
            result.current.enqueueSnackbar("two")
            result.current.enqueueSnackbar("three")
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["kept", "three"]))
    })

    it("drops duplicates by message, or by key if one is given", async () => {
        const {result, act} = await render({preventDuplicate: true})
        await act(() => {
            result.current.enqueueSnackbar("same")
            result.current.enqueueSnackbar("same")
            result.current.enqueueSnackbar("keyed", {key: "k"})
            result.current.enqueueSnackbar("other text", {key: "k"})
            // Allowed for this snackbar
            result.current.enqueueSnackbar("same", {preventDuplicate: false})
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["same", "keyed", "same"]))
    })

    it("allows duplicates without preventDuplicate", async () => {
        const {result, act} = await render()
        await act(() => {
            result.current.enqueueSnackbar("same")
            result.current.enqueueSnackbar("same")
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["same", "same"]))
    })

    it("closes a snackbar by key, also a persisted one", async () => {
        const {result, act} = await render()
        await act(() => {
            result.current.enqueueSnackbar("stays")
            result.current.enqueueSnackbar("goes", {key: "goes", persist: true})
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["stays", "goes"]))
        await act(() => result.current.closeSnackbar("goes"))
        await vi.waitFor(() => expect(alerts()).toEqual(["stays"]))
    })

    it("closes all shown snackbars without a key, but keeps the queued ones", async () => {
        const {result, act} = await render({maxSnack: 1})
        await act(() => {
            result.current.enqueueSnackbar("shown")
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["shown"]))
        // "shown" starts closing to make room, so "queued" waits in the queue
        await act(() => {
            result.current.enqueueSnackbar("queued")
            result.current.closeSnackbar()
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["queued"]))
    })

    it("closes even persisted snackbars when all shown ones are persisted", async () => {
        const {result, act} = await render({maxSnack: 1})
        await act(() => {
            result.current.enqueueSnackbar("persisted", {persist: true})
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["persisted"]))
        await act(() => {
            result.current.enqueueSnackbar("new")
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["new"]))
    })

    it("hides a snackbar after autoHideDuration, but not a persisted one", async () => {
        // Away from where earlier tests left the pointer, which would pause the timer
        const {result, act} = await render({autoHideDuration: 300, anchorOrigin: {vertical: "bottom", horizontal: "right"}})
        await act(() => {
            result.current.enqueueSnackbar("short")
            result.current.enqueueSnackbar("persisted", {persist: true})
        })
        await vi.waitFor(() => expect(alerts()).toEqual(["short", "persisted"]))
        await vi.waitFor(() => expect(alerts()).toEqual(["persisted"]), {timeout: 3000})
        await new Promise(resolve => setTimeout(resolve, 500))
        expect(alerts()).toEqual(["persisted"])
    })

    it("pauses auto hiding while the pointer is on the snackbar", async () => {
        // Long enough that the hover lands before the snackbar would hide, also on a slow CI runner where the hover
        // waits for the appear transition. The wait then outlasts the whole duration, so only the pause keeps it shown.
        const {result, act} = await render({autoHideDuration: 2000})
        await act(() => {
            result.current.enqueueSnackbar("hovered")
        })
        await page.getByRole("alert").hover()
        await new Promise(resolve => setTimeout(resolve, 2500))
        expect(alerts()).toEqual(["hovered"])
        // Resumes with half of autoHideDuration
        await page.getByRole("alert").unhover()
        await vi.waitFor(() => expect(alerts()).toEqual([]), {timeout: 3000})
    })

    it("fails outside of a SnackbarProvider", async () => {
        vi.spyOn(console, "error").mockImplementation(() => undefined)
        await expect(renderHook(() => useSnackbar())).rejects.toThrow("useSnackbar() can only be used in a descendant of <SnackbarProvider>")
    })
})
