import React, {FunctionComponent, ReactElement} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Table,
    TableBody,
    TableCell,
    TableRow,
    Typography
} from "@mui/material";
import {Statistics} from "../../types/app-state";
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import {
    CountryCountIcon,
    ExitRelayIcon,
    FamilyCountIcon,
    GuardRelayIcon,
    OtherRelayIcon,
    TotalRelaysIcon
} from "../../types/icons";
import {getFullName} from "../../util/geojson";
import {useSettings} from "../../util/settings-context";

interface Props {

    /**
     * The currently map statistics of the currently rendered information
     */
    stats: Statistics

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
 * @param stats - the Statistics Object to show
 */
export const MapStats: FunctionComponent<Props> = ({defaultExpanded, elevation, stats}) => {

    const settings = useSettings().settings

    // Construct the stats rows to display
    let rows: StatsRow[] = []
    rows.push({icon: ExitRelayIcon, title: "Exit relays", value: stats.relayExitCount})
    rows.push({icon: GuardRelayIcon, title: "Guard relays", value: stats.relayGuardCount})
    rows.push({icon: OtherRelayIcon, title: "Other relays", value: stats.relayOtherCount})
    rows.push({
        icon: TotalRelaysIcon,
        title: "Total relays",
        value: stats.relayExitCount + stats.relayGuardCount + stats.relayOtherCount
    })
    if (stats.familyCount) rows.push({icon: FamilyCountIcon, title: "Families", value: stats.familyCount})
    if (stats.countryCount) rows.push({icon: CountryCountIcon, title: "Countries", value: stats.countryCount})

    return (
        <Box>
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
        </Box>
    )
}

interface StatsRow {
    icon: ReactElement
    title: string
    value: string | number
}
