import {Close as CloseIcon, ExpandMore as ExpandMoreIcon, RestartAlt as RestartAltIcon, Search as SearchIcon} from "@mui/icons-material";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box, Button,
    Checkbox,
    Chip,
    FormControlLabel,
    FormGroup,
    MenuItem,
    Switch,
    TextField,
    Tooltip,
    Typography,
} from "@mui/material";
import {FunctionComponent, ReactNode} from "react";

import {defaultSettings, tooltipTimeDelay} from "../../config";
import {useSettings} from "../../context/settings-context";
import {RelayFlagLabel, RelayType, RelayTypeLabel, RelayTypeTooltip} from "../../types/relay";
import {
    relayMustIncludeFlagOptions,
    relaysMustIncludeFlagInput,
    Settings,
    showRelayTypesInput,
    topRelayCountOptions
} from "../../types/settings";
import {usageColorGradient} from "../../util/country-usage";
import {nameOfFactory} from "../../util/util";
import {ExternalLink} from "../link/ExternalLink";
import {useAtom} from "jotai";
import {
    relayDetailsDialogSearchAtom,
    relaysForDetailsDialogAtom,
    relaySearchOnMapAtom,
    showRelayDetailsDialogAtom
} from "../dialogs/relay/relay-details-atoms";
import {countryUsageAtom} from "../leaflet/country-usage-atoms";
import {filteredRelaysAtom, shownRelayCountAtom} from "../leaflet/LeafletLayers";
import {RelayTypeColorInput} from "./RelayTypeColorInput";

interface Props {
    elevation: number
}

/**
 * Aligns the content of every section with its heading: text, switches, checkboxes and color swatches share the
 * same left and right inset. The negative margins cancel the padding around a switch or checkbox.
 */
const detailsSx = {
    padding: "0px 16px 20px 16px",
    "& .MuiFormControlLabel-root": {marginLeft: "-12px"},
    "& .MuiFormControlLabel-root:has(.MuiCheckbox-root)": {marginLeft: "-9px"},
}

/**
 * Tells that the map does not show all relays. Removing the chip shows all of them again.
 */
const RelayLimitInfo: FunctionComponent<{ onShowAll: () => void, children: ReactNode }> = ({onShowAll, children}) => (
    <Box sx={{mb: "16px"}}>
        <Chip
            // A solid surface like the accordions below, so the text stays readable on every map color
            variant="filled"
            label={children}
            onDelete={onShowAll}
            deleteIcon={<CloseIcon titleAccess="Show all relays"/>}
            // The panel has a constant width, so a long text wraps instead of growing the chip
            sx={{
                height: "auto",
                maxWidth: "100%",
                backgroundColor: "grey.800",
                color: "text.primary",
                border: "1px solid",
                borderColor: "info.light",
                boxShadow: 3,
                "&:hover": {backgroundColor: "grey.700"},
                "& .MuiChip-deleteIcon": {color: "text.primary", opacity: 0.8, "&:hover": {opacity: 1, color: "text.primary"}},
                "& .MuiChip-label": {whiteSpace: "normal", paddingTop: "6px", paddingBottom: "6px"},
            }}
        />
    </Box>
)

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
    const [countryUsage] = useAtom(countryUsageAtom)
    const [shownRelayCount] = useAtom(shownRelayCountAtom)

    // Util
    const nameOfSetting = nameOfFactory<Settings>()
    const relayTypes = [RelayType.Exit, RelayType.Guard, RelayType.Other]
    // Days that were not reprocessed yet have no weights, then the weight settings would do nothing
    const weightsAvailable = filteredRelays.some(relay => relay.consensusWeight != null)
    // The scale only explains the map while the layer is shown and the day has estimates
    const usageScaleActive = settings.showCountryUsage && countryUsage !== undefined && countryUsage.usage.length > 0
    const shownRelayTypes = relayTypes.filter(relayType => settings.showRelayTypes[relayType])
    const hiddenRelayTypes = relayTypes.filter(relayType => !settings.showRelayTypes[relayType])
    const weightLimitActive = weightsAvailable && settings.sizeByWeight && settings.topRelayCount !== undefined
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
                <RelayLimitInfo onShowAll={() => setRelaySearchOnMap(undefined)}>
                    Matching &quot;{relaySearchOnMap.search}&quot;: {shownRelayCount.shown}/{shownRelayCount.total} relays
                </RelayLimitInfo>}
            {weightLimitActive &&
                <RelayLimitInfo onShowAll={() => setSettings({...settings, topRelayCount: undefined})}>
                    Top consensus weight: {shownRelayCount.shown}/{shownRelayCount.total} relays
                </RelayLimitInfo>}
            {hiddenRelayTypes.length > 0 &&
                <RelayLimitInfo onShowAll={() => setSettings({
                    ...settings,
                    showRelayTypes: {...defaultSettings.showRelayTypes},
                })}>
                    {shownRelayTypes.length > 0 ? `Only ${shownRelayTypes.map(type => RelayTypeLabel[type]).join(", ")}` : "No relay types"}
                    : {shownRelayCount.shown}/{shownRelayCount.total} relays
                </RelayLimitInfo>}
            <Accordion elevation={elevation} defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="group-relays-content"
                    id="group-relays-header"
                >
                    <Typography className={"heading"}>Group relays by</Typography>
                </AccordionSummary>
                <AccordionDetails sx={detailsSx}>
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
                <AccordionDetails sx={detailsSx}>
                    <FormGroup>
                        <Tooltip
                            title={"Sizes the markers by the weight directory authorities assign to a relay. Tor clients use it to pick relays, so it shows how likely a relay is picked, not how much traffic it carried."}
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}
                            describeChild
                        >
                            <FormControlLabel
                                control={<Switch checked={settings.sizeByWeight} onChange={changeSettings} disabled={!weightsAvailable}/>}
                                label={"Size relay markers"}
                                name={nameOfSetting("sizeByWeight")}
                            />
                        </Tooltip>
                        {!weightsAvailable &&
                            <Typography variant="body1" sx={{my: 1}}>
                                There are no consensus weights for this day yet.
                            </Typography>
                        }
                        <TextField
                            select
                            size="small"
                            label="Show top"
                            value={settings.topRelayCount ?? ""}
                            // Keeps the label above the field for "All", so choosing a value does not change the layout
                            slotProps={{inputLabel: {shrink: true}}}
                            disabled={!weightsAvailable || !settings.sizeByWeight}
                            onChange={event => setSettings({
                                ...settings,
                                topRelayCount: event.target.value === "" ? undefined : Number(event.target.value),
                            })}
                            sx={{mt: 1}}
                        >
                            <MenuItem value="">All</MenuItem>
                            {topRelayCountOptions.map(count =>
                                <MenuItem key={count} value={count}>{count} relays</MenuItem>
                            )}
                        </TextField>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="country-usage-content"
                    id="country-usage-header"
                >
                    <Typography className={"heading"}>Tor users</Typography>
                </AccordionSummary>
                <AccordionDetails sx={detailsSx}>
                    <FormGroup>
                        <Tooltip
                            title={"An estimate by the Tor Project, based on the directory requests that relays report. It counts users that connect to Tor directly, not the traffic they cause."}
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}
                            describeChild
                        >
                            <FormControlLabel
                                control={<Switch checked={settings.showCountryUsage} onChange={changeSettings}/>}
                                label={"Color countries"}
                                name={nameOfSetting("showCountryUsage")}
                            />
                        </Tooltip>
                    </FormGroup>
                    <Box
                        aria-disabled={!usageScaleActive}
                        sx={{mt: 1, opacity: usageScaleActive ? 1 : 0.4, filter: usageScaleActive ? "none" : "grayscale(1)"}}
                    >
                        <Box
                            role="img"
                            aria-label="Color scale from few to many users"
                            sx={{height: "8px", borderRadius: "4px", background: usageColorGradient}}
                        />
                        <Box sx={{display: "flex", justifyContent: "space-between"}}>
                            <Typography variant="caption">few users</Typography>
                            <Typography variant="caption">many users</Typography>
                        </Box>
                    </Box>
                    {settings.showCountryUsage && countryUsage && countryUsage.usage.length > 0 &&
                        <Typography variant="body1" sx={{mt: 1}}>
                            Estimate based on {countryUsage.usage[0].frac}% of relays.
                            {countryUsage.usage[0].frac < 30 && " Early estimates are less reliable."}
                        </Typography>
                    }
                    {settings.showCountryUsage && countryUsage && countryUsage.usage.length === 0 &&
                        <Typography variant="body1" sx={{mt: 1}}>
                            {countryUsage.day} has no estimate
                        </Typography>
                    }
                    <Box sx={{mt: 1}}>
                        <ExternalLink
                            href={"https://metrics.torproject.org/reproducible-metrics.html#relay-users"}
                            label={"Learn more"}
                        />
                    </Box>
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation} defaultExpanded>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="relay-types-content"
                    id="relay-types-header"
                >
                    <Typography className={"heading"}>Relay types</Typography>
                </AccordionSummary>
                <AccordionDetails sx={detailsSx}>
                    <FormGroup>
                        {relayTypes.map(relayType =>
                            <Box key={relayType} sx={{display: "flex", alignItems: "center", justifyContent: "space-between"}}>
                                <Tooltip
                                    title={RelayTypeTooltip[relayType]}
                                    placement={"left"}
                                    enterDelay={tooltipTimeDelay}
                                    describeChild
                                >
                                    <FormControlLabel
                                        control={
                                            <Switch
                                                id={relayType.toString()}
                                                checked={settings.showRelayTypes[relayType]}
                                                onChange={changeSettings}
                                            />
                                        }
                                        label={RelayTypeLabel[relayType]}
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
                            sx={{mt: 1, ml: "-5px"}}
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
                <AccordionDetails sx={detailsSx}>
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
                    Reset all settings
                </Button>
            }
        </>
    )
}

