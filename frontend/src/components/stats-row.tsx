import React, {FunctionComponent, useEffect} from "react";
import {
    TableCell,
    TableRow,
    Typography
} from "@material-ui/core";
import {getIcon, Icon} from "../types/icons";

interface Props {
    icon?: Icon
    title: string
    value: string | number
}
//todo: remove file
export const StatsRow: FunctionComponent<Props> = ({icon, title, value}) => {
    return (
        <TableRow>
            <TableCell scope="row">
                {icon !== undefined ? getIcon(icon) : null}
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