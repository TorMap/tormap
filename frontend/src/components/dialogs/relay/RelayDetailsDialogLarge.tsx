import React from "react";
import {Dialog, DialogTitle, Divider, Grid} from "@mui/material";
import {RelayList} from "./RelayList";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";
import {RelayDetailsSelectionHeader} from "./RelayDetailsSelectionHeader";
import {RelayDetailsHeader} from "./RelayDetailsHeader";

export const RelayDetailsDialogLarge: React.FunctionComponent<DetailsDialogProps> = ({
                                                                                         shouldShowDialog,
                                                                                         closeDialog,
                                                                                         relayDetailsMatch,
                                                                                         sortedRelayMatches,
                                                                                         sortRelaysBy,
                                                                                         handleSelectSortByChange,
                                                                                         relayDetailsId,
                                                                                         setRelayDetailsId,
                                                                                     }) => {
    return (
        <Dialog
            open={shouldShowDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={sortedRelayMatches.length > 1 ? "lg" : "md"}
            fullWidth={true}
            sx={{
                paper: {
                    height: '70vh',
                },
            }}
        >
            <DialogTitle>
                <Grid container>
                    {sortedRelayMatches.length > 1 && <Grid item xs={12} sm={3}>
                        <RelayDetailsSelectionHeader
                            sortRelaysBy={sortRelaysBy}
                            handleSelectSortByChange={handleSelectSortByChange}
                        />
                    </Grid>}
                    <Grid item xs={12} sm={sortedRelayMatches.length > 1 ? 9 : 12}>
                        <RelayDetailsHeader
                            closeDialog={closeDialog}
                            relayDetailsMatch={relayDetailsMatch}
                        />
                    </Grid>
                </Grid>
            </DialogTitle>
            <Divider/>
            <Grid container>
                {sortedRelayMatches.length > 1 && <Grid item xs={12} sm={3} sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    <RelayList
                        relayMatches={sortedRelayMatches}
                        selectedRelayId={relayDetailsId}
                        setSelectedRelayId={setRelayDetailsId}
                    />
                </Grid>}
                <Grid item xs={12} sm={sortedRelayMatches.length > 1 ? 9 : 12}
                      sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    {relayDetailsMatch && <RelayDetailsTable relayDetailsMatch={relayDetailsMatch}/>}
                </Grid>
            </Grid>
        </Dialog>
    )
}

