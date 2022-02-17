import React, {FunctionComponent, useEffect, useState} from "react";
import {AppBar, Button, Dialog, DialogActions, DialogContent, IconButton, Toolbar} from "@mui/material";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {RelayList} from "./RelayList";
import CloseIcon from "@mui/icons-material/Close";
import {SlideLeftTransition, SlideUpTransition} from "../../../types/ui";
import {RelayDetailsHeader} from "./RelayDetailsHeader";
import {RelayDetailsSelectionHeader} from "./RelayDetailsSelectionHeader";


export const RelayDetailsDialogSmall: FunctionComponent<DetailsDialogProps> = ({
                                                                                   showDialog,
                                                                                   closeDialog,
                                                                                   relayLocations,
                                                                                   sortRelaysBy,
                                                                                   handleSelectSortByChange,
                                                                                   setRelayDetailsId,
                                                                                   sortedRelayMatches,
                                                                                   relayDetailsId,
                                                                                   relayDetails,
                                                                                   relayLocation,
                                                                               }) => {
    // Component state
    const [showDetailsDialog, setShowDetailsDialog] = useState(false)

    // If relay selection is closed, set details dialog closed too
    useEffect(() => {
        if (!showDialog) setShowDetailsDialog(false)
    }, [showDialog])

    // Show relay details directly if only one relay is selectable
    useEffect(() => {
        if (relayLocations.length === 1) setShowDetailsDialog(true);
    }, [relayLocations])

    const handleDetailsDialogClose = () => {
        if (relayLocations.length === 1) {
            closeDialog()
            setShowDetailsDialog(false)
        } else {
            setShowDetailsDialog(false)
        }
    }

    const handleSelectDetails = (id: number) => {
        setRelayDetailsId(id)
        setShowDetailsDialog(true)
    }

    return (
        <>
            <Dialog
                open={showDialog}
                onClose={closeDialog}
                fullScreen={true}
                TransitionComponent={SlideUpTransition}
            >
                <AppBar sx={{position: 'relative'}}>
                    <Toolbar>
                        <RelayDetailsSelectionHeader
                            sortRelaysBy={sortRelaysBy}
                            handleSelectSortByChange={handleSelectSortByChange}
                        />
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "15px",
                            top: "15px",
                        }} onClick={closeDialog}>
                            <CloseIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    <RelayList
                        relayMatches={sortedRelayMatches}
                        selectedRelay={relayDetailsId}
                        setRelayDetailsId={handleSelectDetails}
                    />
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
                    >
                        Back
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog
                open={showDetailsDialog && showDialog}
                onClose={handleDetailsDialogClose}
                fullScreen={true}
                TransitionComponent={SlideLeftTransition}
            >
                <AppBar sx={{position: 'relative'}}>
                    <Toolbar>
                        <RelayDetailsHeader
                            closeDialog={closeDialog}
                            relayLocation={relayLocation}
                            relayDetails={relayDetails}
                        />
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    {relayDetails && relayLocation &&
                        <RelayDetailsTable relayDetails={relayDetails} relayLocation={relayLocation}/>
                    }
                </DialogContent>
                <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        autoFocus
                        onClick={handleDetailsDialogClose}
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