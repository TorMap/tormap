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
                                title={"Total relays"}
                                value={stats.relayExitCount + stats.relayGuardCount + stats.relayOtherCount}
                            />
                            {stats.familyCount ? <StatsRow
                                icon={<GroupIcon/>}
                                title={"Families"}
                                value={stats.familyCount}
                            /> : null}
                            {stats.countryCount ? <StatsRow
                                icon={<PublicIcon/>}
                                title={"Countries"}
                                value={stats.countryCount}
                            /> : null}
                        </TableBody>
                    </Table>
                </AccordionDetails>
            </Accordion>
        </div>
    )
}