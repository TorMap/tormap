import React, {FunctionComponent, ReactElement} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    makeStyles,
    Table,
    TableBody,
    TableCell,
    TableRow,
    Typography
} from "@material-ui/core";
import {Settings, Statistics} from "../types/app-state";
import ExpandLessIcon from '@material-ui/icons/ExpandLess';
import {
    CountryCountIcon,
    ExitRelayIcon,
    FamilyCountIcon,
    GuardRelayIcon,
    OtherRelayIcon,
    TotalRelaysIcon
} from "../types/icons";
import {getFullName} from "../util/geojson";

/**
 * Styles according to Material UI doc for components used in MapStats component
 */
const useStyle = makeStyles(() => ({
    root: {
        position: "fixed",
        left: "1%",
        bottom: "15px",
        maxWidth: "20%",
    },
    noPadding: {
        padding: 0,
    }
}))

interface Props {
    /**
     * The currently applied app settings
     */
    settings: Settings

    /**
     * The currently map statistics of the currently rendered information
     */
    stats: Statistics
}

/**
 * The Component showing statistics for rendered nodes
 * @param settings - the App Settings
 * @param stats - the Statistics Object to show
 */
export const MapStats: FunctionComponent<Props> = ({settings, stats}) => {
    const classes = useStyle()

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
        <div className={classes.root}>
            <Accordion defaultExpanded={true}>
                <AccordionSummary
                    expandIcon={<ExpandLessIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>
                        Stats{settings.selectedFamily ? " for selected family" : null}
                        {settings.selectedCountry ? " in " + getFullName(settings.selectedCountry) : null}
                    </Typography>
                </AccordionSummary>
                <AccordionDetails classes={{root: classes.noPadding}}>
                    <Table size="small">
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
        </div>
    )
}

interface StatsRow {
    icon: ReactElement
    title: string
    value: string | number
}
