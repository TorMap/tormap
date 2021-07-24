export interface Settings{
    nodeTypes: {
        Guard: boolean
        Exit: boolean
        Default: boolean
    }
    mustInclude: {
        Valid: boolean
        Named: boolean
        Unnamed: boolean
        Running: boolean
        Stable: boolean
        Exit: boolean
        Fast: boolean
        Guard: boolean
        Authority: boolean
        V2Dir: boolean
        HSDir: boolean
        NoEdConsensus: boolean
        StaleDesc: boolean
        Sybil: boolean
        BadExit: boolean
    }

    colorNodesAccordingToType: boolean
    aggregateCoordinates: boolean
    heatMap: boolean
    showMarker: boolean
}

export interface TempSettings{
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
}

export interface Statistics{
    guard: number
    exit: number
    default: number
    maxValueOnSameCoordinate?: number
}
