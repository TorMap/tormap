import React, {FunctionComponent, useEffect, useState} from "react";
import {useMediaQuery, useTheme} from "@mui/material";
import {useSnackbar} from "notistack";
import {FamilySelectionDialogLarge} from "./FamilySelectionDialogLarge";
import {FamilySelectionDialogSmall} from "./FamilySelectionDialogSmall";
import {backend} from "../../../util/util";
import {RelayFamilyIdentifier} from "../../../dto/relay";
import {SnackbarMessage} from "../../../types/ui";

interface FamilySeletionProps {
    /**
     * Whether the modal should currently be visible
     */
    showDialog: boolean

    /**
     * Hide the modal
     */
    closeDialog: () => void

    /**
     * Trigger download of the current day
     */
    refreshDayData: () => void

    /**
     * Families which the user can view detailed information about
     */
    familyIds: number[]

    /**
     * Callback for setting a family as selected
     * @param f the family ID
     */
    familySelectionCallback: (f: number) => void
}

export interface FamilySelectionDialogProps extends Omit<FamilySeletionProps, "refreshDayData"> {
    isLoading: boolean
    familyIdentifiers?: RelayFamilyIdentifier[]
}

export const FamilySelectionDialog: FunctionComponent<FamilySeletionProps> = ({
                                                                                  showDialog,
                                                                                  closeDialog,
                                                                                  refreshDayData,
                                                                                  familyIds,
                                                                                  familySelectionCallback,
                                                                              }) => {
    //Variables for deciding between small and large dialogs
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))
    // Snackbar
    const {enqueueSnackbar} = useSnackbar();
    // FamilySelectionDialog specific variables
    const [isLoading, setIsLoading] = useState(true)
    const [familyIdentifiers, setFamilyIdentifiers] = useState<RelayFamilyIdentifier[]>()

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
                    refreshDayData()
                }
                setIsLoading(false)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                setIsLoading(false)
            })
        }
    }, [closeDialog, enqueueSnackbar, familyIds, refreshDayData])

    return (isLargeScreen ?
            <FamilySelectionDialogLarge
                showDialog={showDialog}
                closeDialog={closeDialog}
                familySelectionCallback={familySelectionCallback}
                familyIds={familyIds}
                isLoading={isLoading}
                familyIdentifiers={familyIdentifiers}
            />
            : <FamilySelectionDialogSmall
                showDialog={showDialog}
                closeDialog={closeDialog}
                familySelectionCallback={familySelectionCallback}
                familyIds={familyIds}
                isLoading={isLoading}
                familyIdentifiers={familyIdentifiers}
            />
    )
}