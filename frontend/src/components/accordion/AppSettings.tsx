import {ExpandMore as ExpandMoreIcon, Search as SearchIcon} from "@mui/icons-material";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box, Button,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Switch,
    Tooltip,
    Typography,
} from "@mui/material";
import {FunctionComponent} from "react";

import {defaultSettings, tooltipTimeDelay} from "../../config";
import {useSettings} from "../../context/settings-context";
import {RelayTypeIcon} from "../../types/icons";
import {RelayFlagLabel, RelayType, RelayTypeLabel, RelayTypeTooltip} from "../../types/relay";
import {relayMustIncludeFlagOptions, Settings} from "../../types/settings";
import {nameOfFactory} from "../../util/util";
import {ExternalLink} from "../link/ExternalLink";
import {useAtom} from "jotai";
import {relaysForDetailsDialogAtom, showRelayDetailsDialogAtom} from "../dialogs/relay/ResponsiveRelayDetailsDialog";
import {filteredRelaysAtom} from "../leaflet/LeafletLayers";
import {relayDetailsDialogSearchAtom} from "../dialogs/relay/RelayDetailsSelectionHeader";
import {RelayTypeColorInput} from "./RelayTypeColorInput";

interface Props {
    elevation: number
}

/**
 * A component for changing the app settings
 */
export const AppSettings: FunctionComponent<Props> = ({elevation = 24}) => {
    // App context
    const {settings, changeSettings, setSettings} = useSettings()

    // Atom state
    const [, setShowRelayDetailsDialog] = useAtom(showRelayDetailsDialogAtom)
    const [, setRelaysForDetailsDialog] = useAtom(relaysForDetailsDialogAtom)
    const [, setRelayDetailsDialogSearch] = useAtom(relayDetailsDialogSearchAtom)
    const [filteredRelays] = useAtom(filteredRelaysAtom)

    // Util
    const nameOfSetting = nameOfFactory<Settings>()
    const relayTypes = [RelayType.Exit, RelayType.Guard, RelayType.Other]
    const hasCustomRelayTypeColors = relayTypes.some(relayType =>
        settings.relayTypeColors[relayType] !== defaultSettings.relayTypeColors[relayType]
    )

    return (
        <>
            <Button variant="contained" startIcon={<SearchIcon />} fullWidth={true} sx={{mb: "16px"}} onClick={() => {
                setRelaysForDetailsDialog(filteredRelays)
                setShowRelayDetailsDialog(true)
                setRelayDetailsDialogSearch("")
            }}>
                Search {filteredRelays.length} relays
            </Button>
            <Accordion elevation={elevation} defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Group relays by</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <FormGroup>
                        <Tooltip
                            key={"Family"}
                            title={"Every verified relay family gets a somewhat unique looking color assigned. A circle's size is relative to the amount of relays located at it's center. Click a circle, to see details."}
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}
                        >
                            <FormControlLabel
                                key={"Family"}
                                control={<Switch checked={settings.sortFamily} onChange={changeSettings}/>}
                                label={"Family"}
                                name={nameOfSetting("sortFamily")}
                            />
                        </Tooltip>
                        <Tooltip
                            key={"Country"}
                            title={"Relays, which are located in the same country, will be painted in the same color. Select a country, to see specific stats for it."}
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}
                        >
                            <FormControlLabel
                                key={"Country"}
                                control={<Switch checked={settings.sortCountry} onChange={changeSettings}/>}
                                label={"Country"}
                                name={nameOfSetting("sortCountry")}
                            />
                        </Tooltip>
                        <Tooltip
                            key={"Coordinates"}
                            title={"A circle is placed on the map when multiple relays are located at the same coordinates. A circle's size is relative to the amount of relays located at it's center. Click a circle, to see details."}
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}
                        >
                            <FormControlLabel
                                key={"Coordinates"}
                                control={<Switch checked={settings.aggregateCoordinates} onChange={changeSettings}/>}
                                label={"Coordinates"}
                                name={nameOfSetting("aggregateCoordinates")}
                            />
                        </Tooltip>
                        <Tooltip
                            key={"Density heatmap"}
                            title={"This heatmap visualizes how many relays are present in an area relative to others."}
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}
                        >
                            <FormControlLabel
                                key={"Density heatmap"}
                                control={<Switch checked={settings.heatMap} onChange={changeSettings}/>}
                                label={"Density heatmap"}
                                name={nameOfSetting("heatMap")}
                            />
                        </Tooltip>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation} defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Show relay types</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <FormGroup>
                        {relayTypes.map(relayType =>
                            <Box key={relayType} sx={{display: "flex", alignItems: "center", justifyContent: "space-between"}}>
                                <Tooltip
                                    title={RelayTypeTooltip[relayType]}
                                    placement={"left"}
                                    enterDelay={tooltipTimeDelay}
                                >
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                id={relayType.toString()}
                                                checked={settings.showRelayTypes[relayType]}
                                                onChange={changeSettings}
                                            />
                                        }
                                        label={
                                            <Box sx={{display: "flex", alignItems: "center"}}>
                                                <span style={{paddingRight: "10px"}}>{RelayTypeLabel[relayType]}</span>
                                                <RelayTypeIcon relayType={relayType}/>
                                            </Box>
                                        }
                                        name={showRelayTypesInput}
                                    />
                                </Tooltip>
                                <RelayTypeColorInput relayType={relayType}/>
                            </Box>
                        )}
                    </FormGroup>
                    {hasCustomRelayTypeColors &&
                        <Button
                            size={"small"}
                            sx={{mt: 1}}
                            onClick={() => setSettings({...settings, relayTypeColors: {...defaultSettings.relayTypeColors}})}
                        >
                            Reset colors
                        </Button>
                    }
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Relays must have flag</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <FormGroup>
                        {relayMustIncludeFlagOptions.map(option =>
                            <Tooltip
                                key={option.relayFlag}
                                title={option.tooltip}
                                placement={"left"}
                                enterDelay={tooltipTimeDelay}
                            >
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            id={option.relayFlag.toString()}
                                            checked={settings.relaysMustHaveFlag[option.relayFlag]}
                                            onChange={changeSettings}
                                        />
                                    }
                                    label={RelayFlagLabel[option.relayFlag]}
                                    name={relaysMustIncludeFlagInput}
                                />
                            </Tooltip>
                        )}
                    </FormGroup>
                    <Box sx={{margin: "8px 8px"}}>
                        <ExternalLink
                            href={"https://github.com/torproject/torspec/blob/main/dir-spec.txt"}
                            label={"Learn more"}
                        />
                    </Box>
                </AccordionDetails>
            </Accordion>
        </>
    )
}

export const showRelayTypesInput = "showRelayTypesInput"
export const relaysMustIncludeFlagInput = "relaysMustIncludeFlagInput"
