import React, {useEffect, useState} from 'react';
import {WorldMap} from "./components/world-map";
import {
    createMuiTheme, CircularProgress, ThemeProvider, makeStyles
} from "@material-ui/core";
import "@material-ui/styles";
import "./index.scss";
import {apiBaseUrl} from "./util/constants";
import {AccordionStats} from "./components/accordion-stats";
import {Settings, Statistics} from "./types/variousTypes";
import {MapStats} from "./components/map-stats";
import {DateSlider} from "./components/date-slider";

const useStyle = makeStyles(() => ({
    progressCircle: {
        position: "fixed",
        left: "calc(50% - 25px)",
        top: "calc(50% - 25px)",
        margin: "auto",
        backgroundColor: "transparent",
        color: "rgba(255,255,255,.6)",
        zIndex: 1000,
    },
}))

function App() {
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [isLoading, setIsLoading] = useState(true)
    const [settings, setSettings] = useState<Settings>({
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

        showMarker: true,
        colorNodesAccordingToType: false,
        aggregateCoordinates: false,
        heatMap: false,

        dateRange: false,
        familyGradient: false,

        sortCountry: false,
        onlyCountry: false,
        selectedCountry: undefined,
        sortFamily: false,
        onlyFamily: false,
        selectedFamily: undefined,
    })
    const [statistics, setStatistics] = useState<Statistics>({
        guard: 0,
        default: 0,
        exit: 0,
    })
    const classes = useStyle()

    const [theme] = useState(createMuiTheme({
        palette: {
            type: "dark",
        },
    }))

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





    return (
        <ThemeProvider theme={theme}>
            <div>
                {isLoading ?
                    <div className={classes.progressCircle}>
                        <CircularProgress color={"inherit"}/>
                    </div>
                    : null
                }
                <WorldMap
                    dayToDisplay={sliderValue >= 0 ? availableDays[sliderValue] : undefined}
                    settings={settings}
                    setSettingsCallback={setSettings}
                    setLoadingStateCallback={setIsLoading}
                    setStatisticsCallback={setStatistics}
                />
                <DateSlider availableDays={availableDays} setValue={setSliderValue} settings={settings}/>
                <AccordionStats settings={settings} onChange={handleInputChange}/>
                <MapStats settings={settings} statistics={statistics}/>
            </div>
        </ThemeProvider>
    )
}

export default App;
