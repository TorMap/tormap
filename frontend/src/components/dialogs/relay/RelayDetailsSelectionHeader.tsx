import {FormControl, InputAdornment, InputLabel, MenuItem, Select, Stack, TextField} from "@mui/material";
import React, {FunctionComponent} from "react";
import {RelayIdentifierMatch} from "../../../types/relay";
import {nameOfFactory} from "../../../util/util";
import {Search} from "@mui/icons-material";
import {atom, useAtom} from "jotai";

export const relayDetailsDialogSearchAtom = atom("");
export const relayDetailsDialogOrderAtom = atom<keyof RelayIdentifierMatch>("nickname");

export const RelayDetailsSelectionHeader: FunctionComponent = () => {
    const nameOfRelayMatch = nameOfFactory<RelayIdentifierMatch>()
    const [, setSearchRelaysBy] = useAtom(relayDetailsDialogSearchAtom)
    const [, setOrderRelaysBy] = useAtom(relayDetailsDialogOrderAtom)

    return (
        <Stack direction={"row"}>
            <TextField
                label="Nickname or fingerprint"
                InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">
                            <Search/>
                        </InputAdornment>
                    ),
                }}
                variant="standard"
                onChange={(event) => setSearchRelaysBy(event.target.value)}
            />
            <FormControl variant="standard" sx={{ml: 3}}>
                <InputLabel id="select-order-by-label">Order by</InputLabel>
                <Select
                    labelId="select-order-by-label"
                    onChange={(event) => setOrderRelaysBy(event.target.value as keyof RelayIdentifierMatch)}
                    defaultValue={nameOfRelayMatch("nickname")}
                >
                    <MenuItem value={nameOfRelayMatch("nickname")}>Nickname</MenuItem>
                    <MenuItem value={nameOfRelayMatch("relayType")}>Type</MenuItem>
                    <MenuItem value={nameOfRelayMatch("familyId")}>Family</MenuItem>
                </Select>
            </FormControl>
        </Stack>

    )
}
