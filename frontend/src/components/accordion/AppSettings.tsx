import React, {FunctionComponent} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Box,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Link,
    Switch,
    Tooltip,
    Typography,
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import {Settings} from "../../types/app-state";
import {RelayFlag, RelayFlagLabel, RelayType, RelayTypeLabel} from "../../types/relay";
import {tooltipTimeDelay} from "../../util/config";
import {getIcon} from "../../types/icons";
import {nameOfFactory} from "../../util/util";
import {useSettings} from "../../util/settings-context";

interface Props {
    elevation: number
}

/**
 * A component for changing the app settings
 */
export const AppSettings: FunctionComponent<Props> = ({elevation = 24}) => {
    const nameOfSetting = nameOfFactory<Settings>()


    const settings = useSettings().settings
    const onChange = useSettings().changeSettings

    // Construct relay type options to display
    const showRelayTypes: RelayType[] = [RelayType.Exit, RelayType.Guard, RelayType.Other]

    // Construct relay flag options to display. Tooltips according to https://github.com/torproject/torspec/blob/main/dir-spec.txt
    const relayMustIncludeFlagOptions: RelayMustIncludeFlagOption[] = [{
        relayFlag: RelayFlag.Authority,
        tooltip: "A router is called an 'Authority' if the authority generating the network-status document believes it is an authority."
    }, {
        relayFlag: RelayFlag.BadExit,
        tooltip: "Any router that is determined to get the BadExit flag doesn't count into computing bandwidth weights."
    }, {
        relayFlag: RelayFlag.Exit,
        tooltip: "A router is called an 'Exit' if it allows exits on both ports 80 and 443. (Up untilTor version 0.3.2, the flag was assigned if relays exit to at least two of the ports 80, 443, and 6667)."
    }, {
        relayFlag: RelayFlag.Fast,
        tooltip: "A router is 'Fast' if it is active, and it's bandwidth is either in the top 7/8 ths for known active routers or at least 100KB/s fast."
    }, {
        relayFlag: RelayFlag.Guard,
        tooltip: "A Guard router is a possible entry point to the network."
    }, {
        relayFlag: RelayFlag.HSDir,
        tooltip: "A router is a v2 hidden service directory."
    }, {
        relayFlag: RelayFlag.Named,
        tooltip: "Directory authorities no longer assign these flags. They were once used to determine whether a relay's nickname was canonically linked to its public key."
    }, {
        relayFlag: RelayFlag.NoEdConsensus,
        tooltip: "Authorities should not vote on this flag. It is produced as part of the consensus for consensus method 22 or later."
    }, {
        relayFlag: RelayFlag.Running,
        tooltip: "A router is 'Running' if the authority managed to connect to it successfully within the last 45 minutes on all its published ORPorts."
    }, {
        relayFlag: RelayFlag.Stable,
        tooltip: "A router is Stable if it is active, and either it's Weighted MTBF is at least the median for known active routers or it's weighted MTBF corresponds to at least 7 days. Routers are never called Stable if they are running a version of Tor known to drop circuits stupidly."
    }, {
        relayFlag: RelayFlag.StaleDesc,
        tooltip: "Authorities should vote to assign this flag if the published time on the descriptor is over 18 hours in the past. (This flag was added in 0.4.0.1-alpha.)"
    }, {
        relayFlag: RelayFlag.Sybil,
        tooltip: "Authorities SHOULD NOT accept more than 2 relays on a single IP. If this happens, the authorities *should* vote for the excess relays, but should omit the Running or Valid flags and instead should assign the flag Sybil."
    }, {
        relayFlag: RelayFlag.Unnamed,
        tooltip: "Directory authorities no longer assign these flags. They were once used to determine whether a relay's nickname was canonically linked to its public key."
    }, {
        relayFlag: RelayFlag.Valid,
        tooltip: "A router is 'Valid' if it is running a version of Tor not known to be broken, and the directory authorities have not blacklisted it as suspicious."
    }, {
        relayFlag: RelayFlag.V2Dir,
        tooltip: "A router supports the v2 directory protocol."
    }]

    return (
        <Box>
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
                            control={<Switch checked={settings.sortFamily} onChange={onChange}/>}
                            label={"Family"}
                            name={nameOfSetting("sortFamily")}
                        />
                        <FormControlLabel
                            key={"Country"}
                            control={<Switch checked={settings.sortCountry} onChange={onChange}/>}
                            label={"Country"}
                            name={nameOfSetting("sortCountry")}
                        />
                        <FormControlLabel
                            key={"Coordinates"}
                            control={<Switch checked={settings.aggregateCoordinates} onChange={onChange}/>}
                            label={"Coordinates"}
                            name={nameOfSetting("aggregateCoordinates")}
                        />
                        <FormControlLabel
                            key={"Density heatmap"}
                            control={<Switch checked={settings.heatMap} onChange={onChange}/>}
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
                        {showRelayTypes.map(relayType =>
                            <FormControlLabel
                                key={relayType}
                                control={
                                    <Checkbox
                                        id={relayType.toString()}
                                        checked={settings.showRelayTypes[relayType]}
                                        onChange={onChange}
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
                                            onChange={onChange}
                                        />
                                    }
                                    label={RelayFlagLabel[option.relayFlag]}
                                    name={relaysMustIncludeFlagInput}
                                />
                            </Tooltip>
                        )}
                    </FormGroup>
                    <Box sx={{margin: "8px 8px"}}>
                        <Link href={"https://github.com/torproject/torspec/blob/main/dir-spec.txt"} target={"_blank"}>
                            More information about flags
                        </Link>
                    </Box>
                </AccordionDetails>
            </Accordion>
        </Box>
    )
}

export const showRelayTypesInput = "showRelayTypesInput"
export const relaysMustIncludeFlagInput = "relaysMustIncludeFlagInput"

interface RelayMustIncludeFlagOption {
    relayFlag: RelayFlag,
    tooltip: string,
}