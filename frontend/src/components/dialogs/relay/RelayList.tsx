import {Box, CircularProgress, List, ListItem, ListItemIcon, ListItemText} from "@mui/material";
import React, {FunctionComponent, useState} from "react";
import InfiniteScroll from "react-infinite-scroller";

import {getIcon, RelayFamilyIcon} from "../../../types/icons";
import {RelayIdentifierMatch} from "../../../types/relay";
import {calculateFamilyColor} from "../../../util/layer-construction";
import {LoadingAnimation} from "../../loading/LoadingAnimation";

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

    return (
        <InfiniteScroll
            pageStart={0}
            loadMore={() => setNumberOfMatchesToDisplay(numberOfMatchesToDisplay + MATCHES_PER_SCROLL)}
            hasMore={numberOfMatchesToDisplay < relayMatches.length}
            loader={<Box sx={{textAlign: "center"}}><CircularProgress
                color={"inherit"}
                sx={{
                    backgroundColor: "transparent",
                    color: "rgba(255,255,255,.6)",
                    zIndex: 1000,
                }}/></Box>}
            useWindow={false}
        >
            {relayMatches.length > 0 ?
                <List>
                    {relayMatches.slice(0, numberOfMatchesToDisplay + 1).map(relayMatch =>
                        (relayMatch.id &&
                            <ListItem
                                key={relayMatch.id}
                                button={true}
                                selected={relayMatch.id === selectedRelayId}
                                onClick={() => setSelectedRelayId(relayMatch.id)}
                            >
                                <ListItemText primary={relayMatch.nickname}/>
                                <ListItemIcon sx={{minWidth: "70px"}}>
                                    {getIcon(relayMatch.relayType)}
                                    {relayMatch.familyId &&
                                        <Box sx={{color: calculateFamilyColor(relayMatch.familyId), ml: 2}}>
                                            {RelayFamilyIcon}
                                        </Box>
                                    }
                                </ListItemIcon>
                            </ListItem>
                        )
                    )}
                </List>
                : <Box sx={{textAlign: "center"}}>No results found</Box>}
        </InfiniteScroll>
    )
}
