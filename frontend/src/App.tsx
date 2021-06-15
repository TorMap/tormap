import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, Checkbox, FormControlLabel, FormGroup, Slider, Switch, Typography} from "@material-ui/core";
import "@material-ui/styles"
import "./index.scss";

const startYear = 2007

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [button, setButton] = useState(false)
    const [state, setState] = useState({
        checkBox: true,
        checkBox2: false,
    })
    const [sliderValue, setSliderValue] = useState<[number, number]>([1, monthsSinceBeginning()])

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setState({...state, [event.target.name]: event.target.checked})
    };

    const handleSliderChange = (event: any, newValue: number | number[]) => {
        setSliderValue(newValue as [number, number])
    };

    function monthsSinceBeginning() {
        const today = new Date()
        const currentYear = today.getFullYear()
        const currentMonth = today.getMonth()
        return (currentYear - startYear) * 12 - 10 + currentMonth
    }

    const mapSliderValueToDate = (value: number, isStartValue: boolean) => {
        value += 10
        const month = value % 12
        const relativeYear = (value - month) / 12
        return isStartValue ?
            new Date(startYear + relativeYear, month + 1, 1)  :
            new Date(startYear + relativeYear, month + 2, 0)
    }

    const formatSliderValue = (value: number, isStartValue: boolean) => {
        const date = mapSliderValueToDate(value, isStartValue)
        return [date.getFullYear(), date.getMonth(), date.getDate()].join("-")
    }

    return (
        <div>
            <WorldMap dateRangeToDisplay={{
                startDate: mapSliderValueToDate(sliderValue[0], true),
                endDate: mapSliderValueToDate(sliderValue[1], false)
            }}/>
            <Button className={"optionPaneButton"} onClick={() => setShowOptionPane(!showOptionPane)}>toggle
                overlay</Button>
            <ReactSlidingPane width={"100%"} isOpen={showOptionPane} onRequestClose={() => setShowOptionPane(false)}
                              from={"bottom"} title={"Optionen"}>
                <FormGroup>
                    <FormControlLabel control={<Switch checked={button} onChange={() => setButton(!button)}/>}
                                      label={"Test Button"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox} onChange={handleChange} name={"checkBox"}/>}
                        label={"test Checkbox"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox2} onChange={handleChange} name={"checkBox2"}/>}
                        label={"test Checkbox2"}/>
                    <FormControlLabel control={
                        <Slider
                            value={sliderValue}
                            onChange={handleSliderChange}
                            valueLabelDisplay={"auto"}
                            aria-labelledby={"range-slider"}
                            name={"slider"}
                            min={1}
                            max={monthsSinceBeginning()}
                        />
                    } label={"test Slider"} className={"slider"}/>
                    <Typography>
                        {"From " + formatSliderValue(sliderValue[0], true) + " until " + formatSliderValue(sliderValue[1], false)}
                    </Typography>
                </FormGroup>
            </ReactSlidingPane>

        </div>
    );
}

export default App;
