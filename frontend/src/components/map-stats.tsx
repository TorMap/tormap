import {FunctionComponent} from "react";
import {
    Card,
    CardContent,
    makeStyles,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    Typography
} from "@material-ui/core";
import {Settings, Statistics} from "../types/variousTypes";

const useStyle = makeStyles(() => ({
    root: {
        position: "fixed",
        left: "10px",
        bottom: "100px",
        width: "250px",
    }
}))

interface Props {
    settings: Settings
    statistics: Statistics
}

export const MapStats: FunctionComponent<Props> = ({settings, statistics}) => {
    const classes = useStyle()
    const table = true

    const rows = (sett: Settings, stat: Statistics) => {
        let row: Array<string[]> = []
        if (stat.exit) row.push(["Exit relays", stat.exit.toString()])
        if (stat.guard) row.push(["Guard relays", stat.guard.toString()])
        if (stat.default) row.push(["Other Relays", stat.default.toString()])
        if (stat.exit || stat.guard || stat.default) row.push(["Total amount of Relays", (stat.exit + stat.guard + stat.default).toString()])
        if (settings.sortFamily) row.push(["Selectet Family", settings.selectedFamily == undefined ? "none" : settings.selectedFamily.toString()])
        if (settings.sortCountry) row.push(["Selectet Country", settings.selectedCountry == undefined ? "none" : settings.selectedCountry])
        return row
    }

    return (
        <div>
            {table ? (
                <TableContainer component={Card} className={classes.root}>
                    <Table size="small">
                        <TableBody>
                            {rows(settings, statistics).map(row => (
                                <TableRow key={row[0]}>
                                    <TableCell component="th" scope="row">
                                        {row[0]}
                                    </TableCell>
                                    <TableCell align={"center"}>
                                        {row[1]}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            ) : (
                <Card variant={"outlined"} className={classes.root}>
                    <CardContent>
                        <Typography>
                            Exit relays: {statistics.exit}
                        </Typography>
                        <Typography>
                            Guard relays: {statistics.guard}
                        </Typography>
                        <Typography>
                            Other relays: {statistics.default}
                        </Typography>
                        <Typography>
                            maxValueOnSameCoordinate: {statistics.maxValueOnSameCoordinate}
                        </Typography>
                        <Typography>
                            selected Country: {settings.selectedCountry}
                        </Typography>
                    </CardContent>
                </Card>
            )}
        </div>
    )
}