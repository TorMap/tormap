import {createTheme, ThemeProvider} from "@mui/material";
import {createStore, Provider as JotaiProvider} from "jotai";
import {SnackbarProvider} from "notistack";
import React from "react";
import {page} from "vitest/browser";

import {DateProvider} from "../context/date-context";
import {SettingsProvider} from "../context/settings-context";
import {StatisticsProvider} from "../context/statistics-context";
import {Settings} from "../types/settings";
import {TorMapTheme} from "../types/TorMapTheme";
import {makeSettings} from "./fixtures";

interface ProviderOptions {
    /**
     * Overrides for the default app settings
     */
    settings?: Partial<Settings>
}

/**
 * Creates a wrapper with the same providers as `index.tsx`. Every call creates a fresh jotai store, so atoms do not leak between tests.
 * Pass it as `wrapper` to `renderHook`, or use `renderWithProviders` for components.
 */
export const createProvidersWrapper = ({settings}: ProviderOptions = {}) => {
    const store = createStore()
    const theme = createTheme(TorMapTheme)
    const defaultSettings = makeSettings(settings)

    return function ProvidersWrapper({children}: { children: React.ReactNode }) {
        return (
            <ThemeProvider theme={theme}>
                <SnackbarProvider maxSnack={3} autoHideDuration={4000} anchorOrigin={{vertical: "top", horizontal: "center"}}>
                    <JotaiProvider store={store}>
                        <SettingsProvider defaultSettings={defaultSettings}>
                            <DateProvider>
                                <StatisticsProvider>
                                    {children}
                                </StatisticsProvider>
                            </DateProvider>
                        </SettingsProvider>
                    </JotaiProvider>
                </SnackbarProvider>
            </ThemeProvider>
        )
    }
}

export const renderWithProviders = (ui: React.ReactNode, options?: ProviderOptions) =>
    page.render(ui, {wrapper: createProvidersWrapper(options)})
