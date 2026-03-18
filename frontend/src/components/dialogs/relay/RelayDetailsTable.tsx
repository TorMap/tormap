import {Box, Table, TableBody, TableCell, TableRow, Typography} from "@mui/material";
import React, {FunctionComponent} from "react";

import {RelayDetailsMatch, RelayFlag, RelayFlagLabel} from "../../../types/relay";
import {ExternalLink} from "../../link/ExternalLink";
import {SelectFamilyButton} from "../../buttons/SelectFamilyButton";

interface Props {
    relayDetailsMatch: RelayDetailsMatch
    closeDialog: () => void
}

export const RelayDetailsTable: FunctionComponent<Props> = ({relayDetailsMatch, closeDialog}) => {
    const tableRows = [
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
            value: renderAutonomousSystem(relayDetailsMatch.autonomousSystemName, relayDetailsMatch.autonomousSystemNumber)
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
        {
            name: "Confirmed family members",
            value: renderConfirmedFamilyMembers(relayDetailsMatch.confirmedFamilyMembers)
        },
        relayDetailsMatch.familyId ? {
            name: "Show family on map",
            value: <SelectFamilyButton
                familyId={relayDetailsMatch.familyId}
                furtherAction={closeDialog}
                label="Show confirmed family"
            />
        } : undefined,
        {name: "Verified host names", value: renderHostNames(relayDetailsMatch.verifiedHostNames)},
        {name: "Unverified host names", value: renderHostNames(relayDetailsMatch.unverifiedHostNames)},
        {name: "Infos published by relay on", value: relayDetailsMatch.day},
    ].filter(isTableRow) as RelayDetailsTableRow[]

    return (
        <Table size={"small"}>
            <TableBody>
                {tableRows.map((row) =>
                    <TableRow key={row.name}>
                        <TableCell scope="row" sx={{minWidth: "150px",}}>
                            <Typography>{row.name}</Typography>
                        </TableCell>
                        <TableCell scope="row">
                            {renderTableRowValue(row.value)}
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

const isTableRow = (row: RelayDetailsTableRow | undefined): row is RelayDetailsTableRow =>
    row !== undefined && row.value !== undefined && row.value !== null && row.value !== ""

const renderTableRowValue = (value: RelayDetailsTableRow["value"]) => typeof value === "string" || typeof value === "number" ?
    <Typography>{value}</Typography> :
    value

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

const renderAutonomousSystem = (autonomousSystemName?: string, autonomousSystemNumber?: number | null) =>
    autonomousSystemName && autonomousSystemNumber !== null && autonomousSystemNumber !== undefined ?
        <ExternalLink
            href={`https://metrics.torproject.org/rs.html#search/as:${autonomousSystemNumber}`}
            label={`${autonomousSystemName} (${autonomousSystemNumber})`}
        /> :
        undefined

const renderConfirmedFamilyMembers = (confirmedFamilyMembers?: {
    id: number
    fingerprint: string
    nickname: string
}[]) => confirmedFamilyMembers && confirmedFamilyMembers.length > 0 ?
    <Box sx={{display: "flex", flexDirection: "column", gap: 0.5}}>
        {confirmedFamilyMembers.map((familyMember) =>
            <ExternalLink
                key={familyMember.id}
                href={`https://metrics.torproject.org/rs.html#details/${familyMember.fingerprint}`}
                label={`${familyMember.nickname} (${familyMember.fingerprint})`}
            />
        )}
    </Box> :
    undefined

const renderHostNames = (hostNames?: string[]) => hostNames && hostNames.length > 0 ?
    <Box sx={{display: "flex", flexDirection: "column", gap: 0.5}}>
        {hostNames.map((hostName) =>
            <Typography key={hostName}>{hostName}</Typography>
        )}
    </Box> :
    undefined
