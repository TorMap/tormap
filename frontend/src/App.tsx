import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, Checkbox, FormControlLabel, FormGroup, Grid, Slider, Switch} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";
import {apiBaseUrl} from "./util/constants";

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [colorNodeFlags, setColorNodeFlags] = useState(true)
    const [state, setState] = useState({
        guard: true,
        exit: true,
        fast: true,
    })
    const [availableMonths, setAvailableMonths] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(0)


    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setState({...state, [event.target.name]: event.target.checked})
    };

    const marks = (count: number) => {
        count--
        let marks = []
        for (let i = 0; i <= count; i++) {
            const mark = {
                value: Math.round(i * (availableMonths.length - 1) / count),
                label: <Moment
                    date={availableMonths[Math.round(i * (availableMonths.length - 1) / count)]}
                    format={"MM/YYYY"}
                />
            }
            marks.push(mark);
        }
        return marks
    }

    useEffect(() => {
        console.log("Fetching available months")
        fetch(`${apiBaseUrl}/archive/geo/relay/months`)
            .then(response => response.json())
            .then(availableMonths => {
                setAvailableMonths(availableMonths)
                setSliderValue(availableMonths.length - 1)
            })
            .catch(console.log)
    }, [])


    return (
        <div>
            <WorldMap
                monthToDisplay={sliderValue ? availableMonths[sliderValue] : undefined}
                colorFlags={colorNodeFlags}
            />
            <div className={"sliderContainer"}>
                <Grid container spacing={2}>
                    <Grid item xs>
                        <Slider
                            className={"slider"}
                            value={sliderValue}
                            onChange={(event: any, newValue: number | number[]) => null}
                            onChangeCommitted={(event: any, newValue: number | number[]) => setSliderValue(newValue as number)}
                            valueLabelDisplay={"on"}
                            aria-labelledby={"discrete-slider-always"}
                            name={"slider"}
                            min={0}
                            max={availableMonths.length - 1}
                            marks={marks(6)}
                            valueLabelFormat={(x) => <Moment date={availableMonths[x]} format={"MM/YY"}/>}
                        />
                    </Grid>
                </Grid>
            </div>
            <Button className={"optionPaneButton"} onClick={() => setShowOptionPane(!showOptionPane)}>toggle
                overlay</Button>
            <ReactSlidingPane width={"100%"} isOpen={showOptionPane} onRequestClose={() => setShowOptionPane(false)}
                              from={"bottom"} title={"Optionen"} className={"optionPane"}>
                <FormGroup>
                    <FormControlLabel control={<Switch checked={colorNodeFlags} onChange={() => setColorNodeFlags(!colorNodeFlags)}/>}
                                      label={"Color nodes according to Flags"}/>
                    <p>Filter by relay flags</p>
                    <FormControlLabel
                        control={<Checkbox checked={state.guard} onChange={handleInputChange} name={"guard"}/>}
                        label={"Guard"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.exit} onChange={handleInputChange} name={"exit"}/>}
                        label={"Exit"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.fast} onChange={handleInputChange} name={"fast"}/>}
                        label={"Fast"}/>
                </FormGroup>
            </ReactSlidingPane>
        </div>
    )
}

export default App;
