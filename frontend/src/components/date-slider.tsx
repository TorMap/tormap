import React, {FunctionComponent, useEffect, useState} from "react";
import {makeStyles, Slider,} from "@material-ui/core";
import {useDebounce} from "../util/hooks";
import {Mark} from "../types/mark";
import Moment from "react-moment";
import {KeyboardDatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import moment from "moment";
import {Settings} from "../types/variousTypes";

/**
 * Styles according to Material UI doc for components used in DateSlider component
 */
const useStyle = makeStyles(() => ({
    slider: {
        position: "fixed",
        bottom: "1%",
        width: "50%",
        left: "25%",
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
        position: "fixed",
        bottom: "15px",
        right: "1%",
        maxWidth: "20%",
    },
}))

interface Props {

    /**
     * A String array of available days available at the backend
     */
    availableDays: string[]

    /**
     * A callback function to update the selected day
     * @param n the nth entry in availableDays array
     */
    setValue: (n: number) => void

    /**
     * The currently applied app settings
     */
    settings: Settings
}
//todo: doc
export const DateSlider: FunctionComponent<Props> = ({availableDays, setValue}) => {

    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])
    const classes = useStyle()
    const debouncedSliderValue = useDebounce<number>(sliderValue, 500);

    // calculate the marks for the slider
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

    // handle debouncing of the slider value
    useEffect(() => {
        setValue(debouncedSliderValue)
    },[debouncedSliderValue])

    return (
        <div className={classes.slider}>
            <MuiPickersUtilsProvider utils={DateFnsUtils}>
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
                    className={classes.datePicker}
                />
            </MuiPickersUtilsProvider>
        </div>
    )

}