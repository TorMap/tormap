import {Box, Tooltip} from "@mui/material";
import {ChangeEvent, FunctionComponent, useEffect, useRef, useState} from "react";

import {tooltipTimeDelay} from "../../config";
import {useSettings} from "../../context/settings-context";
import {RelayType, RelayTypeLabel} from "../../types/relay";

interface Props {
    relayType: RelayType
}

/**
 * A round color swatch, which opens the browser's native color picker for a relay type
 */
export const RelayTypeColorInput: FunctionComponent<Props> = ({relayType}) => {
    // App context
    const {settings, setSettings} = useSettings()

    // Component state
    const [draftColor, setDraftColor] = useState<string>()
    const inputRef = useRef<HTMLInputElement>(null)

    // React's onChange fires on every color the picker passes while dragging. Redrawing all relays that often is slow,
    // so the setting is only updated with the native change event, which fires once a color was picked.
    useEffect(() => {
        const input = inputRef.current
        if (!input) return
        const commitColor = () => {
            setSettings({...settings, relayTypeColors: {...settings.relayTypeColors, [relayType]: input.value}})
            setDraftColor(undefined)
        }
        input.addEventListener("change", commitColor)
        return () => input.removeEventListener("change", commitColor)
    }, [relayType, setSettings, settings])

    const label = `${RelayTypeLabel[relayType]} relay color`
    return (
        <Tooltip title={`Change ${label}`} placement={"left"} enterDelay={tooltipTimeDelay}>
            <Box
                component="input"
                type="color"
                ref={inputRef}
                aria-label={label}
                value={draftColor ?? settings.relayTypeColors[relayType]}
                onChange={(event: ChangeEvent<HTMLInputElement>) => setDraftColor(event.target.value)}
                sx={{
                    width: 22,
                    height: 22,
                    flexShrink: 0,
                    padding: 0,
                    border: "2px solid rgba(255, 255, 255, 0.7)",
                    borderRadius: "50%",
                    background: "none",
                    overflow: "hidden",
                    cursor: "pointer",
                    "&::-webkit-color-swatch-wrapper": {padding: 0},
                    "&::-webkit-color-swatch": {border: "none"},
                    "&::-moz-color-swatch": {border: "none"},
                }}
            />
        </Tooltip>
    )
}
