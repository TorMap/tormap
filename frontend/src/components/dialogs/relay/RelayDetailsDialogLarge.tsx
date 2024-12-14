import {Dialog, DialogTitle, Divider, Grid} from "@mui/material";
import React from "react";

import {RelayDetailsHeader} from "./RelayDetailsHeader";
import {RelayDetailsSelectionHeader} from "./RelayDetailsSelectionHeader";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {RelayList} from "./RelayList";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";

export const RelayDetailsDialogLarge: React.FunctionComponent<DetailsDialogProps> = ({
                                                                                         showDialog,
                                                                                         closeDialog,
                                                                                         relayDetailsMatch,
                                                                                         filteredRelayMatches,
                                                                                         relayDetailsId,
                                                                                         setRelayDetailsId,
                                                                                         canShowRelayList
                                                                                     }) => {
    return (
        <Dialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={canShowRelayList ? "lg" : "md"}
            fullWidth={true}
            PaperProps={{
                sx: {
                    minHeight: "80vh",
                    maxHeight: "80vh",
                }
            }}
        >
            <DialogTitle>
                <Grid container>
                    {canShowRelayList && <Grid item xs={12} sm={4}>
                        <RelayDetailsSelectionHeader/>
                    </Grid>}
                    <Grid item xs={12} sm={canShowRelayList ? 8 : 12}>
                        <RelayDetailsHeader
                            closeDialog={closeDialog}
                            relayDetailsMatch={relayDetailsMatch}
                        />
                    </Grid>
                </Grid>
            </DialogTitle>
            <Divider/>
            <Grid container>
                {canShowRelayList && <Grid item xs={12} sm={4} sx={{maxHeight: "70vh", overflow: 'auto'}}>
                    <RelayList
                        relayMatches={filteredRelayMatches}
                        selectedRelayId={relayDetailsId}
                        setSelectedRelayId={setRelayDetailsId}
                    />
                </Grid>}
                <Grid item xs={12} sm={canShowRelayList ? 8 : 12}
                      sx={{maxHeight: "70vh", overflow: 'auto'}}>
                    {relayDetailsMatch && <RelayDetailsTable relayDetailsMatch={relayDetailsMatch}/>}
                </Grid>
            </Grid>
        </Dialog>
    )
}

