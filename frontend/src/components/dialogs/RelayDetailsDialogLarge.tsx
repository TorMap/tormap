import React from "react";
import {
    Box,
    CircularProgress,
    DialogTitle,
    Divider,
    FormControl,
    Grid,
    IconButton,
    MenuItem,
    Select,
    Typography
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import {getIcon} from "../../types/icons";
import {getRelayType} from "../../util/aggregate-relays";
import {RelayIdentifierDto, RelayLocationDto} from "../../types/responses";
import {FullHeightDialog} from "../../types/ui";
import {RelayType} from "../../types/relay";
import {RelayList} from "./RelayList";
import {RelayDetails} from "./RelayDetails";
import {DetailsDialogProps} from "./DetailsDialogUtil";


/**
 * The Dialog for Relay Details
 * @param showDialog - Whether the details dialog should be displayed
 * @param closeDialog - Event handler for closing the dialog
 * @param relays - Selectable relays
 * @param enqueueSnackbar - The event handler for showing a snackbar message
 */
export const RelayDetailsDialogLarge: React.FunctionComponent<DetailsDialogProps> = ({
                                                                            showDialog,
                                                                            closeDialog,
                                                                            relays,
                                                                        relayIdentifiers,
                                                                        sortRelaysBy,
                                                                        handleSelectSortByChange,
                                                                        rawRelayDetails,
                                                                        setRelayDetailsId,
                                                                        sortedRelayMatches,
                                                                        relayDetailsId,
                                                                        relayDetails,
                                                                        relay,
                                                                        }) => {
    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={relays.length > 1 ? "lg" : "md"}
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
                        <Typography sx={{display: "inline"}} variant="h6">
                            Relays
                        </Typography>
                        <FormControl variant="standard" sx={{marginLeft: "20px"}}>
                            <Select
                                value={sortRelaysBy}
                                label="Sort by"
                                onChange={handleSelectSortByChange}
                            >
                                <MenuItem value={"relayType"}>Type</MenuItem>
                                <MenuItem value={"nickname"}>Nickname</MenuItem>
                            </Select>
                        </FormControl>
                    </Grid>}
                    <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}>
                        {rawRelayDetails ?
                            <Box display="flex" alignItems={"center"}>
                                <Box sx={{
                                    display: "inline",
                                    paddingRight: "16px",
                                }}>
                                    {relay ? getIcon(getRelayType(relay)) : null}
                                </Box>
                                <Typography sx={{display: "inline"}} variant="h6">
                                    {rawRelayDetails.nickname}
                                </Typography>
                            </Box> : <CircularProgress color={"inherit"} size={24}/>
                        }
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "10px",
                            top: "10px",
                        }} onClick={closeDialog}>
                            <CloseIcon/>
                        </IconButton>
                    </Grid>
                </Grid>

            </DialogTitle>
            <Divider/>
            <Grid container>
                {relayIdentifiers.length > 1 && <Grid item xs={12} sm={3}
                                                      sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    <RelayList
                        relayMatches={sortedRelayMatches}
                        selectedRelay={relayDetailsId}
                        setRelayDetailsId={setRelayDetailsId}
                    />
                </Grid>}
                <Grid item xs={12} sm={relayIdentifiers.length > 1 ? 9 : 12}
                      sx={{maxHeight: "65vh", overflow: 'auto'}}>
                    <RelayDetails relayDetails={relayDetails}/>
                </Grid>
            </Grid>
        </FullHeightDialog>
    )
}

export interface RelayMatch extends RelayIdentifierDto {
    location: RelayLocationDto
    relayType: RelayType
}