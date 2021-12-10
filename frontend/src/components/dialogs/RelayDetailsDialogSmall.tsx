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
    MenuItem,
    Select,
    Toolbar,
    Typography
} from "@mui/material";
import {getRelayType} from "../../util/aggregate-relays";
import {DetailsDialogProps} from "./DetailsDialogUtil";
import {RelayDetails} from "./RelayDetails";
import {getIcon} from "../../types/icons";
import {RelayList} from "./RelayList";
import CloseIcon from "@mui/icons-material/Close";
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {Transition} from "../../types/ui";


export const RelayDetailsDialogSmall: FunctionComponent<DetailsDialogProps> = ({
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
    const [showDetailsDialog, setShowDetailsDialog] = useState(false)

    //if Dialog is closed, set details dialog closed, too
    useEffect(() => {
        if (!showDialog) setShowDetailsDialog(false)
    }, [showDialog])

    // show relay details directly if only one relay is selectable
    useEffect(() => {
        if (relays.length === 1) setShowDetailsDialog(true);
    }, [relays])

    const handleDetailsDialogClose = () => {
        if (relays.length === 1) {
            closeDialog()
            setShowDetailsDialog(false)
        }
        else {
            setShowDetailsDialog(false)
        }
    }

    const handleSelectDetails = (id: number) => {
        setRelayDetailsId(id)
        setShowDetailsDialog(true)
    }

    return (
        <Box>
            <Dialog
                open={showDialog}
                onClose={closeDialog}
                fullScreen={true}
                TransitionComponent={Transition}
            >
                <AppBar sx={{ position: 'relative' }}>
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
                        <Button
                            aria-label="close"
                            sx={{
                                position: "absolute",
                                right: "15px",
                                top: "15px",
                            }}
                            variant={"outlined"}
                            onClick={closeDialog}
                            endIcon={<CloseIcon/>}
                        >
                            Close
                        </Button>
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
                TransitionComponent={Transition}
            >
                <AppBar sx={{ position: 'relative' }}>
                    <Toolbar>
                        <Typography variant="h6">
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
                        </Typography>
                        {relays.length > 1 ? <Button
                            autoFocus
                            aria-label="go back"
                            sx={{
                                position: "absolute",
                                right: "125px",
                                top: "15px",
                            }}
                            variant={"outlined"}
                            onClick={handleDetailsDialogClose}
                            startIcon={<ArrowBackIcon/>}
                        >
                            Back
                        </Button> : null}
                        <Button
                            aria-label="close"
                            sx={{
                                position: "absolute",
                                right: "15px",
                                top: "15px",
                            }}
                            variant={"outlined"}
                            onClick={closeDialog}
                            endIcon={<CloseIcon/>}
                        >
                            Close
                        </Button>
                    </Toolbar>
                </AppBar>
                <DialogContent>
                    <RelayDetails relayDetails={relayDetails}/>
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
        </Box>
    )
}