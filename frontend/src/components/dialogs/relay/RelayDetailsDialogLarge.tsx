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
import {getIcon} from "../../../types/icons";
import {getRelayType} from "../../../util/aggregate-relays";
import {RelayIdentifierDto, RelayLocationDto} from "../../../dto/relay";
import {FullHeightDialog} from "../../../types/ui";
import {RelayType} from "../../../types/relay";
import {RelayList} from "./RelayList";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";
import {SelectFamilyButton} from "../../buttons/SelectFamilyButton";

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
        <FullHeightDialog
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
                        {relayDetails && relayLocation ?
                            <Box display="flex" alignItems={"center"}>
                                {relayLocation ? getIcon(getRelayType(relayLocation)) : null}
                                <Typography sx={{display: "inline", padding: "0px 16px"}}
                                            variant="h6">
                                    {relayDetails.nickname}
                                </Typography>

                                {relayLocation?.familyId && <SelectFamilyButton newFamilyId={relayLocation.familyId}
                                                                                furtherAction={closeDialog}/>}
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
                    {relayDetails && relayLocation &&
                        <RelayDetailsTable relayDetails={relayDetails} relayLocation={relayLocation}/>
                    }
                </Grid>
            </Grid>
        </FullHeightDialog>
    )
}

export interface RelayMatch extends RelayIdentifierDto {
    location: RelayLocationDto
    relayType: RelayType
}