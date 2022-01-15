import React, {FunctionComponent} from "react";
import {CircularProgress, Link, Table, TableBody, TableCell, TableRow, Typography} from "@mui/material";
import {RelayDetailsDto, RelayLocationDto} from "../../../dto/relay";
import {RelayFlag, RelayFlagLabel} from "../../../types/relay";
import {ReactJSXElement} from "@emotion/react/types/jsx-namespace";

interface Props {
    relayLocation: RelayLocationDto
    relayDetails: RelayDetailsDto
}

export const RelayDetailsTable: FunctionComponent<Props> = ({relayLocation, relayDetails}) => {
    const tableRows: RelayDetailsTableRow[] = [
        {
            name: "Fingerprint",
            value:
                <Link
                    href={`https://metrics.torproject.org/rs.html#details/${relayDetails.fingerprint}`}
                    target={"_blank"}>
                    {relayDetails.fingerprint}
                </Link>
        },
        {
            name: "IP address",
            value:
                <Link
                    href={`https://metrics.torproject.org/rs.html#search/${relayDetails.address}`}
                    target={"_blank"}>
                    {relayDetails.address}
                </Link>
        },
        {
            name: "Flags assigned by authorities",
            value: constructFlagString(relayLocation.flags)
        },
        {name: "Autonomous System", value: relayDetails.autonomousSystemName},
        {
            name: "Autonomous System Number",
            value:
                <Link
                    href={`https://metrics.torproject.org/rs.html#search/as:${relayDetails.autonomousSystemNumber}`}
                    target={"_blank"}>
                    {relayDetails.autonomousSystemNumber}
                </Link>
        },
        {name: "Platform", value: relayDetails.platform},
        {name: "Uptime", value: formatSecondsToHours(relayDetails.uptime)},
        {name: "Contact", value: relayDetails.contact},
        {name: "Bandwidth for short intervals", value: formatBytesToMBPerSecond(relayDetails.bandwidthBurst)},
        {name: "Bandwidth for long periods", value: formatBytesToMBPerSecond(relayDetails.bandwidthRate)},
        {name: "Bandwidth observed", value: formatBytesToMBPerSecond(relayDetails.bandwidthObserved)},
        {name: "Supported protocols", value: relayDetails.protocols},
        {name: "Allows single hop exit", value: formatBoolean(relayDetails.allowSingleHopExits)},
        {name: "Is hibernating", value: formatBoolean(relayDetails.isHibernating)},
        {name: "Caches extra info", value: formatBoolean(relayDetails.cachesExtraInfo)},
        {name: "Is a hidden service directory", value: formatBoolean(relayDetails.isHiddenServiceDir)},
        {name: "Accepts tunneled directory requests", value: formatBoolean(relayDetails.tunnelledDirServer)},
        {name: "Link protocol versions", value: relayDetails.linkProtocolVersions},
        {name: "Circuit protocol versions", value: relayDetails.circuitProtocolVersions},
        {name: "Self reported family members", value: relayDetails.familyEntries},
        {name: "Infos published by relay on", value: relayDetails.day},
    ]

    return (
        <>
            {tableRows ?
                <Table size={"small"}>
                    <TableBody>
                        {tableRows.map((row) =>
                            row.value &&
                            <TableRow key={row.name}>
                                <TableCell scope="row" sx={{minWidth: "150px",}}>
                                    <Typography>{row.name}</Typography>
                                </TableCell>
                                <TableCell scope="row">
                                    <Typography>{row.value}</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                : <CircularProgress color={"inherit"} size={24}/>}
        </>
    )
}

interface RelayDetailsTableRow {
    name: string
    value: string | number | ReactJSXElement | undefined
}

const formatBytesToMBPerSecond = (bandwidthInBytes?: number) => bandwidthInBytes ?
    (bandwidthInBytes / 1000000).toFixed(2) + " MB/s"
    : undefined

const formatSecondsToHours = (seconds?: number) => seconds ?
    (seconds / 3600).toFixed(2) + " hours"
    : undefined

const formatBoolean = (value?: boolean) => value === null || value === undefined ? undefined : value ? "yes" : "no"

const constructFlagString = (flags: RelayFlag[] | null | undefined) => {
    if (flags) {
        return flags.map(flag => RelayFlagLabel[flag]).join(", ")
    }
    return "no flags assigned"
}