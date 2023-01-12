import CloseIcon from "@mui/icons-material/Close";
import {
    AppBar,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    IconButton,
    Toolbar,
    Typography
} from "@mui/material";
import React, {FunctionComponent} from "react";

import {SlideUpTransition} from "../../../types/ui";
import {FamilySelectionDialogProps} from "./FamilySelectionDialog";
import {FamilySelectionTable} from "./FamilySelectionTable";

export const FamilySelectionDialogSmall: FunctionComponent<FamilySelectionDialogProps> = ({
                                                                                              shouldShowDialog,
                                                                                              closeDialog,
                                                                                              isLoading,
                                                                                              familyIdentifiers,
                                                                                          }) => {
    return (
        <>
            <Dialog
                open={shouldShowDialog}
                onClose={closeDialog}
                fullScreen={true}
                TransitionComponent={SlideUpTransition}
            >
                <AppBar sx={{position: 'relative'}}>
                    <Toolbar>
                        <Typography variant="h6">
                            Families
                        </Typography>
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "10px",
                            top: "10px",
                        }} onClick={closeDialog}>
                            <CloseIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <DialogContent
                    dividers
                >
                    {!isLoading ? <FamilySelectionTable familyIdentifiers={familyIdentifiers}
                                                        closeFamilySelectionDialog={closeDialog}/>
                        : <CircularProgress color={"inherit"} size={22.5} sx={{mt: 1}}/>}
                </DialogContent>
                <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        onClick={closeDialog}
                        variant={"contained"}
                        size={"large"}
                    >
                        Back
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    )
}
