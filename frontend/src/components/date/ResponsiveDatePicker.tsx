import {DatePicker, LocalizationProvider} from "@mui/x-date-pickers";
import {AdapterDateFns} from "@mui/x-date-pickers/AdapterDateFns";
import {format, isValid, parseISO} from "date-fns";
import {enCA} from "date-fns/locale";
import {FunctionComponent, useState} from "react";

import {useDate} from "../../context/date-context";

interface Props {
    /**
     * Whether this date picker is displayed on a large or small screen
     */
    largeScreen: boolean
}

export const ResponsiveDatePicker: FunctionComponent<Props> = ({largeScreen}) => {
    // App context
    const {selectedDate, availableDays, setSelectedDate} = useDate()

    const firstAvailableDate = selectedDate ? parseISO(availableDays[0]) : undefined
    const lastAvailableDate = selectedDate ? parseISO(availableDays[availableDays.length - 1]) : undefined

    // The field keeps what the user is typing. Intermediate values (e.g. a new year while typing a full date)
    // are often not available days and must not be reverted by the controlled value.
    const [inputValue, setInputValue] = useState<Date | null>(() => selectedDate ? parseISO(selectedDate) : null)
    const [syncedSelectedDate, setSyncedSelectedDate] = useState(selectedDate)
    if (syncedSelectedDate !== selectedDate) {
        setSyncedSelectedDate(selectedDate)
        setInputValue(selectedDate ? parseISO(selectedDate) : null)
    }

    const handleDateChange = (date: Date | null) => {
        setInputValue(date)
        if (date && isValid(date)) {
            const day: string = format(date, "yyyy-MM-dd")
            if (availableDays.includes(day)) {
                setSelectedDate(day)
            }
        }
    }

    // Discard input which does not correspond to the loaded day
    const resetInput = () => setInputValue(selectedDate ? parseISO(selectedDate) : null)

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={enCA}>
            <DatePicker
                value={inputValue}
                format={"yyyy-MM-dd"}
                slotProps={{
                    textField: largeScreen ? {
                        variant: "standard",
                        onBlur: resetInput,
                        sx: {
                            position: "fixed",
                            bottom: "37px",
                            right: "1%",
                            maxWidth: "20%",
                        },
                    } : {
                        variant: "standard",
                        onBlur: resetInput,
                        sx: {
                            padding: 2,
                        },
                        helperText: "Select a date",
                    },
                }}
                onChange={handleDateChange}
                onAccept={handleDateChange}
                minDate={firstAvailableDate}
                maxDate={lastAvailableDate}
                shouldDisableDate={(date) => {
                    return !(availableDays.includes(format(date, "yyyy-MM-dd")))
                }}
                views={["year", "month", "day"]}
            />
        </LocalizationProvider>
    )
}
