import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, Checkbox, FormControlLabel, FormGroup, Grid, Slider, Switch} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";

const historicStartYear = 2007
const historicStartMonth = 10

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [button, setButton] = useState(false)
    const [state, setState] = useState({
        checkBox: true,
        checkBox2: true,
        checkBox3: true,
    })
    const [sliderValue, setSliderValue] = useState<[number, number]>([0, monthsSinceBeginning()])

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
        return (currentYear - historicStartYear) * 12 - 9 + currentMonth
    }

    const mapSliderValueToDate = (value: number, isStartValue: boolean) => {
        value += historicStartMonth
        const month = (value % 12)
        const relativeYear = (value - month) / 12
        return isStartValue ?
            new Date(historicStartYear + relativeYear, month - 1, 1) :
            new Date(historicStartYear + relativeYear, month, 0)
    }

    const marks = (count: number) => {
        count -= 1
        let marks = []
        for (let i = 0; i <= count; i++) {
            const mark = {
                value: i * monthsSinceBeginning() / count,
                label: <Moment
                    date={mapSliderValueToDate(i * monthsSinceBeginning() / count, true)}
                    format={"MM/YYYY"}
                />
            }
            marks.push(mark);
        }
        return marks
    }

    return (
        <div>
            <WorldMap dateRangeToDisplay={{
                startDate: mapSliderValueToDate(sliderValue[0], true),
                endDate: mapSliderValueToDate(sliderValue[1], false)
            }}/>
            <div className={"sliderContainer"}>
                <Grid container spacing={2}>
                    <Grid item xs>
                        <Slider
                            className={"slider"}
                            value={sliderValue}
                            onChange={handleSliderChange}
                            valueLabelDisplay={"on"}
                            aria-labelledby={"range-slider"}
                            name={"slider"}
                            min={0}
                            max={monthsSinceBeginning()}
                            marks={marks(6)}
                            valueLabelFormat={(x) => <Moment date={mapSliderValueToDate(x, true)} format={"MM/YY"}/>}
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
                        control={<Checkbox checked={state.checkBox} onChange={handleChange} name={"checkBox"}/>}
                        label={"Guard"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox2} onChange={handleChange} name={"checkBox2"}/>}
                        label={"Exit"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox3} onChange={handleChange} name={"checkBox3"}/>}
                        label={"Fast"}/>
                </FormGroup>
            </ReactSlidingPane>
        </div>
    )
}

export default App;
