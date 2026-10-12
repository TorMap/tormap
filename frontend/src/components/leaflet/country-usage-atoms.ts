import {atom} from "jotai";

import {CountryUsageDto} from "../../dto/relay";

export interface CountryUsage {
    /**
     * The day the estimates belong to
     */
    day: string

    /**
     * The estimates of the countries, empty if there are none for the day
     */
    usage: CountryUsageDto[]
}

/**
 * The estimated users per country of the selected day, set while the layer is shown. Shared with the settings.
 */
export const countryUsageAtom = atom<CountryUsage | undefined>(undefined)
