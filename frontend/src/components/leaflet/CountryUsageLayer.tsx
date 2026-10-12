import {useAtom} from "jotai";
import {useSnackbar} from "notistack";
import {FunctionComponent, useEffect} from "react";
import {useMap} from "react-leaflet";

import {useDate} from "../../context/date-context";
import {useSettings} from "../../context/settings-context";
import {CountryUsageDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {buildCountryUsageLayer, countryUsagePane, countryUsagePaneZIndex} from "../../util/country-usage";
import {backend} from "../../util/util";
import {countryUsageAtom} from "./country-usage-atoms";

/**
 * Colors the countries of the map by their estimated number of Tor users, if enabled in the settings.
 * Has to be a child of the map.
 */
export const CountryUsageLayer: FunctionComponent = () => {
    const map = useMap()
    const {settings} = useSettings()
    const {selectedDate} = useDate()
    const {enqueueSnackbar} = useSnackbar()
    const [countryUsage, setCountryUsage] = useAtom(countryUsageAtom)

    // Load the estimates of the selected day
    useEffect(() => {
        if (!settings.showCountryUsage || !selectedDate) {
            setCountryUsage(undefined)
            return
        }
        let cancelled = false
        backend.get<CountryUsageDto[]>(`/relay/country-usage/day/${selectedDate}`).then(response => {
            if (cancelled) return
            // An older backend does not know the endpoint and a proxy may answer with something else
            if (Array.isArray(response.data)) {
                setCountryUsage({day: selectedDate, usage: response.data})
            } else {
                setCountryUsage(undefined)
            }
        }).catch(() => {
            if (cancelled) return
            setCountryUsage(undefined)
            enqueueSnackbar(SnackbarMessage.CountryUsageUnavailable, {
                variant: "warning",
                key: SnackbarMessage.CountryUsageUnavailable,
            })
        })
        return () => {
            cancelled = true
        }
    }, [settings.showCountryUsage, selectedDate, setCountryUsage, enqueueSnackbar])

    // Draw the estimates
    useEffect(() => {
        if (!countryUsage || countryUsage.usage.length === 0) return
        if (!map.getPane(countryUsagePane)) {
            map.createPane(countryUsagePane).style.zIndex = String(countryUsagePaneZIndex)
        }
        const layer = buildCountryUsageLayer(countryUsage.usage).addTo(map)
        return () => {
            layer.remove()
        }
    }, [map, countryUsage])

    return null
}

export default CountryUsageLayer
