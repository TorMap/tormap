import React, {useCallback, useContext, useMemo, useState} from 'react';

import {relaysMustIncludeFlagInput, showRelayTypesInput} from "../components/accordion/AppSettings";
import {Settings} from "../types/settings";

interface SettingsInterface {
    settings: Settings
    changeSettings: (event: React.ChangeEvent<HTMLInputElement>) => void
    setSettings: (s: Settings) => void
    resetSettings: () => void
    isDefaultSettings: boolean
}

const SettingsContext = React.createContext<SettingsInterface | null>(null)

/**
 * The Context Hook for Settings provided in the SettingsProvider
 */
export function useSettings() {
     
    return useContext(SettingsContext)!
}

interface SettingsProviderProps {
    children?: React.ReactNode;
    defaultSettings: Settings;
}

/**
 * Compares two Settings objects by value, including the nested relay type and flag records
 */
export function settingsEqual(a: Settings, b: Settings): boolean {
    const keys = new Set([...Object.keys(a), ...Object.keys(b)]) as Set<keyof Settings>
    for (const key of keys) {
        const valueA = a[key]
        const valueB = b[key]
        if (typeof valueA === "object" && typeof valueB === "object") {
            const nestedA = valueA as Record<string, boolean>
            const nestedB = valueB as Record<string, boolean>
            const nestedKeys = new Set([...Object.keys(nestedA), ...Object.keys(nestedB)])
            for (const nestedKey of nestedKeys) {
                if (nestedA[nestedKey] !== nestedB[nestedKey]) return false
            }
        } else if (valueA !== valueB) {
            return false
        }
    }
    return true
}

/**
 * Resets the selection if the corresponding grouping is disabled
 */
function withValidSelection(settings: Settings): Settings {
    let result = settings
    if (!result.sortCountry && result.selectedCountry) {
        result = {...result, selectedCountry: undefined}
    }
    if (!result.sortFamily && result.selectedFamily) {
        result = {...result, selectedFamily: undefined}
    }
    return result
}

/**
 * A provider, providing a Settings context that handles all settings
 * @param defaultSettings - a Settings object with the default settings
 * @param children - the child elements in the DOM
 */
export const SettingsProvider: React.FunctionComponent<SettingsProviderProps> = ({defaultSettings, children}) => {
    // Component state
    const [settings, setSettingsState] = useState<Settings>(() => withValidSelection(defaultSettings))

    const setSettings = useCallback((newSettings: Settings) => {
        setSettingsState(withValidSelection(newSettings))
    }, [])

    const resetSettings = useCallback(() => {
        setSettingsState(withValidSelection(defaultSettings))
    }, [defaultSettings])

    const isDefaultSettings = useMemo(
        () => settingsEqual(settings, withValidSelection(defaultSettings)),
        [settings, defaultSettings]
    )

    /**
     * input event handler for setting changes
     * @param event
     */
    const changeSetting = (event: React.ChangeEvent<HTMLInputElement>) => {
        switch (event.target.name) {
            case showRelayTypesInput:
                setSettings({
                    ...settings,
                    showRelayTypes: {...settings.showRelayTypes, [event.target.id]: event.target.checked}
                })
                break;
            case relaysMustIncludeFlagInput:
                setSettings({
                    ...settings,
                    relaysMustHaveFlag: {...settings.relaysMustHaveFlag, [event.target.id]: event.target.checked}
                })
                break;
            default:
                setSettings({...settings, [event.target.name]: event.target.checked})
        }
    };

    return (
        <SettingsContext.Provider value={{settings, changeSettings: changeSetting, setSettings, resetSettings, isDefaultSettings}}>
            {children}
        </SettingsContext.Provider>
    )
}
