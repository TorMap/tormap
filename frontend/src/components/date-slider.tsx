import React, {FunctionComponent, useEffect, useState} from "react";
import {makeStyles} from "@material-ui/core";
import {useDebounce} from "../util/util";
import Moment from "react-moment";
import {KeyboardDatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import moment from "moment";
import {Slider, Box} from "@mui/material";

/**
 * Styles according to Material UI doc for components used in DateSlider component
 */
const useStyle = makeStyles(() => ({
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
}

/**
 * The Date selection
 * @param availableDays - An Array of Strings that represent the available days
 * @param setValue - The change handler for the new date
 */
export const DateSlider: FunctionComponent<Props> = ({availableDays, setValue}) => {

    const [sliderValue, setSliderValue] = useState<number>(-1)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])
    const classes = useStyle()
    const debouncedSliderValue = useDebounce<number>(sliderValue, 500);

    // calculate the marks for the slider
    useEffect(() => {
        if (availableDays.length !== 0) {
            // the count of marks
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
    },[debouncedSliderValue, setValue])

    return (
        <Box sx={{
            position: "fixed",
            bottom: "1%",
            width: "50%",
            left: "25%",
        }}>
            <MuiPickersUtilsProvider utils={DateFnsUtils}>
                <Slider
                    disabled={(availableDays.length === 0)}
                    value={sliderValue}
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
        </Box>
    )

}

interface Mark {
    value: number
    label: JSX.Element
}