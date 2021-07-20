import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import {CircularProgress, Grid, Slider,} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";
import {apiBaseUrl} from "./util/constants";
import {Mark} from "./types/mark";
import {AccordionStats} from "./components/arccordion-stats/accordion-stats";
import {Settings} from "./types/variousTypes";

function App() {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])
    const [isLoading, setIsLoading] = useState(true)
    const [settings, setSettings] = useState<Settings>({
        guard: true,
        default: true,
        exit: true,

        colorNodesAccordingToFlags: true,
    })

    const debouncedSliderValue = useDebounce<number>(sliderValue, 100);

    const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSettings({...settings, [event.target.name]: event.target.checked})
        console.log(`trigger changed ${event.target.checked}`)
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
            />
            <div className={"sliderContainer"}>
                <Grid container spacing={2}>
                    <Grid item xs>
                        <Slider
                            disabled={(availableDays.length === 0)}
                            className={"slider"}
                            value={sliderValue}
                            onChangeCommitted={(event: any, newValue: number | number[]) => setSliderValue(newValue as number)}
                            valueLabelDisplay={(availableDays.length === 0) ? "off" : "on"}
                            name={"slider"}
                            min={0}
                            max={availableDays.length - 1}
                            marks={sliderMarks}
                            valueLabelFormat={(x) => <Moment date={availableDays[x]} format={"YYYY-MM-DD"}/>}
                            track={false}
                        />
                    </Grid>
                </Grid>
            </div>
            <AccordionStats settings={settings} onChange={handleInputChange}/>
        </div>
    )
}

export default App;
