import {FormControl, InputAdornment, InputLabel, MenuItem, Select, Stack, TextField} from "@mui/material";
import {FunctionComponent, useEffect, useRef} from "react";
import {RelayIdentifierMatch} from "../../../types/relay";
import {nameOfFactory} from "../../../util/util";
import {Search} from "@mui/icons-material";
import {useAtom} from "jotai";
import {focusRelaySearchAtom, relayDetailsDialogOrderAtom, relayDetailsDialogSearchAtom} from "./relay-details-atoms";


export const RelayDetailsSelectionHeader: FunctionComponent = () => {
    const nameOfRelayMatch = nameOfFactory<RelayIdentifierMatch>()
    const [searchRelaysBy, setSearchRelaysBy] = useAtom(relayDetailsDialogSearchAtom)
    const [orderRelaysBy, setOrderRelaysBy] = useAtom(relayDetailsDialogOrderAtom)
    const [focusRelaySearch, setFocusRelaySearch] = useAtom(focusRelaySearchAtom)
    const searchInputRef = useRef<HTMLInputElement>(null)

    // Focus the search field when the search was opened with the keyboard shortcut
    useEffect(() => {
        if (focusRelaySearch && searchInputRef.current) {
            searchInputRef.current.focus()
            setFocusRelaySearch(false)
        }
    }, [focusRelaySearch, setFocusRelaySearch])

    return (
        <Stack direction={"row"}>
            <TextField
                label="Nickname or fingerprint"
                slotProps={{
                    input: {
                        startAdornment: (
                            <InputAdornment position="start">
                                <Search/>
                            </InputAdornment>
                        ),
                    },
                }}
                variant="standard"
                inputRef={searchInputRef}
                onChange={(event) => setSearchRelaysBy(event.target.value)}
                value={searchRelaysBy}
            />
            <FormControl variant="standard" sx={{ml: 3}}>
                <InputLabel id="select-order-by-label">Order by</InputLabel>
                <Select
                    labelId="select-order-by-label"
                    onChange={(event) => setOrderRelaysBy(event.target.value as keyof RelayIdentifierMatch)}
                    value={orderRelaysBy}
                >
                    <MenuItem value={nameOfRelayMatch("nickname")}>Nickname</MenuItem>
                    <MenuItem value={nameOfRelayMatch("relayType")}>Type</MenuItem>
                    <MenuItem value={nameOfRelayMatch("familyId")}>Family</MenuItem>
                </Select>
            </FormControl>
        </Stack>

    )
}
