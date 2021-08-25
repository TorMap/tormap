import React, {FunctionComponent,} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary, Button,
    Checkbox,
    FormControlLabel,
    FormGroup, Link,
    makeStyles,
    Switch, Tooltip,
    Typography,
} from "@material-ui/core";
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import {Settings} from "../types/variousTypes";
import {RelayFlagName} from "../types/relay";
import {tooltipTimeDelay} from "../util/Config";

/**
 * Styles according to Material UI doc for components used in AppSettings component
 */
const useStyle = makeStyles(() => ({
    accordion: {
        position: "absolute",
        right: "1%",
        top: "15px",
        paddingBottom: "10px",
        maxWidth: "20%",
    }
}))

interface Props {

    /**
     * The currently applied app settings
     */
    settings: Settings

    /**
     * A callback to handle the change of setting elements
     * @param event the event of a controlling component (E.g. switches, checkboxes...)
     */
    onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

//todo:
export const AppSettings: FunctionComponent<Props> = ({settings, onChange}) => {
    const classes = useStyle()

    //Tooltips according to https://github.com/torproject/torspec/blob/main/dir-spec.txt
    return (
        <div className={classes.accordion}>
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel1a-content"
                    id="panel1a-header"
                >
                    <Typography>Heat-Map</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel control={<Switch checked={settings.heatMap}
                                                           onChange={onChange}/>}
                                          label={"Density of Relays"}
                                          name={"heatMap"}
                        />
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Grouping</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel control={<Switch checked={settings.sortCountry}
                                                           onChange={onChange}/>}
                                          label={"Group by country"}
                                          name={"sortCountry"}
                        />
                        <FormControlLabel control={<Switch checked={settings.sortFamily}
                                                           onChange={onChange}/>}
                                          label={"Group by family"}
                                          name={"sortFamily"}
                        />
                        {//todo: family selection buttons
                            /*(settings.sortFamily ? (
                        <FormControlLabel control={<Button onClick={}/>} label={"select Family"}/>
                        <FormControlLabel control={<Button onClick={}/>} label={"about Family"}/>
                        ) : null)*/}
                        <FormControlLabel control={<Switch checked={settings.aggregateCoordinates}
                                                           onChange={onChange}/>}
                                          label={"Group by coordinates"}
                                          name={"aggregateCoordinates"}
                        />
                    </FormGroup>
                </AccordionDetails>
            </Accordion>

            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Show relay types</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel
                            control={<Checkbox checked={settings.Guard} onChange={onChange}
                                               name={RelayFlagName.Guard}/>}
                            label={RelayFlagName.Guard}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.Exit} onChange={onChange} name={RelayFlagName.Exit}/>}
                            label={RelayFlagName.Exit}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.Default} onChange={onChange} name={"Default"}/>}
                            label={"Other"}/>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon/>}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Relays must include flag</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <Tooltip
                            title={
                                "A router is 'Valid' if it is running a version of Tor not known to be broken, and the directory authority has not blacklisted it as suspicious."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miValid} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Valid}/>}
                                label={RelayFlagName.Valid}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                ""
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miNamed} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Named}/>}
                                label={RelayFlagName.Named}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "Directory authorities no longer assign these flags.\n" +
                                "They were once used to determine whether a relay's nickname was canonically linked to its public key."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miUnnamed} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Unnamed}/>}
                                label={RelayFlagName.Unnamed}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router is 'Running' if the authority managed to connect to it successfully within the last 45 minutes on all its published ORPorts."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miRunning} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Running}/>}
                                label={RelayFlagName.Running}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router is Stable if it is active, and either its Weighted MTBF is at least the median for known active routers or its Weighted MTBF corresponds to at least 7 days. Routers are never called Stable if they are running a version of Tor known to drop circuits stupidly."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miStable} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Stable}/>}
                                label={RelayFlagName.Stable}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router is called an 'Exit' iff it allows exits to atleast one /8 address space on each of ports 80 and 443. (Up untilTor version 0.3.2, the flag was assigned if relays exit to at leasttwo of the ports 80, 443, and 6667.)"
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miExit} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Exit}/>}
                                label={RelayFlagName.Exit}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router is 'Fast' if it is active, and its bandwidth is either in the top 7/8ths for known active routers or at least 100KB/s."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miFast} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Fast}/>}
                                label={RelayFlagName.Fast}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A Guard router is a possible entry point to the network"
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miGuard} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Guard}/>}
                                label={RelayFlagName.Guard}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router is called an 'Authority' if the authority generating the network-status document believes it is an authority."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miAuthority} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Authority}/>}
                                label={RelayFlagName.Authority}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router supports the v2 directory protocol."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miV2Dir} onChange={onChange}
                                                   name={"mi" + RelayFlagName.V2Dir}/>}
                                label={RelayFlagName.V2Dir}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "A router is a v2 hidden service directory"
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miHSDir} onChange={onChange}
                                                   name={"mi" + RelayFlagName.HSDir}/>}
                                label={RelayFlagName.HSDir}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "authorities should not vote on this flag; it is produced as part of the consensus for consensus method 22 or later."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miNoEdConsensus} onChange={onChange}
                                                   name={"mi" + RelayFlagName.NoEdConsensus}/>}
                                label={RelayFlagName.NoEdConsensus}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "authorities should vote to assign this flag if the published time on the descriptor is over 18 hours in the past.  (This flag was added in 0.4.0.1-alpha.)"
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miStaleDesc} onChange={onChange}
                                                   name={"mi" + RelayFlagName.StaleDesc}/>}
                                label={RelayFlagName.StaleDesc}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "authorities SHOULD NOT accept more than 2 relays on a single IP. If this happens, the authority *should* vote for the excess relays, but should omit the Running or Valid flags and instead should assign the flag."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miSybil} onChange={onChange}
                                                   name={"mi" + RelayFlagName.Sybil}/>}
                                label={RelayFlagName.Sybil}/>
                        </Tooltip>
                        <Tooltip
                            title={
                                "Any router that is determined to get the BadExit flag doesn't count when computing bandwidth weights."
                            }
                            placement={"left"}
                            enterDelay={tooltipTimeDelay}>
                            <FormControlLabel
                                control={<Checkbox checked={settings.miBadExit} onChange={onChange}
                                                   name={"mi" + RelayFlagName.BadExit}/>}
                                label={RelayFlagName.BadExit}/>
                        </Tooltip>
                        <Link href={"https://github.com/torproject/torspec/blob/main/dir-spec.txt"} target={"_blanc"}>
                            more information on flags and their meaning
                        </Link>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
        </div>
    )
}
