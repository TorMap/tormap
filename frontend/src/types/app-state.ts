import {RelayFlag, RelayType} from "./relay";

export interface Settings {
    // Grouping settings
    sortCountry: boolean
    selectedCountry: string | undefined
    sortFamily: boolean
    selectedFamily: number | undefined
    aggregateCoordinates: boolean
    heatMap: boolean

    showRelayTypes: Record<RelayType, boolean>
    relaysMustIncludeFlag: Record<RelayFlag, boolean>
}

export interface Statistics {
    relayGuardCount: number
    relayExitCount: number
    relayOtherCount: number
    familyCount?: number
    countryCount?: number
}