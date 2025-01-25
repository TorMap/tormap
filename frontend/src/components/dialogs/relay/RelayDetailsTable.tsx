import {Table, TableBody, TableCell, TableRow, Typography} from "@mui/material";
import React, {FunctionComponent} from "react";

import {RelayDetailsMatch, RelayFlag, RelayFlagLabel} from "../../../types/relay";
import {ExternalLink} from "../../link/ExternalLink";

interface Props {
    relayDetailsMatch: RelayDetailsMatch
}

export const RelayDetailsTable: FunctionComponent<Props> = ({relayDetailsMatch}) => {
    const tableRows: RelayDetailsTableRow[] = [
        {
            name: "Fingerprint",
            value:
                <ExternalLink
                    href={`https://metrics.torproject.org/rs.html#details/${relayDetailsMatch.fingerprint}`}
                    label={relayDetailsMatch.fingerprint}
                />
        },
        {
            name: "IP address",
            value:
                <ExternalLink
                    href={`https://metrics.torproject.org/rs.html#search/${relayDetailsMatch.address}`}
                    label={relayDetailsMatch.address}
                />
        },
        {
            name: "Flags assigned by authorities",
            value: constructFlagString(relayDetailsMatch.flags)
        },
        {
            name: "Autonomous System",
            value: <ExternalLink
                href={`https://metrics.torproject.org/rs.html#search/as:${relayDetailsMatch.autonomousSystemNumber}`}
                label={`${relayDetailsMatch.autonomousSystemName} (${relayDetailsMatch.autonomousSystemNumber})`}
            />
        },
        {name: "Platform", value: relayDetailsMatch.platform},
        {name: "Uptime", value: formatSecondsToHours(relayDetailsMatch.uptime)},
        {name: "Contact", value: relayDetailsMatch.contact},
        {name: "Bandwidth for short intervals", value: formatBytesToMBPerSecond(relayDetailsMatch.bandwidthBurst)},
        {name: "Bandwidth for long periods", value: formatBytesToMBPerSecond(relayDetailsMatch.bandwidthRate)},
        {name: "Bandwidth observed", value: formatBytesToMBPerSecond(relayDetailsMatch.bandwidthObserved)},
        {name: "Supported protocols", value: relayDetailsMatch.protocols},
        {name: "Allows single hop exit", value: formatBoolean(relayDetailsMatch.allowSingleHopExits)},
        {name: "Is hibernating", value: formatBoolean(relayDetailsMatch.isHibernating)},
        {name: "Caches extra info", value: formatBoolean(relayDetailsMatch.cachesExtraInfo)},
        {name: "Is a hidden service directory", value: formatBoolean(relayDetailsMatch.isHiddenServiceDir)},
        {name: "Accepts tunneled directory requests", value: formatBoolean(relayDetailsMatch.tunnelledDirServer)},
        {name: "Link protocol versions", value: relayDetailsMatch.linkProtocolVersions},
        {name: "Circuit protocol versions", value: relayDetailsMatch.circuitProtocolVersions},
        {name: "Self reported family members", value: relayDetailsMatch.familyEntries},
        {name: "Infos published by relay on", value: relayDetailsMatch.day},
    ]

    return (
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
    )
}

interface RelayDetailsTableRow {
    name: string
    value: string | number | React.ReactNode | undefined
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
