import React, {FunctionComponent, useEffect, useState} from "react";
import {useDebounce} from "../../util/util";
import Moment from "react-moment";
import {Box, Slider} from "@mui/material";
import {useDate} from "../../util/date-context";

/**
 * A Slider for date selection
 */
export const DateSlider: FunctionComponent = () => {
    const date = useDate()
    const sliderValue = date.sliderValue
    const availableDays = date.availableDays
    const setSliderValue = date.setSliderValue

    const [localSliderValue, setLocalSliderValue] = useState<number | undefined>(sliderValue)
    const [localSliderValueComitted, setLocalSliderValueComitted] = useState<number | undefined>(sliderValue)
    const [sliderMarks, setSliderMarks] = useState<Mark[]>([])

    const debouncedSliderValue = useDebounce<number | undefined>(localSliderValueComitted, 500);

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
        }
    }, [availableDays])

    // handle debouncing of the slider value when dragging
    useEffect(() => {
        setSliderValue(debouncedSliderValue)
    }, [debouncedSliderValue, setSliderValue])

    useEffect(() => {
        setLocalSliderValue(sliderValue)
    },[sliderValue])


    return (
        <Box>
            <Slider
                disabled={(availableDays.length === 0)}
                value={localSliderValue}
                onChange={(event: any, newValue: number | number[]) => {
                    setLocalSliderValue(newValue as number)
                }}
                onChangeCommitted={(event: any, newValue: number | number[]) => {
                    setLocalSliderValueComitted(newValue as number)
                }}
                valueLabelDisplay={(availableDays.length === 0) ? "off" : "on"}
                name={"slider"}
                min={0}
                max={availableDays.length - 1}
                marks={sliderMarks}
                valueLabelFormat={(x) => <Moment date={availableDays[x]} format={"YYYY-MM-DD"}/>}
                track={false}
            />
        </Box>
    )

}

interface Mark {
    value: number
    label: JSX.Element
}