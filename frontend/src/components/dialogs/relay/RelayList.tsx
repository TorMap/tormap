import {Box, CircularProgress, List, ListItemButton, ListItemIcon, ListItemText} from "@mui/material";
import {FunctionComponent, useEffect, useRef, useState} from "react";

import {RelayFamilyIcon, RelayTypeIcon} from "../../../types/icons";
import {RelayIdentifierMatch} from "../../../types/relay";
import {calculateFamilyColor} from "../../../util/layer-construction";

interface Props {
    /**
     * Relay matches
     */
    relayMatches: RelayIdentifierMatch[]

    /**
     * ID of currently selected relay
     */
    selectedRelayId?: number

    /**
     * Setter for the currently selected relay
     */
    setSelectedRelayId: (id: number) => void
}

const MATCHES_PER_SCROLL = 50

export const RelayList: FunctionComponent<Props> = ({
                                                        relayMatches,
                                                        selectedRelayId,
                                                        setSelectedRelayId,
                                                    }) => {
    // Component state
    const [numberOfMatchesToDisplay, setNumberOfMatchesToDisplay] = useState(MATCHES_PER_SCROLL)

    // Start again with the first batch when the matches change, e.g. after a search
    const [syncedRelayMatches, setSyncedRelayMatches] = useState(relayMatches)
    if (syncedRelayMatches !== relayMatches) {
        setSyncedRelayMatches(relayMatches)
        setNumberOfMatchesToDisplay(MATCHES_PER_SCROLL)
    }

    const hasMore = numberOfMatchesToDisplay < relayMatches.length
    const loaderRef = useRef<HTMLDivElement>(null)

    // Load the next batch once the loader at the end of the list scrolls into view
    useEffect(() => {
        const loader = loaderRef.current
        if (!hasMore || !loader) return
        const observer = new IntersectionObserver((entries) => {
            if (entries.some(entry => entry.isIntersecting)) {
                setNumberOfMatchesToDisplay(count => count + MATCHES_PER_SCROLL)
            }
        })
        observer.observe(loader)
        return () => observer.disconnect()
    }, [hasMore, numberOfMatchesToDisplay])

    return (
        <Box>
            {relayMatches.length > 0 ?
                <List>
                    {relayMatches.slice(0, numberOfMatchesToDisplay).map(relayMatch =>
                        (relayMatch.id &&
                            <ListItemButton
                                key={relayMatch.id}
                                selected={relayMatch.id === selectedRelayId}
                                onClick={() => setSelectedRelayId(relayMatch.id)}
                            >
                                <ListItemText primary={relayMatch.nickname}/>
                                <ListItemIcon sx={{minWidth: "70px"}}>
                                    <RelayTypeIcon relayType={relayMatch.relayType}/>
                                    {relayMatch.familyId &&
                                        <Box sx={{color: calculateFamilyColor(relayMatch.familyId), ml: 2}}>
                                            {RelayFamilyIcon}
                                        </Box>
                                    }
                                </ListItemIcon>
                            </ListItemButton>
                        )
                    )}
                </List>
                : <Box sx={{textAlign: "center", p: "16px"}}>No results found</Box>}
            {hasMore &&
                <Box ref={loaderRef} sx={{textAlign: "center"}}><CircularProgress
                    color={"inherit"}
                    sx={{
                        backgroundColor: "transparent",
                        color: "rgba(255,255,255,.6)",
                        zIndex: 1000,
                    }}/></Box>}
        </Box>
    )
}
