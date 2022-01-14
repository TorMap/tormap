import React, {FunctionComponent} from "react";
import {IconButton} from "@mui/material";
import {RelayFamilyIcon} from "../../types/icons";
import {useSettings} from "../../context/settings-context";

interface Props {
    newFamilyId: number,
    furtherAction?: () => void,
}

export const SelectFamilyButton: FunctionComponent<Props> = ({newFamilyId, furtherAction}) => {
    const settings = useSettings().settings
    const setSettings = useSettings().setSettings

    return (
        <IconButton aria-label="select family" onClick={() => {
            setSettings({
                ...settings,
                selectedFamily: newFamilyId,
                sortFamily: true
            })
            if (furtherAction) {
                furtherAction()
            }
        }}>
            {RelayFamilyIcon}
        </IconButton>
    )
}
