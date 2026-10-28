import {ExpandMore as ExpandMoreIcon} from "@mui/icons-material";
import {Accordion, AccordionDetails, AccordionSummary, Button, FormControlLabel, Switch, Typography} from "@mui/material";
import {FunctionComponent, useState} from "react";

import {requestDataCache, useDataCacheState} from "../../pwa/data-cache";

interface Props {
    elevation: number
}

/**
 * Controls the relay data that the service worker keeps for offline use. Only shown while a service worker controls
 * the page, so browsers without one (e.g. Tor Browser) do not see it.
 */
export const OfflineDataSettings: FunctionComponent<Props> = ({elevation}) => {
    const [expanded, setExpanded] = useState(false)
    // Reloaded when the section opens, as viewing days changes what is saved
    const [state, setState] = useDataCacheState(expanded)
    const [busy, setBusy] = useState(false)
    if (!state) return null

    const update = (request: Parameters<typeof requestDataCache>[0]) => {
        setBusy(true)
        void requestDataCache(request).then(answer => {
            setState(answer)
            setBusy(false)
        })
    }
    const savedDays = state.dates.length

    return (
        <Accordion elevation={elevation} expanded={expanded} onChange={(_event, isExpanded) => setExpanded(isExpanded)}>
            <AccordionSummary
                expandIcon={<ExpandMoreIcon/>}
                aria-controls="offline-data-content"
                id="offline-data-header"
            >
                <Typography className={"heading"}>Offline data</Typography>
            </AccordionSummary>
            <AccordionDetails sx={{padding: "0px 16px 20px 16px", "& .MuiFormControlLabel-root": {marginLeft: "-12px"}}}>
                <FormControlLabel
                    control={<Switch
                        checked={state.enabled}
                        disabled={busy}
                        onChange={event => update({type: "SET_DATA_CACHING", enabled: event.target.checked})}
                    />}
                    label={"Keep viewed days"}
                />
                <Typography variant="body1" sx={{my: 1}}>
                    The last 20 days you viewed stay in this browser for up to 60 days, so they open without network
                    access. Turning this off deletes them.
                </Typography>
                {state.enabled &&
                    <Button
                        size={"small"}
                        sx={{ml: "-5px"}}
                        disabled={busy || savedDays === 0}
                        onClick={() => update({type: "CLEAR_DATA_CACHE"})}
                    >
                        {savedDays === 0 ? "No days saved" : `Delete ${savedDays} saved ${savedDays === 1 ? "day" : "days"}`}
                    </Button>
                }
            </AccordionDetails>
        </Accordion>
    )
}
