import {FunctionComponent} from "react";
import {Card, CardContent, Typography} from "@material-ui/core";
import {Statistics} from "../../types/variousTypes";
import "./map-legend.scss"

interface Props {
    statistics: Statistics
}

export const MapStats: FunctionComponent<Props> = ({statistics}) => {

    return (
        <div className={"map-stats"}>
            <Card variant={"outlined"}>
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
                </CardContent>
            </Card>
        </div>
    )
}