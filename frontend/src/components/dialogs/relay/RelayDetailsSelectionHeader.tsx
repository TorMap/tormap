import React, {FunctionComponent} from "react";
import {Box, FormControl, MenuItem, Select, Typography} from "@mui/material";
import {SelectChangeEvent} from "@mui/material/Select/SelectInput";
import {RelayIdentifierMatch} from "../../../types/relay";
import {nameOfFactory} from "../../../util/util";

interface Props {
    sortRelaysBy: keyof RelayIdentifierMatch
    handleSelectSortByChange: (event: SelectChangeEvent<keyof RelayIdentifierMatch>) => void
}

export const RelayDetailsSelectionHeader: FunctionComponent<Props> = ({sortRelaysBy, handleSelectSortByChange}) => {
    const nameOfRelayMatch = nameOfFactory<RelayIdentifierMatch>()
    return (
        <Box display="flex" alignItems={"center"} sx={{mt: 0.5}}>
            <Typography sx={{display: "inline"}} variant="h6">
                Relays
            </Typography>
            <FormControl variant="standard" sx={{ml: 3}}>
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
        </Box>
    )
}