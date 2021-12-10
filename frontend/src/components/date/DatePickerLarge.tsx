import React, {FunctionComponent, useEffect, useState} from "react";
import AdapterDateFns from "@mui/lab/AdapterDateFns";
import {enCA} from "date-fns/locale";
import {TextField} from "@mui/material";
import dateFormat from "dateformat";
import moment from "moment";
import {DatePicker, LocalizationProvider} from "@mui/lab";
import {useDate} from "../../util/date-context";

export const DatePickerLarge: FunctionComponent = () => {

    const date = useDate()
    const selectedDate = date.selectedDate
    const availableDays = date.availableDays
    const setSelectedDate = date.setSelectedDate
    const [localSelectedDate, setLocalSelectedDate] = useState<string | undefined>()

    useEffect(() => {
        setLocalSelectedDate(selectedDate)
    }, [selectedDate])

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns} locale={enCA}>
            <DatePicker
                value={localSelectedDate}
                renderInput={(params) =>
                    <TextField variant={"standard"}
                               {...params}
                               sx={{
                                   position: "fixed",
                                   bottom: "4%",
                                   right: "2%",
                                   maxWidth: "20%",
                               }}
                               helperText={"A day in the life of the Tor Network"}
                    />
                }
                onChange={(date) => {
                    const day: string = dateFormat(date!!, "yyyy-mm-dd")
                    if (availableDays.includes(day)) {
                        setLocalSelectedDate(day)
                    }
                }}
                onAccept={() => setSelectedDate(localSelectedDate!)}
                minDate={new Date(availableDays[0])}
                maxDate={new Date(availableDays[availableDays.length - 1])}
                shouldDisableDate={date => {
                    return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                }}
                views={["year", "month", "day"]}
                showTodayButton
            />
        </LocalizationProvider>

    )
}