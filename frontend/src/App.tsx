import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import {CircularProgress, Grid, Slider, TextField} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";
import {apiBaseUrl} from "./util/constants";
import {Mark} from "./types/mark";
import {AccordionStats} from "./components/arccordion-stats/accordion-stats";
import {Statistics, TempSettings} from "./types/variousTypes";
import {MapStats} from "./components/legend/map-legend";

function App() {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [settings, setSettings] = useState<TempSettings>({
        Guard: true,
        Exit: true,
        Default: true,

        miValid: false,
        miNamed: false,
        miUnnamed: false,
        miRunning: false,
        miStable: false,
        miExit: false,
        miFast: false,
        miGuard: false,
        miAuthority: false,
        miV2Dir: false,
        miHSDir: false,
        miNoEdConsensus: false,
        miStaleDesc: false,
        miSybil: false,
        miBadExit: false,

        colorNodesAccordingToFlags: true,
    })
    const [statistics, setStatistics] = useState<Statistics>({
        guard: 0,
        default: 0,
        exit: 0,
    })
    const [errorState, setErrorState] = useState(false)

    const debouncedSliderValue = useDebounce<number>(sliderValue, 500);

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSettings({...settings, [event.target.name]: event.target.checked})
        console.log(`trigger changed ${[event.target.name]} to ${event.target.checked}`)
    };

    // Loads available days from the backend
    useEffect(() => {
        setIsLoading(true)
        fetch(`${apiBaseUrl}/archive/geo/relay/days`)
            .then(response => response.json())
            .then(availableDays => {
                setAvailableDays(availableDays)
                setSliderValue(availableDays.length - 1)
                setIsLoading(false)
            })
            .catch(console.log)
    }, [])

    useEffect(() => {
        if (availableDays.length !== 0) {
            let markCount = 6
            markCount--
            let marks = []
            for (let i = 0; i <= markCount; i++) {
                const mark: Mark = {
                    value: Math.round(i * (availableDays.length - 1) / markCount),
                    label: <Moment
                        date={availableDays[Math.round(i * (availableDays.length - 1) / markCount)]}
                        format={"YYYY-MM"}
                    />
                }
                marks.push(mark);
            }
            setSliderMarks(marks)
        }
    }, [availableDays])

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
            {isLoading ?
                <div className={"progressCircle"}>
                    <CircularProgress/>
                </div>
                : null
            }
            <WorldMap
                dayToDisplay={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                settings={settings}
                setLoadingStateCallback={setIsLoading}
                setStatisticsCallback={setStatistics}
            />
            <div className={"sliderContainer"}>
                <Grid container spacing={2}>
                    <Grid item className={"dateGridRight"}>
                        <form className={"textFieldContainer"} noValidate>
                            <TextField
                                id="date"
                                type="date"
                                defaultValue={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                                className={"textField"}
                                InputLabelProps={{
                                    shrink: true,
                                }}
                                disabled={true}
                            />
                        </form>
                    </Grid>
                    <Grid item xs>
                        <Slider
                            disabled={(availableDays.length === 0)}
                            className={"slider"}
                            value={sliderValue}
                            onChange={(event: any, newValue: number | number[]) => {
                                setSliderValue(newValue as number)
                                setErrorState(false)
                            }}
                            valueLabelDisplay={(availableDays.length === 0) ? "off" : "on"}
                            name={"slider"}
                            min={0}
                            max={availableDays.length - 1}
                            marks={sliderMarks}
                            valueLabelFormat={(x) => <Moment date={availableDays[x]} format={"YYYY-MM-DD"}/>}
                            track={false}
                        />
                    </Grid>
                    <Grid item className={"dateGridRight"}>
                        <form className={"textFieldContainer"} noValidate>
                            <TextField
                                id="date"
                                type="date"
                                defaultValue={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                                className={"textField"}
                                InputLabelProps={{
                                    shrink: true,
                                }}
                                disabled={(availableDays.length === 0)}
                                onChange={(event) => {
                                    const value = event.target.value
                                    if (availableDays.includes(value)) {
                                        setErrorState(false)
                                        setSliderValue(availableDays.findIndex(element => element === value))
                                    }else{
                                        setErrorState(true)
                                        console.log(`Day ${value} is not available at the moment`)
                                    }
                                }}
                                value={availableDays[sliderValue]}
                                error={errorState}
                                helperText={errorState ? `Day is not available at the moment` : null}
                            />
                        </form>
                    </Grid>
                </Grid>
            </div>
            <AccordionStats settings={settings} onChange={handleInputChange}/>
            <MapStats statistics={statistics}/>
        </div>
    )
}

export default App;
