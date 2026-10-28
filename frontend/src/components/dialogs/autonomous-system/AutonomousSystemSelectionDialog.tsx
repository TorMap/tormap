import {Close as CloseIcon} from "@mui/icons-material";
import {
    CircularProgress,
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    TextField,
    useMediaQuery,
    useTheme
} from "@mui/material";
import {FunctionComponent, useEffect, useMemo, useState} from "react";

import {AutonomousSystemDto, RelayLocationDto} from "../../../dto/relay";
import {SlideUpTransition} from "../../../types/ui";
import {autonomousSystemMatches, buildAutonomousSystemRows} from "../../../util/autonomous-systems";
import {backend} from "../../../util/util";
import {AutonomousSystemSelectionTable} from "./AutonomousSystemSelectionTable";

interface Props {
    /**
     * Whether the modal should currently be visible
     */
    shouldShowDialog: boolean

    /**
     * Hide the modal
     */
    closeDialog: () => void

    /**
     * The autonomous systems to choose from
     */
    asNumbers: number[]

    /**
     * The relays of each autonomous system on the map
     */
    relayAutonomousSystemMap: Map<number, RelayLocationDto[]>

    /**
     * The consensus weight of all relays on the map
     */
    totalConsensusWeight: number

    /**
     * The month of the shown day, like 2024-05. The names of the autonomous systems are looked up for it.
     */
    month?: string
}

/**
 * A dialog to select an autonomous system. It also works without the names, e.g. offline or with an older backend.
 */
export const AutonomousSystemSelectionDialog: FunctionComponent<Props> = ({
                                                                              shouldShowDialog,
                                                                              closeDialog,
                                                                              asNumbers,
                                                                              relayAutonomousSystemMap,
                                                                              totalConsensusWeight,
                                                                              month,
                                                                          }) => {
    // Component state
    const [names, setNames] = useState<{ month: string, autonomousSystems: AutonomousSystemDto[] }>()
    const [search, setSearch] = useState("")

    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    // Names are only needed once the dialog is shown, and only once per month
    const namesLoaded = names?.month === month
    const [failedMonth, setFailedMonth] = useState<string>()
    useEffect(() => {
        if (!shouldShowDialog || !month || namesLoaded || failedMonth === month) return undefined
        let isCancelled = false
        backend.get<AutonomousSystemDto[]>(`/relay/details/autonomous-systems/${month}`).then(response => {
            if (isCancelled) return
            // A proxy may answer 200 with something else than the expected list
            if (Array.isArray(response.data)) setNames({month, autonomousSystems: response.data})
            else setFailedMonth(month)
        }).catch(() => {
            if (!isCancelled) setFailedMonth(month)
        })
        return () => {
            isCancelled = true
        }
    }, [shouldShowDialog, month, namesLoaded, failedMonth])

    const isLoading = !!month && !namesLoaded && failedMonth !== month
    const rows = useMemo(
        () => buildAutonomousSystemRows(
            asNumbers, relayAutonomousSystemMap, totalConsensusWeight, namesLoaded ? names?.autonomousSystems : undefined
        ),
        [asNumbers, relayAutonomousSystemMap, totalConsensusWeight, names, namesLoaded]
    )
    const matchingRows = useMemo(() => rows.filter(row => autonomousSystemMatches(row, search)), [rows, search])

    const title = isLargeScreen ? "Select an autonomous system" : "Autonomous systems"
    return (
        <Dialog
            open={shouldShowDialog}
            onClose={closeDialog}
            maxWidth={"lg"}
            fullWidth={true}
            fullScreen={!isLargeScreen}
            slots={isLargeScreen ? undefined : {transition: SlideUpTransition}}
            slotProps={{
                paper: {
                    "aria-label": title,
                    sx: isLargeScreen ? {minHeight: "80vh", maxHeight: "80vh"} : undefined,
                }
            }}
        >
            <DialogTitle>
                {title}
                <IconButton aria-label="close" sx={{
                    position: "absolute",
                    right: "10px",
                    top: "10px",
                }} onClick={closeDialog}>
                    <CloseIcon/>
                </IconButton>
            </DialogTitle>
            <DialogContent dividers>
                <TextField
                    label="Search by name or number"
                    size="small"
                    fullWidth
                    value={search}
                    onChange={event => setSearch(event.target.value)}
                    sx={{mb: 1}}
                />
                {isLoading
                    ? <CircularProgress color={"inherit"} size={22.5} sx={{mt: 1}}/>
                    : <AutonomousSystemSelectionTable rows={matchingRows} closeDialog={closeDialog}/>}
            </DialogContent>
        </Dialog>
    )
}

export default AutonomousSystemSelectionDialog
