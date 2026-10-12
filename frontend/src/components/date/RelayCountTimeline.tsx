import {Box, useTheme} from "@mui/material";
import {FunctionComponent, useEffect, useMemo, useState} from "react";

import {useDate} from "../../context/date-context";
import {DailyRelayCountDto} from "../../dto/relay";
import {findOutliers} from "../../util/outliers";
import {backend} from "../../util/util";

const chartWidth = 1000
const chartHeight = 100

/**
 * A small chart of the number of relays per day, aligned with the date slider.
 * Days with an unusual relay count are marked and can be clicked to select them.
 */
export const RelayCountTimeline: FunctionComponent = () => {
    // App context
    const {availableDays, selectedDate, setSelectedDate} = useDate()
    const theme = useTheme()

    // Component state
    const [counts, setCounts] = useState<DailyRelayCountDto[]>([])

    // The timeline is a nice to have, so a failed request just leaves it empty
    useEffect(() => {
        let cancelled = false
        backend.get<DailyRelayCountDto[]>("/relay/statistics/daily-counts").then(response => {
            // An older backend answers 404, and a proxy may answer 200 with something else than the expected list
            if (!cancelled && Array.isArray(response.data)) setCounts(response.data)
        }).catch(() => undefined)
        return () => {
            cancelled = true
        }
    }, [])

    const chart = useMemo(() => {
        if (availableDays.length < 2 || counts.length === 0) return undefined
        const totalByDay = new Map(counts.map(count => [count.day, count.total]))
        const values = availableDays.map(day => totalByDay.get(day))
        const defined = values.filter((value): value is number => value !== undefined)
        if (defined.length === 0) return undefined

        const max = Math.max(...defined)
        const min = Math.min(...defined)
        const range = Math.max(max - min, 1)
        const x = (index: number) => index / (availableDays.length - 1) * chartWidth
        const y = (value: number) => chartHeight - 6 - (value - min) / range * (chartHeight - 12)

        // Missing days break the line instead of interpolating over them
        const segments: string[] = []
        let current: string[] = []
        values.forEach((value, index) => {
            if (value === undefined) {
                if (current.length > 0) segments.push(current.join(" "))
                current = []
            } else {
                current.push(`${x(index).toFixed(1)},${y(value).toFixed(1)}`)
            }
        })
        if (current.length > 0) segments.push(current.join(" "))

        const outliers = findOutliers(values).map(({index, expected}) => {
            const value = values[index]!
            const change = Math.round((value / expected - 1) * 100)
            return {
                day: availableDays[index],
                x: x(index),
                y: y(value),
                label: `${availableDays[index]}: ${value} relays (${change > 0 ? "+" : ""}${change}% compared to the surrounding days)`,
            }
        })
        const selectedIndex = selectedDate ? availableDays.indexOf(selectedDate) : -1
        return {segments, outliers, min, max, selectedX: selectedIndex >= 0 ? x(selectedIndex) : undefined}
    }, [availableDays, counts, selectedDate])

    if (!chart) return null

    return (
        <Box sx={{mb: "28px", opacity: 0.9}}>
            <svg
                role="img"
                aria-label={`Relays per day, from ${chart.min} to ${chart.max}. ${chart.outliers.length} days with an unusual number of relays are marked.`}
                viewBox={`0 0 ${chartWidth} ${chartHeight}`}
                preserveAspectRatio="none"
                width="100%"
                height="48"
                style={{display: "block", overflow: "visible"}}
            >
                {chart.segments.map((points, index) => (
                    <polyline
                        key={index}
                        points={points}
                        fill="none"
                        stroke={theme.palette.primary.main}
                        strokeWidth={2}
                        vectorEffect="non-scaling-stroke"
                    />
                ))}
                {chart.selectedX !== undefined ? (
                    <line
                        x1={chart.selectedX} x2={chart.selectedX} y1={0} y2={chartHeight}
                        stroke={theme.palette.text.secondary}
                        strokeWidth={1}
                        vectorEffect="non-scaling-stroke"
                    />
                ) : null}
                {chart.outliers.map(outlier => (
                    <rect
                        key={outlier.day}
                        data-testid="relay-count-outlier"
                        role="button"
                        tabIndex={0}
                        aria-label={`Select ${outlier.label}`}
                        x={outlier.x - 3} y={outlier.y - 12} width={6} height={24} rx={3}
                        fill={theme.palette.warning.main}
                        style={{cursor: "pointer"}}
                        onClick={() => setSelectedDate(outlier.day)}
                        onKeyDown={event => {
                            if (event.key === "Enter" || event.key === " ") setSelectedDate(outlier.day)
                        }}
                    >
                        <title>{outlier.label}</title>
                    </rect>
                ))}
            </svg>
        </Box>
    )
}

export default RelayCountTimeline
