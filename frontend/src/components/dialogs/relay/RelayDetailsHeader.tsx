import React, {FunctionComponent} from "react";
import {Box, CircularProgress, IconButton, Typography} from "@mui/material";
import {getRelayType} from "../../../util/aggregate-relays";
import {RelayDetailsMatch, RelayType} from "../../../types/relay";
import {getIcon} from "../../../types/icons";
import {SelectFamilyButton} from "../../buttons/SelectFamilyButton";
import CloseIcon from "@mui/icons-material/Close";
import {useSettings} from "../../../context/settings-context";

interface Props {
    /**
     * Hide the modal
     */
    closeDialog: () => void
    relayDetailsMatch?: RelayDetailsMatch,
}

export const RelayDetailsHeader: FunctionComponent<Props> = ({
                                                                 closeDialog,
                                                                 relayDetailsMatch,
                                                             }) => {
    // App context
    const {settings, setSettings} = useSettings()

    return (
        <>
            {relayDetailsMatch ?
                <Box display="flex" alignItems={"center"}>
                    <Typography sx={{display: "inline"}}
                                variant="h6">
                        {relayDetailsMatch.nickname}
                    </Typography>
                    <IconButton
                        aria-label="select relay type"
                        sx={{ml: 1}}
                        onClick={() => {
                            const relayType = getRelayType(relayDetailsMatch)
                            setSettings({
                                ...settings,
                                showRelayTypes: {
                                    [RelayType.Exit]: RelayType.Exit === relayType,
                                    [RelayType.Guard]: RelayType.Guard === relayType,
                                    [RelayType.Other]: RelayType.Other === relayType,
                                },
                            })
                            closeDialog()
                        }}
                    >
                        {getIcon(getRelayType(relayDetailsMatch))}
                    </IconButton>
                    {relayDetailsMatch?.familyId &&
                        <SelectFamilyButton
                            familyId={relayDetailsMatch.familyId}
                            furtherAction={closeDialog}
                        />
                    }
                </Box> : <CircularProgress color={"inherit"} size={22.5} sx={{mt: 1}}/>
            }
            <IconButton aria-label="close" sx={{
                position: "absolute",
                right: "10px",
                top: "16px",
            }} onClick={closeDialog}>
                <CloseIcon/>
            </IconButton>
        </>
    )
}