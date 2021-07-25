export interface Settings{
    Guard: boolean
    Exit: boolean
    Default: boolean

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

    selectedCountry: string | undefined
    sortContry: boolean
    selectedFamily: string | undefined
}

export interface Statistics{
    guard: number
    exit: number
    default: number
    maxValueOnSameCoordinate?: number
}
