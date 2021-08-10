import {Settings} from "./types/variousTypes";

// Default settings, that are selected on page load
export const defaultSettings: Settings = {
    Guard: true,
    Exit: true,
    Default: true,

    miValid: false,
    miNamed: false,
    miUnnamed: false,
    miRunning: false,
    miStable: false,
    miExit: false,
    miFast: false,
    miGuard: false,
    miAuthority: false,
    miV2Dir: false,
    miHSDir: false,
    miNoEdConsensus: false,
    miStaleDesc: false,
    miSybil: false,
    miBadExit: false,

    showMarker: true,
    colorNodesAccordingToType: true,
    aggregateCoordinates: false,
    heatMap: false,

    dateRange: false,
    familyGradient: false,

    sortCountry: false,
    onlyCountry: false,
    selectedCountry: undefined,
    sortFamily: false,
    onlyFamily: false,
    selectedFamily: undefined,
}


// Colors for node types
export const Colors = {
    Exit: "#f96969",
    Guard: "#fcb045",
    Default: "#833ab4",
}