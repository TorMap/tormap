import React, {FunctionComponent} from "react";
import {FormControl, MenuItem, Select, Typography} from "@mui/material";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";
import {RelayMatch} from "../../../types/relay";
import {nameOfFactory} from "../../../util/util";

interface Props {
    sortRelaysBy: keyof RelayMatch
    handleSelectSortByChange: (event: SelectChangeEvent<keyof RelayMatch>) => void
}

export const RelayDetailsSelectionHeader: FunctionComponent<Props> = ({sortRelaysBy, handleSelectSortByChange}) => {
    const nameOfRelayMatch = nameOfFactory<RelayMatch>()
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
                    <MenuItem value={nameOfRelayMatch("nickname")}>Nickname</MenuItem>
                    <MenuItem value={nameOfRelayMatch("relayType")}>Type</MenuItem>
                    <MenuItem value={nameOfRelayMatch("familyId")}>Family</MenuItem>
                </Select>
            </FormControl>
        </>
    )
}