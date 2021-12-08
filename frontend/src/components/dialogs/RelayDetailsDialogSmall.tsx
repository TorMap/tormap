import React, {FunctionComponent, useEffect, useMemo, useState} from "react";
import {
    Box,
    CircularProgress,
    Dialog,
    DialogContent,
    DialogTitle,
    FormControl, IconButton,
    Link,
    MenuItem,
    Select,
    Typography
} from "@mui/material";
import {RelayMatch} from "./RelayDetailsDialogLarge";
import {DetailsInfo, RelayDetailsDto, RelayIdentifierDto, RelayLocationDto} from "../../types/responses";
import {useSnackbar} from "notistack";
import {getRelayType} from "../../util/aggregate-relays";
import {SnackbarMessage} from "../../types/ui";
import {backend} from "../../util/util";
import {RelayFlag, RelayFlagLabel} from "../../types/relay";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";
import {DetailsProps, formatBoolean, formatBytesToMBPerSecond, formatSecondsToHours} from "./DetailsDialogUtil";
import {RelayDetails} from "./RelayDetails";
import {getIcon} from "../../types/icons";
import {RelayList} from "./RelayList";
import CloseIcon from "@mui/icons-material/Close";


export const RelayDetailsDialogSmall: FunctionComponent<DetailsProps> = ({
                                                                     showDialog,
                                                                     closeDialog,
                                                                     relays,
                                                                 }) => {
    const [relayIdentifiers, setRelayIdentifiers] = useState<RelayIdentifierDto[]>([])
    const [relayDetailsId, setRelayDetailsId] = useState<number>()
    const [rawRelayDetails, setRawRelayDetails] = useState<RelayDetailsDto>()
    const [relayDetails, setRelayDetails] = useState<DetailsInfo[]>()
    const [sortRelaysBy, setSortRelaysBy] = useState<keyof RelayMatch>("relayType")
    const {enqueueSnackbar} = useSnackbar();

    const [showDetailsDialog, setShowDetailsDialog] = useState(false)

    //Todo: duplicate code
    const relayDetailsIdToLocationMap = useMemo(() => {
        const relayDetailsIdToLocationMap = new Map<number, RelayLocationDto>()
        relays.filter(relay => relay.detailsId).forEach(relay => relayDetailsIdToLocationMap.set(relay.detailsId!!, relay))
        return relayDetailsIdToLocationMap
    }, [relays])

    //Todo: duplicate code
    const relayMatches = useMemo(
        () => {
            const relayMatches: RelayMatch[] = [];
            relayIdentifiers.forEach(identifier => {
                const relayLocation = relayDetailsIdToLocationMap.get(identifier.id)
                if (relayLocation) {
                    relayMatches.push({
                        ...identifier,
                        location: relayLocation,
                        relayType: getRelayType(relayLocation)
                    })
                }
            })
            return relayMatches.sort((a, b) => a.relayType > b.relayType ? 1 : -1)
        },
        [relayIdentifiers, relayDetailsIdToLocationMap]
    )

    //Todo: duplicate code
    const sortedRelayMatches = useMemo(
        () =>  relayMatches.sort((a, b) => a[sortRelaysBy] > b[sortRelaysBy] ? 1 : -1),
        [relayMatches, sortRelaysBy]
    )

    //Todo: duplicate code
    /**
     * Query relayIdentifiers for relays from backend
     */
    useEffect(() => {
        setRelayDetailsId(undefined)
        setRawRelayDetails(undefined)
        setRelayDetails(undefined)
        setRelayIdentifiers([])
        if (relays.length > 0 && relayDetailsIdToLocationMap.size === 0) {
            enqueueSnackbar(SnackbarMessage.NoRelayDetails, {variant: "warning"})
            closeDialog()
        } else if (relayDetailsIdToLocationMap.size === 1) {
            setRelayDetailsId(relayDetailsIdToLocationMap.keys().next().value)
        } else if (relayDetailsIdToLocationMap.size > 1) {
            backend.post<RelayIdentifierDto[]>(
                '/relay/details/relay/identifiers',
                Array.from(relayDetailsIdToLocationMap.keys())
            ).then(response => {
                const requestedRelayIdentifiers = response.data
                setRelayIdentifiers(requestedRelayIdentifiers)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                closeDialog()
            })
        }

    }, [closeDialog, relays, enqueueSnackbar, relayDetailsIdToLocationMap])

    //Todo: duplicate code
    /**
     * Query more information for the selected relay
     */
    useEffect(() => {
        function constructFlagString(flags: RelayFlag[] | null | undefined): string {
            if (flags) {
                return flags.map(flag => RelayFlagLabel[flag]).join(", ")
            }
            return "no flags assigned"
        }

        if (!relayDetailsId && sortedRelayMatches.length > 0) {
            setRelayDetailsId(sortedRelayMatches[0].id)
        } else if (relayDetailsId) {
            backend.get<RelayDetailsDto>(`/relay/details/relay/${relayDetailsId}`).then(response => {
                const relay = response.data
                setRawRelayDetails(relay)
                setRelayDetails([
                    {
                        name: "Fingerprint",
                        value:
                            <Link
                                href={`https://metrics.torproject.org/rs.html#details/${relay.fingerprint}`}
                                target={"_blank"}>
                                {relay.fingerprint}
                            </Link>
                    },
                    {
                        name: "IP address",
                        value:
                            <Link
                                href={`https://metrics.torproject.org/rs.html#search/${relay.address}`}
                                target={"_blank"}>
                                {relay.address}
                            </Link>
                    },
                    {
                        name: "Flags assigned by authorities",
                        value: constructFlagString(relayDetailsIdToLocationMap.get(relay.id)?.flags)
                    },
                    {name: "Autonomous System", value: relay.autonomousSystemName},
                    {
                        name: "Autonomous System Number",
                        value:
                            <Link
                                href={`https://metrics.torproject.org/rs.html#search/as:${relay.autonomousSystemNumber}`}
                                target={"_blank"}>
                                {relay.autonomousSystemNumber}
                            </Link>
                    },
                    {name: "Platform", value: relay.platform},
                    {name: "Uptime", value: formatSecondsToHours(relay.uptime)},
                    {name: "Contact", value: relay.contact},
                    {name: "Bandwidth for short intervals", value: formatBytesToMBPerSecond(relay.bandwidthBurst)},
                    {name: "Bandwidth for long periods", value: formatBytesToMBPerSecond(relay.bandwidthRate)},
                    {name: "Bandwidth observed", value: formatBytesToMBPerSecond(relay.bandwidthObserved)},
                    {name: "Supported protocols", value: relay.protocols},
                    {name: "Allows single hop exit", value: formatBoolean(relay.allowSingleHopExits)},
                    {name: "Is hibernating", value: formatBoolean(relay.isHibernating)},
                    {name: "Caches extra info", value: formatBoolean(relay.cachesExtraInfo)},
                    {name: "Is a hidden service directory", value: formatBoolean(relay.isHiddenServiceDir)},
                    {name: "Accepts tunneled directory requests", value: formatBoolean(relay.tunnelledDirServer)},
                    {name: "Link protocol versions", value: relay.linkProtocolVersions},
                    {name: "Circuit protocol versions", value: relay.circuitProtocolVersions},
                    {name: "Self reported family members", value: relay.familyEntries},
                    {name: "Infos published by relay on", value: relay.day},
                ])
            })
                .catch(() => {
                    enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                })
        }
    }, [sortedRelayMatches, relayDetailsId, relayDetailsIdToLocationMap, enqueueSnackbar])

    //Todo: duplicate code
    const relay = relays.find((relay) => relayDetailsId && relay.detailsId === relayDetailsId)

    //Todo: duplicate code
    const handeSelectSortByChange = (event: SelectChangeEvent<keyof RelayMatch>) => {
        switch (event.target.value) {
            case "nickname": setSortRelaysBy("nickname")
                break
            default: setSortRelaysBy("relayType")
                break
        }
    }

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
            setRelayDetailsId(undefined)
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
            >
                <DialogTitle>
                    <Typography variant="h6">
                        Relays
                    </Typography>
                    <FormControl variant="standard" sx={{marginLeft: "20px"}}>
                        <Select
                            value={sortRelaysBy}
                            label="Sort by"
                            onChange={handeSelectSortByChange}
                        >
                            <MenuItem value={"relayType"}>Type</MenuItem>
                            <MenuItem value={"nickname"}>Nickname</MenuItem>
                        </Select>
                    </FormControl>
                    <IconButton aria-label="close" sx={{
                        position: "absolute",
                        right: "10px",
                        top: "10px",
                    }} onClick={closeDialog}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent>
                    <RelayList
                        relayMatches={sortedRelayMatches}
                        selectedRelay={relayDetailsId}
                        setRelayDetailsId={handleSelectDetails}
                    />
                </DialogContent>
            </Dialog>
            <Dialog
                open={showDetailsDialog}
                onClose={handleDetailsDialogClose}
                fullScreen={true}
            >
                <DialogTitle>
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
                    <IconButton aria-label="close" sx={{
                        position: "absolute",
                        right: "10px",
                        top: "10px",
                    }} onClick={handleDetailsDialogClose}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent>
                    <RelayDetails relayDetails={relayDetails}/>
                </DialogContent>
            </Dialog>
        </Box>
    )
}