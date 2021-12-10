import React, {FunctionComponent, useContext, useEffect, useState} from "react";

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
     * the Index of the selected Day in availableDays
     */
    sliderValue: number | undefined

    /**
     * Day to be selected in form of index in availableDays
     * @param index
     */
    setSliderValue: (index: number | undefined) => void

    /**
     * An Array of all available Days
     */
    availableDays: string[]

    /**
     * A funktion to set the available Days
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
    const [availableDays, setAvailableDays] = useState<string[]>([])
    const [selectedDate, setSelectedDate] = useState<string | undefined>(undefined)

    const sliderValue = availableDays.findIndex((value) => value === selectedDate)

    function setSliderValue(index: number | undefined) {
        if (index) setSelectedDate(availableDays[index])
        else setSelectedDate(undefined)
    }

    useEffect(() => {
        if (availableDays) setSelectedDate(availableDays[availableDays.length - 1])
    }, [availableDays])

    return (
        <DateContext.Provider
            value={{
                selectedDate,
                setSelectedDate,
                sliderValue,
                setSliderValue,
                availableDays,
                setAvailableDays
            }}>
            {children}
        </DateContext.Provider>
    )
}