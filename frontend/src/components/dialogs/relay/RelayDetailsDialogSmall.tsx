import {Close as CloseIcon} from "@mui/icons-material";
import {AppBar, Button, Dialog, DialogActions, DialogContent, IconButton, Toolbar} from "@mui/material";
import {FunctionComponent, useState} from "react";

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
    const [showRelayDetails, setShowRelayDetails] = useState(!canShowRelayList)

    // Show the details directly if there is no list to choose from, whenever the dialog opens or the list changes
    const [syncedCanShowRelayList, setSyncedCanShowRelayList] = useState(canShowRelayList)
    const [syncedShowDialog, setSyncedShowDialog] = useState(showDialog)
    if (syncedCanShowRelayList !== canShowRelayList || syncedShowDialog !== showDialog) {
        setSyncedCanShowRelayList(canShowRelayList)
        setSyncedShowDialog(showDialog)
        setShowRelayDetails(!canShowRelayList)
    }

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
                slots={{transition: SlideUpTransition}}
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
                        relayDetailsMatch ?
                            <RelayDetailsTable
                                relayDetailsMatch={relayDetailsMatch}
                                closeDialog={closeDialog}
                            /> :
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
