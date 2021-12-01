import {DialogContent, DialogTitle, IconButton, Typography, useMediaQuery, useTheme} from "@mui/material";
import {RelayFamilyIdentifier} from "../../types/responses";
import React, {useEffect, useState} from "react";
import CloseIcon from "@material-ui/icons/Close";
import {FullHeightDialog, SnackbarMessage} from "../../types/ui";
import {backend} from "../../util/util";
import {useSnackbar} from "notistack";
import {FamiliesTable} from "./FamiliesTable";


interface Props {
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

/**
 *
 * A Dialog to select a Family from multiple Families
 * @param showDialog - Whether the family selection dialog should be displayed
 * @param closeDialog - Event handler for closing the dialog
 * @param families - The familyIDs available to select
 * @param familySelectionCallback - the callback function for selecting a family
 */
export const FamilySelectionDialogLarge: React.FunctionComponent<Props> = ({
                                                                          showDialog,
                                                                          closeDialog,
                                                                          refreshDayData,
                                                                          familyIds,
                                                                          familySelectionCallback,
                                                                      }) => {

    const [isLoading, setIsLoading] = useState(true)
    const [familyIdentifiers, setFamilyIdentifiers] = useState<RelayFamilyIdentifier[]>()

    const { enqueueSnackbar } = useSnackbar();

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

    const theme = useTheme()
    const desktop = useMediaQuery(theme.breakpoints.up("lg"))

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={familyIds.length > 1 ? "lg" : "md"}
            fullWidth={!desktop}
        >
            <div>
                <DialogTitle>
                    <Typography
                        variant="h6">Select a family</Typography>
                    <IconButton aria-label="close" sx={{
                        position: "absolute",
                        right: "10px",
                        top: "10px",
                    }} onClick={closeDialog}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent
                    dividers
                >
                    <div>
                        {!isLoading ? <FamiliesTable familyIdentifiers={familyIdentifiers}
                                                     familySelectionCallback={familySelectionCallback}/>
                            : <p>loading...</p>}
                    </div>
                </DialogContent>
            </div>
        </FullHeightDialog>
    )
}
