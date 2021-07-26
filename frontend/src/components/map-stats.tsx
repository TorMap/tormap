import {FunctionComponent} from "react";
import {Card, CardContent, makeStyles, Typography} from "@material-ui/core";
import {Settings, Statistics} from "../types/variousTypes";

const useStyle = makeStyles(() => ({
    root: {
        position: "fixed",
        left: "10px",
        bottom: "100px",
    }
}))

interface Props {
    settings: Settings
    statistics: Statistics
}

export const MapStats: FunctionComponent<Props> = ({settings, statistics}) => {
    const classes = useStyle()
    return (
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
    )
}