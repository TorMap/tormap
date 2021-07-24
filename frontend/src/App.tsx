import React, {useState} from 'react';
import {WorldMap} from "./components/world-map/world-map";
import ReactSlidingPane from "react-sliding-pane";
import {Button, FormControlLabel, Switch, FormGroup, Checkbox, Slider, Typography, Grid} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import Moment from "react-moment";
import {apiBaseUrl} from "./util/constants";
import {Mark} from "./types/mark";
import {AccordionStats} from "./components/arccordion-stats/accordion-stats";
import {Settings, Statistics, TempSettings} from "./types/variousTypes";
import {MapStats} from "./components/legend/map-legend";
import {blue} from "@material-ui/core/colors";
import {MuiPickersUtilsProvider, KeyboardDatePicker} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import moment from 'moment';

const useStyle = makeStyles(theme => ({
    slider: {
        position: "fixed",
        bottom: "20px",
        width: "90%",
        left: "5%",
    },
    sliderValueLabel: {
        top: "-41px",
        left: "calc(-50% - 8px)",
        fontSize: ".7rem",
        "& span":{
            width: "40px",
            height: "40px",
            "& span":{
                padding: "14px 5px 0px 5px",
                textAlign: "center",
            }
        }
    },
    datePicker: {
        width: "100%",
    },
}))


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
        miUnamed: false,
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

    const [theme, setTheme] = useState(createMuiTheme({
        palette: {
            type: "dark",
        },
    }))
    const classes = useStyle()

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
        <ThemeProvider theme={theme}>
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
            <div className={classes.slider}>
                <MuiPickersUtilsProvider utils={DateFnsUtils}>
                    <Grid container spacing={8} justify={"center"}>
                        <Grid item xs={2}>
                            <TextField
                                id="date"
                                type="date"
                                defaultValue={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                                className={classes.datePicker}
                                InputLabelProps={{
                                    shrink: true,
                                }}
                                disabled={true}
                            />
                        </Grid>
                        <Grid item xs>
                            <Slider
                                disabled={(availableDays.length === 0)}
                                value={sliderValue}
                                classes={{
                                    valueLabel: classes.sliderValueLabel,
                                }}
                                onChange={(event: any, newValue: number | number[]) => {
                                    setSliderValue(newValue as number)
                                    setErrorState(false)
                                }}
                                valueLabelDisplay={(availableDays.length === 0) ? "off" : "on"}
                                name={"slider"}
                                min={0}
                                max={availableDays.length - 1}
                                marks={sliderMarks}
                                valueLabelFormat={(x) => <Moment date={availableDays[x]} format={"DD-MM-YYYY"}/>}
                                track={false}
                            />
                        </Grid>
                        <Grid item xs={2} >
                                <TextField
                                    id="date"
                                    type="date"
                                    defaultValue={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                                    className={classes.datePicker}
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
                        </Grid>
                        <Grid item xs={2} >
                            <KeyboardDatePicker
                                autoOk
                                variant="inline"
                                format="yyyy-MM-dd"
                                margin="normal"
                                id="date-picker-2"
                                minDate={availableDays[0]}
                                maxDate={availableDays[availableDays.length-1]}
                                value={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                                onChange={(date, value) => {
                                    if (availableDays.includes(value!!)) {
                                        setErrorState(false)
                                        setSliderValue(availableDays.findIndex(element => element === value))
                                    }else{
                                        setErrorState(true)
                                        console.log(`Day ${value} is not available at the moment`)
                                    }
                                }}
                                KeyboardButtonProps={{
                                    'aria-label': 'change date',
                                }}
                                helperText={errorState ? `Day is not available at the moment` : null}
                                shouldDisableDate={date => {
                                    return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                                }}
                            />
                        </Grid>
                    </Grid>
                </MuiPickersUtilsProvider>
            </div>
            <AccordionStats settings={settings} onChange={handleInputChange}/>
            <MapStats statistics={statistics}/>
        </div>
        </ThemeProvider>
    )
}

export default App;
