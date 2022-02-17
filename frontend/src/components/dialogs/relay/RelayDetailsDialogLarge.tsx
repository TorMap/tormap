import React from "react";
import {Dialog, DialogTitle, Divider, Grid} from "@mui/material";
import {RelayIdentifierDto, RelayLocationDto} from "../../../dto/relay";
import {RelayType} from "../../../types/relay";
import {RelayList} from "./RelayList";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";
import {RelayDetailsSelectionHeader} from "./RelayDetailsSelectionHeader";
import {RelayDetailsHeader} from "./RelayDetailsHeader";

export const RelayDetailsDialogLarge: React.FunctionComponent<DetailsDialogProps> = ({
                                                                                         showDialog,
                                                                                         closeDialog,
                                                                                         relayLocations,
                                                                                         relayIdentifiers,
                                                                                         sortRelaysBy,
                                                                                         handleSelectSortByChange,
                                                                                         setRelayDetailsId,
                                                                                         sortedRelayMatches,
                                                                                         relayDetailsId,
                                                                                         relayDetails,
                                                                                         relayLocation,
                                                                                     }) => {
    return (
        <Dialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={relayLocations.length > 1 ? "lg" : "md"}
            fullWidth={true}
            sx={{
                paper: {
                    height: '70vh',
                },
            }}
        >
            <DialogTitle>
                <Grid container>
                    {relayIdentifiers.length > 1 && <Grid item xs={12} sm={3}>
                        <RelayDetailsSelectionHeader
                            sortRelaysBy={sortRelaysBy}
                            handleSelectSortByChange={handleSelectSortByChange}
                        />
                    </Grid>}
                    <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}>
                        <RelayDetailsHeader
                            closeDialog={closeDialog}
                            relayLocation={relayLocation}
                            relayDetails={relayDetails}
                        />
                    </Grid>
                </Grid>
            </DialogTitle>
            <Divider/>
            <Grid container>
                {relayIdentifiers.length > 1 && <Grid item xs={12} sm={3} sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    <RelayList
                        relayMatches={sortedRelayMatches}
                        selectedRelay={relayDetailsId}
                        setRelayDetailsId={setRelayDetailsId}
                    />
                </Grid>}
                <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}
                      sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    {relayDetails && relayLocation &&
                        <RelayDetailsTable relayDetails={relayDetails} relayLocation={relayLocation}/>
                    }
                </Grid>
            </Grid>
        </Dialog>
    )
}

export interface RelayMatch extends RelayIdentifierDto {
    location: RelayLocationDto
    relayType: RelayType
}