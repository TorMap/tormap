import React, {useCallback, useContext, useState} from 'react';

import {relaysMustIncludeFlagInput, showRelayTypesInput} from "../components/accordion/AppSettings";
import {Settings} from "../types/settings";

interface SettingsInterface {
    settings: Settings
    changeSettings: (event: React.ChangeEvent<HTMLInputElement>) => void
    setSettings: (s: Settings) => void
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
        <SettingsContext.Provider value={{settings, changeSettings: changeSetting, setSettings}}>
            {children}
        </SettingsContext.Provider>
    )
}
