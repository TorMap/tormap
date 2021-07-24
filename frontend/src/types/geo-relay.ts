// noinspection JSUnusedGlobalSymbols

import {RelayFlag} from "./relay";

export interface GeoRelayView {
    lat: number
    long: number
    flags?: RelayFlag[]
    detailsId: string
    familyId: string
}
