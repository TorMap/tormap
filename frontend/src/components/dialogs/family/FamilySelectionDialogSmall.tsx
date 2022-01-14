import React, {FunctionComponent} from "react";
import {FamilySelectionDialogProps} from "./FamilySelectionUtil";
import {AppBar, Box, Button, Dialog, DialogActions, DialogContent, Toolbar, Typography} from "@mui/material";
import {SlideUpTransition} from "../../../types/ui";
import CloseIcon from "@mui/icons-material/Close";
import {FamiliesTable} from "./FamiliesTable";

export const FamilySelectionDialogSmall: FunctionComponent<FamilySelectionDialogProps> = ({
                                                                                              showDialog,
                                                                                              closeDialog,
                                                                                              familyIds,
                                                                                              isLoading,
                                                                                              familyIdentifiers,
                                                                                          }) => {
    return (
        <Box>
            <Dialog
                open={showDialog}
                onClose={closeDialog}
                fullScreen={true}
                TransitionComponent={SlideUpTransition}
            >
                <AppBar sx={{position: 'relative'}}>
                    <Toolbar>
                        <Typography variant="h6">
                            Families
                        </Typography>
                        <Button
                            aria-label="close"
                            sx={{
                                position: "absolute",
                                right: "15px",
                                top: "15px",
                            }}
                            variant={"outlined"}
                            onClick={closeDialog}
                            endIcon={<CloseIcon/>}
                        >
                            Close
                        </Button>
                    </Toolbar>
                </AppBar>
                <DialogContent
                    dividers
                >
                    <div>
                        {!isLoading ? <FamiliesTable familyIdentifiers={familyIdentifiers}
                                                     closeFamilySelectionDialog={closeDialog}/>
                            : <p>loading...</p>}
                    </div>
                </DialogContent>
                <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        autoFocus
                        onClick={closeDialog}
                        variant={"contained"}
                        size={"large"}
                        endIcon={<CloseIcon/>}
                    >
                        close
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    )
}