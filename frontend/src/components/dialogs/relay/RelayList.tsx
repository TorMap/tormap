import React, {FunctionComponent} from "react";
import {Box, List, ListItem, ListItemIcon, ListItemText} from "@mui/material";
import {getIcon, RelayFamilyIcon} from "../../../types/icons";
import {calculateFamilyColor} from "../../../util/layer-construction";
import {RelayMatch} from "../../../types/relay";

interface Props {
    /**
     * Relay Identifiers
     */
    relayMatches: RelayMatch[]

    /**
     * ID of currently selected Relay
     */
    selectedRelay?: number

    /**
     * Setter for the relayDetailsId
     */
    setRelayDetailsId: (id: number) => void
}

/**
 * A List with Relays to select one
 * @param relays
 * @param relayMatches
 * @param selectedRelay
 * @param setRelayDetailsId
 * @constructor
 */
export const RelayList: FunctionComponent<Props> = ({
                                                        relayMatches,
                                                        selectedRelay,
                                                        setRelayDetailsId,
                                                    }) => {
    return (
        <List>
            {relayMatches.map((relayMatch) =>
                (relayMatch.id &&
                    <ListItem
                        key={relayMatch.id}
                        button={true}
                        selected={relayMatch.id === selectedRelay}
                        onClick={() => setRelayDetailsId(relayMatch.id)}
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