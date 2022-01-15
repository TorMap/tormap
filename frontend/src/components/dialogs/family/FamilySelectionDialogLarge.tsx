import {DialogContent, DialogTitle, IconButton, Typography} from "@mui/material";
import React from "react";
import CloseIcon from "@mui/icons-material/Close";
import {FullHeightDialog} from "../../../types/ui";
import {FamiliesTable} from "./FamiliesTable";
import {FamilySelectionDialogProps} from "./FamilySelectionUtil";

/**
 *
 * A Dialog to select a Family from multiple Families
 * @param showDialog - Whether the family selection dialog should be displayed
 * @param closeDialog - Event handler for closing the dialog
 * @param families - The familyIDs available to select
 */
export const FamilySelectionDialogLarge: React.FunctionComponent<FamilySelectionDialogProps> = ({
                                                                                                    showDialog,
                                                                                                    closeDialog,
                                                                                                    familyIds,
                                                                                                    isLoading,
                                                                                                    familyIdentifiers,
                                                                                                }) => {
    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={familyIds.length > 1 ? "lg" : "md"}
        >
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
                {!isLoading ? <FamiliesTable familyIdentifiers={familyIdentifiers}
                                             closeFamilySelectionDialog={closeDialog}/>
                    : <p>loading...</p>}
            </DialogContent>
        </FullHeightDialog>
    )
}
