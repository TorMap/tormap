import {useMediaQuery, useTheme} from "@mui/material";
import {FunctionComponent, useEffect, useMemo, useState} from "react";

import {useSnackbar} from "../../../context/snackbar-context";
import {FamilyV1Dto, RelayFamilyIdentifier, RelayLocationDto} from "../../../dto/relay";
import {SnackbarMessage} from "../../../types/ui";
import {RelayGroupSummary, summarizeRelays} from "../../../util/relay-group-summary";
import {toFamilyIdentifier} from "../../../util/api-v1";
import {backend} from "../../../util/util";
import {FamilySelectionDialogLarge} from "./FamilySelectionDialogLarge";
import {FamilySelectionDialogSmall} from "./FamilySelectionDialogSmall";

interface FamilySelectionProps {
    /**
     * Whether the modal should currently be visible
     */
    shouldShowDialog: boolean

    /**
     * Hide the modal
     */
    closeDialog: () => void

    /**
     * Trigger download of the current day
     */
    reloadSelectedDay: () => void

    /**
     * Families which the user can view detailed information about
     */
    familyIds: number[]

    /**
     * The relays of each family on the map
     */
    relayFamilyMap?: Map<number, RelayLocationDto[]>

    /**
     * The consensus weight of all relays on the map
     */
    totalConsensusWeight?: number

    /**
     * The month of the selected day (YYYY-MM), families are computed per month
     */
    month?: string
}

/**
 * A family with what the map shows of it
 */
export type FamilyRow = RelayFamilyIdentifier & RelayGroupSummary

export interface FamilySelectionDialogProps {
    shouldShowDialog: boolean
    closeDialog: () => void
    isLoading: boolean
    families?: FamilyRow[]
}

/**
 * Adds what the map shows of each family. Families without relays on the map keep the member count of the backend.
 */
export const buildFamilyRows = (
    familyIdentifiers: RelayFamilyIdentifier[],
    relayFamilyMap: Map<number, RelayLocationDto[]>,
    totalConsensusWeight: number,
): FamilyRow[] => familyIdentifiers.map(family => {
    const summary = summarizeRelays(relayFamilyMap.get(family.id) ?? [], totalConsensusWeight)
    return {...family, ...summary, relayCount: summary.relayCount || family.memberCount}
})

export const FamilySelectionDialog: FunctionComponent<FamilySelectionProps> = ({
                                                                                   shouldShowDialog,
                                                                                   closeDialog,
                                                                                   reloadSelectedDay,
                                                                                   familyIds,
                                                                                   relayFamilyMap,
                                                                                   totalConsensusWeight = 0,
                                                                                   month,
                                                                               }) => {
    // Component state
    const [isLoading, setIsLoading] = useState(true)
    const [familyIdentifiers, setFamilyIdentifiers] = useState<RelayFamilyIdentifier[]>([])

    // App context
    const {enqueueSnackbar} = useSnackbar();

    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    const families = useMemo(
        () => buildFamilyRows(familyIdentifiers, relayFamilyMap ?? new Map(), totalConsensusWeight),
        [familyIdentifiers, relayFamilyMap, totalConsensusWeight]
    )

    // Discard the results of the previous families
    const [syncedFamilyIds, setSyncedFamilyIds] = useState(familyIds)
    if (syncedFamilyIds !== familyIds) {
        setSyncedFamilyIds(familyIds)
        setFamilyIdentifiers([])
        if (familyIds.length > 0) {
            setIsLoading(true)
        }
    }

    /**
     * Query more information about the Families specified in "families" parameter
     */
    useEffect(() => {
        if (familyIds.length > 0 && month) {
            // All families of the month are one cached response, the dialog shows the ones on the map
            backend.get<FamilyV1Dto[]>(`/v1/families?month=${encodeURIComponent(month)}`).then(response => {
                const wanted = new Set(familyIds)
                const identifiers = response.data.filter(family => wanted.has(family.id)).map(toFamilyIdentifier)
                if (identifiers.length > 0) {
                    setFamilyIdentifiers(identifiers)
                } else {
                    closeDialog()
                    enqueueSnackbar(SnackbarMessage.UpdatedData, {variant: "success"})
                    reloadSelectedDay()
                }
                setIsLoading(false)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                setIsLoading(false)
            })
        }
    }, [closeDialog, enqueueSnackbar, familyIds, month, reloadSelectedDay])

    return (isLargeScreen ?
            <FamilySelectionDialogLarge
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                isLoading={isLoading}
                families={families}
            />
            : <FamilySelectionDialogSmall
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                isLoading={isLoading}
                families={families}
            />
    )
}

export default FamilySelectionDialog
