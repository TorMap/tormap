import {useMediaQuery, useTheme} from "@mui/material";
import {useSnackbar} from "notistack";
import React, {FunctionComponent, useEffect, useState} from "react";

import {RelayFamilyIdentifier} from "../../../dto/relay";
import {SnackbarMessage} from "../../../types/ui";
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
}

export interface FamilySelectionDialogProps {
    shouldShowDialog: boolean
    closeDialog: () => void
    isLoading: boolean
    familyIdentifiers?: RelayFamilyIdentifier[]
}

export const FamilySelectionDialog: FunctionComponent<FamilySelectionProps> = ({
                                                                                   shouldShowDialog,
                                                                                   closeDialog,
                                                                                   reloadSelectedDay,
                                                                                   familyIds,
                                                                               }) => {
    // Component state
    const [isLoading, setIsLoading] = useState(true)
    const [familyIdentifiers, setFamilyIdentifiers] = useState<RelayFamilyIdentifier[]>()

    // App context
    const {enqueueSnackbar} = useSnackbar();

    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    /**
     * Query more information about the Families specified in "families" parameter
     */
    useEffect(() => {
        setFamilyIdentifiers([])
        if (familyIds.length > 0) {
            setIsLoading(true)
            backend.post<RelayFamilyIdentifier[]>(
                '/relay/details/family/identifiers',
                familyIds
            ).then(response => {
                const identifiers = response.data
                if (identifiers.length > 0) {
                    setFamilyIdentifiers(response.data)
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
    }, [closeDialog, enqueueSnackbar, familyIds, reloadSelectedDay])

    return (isLargeScreen ?
            <FamilySelectionDialogLarge
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                isLoading={isLoading}
                familyIdentifiers={familyIdentifiers}
            />
            : <FamilySelectionDialogSmall
                shouldShowDialog={shouldShowDialog}
                closeDialog={closeDialog}
                isLoading={isLoading}
                familyIdentifiers={familyIdentifiers}
            />
    )
}

export default FamilySelectionDialog
