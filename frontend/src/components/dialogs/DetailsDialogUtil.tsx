/**
 * Format number represented as bytes to rounded mega byte string representation
 * @param bandwidthInBytes - number to be formatted
 */
import {FunctionComponent} from "react";
import {RelayDetailsDialogLarge} from "./RelayDetailsDialogLarge";
import {RelayLocationDto} from "../../types/responses";
import {useMediaQuery, useTheme} from "@mui/material";
import {RelayDetailsDialogSmall} from "./RelayDetailsDialogSmall";

export const formatBytesToMBPerSecond = (bandwidthInBytes?: number) => bandwidthInBytes ?
    (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
    : undefined

/**
 * Format number of seconds into an string representation in hours
 * @param seconds - number to be formatted
 */
export const formatSecondsToHours = (seconds?: number) => seconds ?
    (seconds / 3600).toFixed(2) + " hours"
    : undefined

/**
 * Format a boolean value to a string representation
 * @param value - value to be formatted
 */
export const formatBoolean = (value?: boolean) => value === null || value === undefined ? undefined : value ? "yes" : "no"

export interface DetailsProps {
    /**
     * Whether the modal should currently be visible
     */
    showDialog: boolean

    /**
     * Hide the modal
     */
    closeDialog: () => void

    /**
     * Relays which the user can view detailed information about
     */
    relays: RelayLocationDto[]
}

export const RelayDetailsDialog: FunctionComponent<DetailsProps> = ({
                                                                showDialog,
                                                                closeDialog,
                                                                relays,
                                                            }) => {
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    return (isLargeScreen ? <RelayDetailsDialogLarge showDialog={showDialog} closeDialog={closeDialog} relays={relays}/>
: <RelayDetailsDialogSmall showDialog={showDialog} closeDialog={closeDialog} relays={relays}/>)
}