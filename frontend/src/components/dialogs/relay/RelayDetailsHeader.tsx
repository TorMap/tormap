import React, {FunctionComponent} from "react";
import {Box, CircularProgress, IconButton, Typography} from "@mui/material";
import {getRelayType} from "../../../util/aggregate-relays";
import {RelayType} from "../../../types/relay";
import {getIcon} from "../../../types/icons";
import {SelectFamilyButton} from "../../buttons/SelectFamilyButton";
import CloseIcon from "@mui/icons-material/Close";
import {useSettings} from "../../../context/settings-context";
import {RelayDetailsDto, RelayLocationDto} from "../../../dto/relay";

interface Props {
    /**
     * Hide the modal
     */
    closeDialog: () => void
    relayDetails?: RelayDetailsDto
    relayLocation?: RelayLocationDto
}

export const RelayDetailsHeader: FunctionComponent<Props> = ({
                                                                 closeDialog,
                                                                 relayDetails,
                                                                 relayLocation,
                                                             }) => {
    // App context
    const {settings, setSettings} = useSettings()

    return (
        <>
            {relayDetails && relayLocation ?
                <Box display="flex" alignItems={"center"}>
                    <Typography sx={{display: "inline"}}
                                variant="h6">
                        {relayDetails.nickname}
                    </Typography>
                    {relayLocation &&
                        <IconButton
                            aria-label="select relay type"
                            sx={{ml: 1}}
                            onClick={() => {
                                const relayType = getRelayType(relayLocation)
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
                            {getIcon(getRelayType(relayLocation))}
                        </IconButton>
                    }
                    {relayLocation?.familyId &&
                        <SelectFamilyButton
                            familyId={relayLocation.familyId}
                            furtherAction={closeDialog}
                        />
                    }
                </Box> : <CircularProgress color={"inherit"} size={24}/>
            }
            <IconButton aria-label="close" sx={{
                position: "absolute",
                right: "10px",
                top: "10px",
            }} onClick={closeDialog}>
                <CloseIcon/>
            </IconButton>
        </>
    )
}