// noinspection JSUnusedGlobalSymbols

import {RelayFlag} from "./relay";

export interface GeoRelayView {
    lat: number
    long: number
    country: string
    flags?: RelayFlag[]
    detailsId: string
    familyId: number
}
