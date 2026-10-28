import {differenceInDays} from "date-fns";
import React, {FunctionComponent, useCallback, useContext, useEffect, useState} from "react";

import {SnackbarMessage} from "../types/ui";
import {readPermalink, writePermalink} from "../util/permalink";
import {useSnackbar} from "./snackbar-context";

interface DateInterface {
    /**
     * Day to be displayed
     */
    selectedDate: string | undefined

    /**
     * Day to be selected
     * @param newDate Day in form of YYYY-MM-DD
     */
    setSelectedDate: (newDate: string) => void

    /**
     * An Array of all available Days
     */
    availableDays: string[]

    /**
     * A function to set the available days
     * @param arr
     */
    setAvailableDays: (arr: string[]) => void
}

const DateContext = React.createContext<DateInterface | null>(null)

export function useDate() {
     
    return useContext(DateContext)!
}

interface DateProviderProps {
    children?: React.ReactNode;
}

export const DateProvider: FunctionComponent<DateProviderProps> = ({children}) => {
    // Component state
    const [availableDays, setAvailableDaysState] = useState<string[]>([])
    const [selectedDate, setSelectedDate] = useState<string | undefined>(undefined)
    const {enqueueSnackbar} = useSnackbar();

    // Keeps the day in the URL, so the current view can be shared
    useEffect(() => {
        if (selectedDate) writePermalink({day: selectedDate})
    }, [selectedDate])

    // Selects the day from the URL if available, otherwise the latest day, whenever new days are available
    const setAvailableDays = useCallback((days: string[]) => {
        setAvailableDaysState(days)
        if (days.length > 0) {
            const permalinkDay = readPermalink().day
            setSelectedDate(permalinkDay && days.includes(permalinkDay) ? permalinkDay : days[days.length - 1])
            const oldestConsensusDateAvailable = new Date("2007-10-27")
            const minExpectedNumberOfDays = Math.abs(differenceInDays(oldestConsensusDateAvailable, new Date())) - 5
            if (days.length < minExpectedNumberOfDays) {
                enqueueSnackbar(SnackbarMessage.HistoricDataProcessing, {variant: "info"})
            }
        }
    }, [enqueueSnackbar])

    return (
        <DateContext.Provider
            value={{
                selectedDate,
                setSelectedDate,
                availableDays,
                setAvailableDays
            }}>
            {children}
        </DateContext.Provider>
    )
}
