import React, {FunctionComponent, ReactElement} from "react";
import {
    TableCell,
    TableRow,
    Typography
} from "@material-ui/core";

interface Props {
    icon?: ReactElement
    title: string
    value: string | number
}

export const StatsRow: FunctionComponent<Props> = ({icon, title, value}) => {
    return (
        <TableRow>
            <TableCell scope="row">
                {icon ? icon : ""}
            </TableCell>
            <TableCell scope="row">
                <Typography>{title}</Typography>
            </TableCell>
            <TableCell align={"right"}>
                {value}
            </TableCell>
        </TableRow>
    )
}