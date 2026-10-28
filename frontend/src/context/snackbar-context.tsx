/*
 Stacked snackbars, replacing notistack 3.0.2 with the parts of its API that TorMap uses: enqueueSnackbar with a
 variant, key, action and persist, closeSnackbar for one or all shown snackbars, and the provider options maxSnack,
 autoHideDuration, variant, anchorOrigin and preventDuplicate. Queueing, duplicates and auto hiding follow notistack.
 Built on MUI's Alert, with notistack's colors. The queueing, duplicate and auto hiding logic is adapted from notistack
 3.0.2 (https://github.com/iamhosseindhv/notistack), which is licensed as follows:

 Copyright (c) © 2018-2020 Hossein Dehnokhalaji

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import {Alert, Box, Grow} from "@mui/material";
import React, {useCallback, useContext, useEffect, useMemo, useRef, useState} from "react";

export type VariantType = "default" | "info" | "success" | "warning" | "error"
export type SnackbarKey = string | number

export interface OptionsObject {
    variant?: VariantType
    /** Identifies the snackbar for closeSnackbar and, with preventDuplicate, for duplicates */
    key?: SnackbarKey
    action?: React.ReactNode
    /** Stays until closed with closeSnackbar */
    persist?: boolean
    /** Overrides preventDuplicate of the provider for this snackbar */
    preventDuplicate?: boolean
}

interface SnackbarInterface {
    /** Shows the message, or queues it while maxSnack are shown. Returns the key, also when dropped as a duplicate. */
    enqueueSnackbar: (message: string, options?: OptionsObject) => SnackbarKey
    /** Closes the snackbar with the key, or all shown snackbars without a key */
    closeSnackbar: (key?: SnackbarKey) => void
}

interface SnackbarItem {
    key: SnackbarKey
    message: string
    variant: VariantType
    action?: React.ReactNode
    persist: boolean
    open: boolean
}

const SnackbarContext = React.createContext<SnackbarInterface | null>(null)

/**
 * The Context Hook for snackbars provided in the SnackbarProvider
 */
export function useSnackbar(): SnackbarInterface {
    const context = useContext(SnackbarContext)
    if (context === null) {
        throw new Error("useSnackbar() can only be used in a descendant of <SnackbarProvider>")
    }
    return context
}

interface SnackbarProviderProps {
    children?: React.ReactNode
    maxSnack?: number
    autoHideDuration?: number
    variant?: VariantType
    anchorOrigin?: { vertical: "top" | "bottom", horizontal: "left" | "center" | "right" }
    preventDuplicate?: boolean
}

// notistack's colors, white text on all of them
const variantColors: Record<VariantType, string> = {
    default: "#313131",
    info: "#2196f3",
    success: "#43a047",
    warning: "#ff9800",
    error: "#d32f2f",
}

let nextKey = 0

interface SnackbarState {
    // Shown snackbars, including closing ones until their exit animation ends
    shown: SnackbarItem[]
    queue: SnackbarItem[]
}

export const SnackbarProvider = ({
                                     children,
                                     maxSnack = 3,
                                     autoHideDuration = 5000,
                                     variant = "default",
                                     anchorOrigin = {vertical: "bottom", horizontal: "left"},
                                     preventDuplicate = false,
                                 }: SnackbarProviderProps) => {
    const [state, setState] = useState<SnackbarState>({shown: [], queue: []})

    // The reducers below follow notistack: a new snackbar beyond maxSnack starts closing the oldest one that is not
    // persisted, and the next queued snackbar is shown once a closing one has left the screen.
    const showNext = useCallback(({shown, queue}: SnackbarState): SnackbarState =>
        queue.length > 0 ? {shown: [...shown, queue[0]], queue: queue.slice(1)} : {shown, queue}, [])

    const dismissOldest = useCallback((current: SnackbarState): SnackbarState => {
        if (current.shown.some(item => !item.open)) return current
        const allPersisted = current.shown.filter(item => item.open && item.persist).length === maxSnack
        const oldest = current.shown.find(item => !item.persist || allPersisted)
        return {...current, shown: current.shown.map(item => item === oldest ? {...item, open: false} : item)}
    }, [maxSnack])

    const enqueueSnackbar = useCallback((message: string, options: OptionsObject = {}) => {
        const hasSpecifiedKey = options.key !== undefined
        const key = options.key ?? `snackbar-${nextKey++}`
        const item: SnackbarItem = {
            key,
            message,
            variant: options.variant ?? variant,
            action: options.action,
            persist: options.persist ?? false,
            open: true,
        }
        setState(current => {
            if (options.preventDuplicate ?? preventDuplicate) {
                const isDuplicate = (other: SnackbarItem) => hasSpecifiedKey ? other.key === key : other.message === message
                if (current.queue.some(isDuplicate) || current.shown.some(isDuplicate)) return current
            }
            const queued = {...current, queue: [...current.queue, item]}
            return current.shown.length >= maxSnack ? dismissOldest(queued) : showNext(queued)
        })
        return key
    }, [preventDuplicate, variant, maxSnack, dismissOldest, showNext])

    const closeSnackbar = useCallback((key?: SnackbarKey) => {
        // Like notistack, closing all snackbars only closes the shown ones and keeps the queue
        setState(current => ({
            shown: current.shown.map(item => key === undefined || item.key === key ? {...item, open: false} : item),
            queue: current.queue.filter(item => item.key !== key),
        }))
    }, [])

    const handleExited = useCallback((key: SnackbarKey) => {
        setState(current => {
            const next = showNext({...current, shown: current.shown.filter(item => item.key !== key)})
            return next.queue.length === 0 ? next : dismissOldest(next)
        })
    }, [showNext, dismissOldest])

    const context = useMemo(() => ({enqueueSnackbar, closeSnackbar}), [enqueueSnackbar, closeSnackbar])
    const isTop = anchorOrigin.vertical === "top"

    return (
        <SnackbarContext value={context}>
            {children}
            <Box sx={{
                position: "fixed",
                zIndex: "snackbar",
                display: "flex",
                flexDirection: isTop ? "column" : "column-reverse",
                alignItems: {left: "flex-start", center: "center", right: "flex-end"}[anchorOrigin.horizontal],
                gap: 1,
                [anchorOrigin.vertical]: {xs: 14, sm: 20},
                left: {xs: 8, sm: anchorOrigin.horizontal === "right" ? "auto" : 20},
                right: {xs: 8, sm: anchorOrigin.horizontal === "left" ? "auto" : 20},
                pointerEvents: "none",
            }}>
                {state.shown.map(item => (
                    <SnackbarAlert
                        key={item.key}
                        item={item}
                        autoHideDuration={autoHideDuration}
                        onClose={closeSnackbar}
                        onExited={handleExited}
                    />
                ))}
            </Box>
        </SnackbarContext>
    )
}

interface SnackbarAlertProps {
    item: SnackbarItem
    autoHideDuration: number
    onClose: (key: SnackbarKey) => void
    onExited: (key: SnackbarKey) => void
}

/**
 * One snackbar. Like notistack, it closes itself after autoHideDuration unless persisted. Pointing at it or leaving the
 * window pauses the time, and it then closes after half of autoHideDuration. A snackbar closed before it finished
 * appearing first appears, so its exit (which shows the next queued one) always runs.
 */
const SnackbarAlert = ({item, autoHideDuration, onClose, onExited}: SnackbarAlertProps) => {
    const timer = useRef<ReturnType<typeof setTimeout>>(undefined)
    const [entered, setEntered] = useState(false)
    const {key, open, persist} = item

    const pause = useCallback(() => clearTimeout(timer.current), [])
    const start = useCallback((duration: number) => {
        clearTimeout(timer.current)
        if (!persist) timer.current = setTimeout(() => onClose(key), duration)
    }, [persist, onClose, key])
    const resume = useCallback(() => start(autoHideDuration * 0.5), [start, autoHideDuration])

    useEffect(() => {
        if (!open) return
        start(autoHideDuration)
        window.addEventListener("focus", resume)
        window.addEventListener("blur", pause)
        return () => {
            clearTimeout(timer.current)
            window.removeEventListener("focus", resume)
            window.removeEventListener("blur", pause)
        }
    }, [open, autoHideDuration, start, resume, pause])

    return (
        <Grow in={open || !entered} appear onEntered={() => setEntered(true)} onExited={() => onExited(key)}>
            <Alert
                variant="filled"
                severity={item.variant === "default" ? "info" : item.variant}
                icon={item.variant === "default" ? false : undefined}
                action={item.action}
                onMouseEnter={pause}
                onMouseLeave={resume}
                sx={{
                    pointerEvents: "auto",
                    backgroundColor: variantColors[item.variant],
                    color: "#fff",
                    fontWeight: "fontWeightRegular",
                    alignItems: "center",
                    minWidth: {sm: 288},
                    maxWidth: "100%",
                    boxShadow: 6,
                }}
            >
                {item.message}
            </Alert>
        </Grow>
    )
}
