import React, {FunctionComponent, useEffect, useState} from "react";
import {useDebounce} from "../../util/util";
import Moment from "react-moment";
import {Slider} from "@mui/material";
import {useDate} from "../../context/date-context";

/**
 * A Slider for date selection
 */
export const DateSlider: FunctionComponent = () => {
    // App context
    const {availableDays, setSelectedDate, selectedDate} = useDate()

    // Component state
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])
    const [sliderValue, setSliderValue] = useState<number>(availableDays.length - 1)

    let debouncedSliderValue = useDebounce<number>(sliderValue, 500)

    useEffect(() => {
        setSliderValue(availableDays.findIndex((value) => value === selectedDate))
    }, [availableDays, selectedDate])

    // Calculate the marks for the slider
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
        }
    }, [availableDays])

    // Handle debouncing of the slider value when dragging
    useEffect(() => {
        if (debouncedSliderValue !== undefined) {
            setSelectedDate(availableDays[debouncedSliderValue])
        }
    }, [availableDays, debouncedSliderValue, setSelectedDate])

    return (
        <>
            <Slider
                disabled={(availableDays.length === 0)}
                value={sliderValue}
                onChange={(event: any, newValue: number | number[]) => {
                    setSliderValue(newValue as number)
                }}
                onChangeCommitted={(event: any, newValue: number | number[]) => {
                    debouncedSliderValue = newValue as number
                }}
                valueLabelDisplay={(availableDays.length === 0) ? "off" : "on"}
                name={"slider"}
                min={0}
                max={availableDays.length - 1}
                marks={sliderMarks}
                valueLabelFormat={(x) => x > 0 ? availableDays[x] : undefined}
                track={false}
            />
        </>
    )

}

interface Mark {
    value: number
    label: JSX.Element
}