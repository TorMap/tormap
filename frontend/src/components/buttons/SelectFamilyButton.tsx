import {IconButton} from "@mui/material";
import React, {FunctionComponent} from "react";

import {useSettings} from "../../context/settings-context";
import {RelayFamilyIcon} from "../../types/icons";
import {calculateFamilyColor} from "../../util/layer-construction";

interface Props {
    familyId: number,
    furtherAction?: () => void,
}

export const SelectFamilyButton: FunctionComponent<Props> = ({familyId, furtherAction}) => {
    // App context
    const {settings, setSettings} = useSettings()

    return (
        <IconButton
            aria-label="select family"
            onClick={() => {
                setSettings({
                    ...settings,
                    selectedFamily: familyId,
                    sortFamily: true
                })
                if (furtherAction) {
                    furtherAction()
                }
            }}
            sx={{
                color: calculateFamilyColor(familyId)
            }}
        >
            {RelayFamilyIcon}
        </IconButton>
    )
}
