export interface Settings {

    // Show relay types
    Guard: boolean
    Exit: boolean
    Default: boolean

    //Relay Must Include flags
    miValid: boolean
    miNamed: boolean
    miUnnamed: boolean
    miRunning: boolean
    miStable: boolean
    miExit: boolean
    miFast: boolean
    miGuard: boolean
    miAuthority: boolean
    miV2Dir: boolean
    miHSDir: boolean
    miNoEdConsensus: boolean
    miStaleDesc: boolean
    miSybil: boolean
    miBadExit: boolean

    aggregateCoordinates: boolean

    //Heatmap settings
    heatMap: boolean

    //Grouping settings
    sortCountry: boolean
    selectedCountry: string | undefined
    sortFamily: boolean
    selectedFamily: number | undefined
}

export interface Statistics {
    relayGuardCount: number
    relayExitCount: number
    relayOtherCount: number
    familyCount?: number
    countryCount?: number
}

export type SnackbarMessage = {
    message: string,
    severity: "error" | "warning" | "info" | "success",
}

export interface Mark {
    value: number
    label: JSX.Element
}
