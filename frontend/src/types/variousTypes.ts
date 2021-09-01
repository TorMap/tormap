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

export const nameOfFactory = <T>() => (name: keyof T) => name;

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
