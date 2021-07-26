import React, {FunctionComponent, useEffect, useState} from "react";
import {Grid, makeStyles, Slider, } from "@material-ui/core";
import {useDebounce} from "../util/hooks";
import {Mark} from "../types/mark";
import Moment from "react-moment";
import {KeyboardDatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import moment from "moment";
import {Settings} from "../types/variousTypes";

const useStyle = makeStyles(() => ({
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

interface Props {
    availableDays: string[]
    setValue: (n: number) => void
    settings: Settings
}

export const DateSlider: FunctionComponent<Props> = ({availableDays, setValue, settings}) => {

    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])
    const classes = useStyle()
    const debouncedSliderValue = useDebounce<number>(sliderValue, 500);

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
            setSliderValue(availableDays.length-1)
        }
    }, [availableDays])

    useEffect(() => {
        setValue(debouncedSliderValue)
    },[debouncedSliderValue])

    return (
        <div className={classes.slider}>
            <MuiPickersUtilsProvider utils={DateFnsUtils}>
                <Grid container spacing={8} justify={"center"}>
                    <Grid item xs={2}>
                        {settings.dateRange ? (
                            <KeyboardDatePicker
                                autoOk
                                variant="inline"
                                format="yyyy-MM-dd"
                                margin="normal"
                                id="date-picker-1"
                                minDate={availableDays[0]}
                                maxDate={availableDays[availableDays.length-1]}
                                value={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                                onChange={(date, value) => {
                                    if (availableDays.includes(value!!)) {
                                        setSliderValue(availableDays.findIndex(element => element === value))
                                    }else{
                                        console.log(`Day ${value} is not available at the moment`)
                                    }
                                }}
                                KeyboardButtonProps={{
                                    'aria-label': 'change date',
                                }}
                                helperText={`Day in the life of the tor network`}
                                shouldDisableDate={date => {
                                    return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                                }}
                            />
                        ) : null}

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
                                    setSliderValue(availableDays.findIndex(element => element === value))
                                }else{
                                    console.log(`Day ${value} is not available at the moment`)
                                }
                            }}
                            KeyboardButtonProps={{
                                'aria-label': 'change date',
                            }}
                            helperText={`Day in the life of the tor network`}
                            shouldDisableDate={date => {
                                return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                            }}
                        />
                    </Grid>
                </Grid>
            </MuiPickersUtilsProvider>
        </div>
    )

}