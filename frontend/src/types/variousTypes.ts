export interface Settings{

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

    colorNodesAccordingToType: boolean
    aggregateCoordinates: boolean
    heatMap: boolean
    showMarker: boolean

    daterange: boolean
    familyGradient: boolean

    sortContry: boolean
    selectedCountry: string | undefined
    sortFamily: boolean
    selectedFamily: number | undefined
}

export interface Statistics{
    guard: number
    exit: number
    default: number
    maxValueOnSameCoordinate?: number
}
