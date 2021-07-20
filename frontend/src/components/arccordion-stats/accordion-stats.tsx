
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
import {Settings} from "../../types/variousTypes";
import ReactSlidingPane from "react-sliding-pane";

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
                        <FormControlLabel control={<Switch checked={settings.colorNodesAccordingToFlags}
                                                           onChange={onChange}
                                                           disabled={true}
                                                    />}
                                                  label={"Color nodes according to Flags"}
                                                  name={"colorNodesAccordingToFlags"}
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
                    <Typography className={"heading"}>Filter by relay flags</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <FormGroup>
                        <FormControlLabel
                            control={<Checkbox checked={settings.guard} onChange={onChange} name={"guard"}/>}
                            label={"Guard"}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.exit} onChange={onChange} name={"exit"}/>}
                            label={"Exit"}/>
                        <FormControlLabel
                            control={<Checkbox checked={settings.default} onChange={onChange} name={"default"}/>}
                            label={"default"}/>
                    </FormGroup>
                </AccordionDetails>
            </Accordion>
        </div>
    )
}