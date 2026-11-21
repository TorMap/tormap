import {Box, Button, ButtonBase, Skeleton, Tooltip, Typography, useTheme} from "@mui/material";
import {useSetAtom} from "jotai";
import React, {FunctionComponent, useEffect, useMemo, useState} from "react";

import {useDate} from "../../../context/date-context";
import {RelayMonthDto} from "../../../dto/relay";
import {monthOfDay} from "../../../util/api-v1";
import {backend} from "../../../util/util";
import {followRelayAtom} from "./relay-details-atoms";

// The months of the last relays shown, so jumping between months does not download them again
const monthsCache = new Map<string, RelayMonthDto[]>()
const maximumCachedRelays = 20

/**
 * Forgets the cached months. For tests, which mock different months for the same relay.
 */
export const clearRelayMonthsCache = () => monthsCache.clear()

const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

interface Props {
    fingerprint: string
}

/**
 * The months in which a relay was seen: an overview per year and buttons to the previous and next of those months.
 * A jump selects a day of that month on the map and keeps showing the relay.
 */
export const RelayMonths: FunctionComponent<Props> = ({fingerprint}) => {
    const theme = useTheme()
    const {selectedDate, setSelectedDate, availableDays} = useDate()
    const followRelay = useSetAtom(followRelayAtom)
    const [loaded, setLoaded] = useState<{ fingerprint: string, months?: RelayMonthDto[] }>(() =>
        ({fingerprint, months: monthsCache.get(fingerprint)}))
    const months = loaded.fingerprint === fingerprint ? loaded.months : monthsCache.get(fingerprint)
    const [failed, setFailed] = useState<string>()

    useEffect(() => {
        if (monthsCache.has(fingerprint)) return undefined
        let isCancelled = false
        backend.get<RelayMonthDto[]>(`/v1/relays/${encodeURIComponent(fingerprint)}/months`).then(response => {
            if (isCancelled || !Array.isArray(response.data)) return
            if (monthsCache.size >= maximumCachedRelays) monthsCache.delete(monthsCache.keys().next().value!)
            monthsCache.set(fingerprint, response.data)
            setLoaded({fingerprint, months: response.data})
        }).catch(() => {
            if (!isCancelled) setFailed(fingerprint)
        })
        return () => {
            isCancelled = true
        }
    }, [fingerprint])

    const currentMonth = selectedDate ? monthOfDay(selectedDate) : undefined

    // The day a jump selects: the relay's descriptor day if the map has it, otherwise the month's latest day with data
    const targetDays = useMemo(() => {
        const available = new Set(availableDays)
        const latestDayOfMonth = new Map<string, string>()
        availableDays.forEach(day => latestDayOfMonth.set(monthOfDay(day), day))
        return new Map((months ?? []).flatMap(month => {
            const day = available.has(month.day) ? month.day : latestDayOfMonth.get(month.month)
            return day ? [[month.month, day] as const] : []
        }))
    }, [months, availableDays])

    if (failed === fingerprint) return null
    if (!months) return <Skeleton variant="rounded" height={64} sx={{mb: 2}}/>
    if (months.length === 0) return null

    const jump = (month: string) => {
        const day = targetDays.get(month)
        if (!day || month === currentMonth) return
        followRelay({fingerprint, day})
        setSelectedDate(day)
    }
    const reachable = months.map(month => month.month).filter(month => targetDays.has(month))
    const previous = currentMonth ? reachable.filter(month => month < currentMonth).at(-1) : undefined
    const next = currentMonth ? reachable.find(month => month > currentMonth) : undefined
    const seen = new Map(months.map(month => [month.month, month]))
    const firstYear = Number(months[0].month.slice(0, 4))
    const lastYear = Number(months[months.length - 1].month.slice(0, 4))
    const years = Array.from({length: lastYear - firstYear + 1}, (_, index) => firstYear + index)

    return (
        <Box component="section" aria-label="Months seen" sx={{px: 2, pt: 1, pb: 2}}>
            <Box sx={{display: "flex", flexWrap: "wrap", alignItems: "center", gap: 1, mb: 1}}>
                <Typography sx={{flexGrow: 1}}>
                    Seen in {months.length} {months.length === 1 ? "month" : "months"}
                    {months.length > 1 && ` (${months[0].month} to ${months[months.length - 1].month})`}
                </Typography>
                <Button size="small" variant="outlined" disabled={!previous} onClick={() => previous && jump(previous)}
                        aria-label={previous ? `Previous month seen: ${previous}` : "No earlier month seen"}>
                    ‹ {previous ?? "Earlier"}
                </Button>
                <Button size="small" variant="outlined" disabled={!next} onClick={() => next && jump(next)}
                        aria-label={next ? `Next month seen: ${next}` : "No later month seen"}>
                    {next ?? "Later"} ›
                </Button>
            </Box>
            <Box sx={{display: "grid", gridTemplateColumns: "auto repeat(12, minmax(14px, 22px))", gap: "3px", alignItems: "center", justifyContent: "start"}}>
                {years.map(year =>
                    <React.Fragment key={year}>
                        <Typography variant="caption" sx={{pr: 1}}>{year}</Typography>
                        {monthNames.map((name, index) => {
                            const month = `${year}-${String(index + 1).padStart(2, "0")}`
                            const details = seen.get(month)
                            const isCurrent = month === currentMonth
                            const canJump = !!details && targetDays.has(month) && !isCurrent
                            const label = details ?
                                `${name} ${year}: ${details.nickname}, ${details.address}${details.platform ? `, ${details.platform}` : ""}` +
                                (targetDays.has(month) ? "" : " (no map data)") :
                                `${name} ${year}: not seen`
                            return (
                                <Tooltip key={month} title={label} disableInteractive>
                                    <span>
                                        <ButtonBase
                                            aria-label={details ? `Show ${month}` : `${month} not seen`}
                                            aria-current={isCurrent ? "date" : undefined}
                                            disabled={!canJump}
                                            onClick={() => jump(month)}
                                            sx={{
                                                width: "100%",
                                                aspectRatio: "1",
                                                borderRadius: "3px",
                                                bgcolor: details ? theme.palette.primary.main : theme.palette.action.disabledBackground,
                                                opacity: details && !targetDays.has(month) ? 0.5 : 1,
                                                outline: isCurrent ? `2px solid ${theme.palette.text.primary}` : undefined,
                                                outlineOffset: "1px",
                                            }}
                                        />
                                    </span>
                                </Tooltip>
                            )
                        })}
                    </React.Fragment>
                )}
            </Box>
        </Box>
    )
}
