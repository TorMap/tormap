
import React, {FunctionComponent, } from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary, Checkbox,
    FormControlLabel,
    FormGroup, Switch,
    Typography
} from "@material-ui/core";
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import "./accordion-stats.scss"
import {Settings, TempSettings} from "../../types/variousTypes";
import ReactSlidingPane from "react-sliding-pane";
import {RelayFlagName} from "../../types/relay";

interface Props {
    settings: TempSettings

    onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

export const AccordionStats: FunctionComponent<Props> = ({settings, onChange}) => {
    return (
        <div className={"accordion-stats"}>
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel1a-content"
                    id="panel1a-header"
                >
                    <Typography className={"heading"}>Map Settings</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel control={<Switch checked={settings.colorNodesAccordingToType}
                                                           onChange={onChange}
                                                    />}
                                          label={"Color relays according to Type"}
                                          name={"colorNodesAccordingToFlags"}
                        />
                        <FormControlLabel control={<Switch checked={settings.agregateCoordinates}
                                                           onChange={onChange}
                        />}
                                          label={"Aggregate Relays that have the same coordinates"}
                                          name={"agregateCoordinates"}
                        />
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
            <Accordion>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Show node types</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel
                            control={<Checkbox checked={settings.Guard} onChange={onChange} name={RelayFlagName.Guard}/>}
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
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel2a-content"
                    id="panel2a-header"
                >
                    <Typography className={"heading"}>Node must include flag</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miValid} onChange={onChange} name={"mi"+RelayFlagName.Valid}/>}
                            label={RelayFlagName.Valid}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miNamed} onChange={onChange} name={"mi"+RelayFlagName.Named}/>}
                            label={RelayFlagName.Named}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miUnamed} onChange={onChange} name={"mi"+RelayFlagName.Unamed}/>}
                            label={RelayFlagName.Unamed}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miRunning} onChange={onChange} name={"mi"+RelayFlagName.Running}/>}
                            label={RelayFlagName.Running}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miStable} onChange={onChange} name={"mi"+RelayFlagName.Stable}/>}
                            label={RelayFlagName.Stable}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miExit} onChange={onChange} name={"mi"+RelayFlagName.Exit}/>}
                            label={RelayFlagName.Exit}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miFast} onChange={onChange} name={"mi"+RelayFlagName.Fast}/>}
                            label={RelayFlagName.Fast}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miGuard} onChange={onChange} name={"mi"+RelayFlagName.Guard}/>}
                            label={RelayFlagName.Guard}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miAuthority} onChange={onChange} name={"mi"+RelayFlagName.Authority}/>}
                            label={RelayFlagName.Authority}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miV2Dir} onChange={onChange} name={"mi"+RelayFlagName.V2Dir}/>}
                            label={RelayFlagName.V2Dir}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miHSDir} onChange={onChange} name={"mi"+RelayFlagName.HSDir}/>}
                            label={RelayFlagName.HSDir}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miNoEdConsensus} onChange={onChange} name={"mi"+RelayFlagName.NoEdConsensus}/>}
                            label={RelayFlagName.NoEdConsensus}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miStaleDesc} onChange={onChange} name={"mi"+RelayFlagName.StaleDesc}/>}
                            label={RelayFlagName.StaleDesc}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miSybil} onChange={onChange} name={"mi"+RelayFlagName.Sybil}/>}
                            label={RelayFlagName.Sybil}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.miBadExit} onChange={onChange} name={"mi"+RelayFlagName.BadExit}/>}
                            label={RelayFlagName.BadExit}/>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
        </div>
    )
}