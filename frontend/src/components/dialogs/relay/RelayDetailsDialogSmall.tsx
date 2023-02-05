import CloseIcon from "@mui/icons-material/Close";
import {AppBar, Button, Dialog, DialogActions, DialogContent, IconButton, Toolbar} from "@mui/material";
import React, {FunctionComponent, useEffect, useState} from "react";

import {SlideUpTransition} from "../../../types/ui";
import {LoadingAnimation} from "../../loading/LoadingAnimation";
import {RelayDetailsHeader} from "./RelayDetailsHeader";
import {RelayDetailsSelectionHeader} from "./RelayDetailsSelectionHeader";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {RelayList} from "./RelayList";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";


export const RelayDetailsDialogSmall: FunctionComponent<DetailsDialogProps> = ({
                                                                                   showDialog,
                                                                                   closeDialog,
                                                                                   relayDetailsMatch,
                                                                                   filteredRelayMatches,
                                                                                   relayDetailsId,
                                                                                   setRelayDetailsId,
                                                                                   canShowRelayList
                                                                               }) => {
    // Component state
    const [showRelayDetails, setShowRelayDetails] = useState(false)

    useEffect(() => {
        setShowRelayDetails(!canShowRelayList)
    }, [canShowRelayList, showDialog])

    const handleSelectDetails = (id: number) => {
        setRelayDetailsId(id)
        setShowRelayDetails(true)
    }

    function closeDialogOrOnlyRelayDetails() {
        if (canShowRelayList && showRelayDetails) {
            setShowRelayDetails(false)
        } else {
            closeDialog()
        }
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
                        {showRelayDetails ?
                            <RelayDetailsHeader
                                closeDialog={closeDialogOrOnlyRelayDetails}
                                finishQuickAction={closeDialog}
                                relayDetailsMatch={relayDetailsMatch}
                            /> :
                            <>
                                <RelayDetailsSelectionHeader/>
                                <IconButton aria-label="close" sx={{
                                    position: "absolute",
                                    right: "16px",
                                }} onClick={closeDialog}>
                                    <CloseIcon/>
                                </IconButton>
                            </>
                        }
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    {showRelayDetails ?
                        relayDetailsMatch ? <RelayDetailsTable relayDetailsMatch={relayDetailsMatch}/> :
                            <LoadingAnimation/> :
                        <RelayList
                            relayMatches={filteredRelayMatches}
                            selectedRelayId={relayDetailsId}
                            setSelectedRelayId={handleSelectDetails}
                        />
                    }
                </DialogContent>
                <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        onClick={closeDialogOrOnlyRelayDetails}
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
