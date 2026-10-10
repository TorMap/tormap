import {Table, TableBody, TableCell, TableHead, TableRow, TableSortLabel, Typography} from "@mui/material";
import {FunctionComponent, useMemo, useState} from "react";

import {useSettings} from "../../../context/settings-context";
import {RelayFamilyIdentifier} from "../../../dto/relay";

interface Props {
    /**
     * Family Identifiers
     */
    familyIdentifiers?: RelayFamilyIdentifier[]

    /**
     * Callback for closing the dialog
     */
    closeFamilySelectionDialog: () => void
}

type SortColumn = "nicknames" | "memberCount" | "autonomousSystems"
type SortDirection = "asc" | "desc"

const columns: { key: SortColumn, label: string, firstDirection: SortDirection, minWidth?: string }[] = [
    {key: "nicknames", label: "Relay nicknames", firstDirection: "asc"},
    {key: "memberCount", label: "Count", firstDirection: "desc", minWidth: "75px"},
    {key: "autonomousSystems", label: "Autonomous Systems", firstDirection: "asc"},
]

const collator = new Intl.Collator(undefined, {numeric: true, sensitivity: "base"})

/**
 * Sort families by a column. Families without autonomous systems stay last in both directions.
 */
export const sortFamilyIdentifiers = (
    families: RelayFamilyIdentifier[],
    column: SortColumn,
    direction: SortDirection,
): RelayFamilyIdentifier[] => {
    const sign = direction === "asc" ? 1 : -1
    return [...families].sort((a, b) => {
        if (column === "memberCount") {
            return sign * (a.memberCount - b.memberCount)
        }
        const valueA = a[column]
        const valueB = b[column]
        if (!valueA || !valueB) {
            return (valueA ? 0 : 1) - (valueB ? 0 : 1)
        }
        return sign * collator.compare(valueA, valueB)
    })
}

/**
 * A Component to show a Table with Families to select one
 */
export const FamilySelectionTable: FunctionComponent<Props> = ({
                                                                   familyIdentifiers,
                                                                   closeFamilySelectionDialog,
                                                               }) => {
    // App context
    const {settings, setSettings} = useSettings()

    // Component state
    const [sortColumn, setSortColumn] = useState<SortColumn>("memberCount")
    const [sortDirection, setSortDirection] = useState<SortDirection>("desc")

    const sortedFamilyIdentifiers = useMemo(
        () => familyIdentifiers && sortFamilyIdentifiers(familyIdentifiers, sortColumn, sortDirection),
        [familyIdentifiers, sortColumn, sortDirection]
    )

    const changeSort = (column: SortColumn, firstDirection: SortDirection) => {
        if (column === sortColumn) {
            setSortDirection(sortDirection === "asc" ? "desc" : "asc")
        } else {
            setSortColumn(column)
            setSortDirection(firstDirection)
        }
    }

    return (
        <>
            {sortedFamilyIdentifiers ? <Table size={"small"}>
                    <TableHead>
                        <TableRow>
                            {columns.map(({key, label, firstDirection, minWidth}) =>
                                <TableCell
                                    key={key}
                                    sx={minWidth ? {minWidth} : undefined}
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
                        {sortedFamilyIdentifiers.map((family) =>
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
                                <TableCell scope="row" sx={{minWidth: "75px",}}>
                                    <Typography>{family.memberCount}</Typography>
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