import React, {FunctionComponent, useEffect, useState} from "react";
import {useDebounce} from "../util/util";
import Moment from "react-moment";
import moment from "moment";
import {Box, Slider, TextField} from "@mui/material";
import {DatePicker, LocalizationProvider} from "@mui/lab";
import AdapterDateFns from '@mui/lab/AdapterDateFns';
import dateFormat from "dateformat";
import {enCA} from "date-fns/locale";

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
            bottom: "2%",
            width: "50%",
            left: "25%",
        }}>
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
                <LocalizationProvider dateAdapter={AdapterDateFns} locale={enCA}>
                    <DatePicker
                        value={debouncedSliderValue >= 0 ? availableDays[debouncedSliderValue] : undefined}
                        renderInput={(params) =>
                            <TextField variant={"standard"}
                                       {...params}
                                       sx={{
                                            position: "fixed",
                                            right: "2%",
                                            maxWidth: "20%",
                                        }}
                                       helperText={"A day in the life of the Tor Network"}

                            />
                        }
                        onChange={(date) => {
                            const day: string = dateFormat(date!!, "yyyy-mm-dd")
                            if (availableDays.includes(day)) {
                                setSliderValue(availableDays.findIndex(element => element === day))
                            }
                        }}
                        onError={() => console.log("error")}
                        minDate={new Date(availableDays[0])}
                        maxDate={new Date(availableDays[availableDays.length-1])}
                        shouldDisableDate={date => {
                            return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                        }}
                        views={["year","month","day"]}
                    />
                </LocalizationProvider>
        </Box>
    )

}

interface Mark {
    value: number
    label: JSX.Element
}