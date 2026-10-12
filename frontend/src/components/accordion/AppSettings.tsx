import {ExpandMore as ExpandMoreIcon, RestartAlt as RestartAltIcon, Search as SearchIcon} from "@mui/icons-material";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box, Button,
    Checkbox,
    FormControlLabel,
    FormGroup,
    MenuItem,
    Switch,
    TextField,
    Tooltip,
    Typography,
} from "@mui/material";
import {FunctionComponent} from "react";

import {defaultSettings, tooltipTimeDelay} from "../../config";
import {useSettings} from "../../context/settings-context";
import {RelayTypeIcon} from "../../types/icons";
import {RelayFlagLabel, RelayType, RelayTypeLabel, RelayTypeTooltip} from "../../types/relay";
import {
    relayMustIncludeFlagOptions,
    relaysMustIncludeFlagInput,
    Settings,
    showRelayTypesInput,
    topRelayCountOptions
} from "../../types/settings";
import {nameOfFactory} from "../../util/util";
import {ExternalLink} from "../link/ExternalLink";
import {useAtom} from "jotai";
import {
    relayDetailsDialogSearchAtom,
    relaysForDetailsDialogAtom,
    relaySearchOnMapAtom,
    showRelayDetailsDialogAtom
} from "../dialogs/relay/relay-details-atoms";
import {filteredRelaysAtom} from "../leaflet/LeafletLayers";
import {RelayTypeColorInput} from "./RelayTypeColorInput";

interface Props {
    elevation: number
}

/**
 * A component for changing the app settings
 */
export const AppSettings: FunctionComponent<Props> = ({elevation = 24}) => {
    // App context
    const {settings, changeSettings, setSettings, resetSettings, isDefaultSettings} = useSettings()

    // Atom state
    const [, setShowRelayDetailsDialog] = useAtom(showRelayDetailsDialogAtom)
    const [, setRelaysForDetailsDialog] = useAtom(relaysForDetailsDialogAtom)
    const [, setRelayDetailsDialogSearch] = useAtom(relayDetailsDialogSearchAtom)
    const [filteredRelays] = useAtom(filteredRelaysAtom)
    const [relaySearchOnMap, setRelaySearchOnMap] = useAtom(relaySearchOnMapAtom)

    // Util
    const nameOfSetting = nameOfFactory<Settings>()
    const relayTypes = [RelayType.Exit, RelayType.Guard, RelayType.Other]
    // Days that were not reprocessed yet have no weights, then the weight settings would do nothing
    const weightsAvailable = filteredRelays.some(relay => relay.consensusWeight != null)
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
            {relaySearchOnMap &&
                <Alert
                    severity="info"
                    sx={{mb: "16px"}}
                    action={
                        <Button color="inherit" size="small" onClick={() => setRelaySearchOnMap(undefined)}>
                            Show all
                        </Button>
                    }
                >
                    Map only shows relays matching &quot;{relaySearchOnMap.search}&quot;
                </Alert>}
            <Accordion elevation={elevation} defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="group-relays-content"
                    id="group-relays-header"
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
            <Accordion elevation={elevation}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="consensus-weight-content"
                    id="consensus-weight-header"
                >
                    <Typography className={"heading"}>Consensus weight</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <Typography variant="body2" sx={{mb: 1}}>
                        {weightsAvailable
                            ? "The weight directory authorities assign to a relay. Tor clients use it to choose relays, so it shows how likely a relay is picked, not how much traffic it carried."
                            : "There are no consensus weights for this day yet."}
                    </Typography>
                    <FormGroup>
                        <FormControlLabel
                            control={<Switch checked={settings.sizeByWeight} onChange={changeSettings} disabled={!weightsAvailable}/>}
                            label={"Size by weight"}
                            name={nameOfSetting("sizeByWeight")}
                        />
                        <TextField
                            select
                            size="small"
                            label="Show only the heaviest"
                            value={settings.topRelayCount ?? ""}
                            disabled={!weightsAvailable}
                            onChange={event => setSettings({
                                ...settings,
                                topRelayCount: event.target.value === "" ? undefined : Number(event.target.value),
                            })}
                            sx={{mt: 1}}
                        >
                            <MenuItem value="">All relays</MenuItem>
                            {topRelayCountOptions.map(count =>
                                <MenuItem key={count} value={count}>{count} relays</MenuItem>
                            )}
                        </TextField>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation} defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="relay-types-content"
                    id="relay-types-header"
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
                    aria-controls="relay-flags-content"
                    id="relay-flags-header"
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
            {(!isDefaultSettings || relaySearchOnMap) &&
                <Button
                    variant="outlined"
                    startIcon={<RestartAltIcon/>}
                    fullWidth={true}
                    sx={{mt: "16px"}}
                    onClick={() => {
                        resetSettings()
                        setRelaySearchOnMap(undefined)
                    }}
                >
                    Restore settings
                </Button>
            }
        </>
    )
}

