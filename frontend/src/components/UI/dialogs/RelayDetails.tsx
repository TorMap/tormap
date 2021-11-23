import React, {FunctionComponent} from "react";
import {CircularProgress, Table, TableBody, TableCell, TableRow, Typography} from "@mui/material";
import {DetailsInfo} from "../../../types/responses";

interface Props {
    /**
     * Details for a Relay
     */
    relayDetails?: DetailsInfo[]
}

/**
 * A Component to show more detailed information on a Relay
 * @param relayDetails
 */
export const RelayDetails: FunctionComponent<Props> = ({relayDetails}) => {
    return (
        <div>
            {relayDetails?
                <Table size={"small"}>
                    <TableBody>
                        {relayDetails.map((relayInfo) =>
                            relayInfo.value &&
                            <TableRow key={relayInfo.name}>
                                <TableCell scope="row" sx={{minWidth: "150px",}}>
                                    <Typography>{relayInfo.name}</Typography>
                                </TableCell>
                                <TableCell scope="row">
                                    <Typography>{relayInfo.value}</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                : <CircularProgress color={"inherit"} size={24}/>}
        </div>
    )
}