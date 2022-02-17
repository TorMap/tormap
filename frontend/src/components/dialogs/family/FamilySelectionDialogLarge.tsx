import {DialogContent, DialogTitle, IconButton} from "@mui/material";
import React, {FunctionComponent} from "react";
import CloseIcon from "@mui/icons-material/Close";
import {FullHeightDialog} from "../../../types/ui";
import {FamiliesTable} from "./FamiliesTable";
import {FamilySelectionDialogProps} from "./FamilySelectionDialog";

/**
 * A Dialog to select a Family from multiple Families
 */
export const FamilySelectionDialogLarge: FunctionComponent<FamilySelectionDialogProps> = ({
                                                                                                    shouldShowDialog,
                                                                                                    closeDialog,
                                                                                                    familyIds,
                                                                                                    isLoading,
                                                                                                    familyIdentifiers,
                                                                                                }) => {
    return (
        <FullHeightDialog
            open={shouldShowDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={familyIds.length > 1 ? "lg" : "md"}
        >
            <DialogTitle>
                Select a family
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
