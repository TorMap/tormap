import {atom} from "jotai";

import {RelayLocationDto} from "../../../dto/relay";
import {RelayIdentifierMatch} from "../../../types/relay";

/*
 State shared between the relay details dialog and the rest of the app. Kept apart from the dialog components, so that
 importing it does not pull the lazily loaded dialog into the main bundle.
 */

export const showRelayDetailsDialogAtom = atom(false);
export const relaysForDetailsDialogAtom = atom<RelayLocationDto[]>([]);

export interface RelaySearchOnMap {
    /**
     * The search term which found the relays
     */
    search: string

    /**
     * The relays found by the search
     */
    relays: RelayLocationDto[]
}

/**
 * If set, the map only shows the relays of this search
 */
export const relaySearchOnMapAtom = atom<RelaySearchOnMap | undefined>(undefined);

export const relayDetailsDialogSearchAtom = atom("");
export const relayDetailsDialogOrderAtom = atom<keyof RelayIdentifierMatch>("nickname");
