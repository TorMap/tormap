import React, {FunctionComponent} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Switch,
    Tooltip,
    Typography,
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import {relayMustIncludeFlagOptions, Settings} from "../../types/settings";
import {RelayFlagLabel, RelayType, RelayTypeLabel} from "../../types/relay";
import {tooltipTimeDelay} from "../../config";
import {getIcon} from "../../types/icons";
import {nameOfFactory} from "../../util/util";
import {useSettings} from "../../context/settings-context";
import {ExternalLink} from "../link/ExternalLink";

interface Props {
    elevation: number
}

/**
 * A component for changing the app settings
 */
export const AppSettings: FunctionComponent<Props> = ({elevation = 24}) => {
    // App context
    const {settings, changeSettings} = useSettings()

    // Util
    const nameOfSetting = nameOfFactory<Settings>()

    return (
        <>
            <Accordion elevation={elevation}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Group relays by</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <FormGroup>
                        <FormControlLabel
                            key={"Family"}
                            control={<Switch checked={settings.sortFamily} onChange={changeSettings}/>}
                            label={"Family"}
                            name={nameOfSetting("sortFamily")}
                        />
                        <FormControlLabel
                            key={"Country"}
                            control={<Switch checked={settings.sortCountry} onChange={changeSettings}/>}
                            label={"Country"}
                            name={nameOfSetting("sortCountry")}
                        />
                        <FormControlLabel
                            key={"Coordinates"}
                            control={<Switch checked={settings.aggregateCoordinates} onChange={changeSettings}/>}
                            label={"Coordinates"}
                            name={nameOfSetting("aggregateCoordinates")}
                        />
                        <FormControlLabel
                            key={"Density heatmap"}
                            control={<Switch checked={settings.heatMap} onChange={changeSettings}/>}
                            label={"Density heatmap"}
                            name={nameOfSetting("heatMap")}
                        />
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Show relay types</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <FormGroup>
                        {[RelayType.Exit, RelayType.Guard, RelayType.Other].map(relayType =>
                            <FormControlLabel
                                key={relayType}
                                control={
                                    <Checkbox
                                        id={relayType.toString()}
                                        checked={settings.showRelayTypes[relayType]}
                                        onChange={changeSettings}
                                    />
                                }
                                label={
                                    <Box
                                        display="flex"
                                        alignItems="center"
                                    >
                                        <span style={{paddingRight: "10px"}}>{RelayTypeLabel[relayType]}</span>
                                        {getIcon(relayType)}
                                    </Box>
                                }
                                name={showRelayTypesInput}
                            />
                        )}
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion elevation={elevation}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Relays must include flag</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{padding: "0px 8px 20px 8px"}}>
                    <FormGroup>
                        {relayMustIncludeFlagOptions.map(option =>
                            <Tooltip
                                key={option.relayFlag}
                                title={option.tooltip}
                                placement={"left"}
                                enterDelay={tooltipTimeDelay}>
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            id={option.relayFlag.toString()}
                                            checked={settings.relaysMustIncludeFlag[option.relayFlag]}
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
                            label={"More information about flags"}
                        />
                    </Box>
                </AccordionDetails>
            </Accordion>
        </>
    )
}

export const showRelayTypesInput = "showRelayTypesInput"
export const relaysMustIncludeFlagInput = "relaysMustIncludeFlagInput"
