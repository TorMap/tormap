import {Settings} from "../types/variousTypes";

export const apiBaseUrl = "http://localhost:8080"

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

    aggregateCoordinates: false,
    heatMap: false,

    sortCountry: false,
    selectedCountry: undefined,
    sortFamily: false,
    selectedFamily: undefined,
}


// Colors for node types
export const Colors = {
    Exit: "#f96969",
    Guard: "#fcb045",
    Default: "#b337c6",
}