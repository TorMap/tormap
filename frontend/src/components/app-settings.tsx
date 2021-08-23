import React, {FunctionComponent,} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Checkbox,
    FormControlLabel,
    FormGroup,
    makeStyles,
    Switch, Tooltip,
    Typography,
} from "@material-ui/core";
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import {Settings} from "../types/variousTypes";
import {RelayFlagName} from "../types/relay";

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

//todo: doc
export const AppSettings: FunctionComponent<Props> = ({settings, onChange}) => {
    const classes = useStyle()

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
                    <Tooltip title={"Heat-map on relay density"} placement={"left"} enterDelay={700}>
                        <FormGroup>
                            <FormControlLabel control={<Switch checked={settings.heatMap}
                                                               onChange={onChange}/>}
                                              label={"Density of Relays"}
                                              name={"heatMap"}
                            />
                        </FormGroup>
                    </Tooltip>
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
                        <Tooltip title={"Group relays by country"} placement={"left"}>
                            <FormControlLabel control={<Switch checked={settings.sortCountry}
                                                               onChange={onChange}/>}
                                              label={"Group by country"}
                                              name={"sortCountry"}
                            />
                        </Tooltip>
                        <Tooltip title={"Group relays by family"} placement={"left"}>
                            <FormControlLabel control={<Switch checked={settings.sortFamily}
                                                               onChange={onChange}/>}
                                              label={"Group by family"}
                                              name={"sortFamily"}
                            />
                        </Tooltip>
                        <Tooltip title={"Group relays by coordinate"} placement={"left"}>
                            <FormControlLabel control={<Switch checked={settings.aggregateCoordinates}
                                                               onChange={onChange}/>}
                                              label={"Group by coordinates"}
                                              name={"aggregateCoordinates"}
                            />
                        </Tooltip>
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
                        <FormControlLabel
                            control={<Checkbox checked={settings.miValid} onChange={onChange}
                                               name={"mi" + RelayFlagName.Valid}/>}
                            label={RelayFlagName.Valid}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miNamed} onChange={onChange}
                                               name={"mi" + RelayFlagName.Named}/>}
                            label={RelayFlagName.Named}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miUnnamed} onChange={onChange}
                                               name={"mi" + RelayFlagName.Unnamed}/>}
                            label={RelayFlagName.Unnamed}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miRunning} onChange={onChange}
                                               name={"mi" + RelayFlagName.Running}/>}
                            label={RelayFlagName.Running}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miStable} onChange={onChange}
                                               name={"mi" + RelayFlagName.Stable}/>}
                            label={RelayFlagName.Stable}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miExit} onChange={onChange}
                                               name={"mi" + RelayFlagName.Exit}/>}
                            label={RelayFlagName.Exit}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miFast} onChange={onChange}
                                               name={"mi" + RelayFlagName.Fast}/>}
                            label={RelayFlagName.Fast}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miGuard} onChange={onChange}
                                               name={"mi" + RelayFlagName.Guard}/>}
                            label={RelayFlagName.Guard}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miAuthority} onChange={onChange}
                                               name={"mi" + RelayFlagName.Authority}/>}
                            label={RelayFlagName.Authority}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miV2Dir} onChange={onChange}
                                               name={"mi" + RelayFlagName.V2Dir}/>}
                            label={RelayFlagName.V2Dir}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miHSDir} onChange={onChange}
                                               name={"mi" + RelayFlagName.HSDir}/>}
                            label={RelayFlagName.HSDir}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miNoEdConsensus} onChange={onChange}
                                               name={"mi" + RelayFlagName.NoEdConsensus}/>}
                            label={RelayFlagName.NoEdConsensus}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miStaleDesc} onChange={onChange}
                                               name={"mi" + RelayFlagName.StaleDesc}/>}
                            label={RelayFlagName.StaleDesc}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miSybil} onChange={onChange}
                                               name={"mi" + RelayFlagName.Sybil}/>}
                            label={RelayFlagName.Sybil}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miBadExit} onChange={onChange}
                                               name={"mi" + RelayFlagName.BadExit}/>}
                            label={RelayFlagName.BadExit}/>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
        </div>
    )
}
