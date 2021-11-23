import React, {useContext, useState} from 'react';
import {Settings} from "../types/app-state";
import {relaysMustIncludeFlagInput, showRelayTypesInput} from "../components/UI/UI-elements/app-settings";

interface SettingsInterface {
    settings: Settings
    changeSettings: (event: React.ChangeEvent<HTMLInputElement>) => void
    setSettings: React.Dispatch<React.SetStateAction<Settings>>
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
export const SettingsProvider: React.FunctionComponent<SettingsProviderProps> = ({defaultSettings, children }) => {
    const [settings, setSettings] = useState<Settings>(defaultSettings)

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
                    relaysMustIncludeFlag: {...settings.relaysMustIncludeFlag, [event.target.id]: event.target.checked}
                })
                break;
            default:
                setSettings({...settings, [event.target.name]: event.target.checked})
        }
    };

    return (
        <SettingsContext.Provider value={{settings, changeSettings, setSettings}}>
                { children }
        </SettingsContext.Provider>
    )
}