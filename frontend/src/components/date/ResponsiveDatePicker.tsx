import React, {FunctionComponent} from "react";
import AdapterDateFns from "@mui/lab/AdapterDateFns";
import {TextField} from "@mui/material";
import dateFormat from "dateformat";
import moment from "moment";
import {DatePicker, LocalizationProvider} from "@mui/lab";
import {useDate} from "../../context/date-context";
import {isValid} from "date-fns";
import {enCA} from "date-fns/locale";

interface Props {
    /**
     * Whether this date picker is displayed on a large or small screen
     */
    largeScreen: boolean
}

export const ResponsiveDatePicker: FunctionComponent<Props> = ({largeScreen}) => {
    // App context
    const {selectedDate, availableDays, setSelectedDate} = useDate()

    const firstAvailableDate = selectedDate ? new Date(availableDays[0]) : undefined
    const lastAvailableDate = selectedDate ? new Date(availableDays[availableDays.length - 1]) : undefined

    const handleDateChange = (date: Date | null) => {
        if (date && isValid(date)) {
            const day: string = dateFormat(date, "yyyy-mm-dd")
            if (availableDays.includes(day)) {
                setSelectedDate(day)
            }
        }
    }

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns} locale={enCA}>
            <DatePicker
                value={selectedDate}
                mask={"____-__-__"}
                renderInput={(params) =>
                    largeScreen ? <TextField variant={"standard"}
                                             {...params}
                                             sx={{
                                                 position: "fixed",
                                                 bottom: "4%",
                                                 right: "2%",
                                                 maxWidth: "20%",
                                             }}
                                             helperText={"A day in the life of the Tor Network"}
                        /> :
                        <TextField variant={"standard"}
                                   {...params}
                                   sx={{
                                       padding: 2
                                   }}
                                   helperText={"A day in the life of the Tor Network"}
                        />
                }
                onChange={handleDateChange}
                onAccept={handleDateChange}
                minDate={firstAvailableDate}
                maxDate={lastAvailableDate}
                shouldDisableDate={date => {
                    return !(availableDays.includes(moment(date).format("YYYY-MM-DD")))
                }}
                views={["year", "month", "day"]}
                showTodayButton
            />
        </LocalizationProvider>
    )
}