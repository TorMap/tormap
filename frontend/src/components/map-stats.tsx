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
import GroupIcon from '@material-ui/icons/Group';
import PublicIcon from '@material-ui/icons/Public';
import ExpandLessIcon from '@material-ui/icons/ExpandLess';
import {Colors} from "../util/Config";
import {StatsRow} from "./stats-row";

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
    stats: Statistics
}

export const MapStats: FunctionComponent<Props> = ({settings, stats}) => {
    const classes = useStyle()

    // The rows for the node type statistics
    const typeRows = (sett: Settings, stats: Statistics) => {
        let row: Array<Array<string | number | any>> = []
        row.push(["Exit relays", stats.relayExitCount, <DirectionsRunIcon style={{color: Colors.Exit}}/>])
        row.push(["Guard relays", stats.relayGuardCount, <SecurityIcon style={{color: Colors.Guard}}/>])
        row.push(["Other Relays", stats.relayOtherCount, <TimelineIcon style={{color: Colors.Default}}/>])
        row.push(["Total amount of Relays", (stats.relayExitCount + stats.relayGuardCount + stats.relayOtherCount), <SubdirectoryArrowRightIcon/>])
        return row
    }

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
                            <StatsRow
                                icon={<DirectionsRunIcon style={{color: Colors.Exit}}/>}
                                title={"Exit relays"}
                                value={stats.relayExitCount}
                            />
                            <StatsRow
                                icon={<SecurityIcon style={{color: Colors.Guard}}/>}
                                title={"Guard relays"}
                                value={stats.relayGuardCount}
                            />
                            <StatsRow
                                icon={<TimelineIcon style={{color: Colors.Default}}/>}
                                title={"Other relays"}
                                value={stats.relayOtherCount}
                            />
                            <StatsRow
                                icon={<SubdirectoryArrowRightIcon/>}
                                title={"Total amount of Relays"}
                                value={stats.relayExitCount + stats.relayGuardCount + stats.relayOtherCount}
                            />
                            <StatsRow // TODO count families according to selected country
                                icon={<GroupIcon/>}
                                title={"Different families"}
                                value={stats.familyCount}
                            />
                            <StatsRow
                                icon={<PublicIcon/>}
                                title={"Different countries"}
                                value={stats.countryCount}
                            />
                        </TableBody>
                    </Table>
                </AccordionDetails>
            </Accordion>
        </div>
    )
}