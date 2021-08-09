import {FunctionComponent, ReactComponentElement} from "react";
import {
    Card,
    CardContent, Icon,
    makeStyles, SvgIcon,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    Typography
} from "@material-ui/core";
import {rowType, Settings, Statistics} from "../types/variousTypes";
import {ReactComponent} from "*.svg";
import DirectionsRunIcon from '@material-ui/icons/DirectionsRun';
import SecurityIcon from '@material-ui/icons/Security';
import DeviceHubIcon from '@material-ui/icons/DeviceHub';
import FlagIcon from '@material-ui/icons/Flag';
import TimelineIcon from '@material-ui/icons/Timeline';
import StorageIcon from '@material-ui/icons/Storage';
import SubdirectoryArrowRightIcon from '@material-ui/icons/SubdirectoryArrowRight';
import {SvgIconComponent} from "@material-ui/icons";

const useStyle = makeStyles(() => ({
    root: {
        position: "fixed",
        left: "10px",
        bottom: "100px",
        width: "300px",
    },
}))

interface Props {
    settings: Settings
    statistics: Statistics
}

export const MapStats: FunctionComponent<Props> = ({settings, statistics}) => {
    const classes = useStyle()
    const table = true

    const rows = (sett: Settings, stats: Statistics) => {
        let row: Array<Array<string|number|any>> = []
        row.push(["Exit relays", stats.exit, <DirectionsRunIcon style={{color: "#f96969"}}/>])
        row.push(["Guard relays", stats.guard, <SecurityIcon style={{color: "#fcb045"}}/>])
        row.push(["Other Relays", stats.default, <TimelineIcon style={{color: "#833ab4"}}/>])
        row.push(["Total amount of Relays", (stats.exit + stats.guard + stats.default), <SubdirectoryArrowRightIcon/>])


        row.push(["", ""])
        row.push(["Family count", stats.familyCount, <StorageIcon/>])
        row.push(["Selectet Family (internal ID)", settings.selectedFamily === undefined ? "none" : settings.selectedFamily, <StorageIcon/>])
        row.push(["Relays in selectet Family", stats.familyRelayCount === undefined ? "none" : stats.familyRelayCount, <StorageIcon/>])


        row.push(["", ""])
        row.push(["Country count", stats.countryCount, <FlagIcon/>])
        row.push(["Selectet Country", settings.selectedCountry === undefined ? "none" : settings.selectedCountry, <FlagIcon/>])
        row.push(["Relays in selectet Country", stats.countryRelayCount === undefined ? "none" : stats.countryRelayCount, <FlagIcon/>])
        return row
    }

    return (
        <div>
            <TableContainer component={Card} className={classes.root}>
                <Table size="small">
                    <TableBody>
                        {rows(settings, statistics).map(row => (
                            <TableRow>
                                {row[2] ? (
                                    <TableCell scope="row">
                                        {row[2]}
                                    </TableCell>
                                ) : null}
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
            </TableContainer>
        </div>
    )
}