import {Button, IconButton} from "@mui/material";
import React, {FunctionComponent} from "react";

import {useSettings} from "../../context/settings-context";
import {RelayFamilyIcon} from "../../types/icons";
import {calculateFamilyColor} from "../../util/layer-construction";

interface Props {
    familyId: number,
    furtherAction?: () => void,
    label?: string,
}

export const SelectFamilyButton: FunctionComponent<Props> = ({familyId, furtherAction, label}) => {
    // App context
    const {settings, setSettings} = useSettings()
    const familyColor = calculateFamilyColor(familyId)

    const handleSelectFamily = () => {
        setSettings({
            ...settings,
            selectedFamily: familyId,
            sortFamily: true
        })
        if (furtherAction) {
            furtherAction()
        }
    }

    return label ? (
        <Button
            aria-label="select family"
            onClick={handleSelectFamily}
            startIcon={RelayFamilyIcon}
            variant="outlined"
            sx={{
                color: familyColor,
                borderColor: familyColor,
                "&:hover": {
                    borderColor: familyColor,
                }
            }}
        >
            {label}
        </Button>
    ) : (
        <IconButton
            aria-label="select family"
            onClick={handleSelectFamily}
            sx={{
                color: familyColor
            }}
        >
            {RelayFamilyIcon}
        </IconButton>
    )
}
