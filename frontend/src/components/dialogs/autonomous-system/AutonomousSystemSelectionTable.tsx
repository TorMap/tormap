import {Table, TableBody, TableCell, TableHead, TableRow, TableSortLabel, Typography} from "@mui/material";
import {FunctionComponent, useMemo, useState} from "react";

import {useSettings} from "../../../context/settings-context";
import {AutonomousSystemRow} from "../../../util/autonomous-systems";
import {formatWeightShare} from "../../../util/layer-construction";
import {consensusWeightColumnTooltip} from "../../../util/relay-group-summary";

interface Props {
    rows: AutonomousSystemRow[]

    /**
     * Callback for closing the dialog
     */
    closeDialog: () => void
}

type SortColumn = "name" | "number" | "relayCount" | "exitCount" | "consensusWeight" | "countryCount"
type SortDirection = "asc" | "desc"

const columns: {
    key: SortColumn,
    label: string,
    firstDirection: SortDirection,
    numeric?: boolean,
    tooltip?: string,
}[] = [
    {key: "name", label: "Name", firstDirection: "asc"},
    {key: "number", label: "Number", firstDirection: "asc", numeric: true},
    {key: "relayCount", label: "Relays", firstDirection: "desc", numeric: true},
    {key: "exitCount", label: "Exits", firstDirection: "desc", numeric: true},
    {
        key: "consensusWeight",
        label: "Consensus weight",
        firstDirection: "desc",
        numeric: true,
        tooltip: consensusWeightColumnTooltip("autonomous system"),
    },
    {key: "countryCount", label: "Countries", firstDirection: "desc", numeric: true},
]

const collator = new Intl.Collator(undefined, {numeric: true, sensitivity: "base"})

/**
 * Sort autonomous systems by a column. Values that are not known, e.g. a missing name, stay last in both directions.
 */
export const sortAutonomousSystemRows = (
    rows: AutonomousSystemRow[],
    column: SortColumn,
    direction: SortDirection,
): AutonomousSystemRow[] => {
    const sign = direction === "asc" ? 1 : -1
    return [...rows].sort((a, b) => {
        const valueA = a[column]
        const valueB = b[column]
        if (valueA === undefined || valueB === undefined) {
            return (valueA === undefined ? 1 : 0) - (valueB === undefined ? 1 : 0)
        }
        if (typeof valueA === "string" && typeof valueB === "string") return sign * collator.compare(valueA, valueB)
        return sign * ((valueA as number) - (valueB as number))
    })
}

/**
 * A table of autonomous systems, clicking a row selects the autonomous system on the map
 */
export const AutonomousSystemSelectionTable: FunctionComponent<Props> = ({rows, closeDialog}) => {
    // App context
    const {settings, setSettings} = useSettings()

    // Component state
    const hasWeights = rows.some(row => row.consensusWeight !== undefined)
    const [sort, setSort] = useState<{ column: SortColumn, direction: SortDirection }>()
    // Without a choice of the user, the heaviest autonomous systems come first, or the largest on days without weights
    const sortColumn = sort?.column ?? (hasWeights ? "consensusWeight" : "relayCount")
    const sortDirection = sort?.direction ?? "desc"

    const sortedRows = useMemo(
        () => sortAutonomousSystemRows(rows, sortColumn, sortDirection),
        [rows, sortColumn, sortDirection]
    )

    const changeSort = (column: SortColumn, firstDirection: SortDirection) => {
        if (column === sortColumn) {
            setSort({column, direction: sortDirection === "asc" ? "desc" : "asc"})
        } else {
            setSort({column, direction: firstDirection})
        }
    }

    if (rows.length === 0) {
        return <Typography sx={{mt: 1}}>There are no matching autonomous systems.</Typography>
    }

    return (
        <Table size={"small"}>
            <TableHead>
                <TableRow>
                    {columns.map(({key, label, firstDirection, numeric, tooltip}) =>
                        <TableCell
                            key={key}
                            align={numeric ? "right" : "left"}
                            title={tooltip}
                            sortDirection={sortColumn === key ? sortDirection : false}
                        >
                            <TableSortLabel
                                active={sortColumn === key}
                                direction={sortColumn === key ? sortDirection : firstDirection}
                                onClick={() => changeSort(key, firstDirection)}
                            >
                                <Typography sx={{fontWeight: "bold"}}>{label}</Typography>
                            </TableSortLabel>
                        </TableCell>
                    )}
                </TableRow>
            </TableHead>
            <TableBody>
                {sortedRows.map(row =>
                    <TableRow
                        key={row.number}
                        onClick={() => {
                            setSettings({...settings, sortAutonomousSystem: true, selectedAutonomousSystem: row.number})
                            closeDialog()
                        }}
                        hover={true}
                        sx={{"&:hover": {cursor: "pointer"}}}
                    >
                        <TableCell scope="row">
                            <Typography>{row.name ?? "Unknown name"}</Typography>
                        </TableCell>
                        <TableCell align="right">
                            <Typography>AS{row.number}</Typography>
                        </TableCell>
                        <TableCell align="right">
                            <Typography>{row.relayCount}</Typography>
                        </TableCell>
                        <TableCell align="right">
                            <Typography>{row.exitCount}</Typography>
                        </TableCell>
                        <TableCell align="right">
                            <Typography>
                                {row.consensusWeightShare !== undefined ? formatWeightShare(row.consensusWeightShare) : "-"}
                            </Typography>
                        </TableCell>
                        <TableCell align="right">
                            <Typography>{row.countryCount}</Typography>
                        </TableCell>
                    </TableRow>
                )}
            </TableBody>
        </Table>
    )
}
