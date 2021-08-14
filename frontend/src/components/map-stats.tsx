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
import DirectionsRunIcon from '@material-ui/icons/DirectionsRun';
import SecurityIcon from '@material-ui/icons/Security';
import FlagIcon from '@material-ui/icons/Flag';
import TimelineIcon from '@material-ui/icons/Timeline';
import StorageIcon from '@material-ui/icons/Storage';
import SubdirectoryArrowRightIcon from '@material-ui/icons/SubdirectoryArrowRight';
import {Colors} from "../util/Config";
import ExpandLessIcon from '@material-ui/icons/ExpandLess';

/**
 * Styles according to Material UI doc for components used in MapStats component
 */
const useStyle = makeStyles(() => ({
    root: {
        position: "fixed",
        left: "10px",
        bottom: "50px",
        width: "300px",
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
    statistics: Statistics
}

export const MapStats: FunctionComponent<Props> = ({settings, statistics}) => {
    const classes = useStyle()

    // The rows for the node type statistics
    const typeRows = (sett: Settings, stats: Statistics) => {
        let row: Array<Array<string | number | any>> = []
        row.push(["Exit relays", stats.exit, <DirectionsRunIcon style={{color: Colors.Exit}}/>])
        row.push(["Guard relays", stats.guard, <SecurityIcon style={{color: Colors.Guard}}/>])
        row.push(["Other Relays", stats.default, <TimelineIcon style={{color: Colors.Default}}/>])
        row.push(["Total amount of Relays", (stats.exit + stats.guard + stats.default), <SubdirectoryArrowRightIcon/>])
        return row
    }
    // The rows for the node family statistics
    const familyRows = (sett: Settings, stats: Statistics) => {
        let row: Array<Array<string | number | any>> = []
        row.push(["Different families", stats.familyCount, <StorageIcon/>])
        //row.push(["Selectet Family (internal ID)", settings.selectedFamily === undefined ? "none" : settings.selectedFamily, <StorageIcon/>])
        row.push(["Relays in selectet Family", stats.familyRelayCount === undefined ? "none" : stats.familyRelayCount, <StorageIcon/>])
        return row
    }
    // The rows for the node country statistics
    const contryRows = (sett: Settings, stats: Statistics) => {
        let row: Array<Array<string | number | any>> = []
        row.push(["Different countries", stats.countryCount, <FlagIcon/>])
        //row.push(["Selectet Country", settings.selectedCountry === undefined ? "none" : settings.selectedCountry, <FlagIcon/>])
        row.push(["Relays in selectet Country", stats.countryRelayCount === undefined ? "none" : stats.countryRelayCount, <FlagIcon/>])
        return row
    }

    return (
        <div className={classes.root}>
            <Accordion defaultExpanded={true}>
                <AccordionSummary
                    expandIcon={<ExpandLessIcon />}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Relay types</Typography>
                </AccordionSummary>
                <AccordionDetails classes={{root: classes.noPadding}}>
                        <Table size="small">
                            <TableBody>
                                {typeRows(settings, statistics).map(row => (
                                    <TableRow>
                                        <TableCell scope="row">
                                            {row[2] ? row[2] : ""}
                                        </TableCell>
                                        <TableCell scope="row">
                                            <Typography>{row[0]}</Typography>
                                        </TableCell>
                                        <TableCell align={"right"}>
                                            {row[1]}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                </AccordionDetails>
            </Accordion>

            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandLessIcon />}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Family{settings.selectedFamily ? (": " + settings.selectedFamily) : null}</Typography>
                </AccordionSummary>
                <AccordionDetails classes={{root: classes.noPadding}}>
                        <Table size="small">
                            <TableBody>
                                {familyRows(settings, statistics).map(row => (
                                    <TableRow>
                                        <TableCell scope="row">
                                            {row[2] ? row[2] : ""}
                                        </TableCell>
                                        <TableCell scope="row">
                                            <Typography>{row[0]}</Typography>
                                        </TableCell>
                                        <TableCell align={"right"}>
                                            {row[1]}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                </AccordionDetails>
            </Accordion>

            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandLessIcon />}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Country{settings.selectedCountry ? (": " + settings.selectedCountry) : null}</Typography>
                </AccordionSummary>
                <AccordionDetails classes={{root: classes.noPadding}}>
                        <Table size="small">
                            <TableBody>
                                {contryRows(settings, statistics).map(row => (
                                    <TableRow>
                                        <TableCell scope="row">
                                            {row[2] ? row[2] : ""}
                                        </TableCell>
                                        <TableCell scope="row">
                                            <Typography>{row[0]}</Typography>
                                        </TableCell>
                                        <TableCell align={"right"}>
                                            {row[1]}
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