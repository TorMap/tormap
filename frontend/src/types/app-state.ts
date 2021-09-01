import {RelayFlag, RelayType} from "./relay";

export interface Settings {
    showRelayTypes: Record<RelayType, boolean>
    relaysMustIncludeFlag: Record<RelayFlag, boolean>

    // Grouping settings
    sortCountry: boolean
    selectedCountry: string | undefined
    sortFamily: boolean
    selectedFamily: number | undefined
    aggregateCoordinates: boolean
    heatMap: boolean
}

export interface Statistics {
    relayGuardCount: number
    relayExitCount: number
    relayOtherCount: number
    familyCount?: number
    countryCount?: number
}