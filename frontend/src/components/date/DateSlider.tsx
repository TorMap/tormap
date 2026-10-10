import {Slider, SliderProps} from "@mui/material";
import {format} from "date-fns";
import {FunctionComponent, useEffect, useMemo, useState} from "react";

import {useDate} from "../../context/date-context";
import {useDebounce} from "../../util/util";

type Mark = Exclude<SliderProps["marks"], boolean | undefined>[number]

/**
 * A Slider for date selection
 */
export const DateSlider: FunctionComponent = () => {
    // App context
    const {availableDays, setSelectedDate, selectedDate} = useDate()

    // Component state
    const [sliderValue, setSliderValue] = useState<number>(() => availableDays.findIndex((value) => value === selectedDate))
    const [syncedAvailableDays, setSyncedAvailableDays] = useState(availableDays)
    const [syncedSelectedDate, setSyncedSelectedDate] = useState(selectedDate)

    const debouncedSliderValue = useDebounce<number>(sliderValue, 500)

    // Move the slider whenever the selected day got changed from outside
    if (syncedAvailableDays !== availableDays || syncedSelectedDate !== selectedDate) {
        setSyncedAvailableDays(availableDays)
        setSyncedSelectedDate(selectedDate)
        setSliderValue(availableDays.findIndex((value) => value === selectedDate))
    }

    // Calculate the marks for the slider
    const sliderMarks = useMemo(() => {
        const marks: Mark[] = []
        if (availableDays.length > 0) {
            const markCount = Math.min(6, availableDays.length)
            for (let i = 0; i < markCount; i++) {
                const dateIndex = Math.round(i * (availableDays.length - 1) / Math.max(markCount - 1, 1))
                marks.push({
                    value: dateIndex,
                    label: format(new Date(availableDays[dateIndex]), "yyyy-MM")
                })
            }
        }
        return marks
    }, [availableDays])

    // Handle debouncing of the slider value when dragging
    useEffect(() => {
        // The index is -1 until a day is selected, which must not overwrite the selection
        if (debouncedSliderValue !== undefined && availableDays[debouncedSliderValue]) {
            setSelectedDate(availableDays[debouncedSliderValue])
        }
    }, [availableDays, debouncedSliderValue, setSelectedDate])

    return (
        <Slider
            disabled={(availableDays.length === 0)}
            value={sliderValue}
            onChange={(_, newValue: number | number[]) => {
                setSliderValue(newValue as number)
            }}
            onChangeCommitted={(_, newValue: number | number[]) => {
                // Select the day right away instead of waiting for the debounce
                setSelectedDate(availableDays[newValue as number])
            }}
            valueLabelDisplay={(availableDays.length === 0) ? "off" : "on"}
            name={"slider"}
            min={0}
            max={availableDays.length - 1}
            marks={sliderMarks}
            valueLabelFormat={x => availableDays[x]}
            track={false}
        />
    )
}
