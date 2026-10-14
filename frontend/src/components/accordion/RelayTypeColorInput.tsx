import {Box, Tooltip} from "@mui/material";
import {ChangeEvent, FunctionComponent, useEffect, useRef, useState} from "react";

import {tooltipTimeDelay} from "../../config";
import {useSettings} from "../../context/settings-context";
import {RelayTypeIcon} from "../../types/icons";
import {RelayType, RelayTypeLabel} from "../../types/relay";

interface Props {
    relayType: RelayType
}

/**
 * The icon of a relay type in its color, which opens the browser's native color picker for the relay type
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
    const color = draftColor ?? settings.relayTypeColors[relayType]
    return (
        <Tooltip title={`Change ${label}`} placement={"left"} enterDelay={tooltipTimeDelay}>
            <Box sx={{position: "relative", display: "inline-flex", flexShrink: 0, cursor: "pointer"}}>
                <RelayTypeIcon relayType={relayType} color={color}/>
                {/* The native color input covers the icon, so a click opens the browser's color picker */}
                <Box
                    component="input"
                    type="color"
                    ref={inputRef}
                    aria-label={label}
                    value={color}
                    onChange={(event: ChangeEvent<HTMLInputElement>) => setDraftColor(event.target.value)}
                    sx={{
                        position: "absolute",
                        inset: 0,
                        width: "100%",
                        height: "100%",
                        padding: 0,
                        border: "none",
                        opacity: 0,
                        cursor: "pointer",
                    }}
                />
            </Box>
        </Tooltip>
    )
}
