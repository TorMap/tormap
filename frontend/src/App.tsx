import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, Checkbox, FormControlLabel, FormGroup, Grid, Slider, Switch} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";
import {apiBaseUrl} from "./util/constants";
import {Mark} from "./types/mark";

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [colorNodeFlags, setColorNodeFlags] = useState(true)
    const [preLoadMonths, setPreLoadMonths] = useState(false)
    const [state, setState] = useState({
        guard: true,
        exit: true,
        default: true,
    })
    const [availableMonths, setAvailableMonths] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])

    const debouncedSliderValue = useDebounce<number>(sliderValue, 100);

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setState({...state, [event.target.name]: event.target.checked})
    };

    const marks = (count: number) : Mark[] => {
        console.log("entered marks()")
        if (count < 2) return []
        count--
        let marks = []
        for (let i = 0; i <= count; i++) {
            const mark: Mark = {
                value: Math.round(i * (availableMonths.length - 1) / count),
                label: <Moment
                    date={availableMonths[Math.round(i * (availableMonths.length - 1) / count)]}
                    format={"MM/YYYY"}
                />
            }
            console.log(mark.label)
            marks.push(mark);
        }
        return marks
    }

    useEffect(() => {
        fetch(`${apiBaseUrl}/archive/geo/relay/months`)
            .then(response => response.json())
            .then(availableMonths => {
                setAvailableMonths(availableMonths)
                setSliderValue(availableMonths.length - 1)
            })
            .catch(console.log)
    }, [])

    useEffect(() => {
        if(availableMonths.length != 0){
            setSliderMarks(marks(6))
        }
    }, [availableMonths])

    // Hook
    // T is a generic type for value parameter, our case this will be string
    function useDebounce<T>(value: T, delay: number): T {
        // State and setters for debounced value
        const [debouncedValue, setDebouncedValue] = useState<T>(value);
        useEffect(
            () => {
                // Update debounced value after delay
                const handler = setTimeout(() => {
                    setDebouncedValue(value);
                }, delay);
                // Cancel the timeout if value changes (also on delay change or unmount)
                // This is how we prevent debounced value from updating if value is changed ...
                // .. within the delay period. Timeout gets cleared and restarted.
                return () => {
                    clearTimeout(handler);
                };
            },
            [value, delay] // Only re-call effect if value or delay changes
        );
        return debouncedValue;
    }

    return (
        <div>
            <WorldMap
                monthToDisplay={debouncedSliderValue >= 0 ? availableMonths[debouncedSliderValue] : undefined}
                colorFlags={colorNodeFlags}
                preLoadMonths={preLoadMonths ? availableMonths : undefined}
                filter={state}
            />
            <div className={"sliderContainer"}>
                <Grid container spacing={2}>
                    <Grid item xs>
                        <Slider
                            disabled={(availableMonths.length == 0)}
                            className={"slider"}
                            value={sliderValue}
                            onChangeCommitted={(event: any, newValue: number | number[]) => setSliderValue(newValue as number)}
                            valueLabelDisplay={(availableMonths.length == 0) ? "off" : "on"}
                            name={"slider"}
                            min={0}
                            max={availableMonths.length - 1}
                            marks={sliderMarks}
                            valueLabelFormat={(x) => <Moment date={availableMonths[x]} format={"MM/YY"}/>}
                            track={false}
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
                    <FormControlLabel control={<Switch checked={preLoadMonths} onChange={() => setPreLoadMonths(!preLoadMonths)}/>}
                                      label={"Loads the data for all months in the background"}/>
                    <p>Filter by relay flags</p>
                    <FormControlLabel
                        control={<Checkbox checked={state.guard} onChange={handleInputChange} name={"guard"}/>}
                        label={"Guard"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.exit} onChange={handleInputChange} name={"exit"}/>}
                        label={"Exit"}/>
                    <FormControlLabel
                        control={<Checkbox checked={state.default} onChange={handleInputChange} name={"default"}/>}
                        label={"default"}/>
                </FormGroup>
            </ReactSlidingPane>
        </div>
    )
}

export default App;
