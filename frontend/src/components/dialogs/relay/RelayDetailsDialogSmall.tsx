import React, {FunctionComponent, useEffect, useState} from "react";
import {
    AppBar,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    FormControl,
    IconButton,
    MenuItem,
    Select,
    Toolbar,
    Typography
} from "@mui/material";
import {getRelayType} from "../../../util/aggregate-relays";
import {DetailsDialogProps} from "./ResponsiveRelayDetailsDialog";
import {RelayDetailsTable} from "./RelayDetailsTable";
import {getIcon} from "../../../types/icons";
import {RelayList} from "./RelayList";
import CloseIcon from "@mui/icons-material/Close";
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {SlideLeftTransition, SlideUpTransition} from "../../../types/ui";
import {SelectFamilyButton} from "../../buttons/SelectFamilyButton";


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
    const [showDetailsDialog, setShowDetailsDialog] = useState(false)

    //if Dialog is closed, set details dialog closed, too
    useEffect(() => {
        if (!showDialog) setShowDetailsDialog(false)
    }, [showDialog])

    // show relay details directly if only one relay is selectable
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
                        <Typography variant="h6">
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
                        endIcon={<CloseIcon/>}
                    >
                        close
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
                        <Typography variant="h6">
                            {relayDetails ?
                                <Box display="flex" alignItems={"center"}>
                                    {relayLocation ? getIcon(getRelayType(relayLocation)) : null}
                                    <Typography sx={{display: "inline", padding: "0px 16px"}} variant="h6">
                                        {relayDetails.nickname}
                                    </Typography>
                                    {relayLocation?.familyId && <SelectFamilyButton newFamilyId={relayLocation.familyId}
                                                                                    furtherAction={closeDialog}/>}
                                </Box> : <CircularProgress color={"inherit"} size={24}/>
                            }
                        </Typography>
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "15px",
                            top: "15px",
                        }} onClick={handleDetailsDialogClose}>
                            <ArrowBackIcon/>
                        </IconButton>
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
                        startIcon={<ArrowBackIcon/>}
                    >
                        Back
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    )
}