import {useAtom} from "jotai";
import {FunctionComponent, useEffect, useState} from "react";

import {useDate} from "../../context/date-context";
import {useSettings} from "../../context/settings-context";
import {useSnackbar} from "../../context/snackbar-context";
import {CountryUsageDto} from "../../dto/relay";
import {SnackbarMessage} from "../../types/ui";
import {buildCountryUsageLayer, countryUsagePane, countryUsagePaneZIndex} from "../../util/country-usage";
import {CountryFeature, loadWorldFeatures} from "../../util/geojson";
import {backend} from "../../util/util";
import {countryUsageAtom} from "./country-usage-atoms";
import {useMap} from "./map";

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
    const [worldFeatures, setWorldFeatures] = useState<CountryFeature[]>()

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

    // Load the country borders once the estimates are shown, switching the setting again retries a failed load
    useEffect(() => {
        if (!settings.showCountryUsage || worldFeatures) return undefined
        let cancelled = false
        loadWorldFeatures().then(features => {
            if (!cancelled) setWorldFeatures(features)
        }).catch(() => {
            if (!cancelled) enqueueSnackbar(SnackbarMessage.CountryBordersFailed, {variant: "error"})
        })
        return () => {
            cancelled = true
        }
    }, [settings.showCountryUsage, worldFeatures, enqueueSnackbar])

    // Draw the estimates
    useEffect(() => {
        if (!countryUsage || countryUsage.usage.length === 0 || !worldFeatures) return
        if (!map.getPane(countryUsagePane)) {
            map.createPane(countryUsagePane).style.zIndex = String(countryUsagePaneZIndex)
        }
        const layer = buildCountryUsageLayer(countryUsage.usage, worldFeatures).addTo(map)
        return () => {
            layer.remove()
        }
    }, [map, countryUsage, worldFeatures])

    return null
}

export default CountryUsageLayer
