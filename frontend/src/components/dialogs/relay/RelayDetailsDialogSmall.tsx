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
                                                                                   shouldShowDialog,
                                                                                   closeDialog,
                                                                                   relayDetailsMatch,
                                                                                   sortedRelayMatches,
                                                                                   sortRelaysBy,
                                                                                   handleSelectSortByChange,
                                                                                   relayDetailsId,
                                                                                   setRelayDetailsId,
                                                                               }) => {
    // Component state
    const [showDetailsDialog, setShowDetailsDialog] = useState(false)

    // If relay selection is closed, set details dialog closed too
    useEffect(() => {
        if (!shouldShowDialog) setShowDetailsDialog(false)
    }, [shouldShowDialog])

    // Show relay details directly if only one relay is selectable
    useEffect(() => {
        if (sortedRelayMatches.length === 1) setShowDetailsDialog(true);
    }, [sortedRelayMatches])

    const handleDetailsDialogClose = () => {
        if (sortedRelayMatches.length === 1) {
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
                open={shouldShowDialog}
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
                        selectedRelayId={relayDetailsId}
                        setSelectedRelayId={handleSelectDetails}
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
                open={showDetailsDialog && shouldShowDialog}
                onClose={handleDetailsDialogClose}
                fullScreen={true}
                TransitionComponent={SlideLeftTransition}
            >
                <AppBar sx={{position: 'relative'}}>
                    <Toolbar>
                        <RelayDetailsHeader
                            closeDialog={closeDialog}
                            relayDetailsMatch={relayDetailsMatch}
                        />
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    {relayDetailsMatch &&
                        <RelayDetailsTable relayDetailsMatch={relayDetailsMatch}/>
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