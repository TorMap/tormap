import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Table,
    TableBody,
    TableCell,
    TableRow,
    Typography
} from "@mui/material";
import React, {FunctionComponent, ReactElement} from "react";

import {useSettings} from "../../context/settings-context";
import {useStatistics} from "../../context/statistics-context";
import {
    EarthIcon,
    ExitRelayIcon,
    GuardRelayIcon,
    OtherRelayIcon,
    RelayFamilyIcon,
    TotalRelaysIcon
} from "../../types/icons";
import {getFullName} from "../../util/geojson";

interface Props {
    /**
     * whether the statistics are expanded by default
     */
    defaultExpanded: boolean

    /**
     * elevation for material ui styling
     */
    elevation: number
}

/**
 * The Component showing statistics for rendered relays
 * @param settings - the App Settings
 */
export const MapStats: FunctionComponent<Props> = ({defaultExpanded, elevation}) => {
    // App context
    const {settings} = useSettings()
    const {statistics} = useStatistics()

    // Construct the stats rows to display
    const rows: StatsRow[] = []
    rows.push({icon: ExitRelayIcon, title: "Exit relays", value: statistics.relayExitCount})
    rows.push({icon: GuardRelayIcon, title: "Guard relays", value: statistics.relayGuardCount})
    rows.push({icon: OtherRelayIcon, title: "Other relays", value: statistics.relayOtherCount})
    rows.push({
        icon: TotalRelaysIcon,
        title: "Total relays",
        value: statistics.relayExitCount + statistics.relayGuardCount + statistics.relayOtherCount
    })
    if (statistics.familyCount) rows.push({icon: RelayFamilyIcon, title: "Families", value: statistics.familyCount})
    if (statistics.countryCount) rows.push({icon: EarthIcon, title: "Countries", value: statistics.countryCount})

    return (
        <>
            <Accordion defaultExpanded={defaultExpanded} elevation={elevation}>
                <AccordionSummary
                    expandIcon={defaultExpanded ? <ExpandLessIcon/> : <ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>
                        Stats{settings.selectedFamily ? " for family" : null}
                        {settings.selectedCountry ? " in " + getFullName(settings.selectedCountry) : null}
                    </Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <Table size={"small"}>
                        <TableBody>
                            {rows.map(statsRow =>
                                <TableRow key={statsRow.title}>
                                    <TableCell scope="row">
                                        {statsRow.icon}
                                    </TableCell>
                                    <TableCell scope="row">
                                        <Typography>{statsRow.title}</Typography>
                                    </TableCell>
                                    <TableCell align={"right"}>
                                        {statsRow.value}
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </AccordionDetails>
            </Accordion>
        </>
    )
}

interface StatsRow {
    icon: ReactElement
    title: string
    value: string | number
}
