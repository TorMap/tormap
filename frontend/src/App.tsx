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
    const [button, setButton] = useState(false)
    const [state, setState] = useState({
        checkBox: true,
        checkBox2: true,
        checkBox3: true,
    })
    const [availableMonths, setAvailableMonths] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(0)


    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setState({...state, [event.target.name]: event.target.checked})
    };

    const marks = (count: number) => {
        let marks = []
        for (let i = 0; i < count; i++) {
            const mark = {
                value: i * availableMonths.length / count,
                label: <Moment
                    date={availableMonths[i * availableMonths.length / count]}
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
            />
            <div className={"sliderContainer"}>
                <Grid container spacing={2}>
                    <Grid item xs>
                        <Slider
                            className={"slider"}
                            value={sliderValue}
                            onChange={(event: any, newValue: number | number[]) => setSliderValue(newValue as number)}
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
                    <FormControlLabel control={<Switch checked={button} onChange={() => setButton(!button)}/>}
                                      label={"Test Button"}/>
                    <p>Filter by relay flags</p>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox} onChange={handleInputChange} name={"checkBox"}/>}
                        label={"Guard"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox2} onChange={handleInputChange} name={"checkBox2"}/>}
                        label={"Exit"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox3} onChange={handleInputChange} name={"checkBox3"}/>}
                        label={"Fast"}/>
                </FormGroup>
            </ReactSlidingPane>
        </div>
    )
}

export default App;
