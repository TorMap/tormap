import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {
    Accordion,
    Button,
    Checkbox,
    CircularProgress,
    FormControlLabel,
    FormGroup,
    Grid,
    Slider,
    Switch
} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";
import {apiBaseUrl} from "./util/constants";
import {Mark} from "./types/mark";
import {AccordionStats} from "./components/arccordion-stats/accordion-stats";
import {Settings} from "./types/variousTypes";

function App() {
    const [showOptionPane, setShowOptionPane] = useState(false)
    const [preLoadMonths, setPreLoadMonths] = useState(false)
    const [availableMonths, setAvailableMonths] = useState<string[]>([])
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


    const marks = (count: number) : Mark[] => {
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
            marks.push(mark);
        }
        return marks
    }

    // Loads available months from the backend
    useEffect(() => {
        setIsLoading(true)
        fetch(`${apiBaseUrl}/archive/geo/relay/months`)
            .then(response => response.json())
            .then(availableMonths => {
                setAvailableMonths(availableMonths)
                setSliderValue(availableMonths.length - 1)
                setIsLoading(false)
            })
            .catch(console.log)
    }, [])

    useEffect(() => {
        if(availableMonths.length !== 0){
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

    const Loading = (
        <div className={"progressCircle"}>
            <CircularProgress/>
        </div>
    )

    return (
        <div>
            {isLoading ? (Loading) : null}
            <WorldMap
                monthToDisplay={debouncedSliderValue >= 0 ? availableMonths[debouncedSliderValue] : undefined}
                preLoadMonths={preLoadMonths ? availableMonths : undefined}
                settings={settings}
                setLoadingStateCallback={setIsLoading}
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
            <AccordionStats settings={settings} onChange={handleInputChange}/>
        </div>
    )
}

export default App;
