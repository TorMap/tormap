import {Settings} from "../types/app-state";
import {RelayFlag, RelayType} from "../types/relay";

export const apiBaseUrl = "http://localhost:8080"

// Default settings, that are selected on page load
export const defaultSettings: Settings = {
    showRelayTypes: {
        [RelayType.Exit]: true,
        [RelayType.Guard]: true,
        [RelayType.Other]: true,
    },

    relaysMustIncludeFlag: {
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

    // Group relays by settings
    sortCountry: false,
    selectedCountry: undefined,
    sortFamily: false,
    selectedFamily: undefined,
    aggregateCoordinates: false,
    heatMap: false,
}

// The time it takes before the tooltip is shown
export const tooltipTimeDelay = 750

// Colors for node types
export const Colors = {
    Exit: "#FF4848",
    Guard: "#FFD371",
    Default: "#b78aff",
}
