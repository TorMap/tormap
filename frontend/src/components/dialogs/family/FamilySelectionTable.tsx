import {Table, TableBody, TableCell, TableHead, TableRow, TableSortLabel, TextField, Typography} from "@mui/material";
import {FunctionComponent, useMemo, useState} from "react";

import {useSettings} from "../../../context/settings-context";
import {formatWeightShare} from "../../../util/layer-construction";
import {consensusWeightColumnTooltip} from "../../../util/relay-group-summary";
import {FamilyRow} from "./FamilySelectionDialog";

interface Props {
    /**
     * The families to choose from
     */
    families?: FamilyRow[]

    /**
     * Callback for closing the dialog
     */
    closeFamilySelectionDialog: () => void
}

type SortColumn = "nicknames" | "relayCount" | "exitCount" | "consensusWeight" | "countryCount" | "autonomousSystems"
type SortDirection = "asc" | "desc"

const columns: {
    key: SortColumn,
    label: string,
    firstDirection: SortDirection,
    numeric?: boolean,
    tooltip?: string,
}[] = [
    {key: "nicknames", label: "Relay nicknames", firstDirection: "asc"},
    {key: "relayCount", label: "Relays", firstDirection: "desc", numeric: true},
    {key: "exitCount", label: "Exits", firstDirection: "desc", numeric: true},
    {
        key: "consensusWeight",
        label: "Consensus weight",
        firstDirection: "desc",
        numeric: true,
        tooltip: consensusWeightColumnTooltip("family"),
    },
    {key: "countryCount", label: "Countries", firstDirection: "desc", numeric: true},
    {key: "autonomousSystems", label: "Autonomous Systems", firstDirection: "asc"},
]

const collator = new Intl.Collator(undefined, {numeric: true, sensitivity: "base"})

/**
 * Sort families by a column. Values that are not known, e.g. missing autonomous systems, stay last in both directions.
 */
export const sortFamilies = (
    families: FamilyRow[],
    column: SortColumn,
    direction: SortDirection,
): FamilyRow[] => {
    const sign = direction === "asc" ? 1 : -1
    return [...families].sort((a, b) => {
        const valueA = a[column]
        const valueB = b[column]
        if (valueA === undefined || valueA === "" || valueB === undefined || valueB === "") {
            return (valueA === undefined || valueA === "" ? 1 : 0) - (valueB === undefined || valueB === "" ? 1 : 0)
        }
        if (typeof valueA === "string" && typeof valueB === "string") return sign * collator.compare(valueA, valueB)
        return sign * ((valueA as number) - (valueB as number))
    })
}

/**
 * Whether a family matches a search for one of its nicknames or autonomous systems
 */
export const familyMatches = (family: FamilyRow, search: string): boolean => {
    const term = search.trim().toLowerCase()
    return !term || [family.nicknames, family.autonomousSystems].some(value => value?.toLowerCase().includes(term))
}

/**
 * A Component to show a Table with Families to select one
 */
export const FamilySelectionTable: FunctionComponent<Props> = ({
                                                                   families,
                                                                   closeFamilySelectionDialog,
                                                               }) => {
    // App context
    const {settings, setSettings} = useSettings()

    // Component state
    const hasWeights = families?.some(family => family.consensusWeight !== undefined) ?? false
    const [sort, setSort] = useState<{ column: SortColumn, direction: SortDirection }>()
    // Without a choice of the user, the heaviest families come first, or the largest on days without weights
    const sortColumn = sort?.column ?? (hasWeights ? "consensusWeight" : "relayCount")
    const sortDirection = sort?.direction ?? "desc"
    const [search, setSearch] = useState("")

    const sortedFamilies = useMemo(
        () => families && sortFamilies(families.filter(family => familyMatches(family, search)), sortColumn, sortDirection),
        [families, search, sortColumn, sortDirection]
    )

    const changeSort = (column: SortColumn, firstDirection: SortDirection) => {
        if (column === sortColumn) {
            setSort({column, direction: sortDirection === "asc" ? "desc" : "asc"})
        } else {
            setSort({column, direction: firstDirection})
        }
    }

    return (
        <>
            {/* Only worth it for longer lists, e.g. all families of the map */}
            {families && families.length > 5 &&
                <TextField
                    label="Search by nickname or autonomous system"
                    size="small"
                    fullWidth
                    value={search}
                    onChange={event => setSearch(event.target.value)}
                    sx={{mb: 1}}
                />}
            {sortedFamilies ? <Table size={"small"}>
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
                                        <Typography sx={{fontWeight: "bold",}}>{label}</Typography>
                                    </TableSortLabel>
                                </TableCell>
                            )}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {sortedFamilies.map((family) =>
                            <TableRow
                                key={family.id}
                                onClick={() => {
                                    setSettings({...settings, selectedFamily: family.id})
                                    closeFamilySelectionDialog()
                                }}
                                hover={true}
                                sx={{
                                    "&:hover": {
                                        cursor: "pointer",
                                    }
                                }}
                            >
                                <TableCell scope="row">
                                    <Typography>{family.nicknames}</Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Typography>{family.relayCount}</Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Typography>{family.exitCount}</Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Typography>
                                        {family.consensusWeightShare !== undefined ? formatWeightShare(family.consensusWeightShare) : "-"}
                                    </Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Typography>{family.countryCount}</Typography>
                                </TableCell>
                                <TableCell scope="row">
                                    <Typography>
                                        {(family.autonomousSystems) ?
                                            (family.autonomousSystems) :
                                            "data not yet available"}
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                : <p>We do not have any information about these families for this date.</p>}
        </>
    )
}
