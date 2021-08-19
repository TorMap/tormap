import React, {FunctionComponent} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    makeStyles,
    Table,
    TableBody, TableCell, TableRow,
    Typography
} from "@material-ui/core";
import {Settings, Statistics} from "../types/variousTypes";
import ExpandLessIcon from '@material-ui/icons/ExpandLess';
import {StatsRow} from "./stats-row";
import {getIcon, Icon} from "../types/icons";
import {Colors} from "../util/Config";
import DirectionsRunIcon from "@material-ui/icons/DirectionsRun";
import {icon} from "leaflet";

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

export const MapStats: FunctionComponent<Props> = ({settings, stats}) => {
    const classes = useStyle()

    let rows: StatsRow[] = []
    rows.push({icon: Icon.ExitRelay, title: "Exit Relays", value: stats.relayExitCount})
    rows.push({icon: Icon.GuardRelay, title: "Guard relays", value: stats.relayGuardCount})
    rows.push({icon: Icon.DefaultRelay, title: "Other Relays", value: stats.relayOtherCount})
    rows.push({icon: Icon.TotalRelays, title: "Total Relays", value: stats.relayExitCount + stats.relayGuardCount + stats.relayOtherCount})
    if(stats.familyCount) rows.push({icon: Icon.FamilyCount, title: "Families", value: stats.familyCount})
    if(stats.countryCount) rows.push({icon: Icon.CountryCount, title: "Countries", value: stats.countryCount})

    return (
        <div className={classes.root}>
            <Accordion defaultExpanded={true}>
                <AccordionSummary
                    expandIcon={<ExpandLessIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>
                        Stats{settings.selectedFamily ? " for family" : null}
                        {settings.selectedCountry ? " in " + settings.selectedCountry : null}
                    </Typography>
                </AccordionSummary>
                <AccordionDetails classes={{root: classes.noPadding}}>
                    <Table size="small">
                        <TableBody>
                            {rows.map(row =>
                                (<TableRow>
                                        <TableCell scope="row">
                                            {icon !== undefined ? getIcon(row.icon) : null}
                                        </TableCell>
                                        <TableCell scope="row">
                                            <Typography>{row.title}</Typography>
                                        </TableCell>
                                        <TableCell align={"right"}>
                                            {row.value}
                                        </TableCell>
                                    </TableRow>
                                ))}
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