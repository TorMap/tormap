import React, {FunctionComponent} from "react";
import {Box, CircularProgress, IconButton, Tooltip, Typography} from "@mui/material";
import {getRelayType} from "../../../util/aggregate-relays";
import {RelayDetailsMatch, RelayType, RelayTypeLabel} from "../../../types/relay";
import {getIcon} from "../../../types/icons";
import {SelectFamilyButton} from "../../buttons/SelectFamilyButton";
import CloseIcon from "@mui/icons-material/Close";
import {useSettings} from "../../../context/settings-context";

interface Props {
    /**
     * Hide the modal
     */
    closeDialog: () => void
    finishQuickAction?: () => void
    relayDetailsMatch?: RelayDetailsMatch,
}

export const RelayDetailsHeader: FunctionComponent<Props> = ({
                                                                 closeDialog,
                                                                 finishQuickAction = closeDialog,
                                                                 relayDetailsMatch,
                                                             }) => {
    // App context
    const {settings, setSettings} = useSettings()
    const relayType = relayDetailsMatch ? getRelayType(relayDetailsMatch) : undefined

    return (
        <Box display="flex" alignItems={"center"}>
            {relayDetailsMatch ? <>
                <Tooltip title={`Relay's nickname`}>
                    <Typography sx={{display: "inline"}}
                                variant="h6">
                        {relayDetailsMatch.nickname}
                    </Typography>
                </Tooltip>
                <Tooltip title={`${RelayTypeLabel[relayType!]} relay`}>
                    <IconButton
                        aria-label="select relay type"
                        sx={{ml: 1}}
                        onClick={() => {
                            setSettings({
                                ...settings,
                                showRelayTypes: {
                                    [RelayType.Exit]: RelayType.Exit === relayType,
                                    [RelayType.Guard]: RelayType.Guard === relayType,
                                    [RelayType.Other]: RelayType.Other === relayType,
                                },
                            })
                            finishQuickAction()
                        }}
                    >
                        {getIcon(getRelayType(relayDetailsMatch))}
                    </IconButton>
                </Tooltip>
                {relayDetailsMatch?.familyId &&
                    <Tooltip title={`Show family on map`}>
                        <Box>
                            <SelectFamilyButton
                                familyId={relayDetailsMatch.familyId}
                                furtherAction={finishQuickAction}
                            />
                        </Box>
                    </Tooltip>
                }
            </> : <CircularProgress color={"inherit"} size={22.5} sx={{mt: 1}}/>}
            <IconButton aria-label="close" sx={{
                position: "absolute",
                right: "16px",
            }} onClick={closeDialog}>
                <CloseIcon/>
            </IconButton>
        </Box>
    )
}