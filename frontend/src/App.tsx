import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, FormControlLabel, Switch, FormGroup, Checkbox, Slider, Typography, Grid} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";

const historicStartYear = 2007
const historicStartMonth = 10

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [button, setButton] = useState(false)
    const [state, setState] = useState({
        checkBox: true,
        checkBox2: false,
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
            new Date(historicStartYear + relativeYear, month-1, 1)  :
            new Date(historicStartYear + relativeYear, month, 0)
    }

    const formatSliderValue = (value: number, isStartValue: boolean) => {
        return mapSliderValueToDate(value, isStartValue).toLocaleDateString("en-CA")
    }
    const formatSliderValueMonths = (value: number, isStartValue: boolean) => {
        const date = mapSliderValueToDate(value, isStartValue).toLocaleDateString("en-CA")
        return date.replace(/(-\d\d)$/,"")
    }

    const marks = (anzahl: number) => {
        anzahl -= 1
        let marks=[]
        for (let i = 0; i <= anzahl; i++){
            const mark = {
                value: i * monthsSinceBeginning()/ anzahl,
                label: formatSliderValueMonths(i * monthsSinceBeginning()/ anzahl, true)
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
                <Grid container spacing={2} >
                    <Grid item >
                        <Typography>
                            {formatSliderValue(sliderValue[0], true)}
                        </Typography>
                    </Grid>
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
                        />
                    </Grid>
                    <Grid item>
                        <Typography>
                            {formatSliderValue(sliderValue[1], false)}
                        </Typography>
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
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox} onChange={handleChange} name={"checkBox"}/>}
                        label={"test Checkbox"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.checkBox2} onChange={handleChange} name={"checkBox2"}/>}
                        label={"test Checkbox2"}/>

                </FormGroup>
            </ReactSlidingPane>
        </div>
    );
}

export default App;
