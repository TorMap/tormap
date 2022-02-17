import React, {FunctionComponent} from "react";
import {FormControl, MenuItem, Select, Typography} from "@mui/material";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";
import {RelayMatch} from "./RelayDetailsDialogLarge";

interface Props {
    sortRelaysBy: keyof RelayMatch
    handleSelectSortByChange: (event: SelectChangeEvent<keyof RelayMatch>) => void
}

export const RelayDetailsSelectionHeader: FunctionComponent<Props> = ({sortRelaysBy, handleSelectSortByChange}) => {
    return (
        <>
            <Typography sx={{display: "inline"}} variant="h6">
                Relays
            </Typography>
            <FormControl variant="standard" sx={{marginLeft: "20px"}}>
                <Select
                    value={sortRelaysBy}
                    label="Sort by"
                    onChange={handleSelectSortByChange}
                >
                    <MenuItem value={"relayType"}>Type</MenuItem>
                    <MenuItem value={"nickname"}>Nickname</MenuItem>
                </Select>
            </FormControl>
        </>
    )
}