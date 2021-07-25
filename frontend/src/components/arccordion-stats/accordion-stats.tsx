import React, {FunctionComponent,} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Checkbox,
    FormControlLabel,
    FormGroup,
    Switch,
    Typography
} from "@material-ui/core";
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import "./accordion-stats.scss"
import {Settings} from "../../types/variousTypes";
import {RelayFlagName} from "../../types/relay";

interface Props {
    settings: Settings

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
                        <FormControlLabel control={<Switch checked={settings.showMarker}
                                                           onChange={onChange}
                        />}
                                          label={"Draw marker"}
                                          name={"showMarker"}
                        />
                        <FormControlLabel control={<Switch checked={settings.colorNodesAccordingToType}
                                                           onChange={onChange}
                                                    />}
                                          label={"Color relays according to Type"}
                                          name={"colorNodesAccordingToType"}
                        />
                        <FormControlLabel control={<Switch checked={settings.aggregateCoordinates}
                                                           onChange={onChange}
                                                    />}
                                          label={"Aggregate Relays that have the same coordinates"}
                                          name={"aggregateCoordinates"}
                        />
                        <FormControlLabel control={<Switch checked={settings.heatMap}
                                                           onChange={onChange}
                        />}
                                          label={"Aggregate Relays to density heatmap"}
                                          name={"heatMap"}
                        />
                        <FormControlLabel control={<Switch checked={settings.sortContry}
                                                           onChange={onChange}
                        />}
                                          label={"Sort relays according to country"}
                                          name={"sortContry"}
                        />
                        <FormControlLabel control={<Switch checked={settings.sortFamily}
                                                           onChange={onChange}
                        />}
                                          label={"Sort relays according to family"}
                                          name={"sortFamily"}
                        />
                        <FormControlLabel control={<Switch checked={settings.daterange}
                                                           onChange={onChange}
                        />}
                                          label={"Enable date range selection on slider"}
                                          name={"daterange"}
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
                    <Typography className={"heading"}>Show relay types</Typography>
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
                    <Typography className={"heading"}>Relay must include flag</Typography>
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
                            control={<Checkbox checked={settings.miUnnamed} onChange={onChange} name={"mi"+RelayFlagName.Unnamed}/>}
                            label={RelayFlagName.Unnamed}/>
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
