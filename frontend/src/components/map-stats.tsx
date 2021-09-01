import React, {FunctionComponent} from "react";
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
import {Settings, Statistics} from "../types/variousTypes";
import ExpandLessIcon from '@material-ui/icons/ExpandLess';
import {getIcon, Icon} from "../types/icons";
import {icon} from "leaflet";
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
 * @constructor
 */
export const MapStats: FunctionComponent<Props> = ({settings, stats}) => {
    const classes = useStyle()

    // Construct the stats rows to display
    let rows: StatsRow[] = []
    rows.push({icon: Icon.ExitRelay, title: "Exit relays", value: stats.relayExitCount})
    rows.push({icon: Icon.GuardRelay, title: "Guard relays", value: stats.relayGuardCount})
    rows.push({icon: Icon.DefaultRelay, title: "Other relays", value: stats.relayOtherCount})
    rows.push({
        icon: Icon.TotalRelays,
        title: "Total relays",
        value: stats.relayExitCount + stats.relayGuardCount + stats.relayOtherCount
    })
    if (stats.familyCount) rows.push({icon: Icon.FamilyCount, title: "Families", value: stats.familyCount})
    if (stats.countryCount) rows.push({icon: Icon.CountryCount, title: "Countries", value: stats.countryCount})

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
                                        {icon !== undefined ? getIcon(statsRow.icon) : null}
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
    icon?: Icon
    title: string
    value: string | number
}
