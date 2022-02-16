import React, {useContext, useEffect, useState} from 'react';
import {Settings} from "../types/settings";
import {relaysMustIncludeFlagInput, showRelayTypesInput} from "../components/accordion/AppSettings";

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
 * A provider, providing a Settings context that handles all settings
 * @param defaultSettings - a Settings object with the default settings
 * @param children - the child elements in the DOM
 */
export const SettingsProvider: React.FunctionComponent<SettingsProviderProps> = ({defaultSettings, children}) => {
    // Component state
    const [settings, setSettings] = useState<Settings>(defaultSettings)

    // Resets selection if grouping gets disabled
    useEffect(() => {
        if (!settings.sortCountry && settings.selectedCountry) {
            setSettings({...settings, selectedCountry: undefined})
        }
        if (!settings.sortFamily && settings.selectedFamily) {
            setSettings({...settings, selectedFamily: undefined})
        }
    }, [settings, setSettings])

    /**
     * input event handler for setting changes
     * @param event
     */
    const changeSettings = (event: React.ChangeEvent<HTMLInputElement>) => {
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
        <SettingsContext.Provider value={{settings, changeSettings, setSettings}}>
            {children}
        </SettingsContext.Provider>
    )
}