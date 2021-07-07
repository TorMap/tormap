// noinspection JSUnusedGlobalSymbols

import {RelayFlag} from "./relay";

export interface ArchiveGeoRelayView {
    finger: string
    lat: number
    long: number
    flags?: RelayFlag[]
}
