import React, {FunctionComponent} from "react";
import {Box, List, ListItem, ListItemIcon, ListItemText} from "@mui/material";
import {getIcon, RelayFamilyIcon} from "../../../types/icons";
import {calculateFamilyColor} from "../../../util/layer-construction";
import {RelayMatch} from "../../../types/relay";

interface Props {
    /**
     * Relay matches
     */
    relayMatches: RelayMatch[]

    /**
     * ID of currently selected relay
     */
    selectedRelayId?: number

    /**
     * Setter for the currently selected relay
     */
    setSelectedRelayId: (id: number) => void
}

export const RelayList: FunctionComponent<Props> = ({
                                                        relayMatches,
                                                        selectedRelayId,
                                                        setSelectedRelayId,
                                                    }) => {
    return (
        <List>
            {relayMatches.map((relayMatch) =>
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
    )
}