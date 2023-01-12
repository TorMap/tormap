import {RelayFlag, RelayType} from "./types/relay";
import {Settings} from "./types/settings";

export const backendApiUrl = import.meta.env.VITE_BACKEND_API_URL?.toString()

// Default settings, that are selected on page load
export const defaultSettings: Settings = {
    // Group relays by settings
    sortCountry: false,
    selectedCountry: undefined,
    sortFamily: false,
    selectedFamily: undefined,
    aggregateCoordinates: false,
    heatMap: false,

    showRelayTypes: {
        [RelayType.Exit]: true,
        [RelayType.Guard]: true,
        [RelayType.Other]: true,
    },

    relaysMustHaveFlag: {
        [RelayFlag.Authority]: false,
        [RelayFlag.BadExit]: false,
        [RelayFlag.Exit]: false,
        [RelayFlag.Fast]: false,
        [RelayFlag.Guard]: false,
        [RelayFlag.HSDir]: false,
        [RelayFlag.Named]: false,
        [RelayFlag.NoEdConsensus]: false,
        [RelayFlag.Running]: false,
        [RelayFlag.Stable]: false,
        [RelayFlag.StaleDesc]: false,
        [RelayFlag.Sybil]: false,
        [RelayFlag.Unnamed]: false,
        [RelayFlag.Valid]: false,
        [RelayFlag.V2Dir]: false,
    },
}

// The time it takes before the tooltip is shown
export const tooltipTimeDelay = 750

// Colors for relay types
export const Colors = {
    Exit: "#FF4848",
    Guard: "#FFD371",
    Default: "#b78aff",
}
