import React, {FunctionComponent, useEffect, useState} from "react";
import {AppBar, Button, Dialog, DialogActions, DialogContent, IconButton, Toolbar} from "@mui/material";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {RelayList} from "./RelayList";
import CloseIcon from "@mui/icons-material/Close";
import {SlideUpTransition} from "../../../types/ui";
import {RelayDetailsHeader} from "./RelayDetailsHeader";
import {RelayDetailsSelectionHeader} from "./RelayDetailsSelectionHeader";
import {LoadingAnimation} from "../../loading/LoadingAnimation";


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
    const [showRelayDetails, setShowRelayDetails] = useState(false)

    // Show relay details directly if only one relay is selectable
    useEffect(() => {
        setShowRelayDetails(sortedRelayMatches.length <= 1)
    }, [sortedRelayMatches])

    const handleSelectDetails = (id: number) => {
        setRelayDetailsId(id)
        setShowRelayDetails(true)
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
                        {showRelayDetails ?
                            <RelayDetailsHeader
                                closeDialog={() => {
                                    if (sortedRelayMatches.length > 1) {
                                        setShowRelayDetails(false)
                                    } else {
                                        closeDialog()
                                    }
                                }}
                                finishQuickAction={closeDialog}
                                relayDetailsMatch={relayDetailsMatch}
                            /> : <>
                                <RelayDetailsSelectionHeader
                                    sortRelaysBy={sortRelaysBy}
                                    handleSelectSortByChange={handleSelectSortByChange}
                                />
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
                            relayMatches={sortedRelayMatches}
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
                        autoFocus
                        onClick={() => {
                            if (showRelayDetails) {
                                setShowRelayDetails(false)
                            } else {
                                closeDialog()
                            }
                        }}
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