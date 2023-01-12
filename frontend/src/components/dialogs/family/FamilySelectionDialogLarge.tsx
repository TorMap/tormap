import CloseIcon from "@mui/icons-material/Close";
import {CircularProgress, Dialog, DialogContent, DialogTitle, IconButton} from "@mui/material";
import React, {FunctionComponent} from "react";

import {FamilySelectionDialogProps} from "./FamilySelectionDialog";
import {FamilySelectionTable} from "./FamilySelectionTable";

/**
 * A Dialog to select a Family from multiple Families
 */
export const FamilySelectionDialogLarge: FunctionComponent<FamilySelectionDialogProps> = ({
                                                                                                    shouldShowDialog,
                                                                                                    closeDialog,
                                                                                                    isLoading,
                                                                                                    familyIdentifiers,
                                                                                                }) => {
    return (
        <Dialog
            open={shouldShowDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={"lg"}
            fullWidth={true}
            PaperProps={{
                sx: {
                    minHeight: "80vh",
                    maxHeight: "80vh",
                }
            }}
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
                {!isLoading ? <FamilySelectionTable familyIdentifiers={familyIdentifiers}
                                                    closeFamilySelectionDialog={closeDialog}/>
                    : <CircularProgress color={"inherit"} size={22.5} sx={{mt: 1}}/>}
            </DialogContent>
        </Dialog>
    )
}
