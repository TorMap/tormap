import React, {FunctionComponent} from "react";
import {List, ListItem, ListItemIcon, ListItemText} from "@mui/material";
import {getIcon} from "../../types/icons";
import {RelayMatch} from "./RelayDetailsDialogLarge";

interface Props {
    /**
     * Relay Identifiers
     */
    relayMatches: RelayMatch[]

    /**
     * ID of currently selected Relay
     */
    relayDetailsId?: number

    /**
     * Setter for the relayDetailsId
     */
    setRelayDetailsId: (id: number) => void
}

/**
 * A List with Relays to select one
 * @param relays
 * @param relayMatches
 * @param relayDetailsId
 * @param setRelayDetailsId
 * @constructor
 */
export const RelayList: FunctionComponent<Props> = ({
                                                        relayMatches,
                                                        relayDetailsId,
                                                        setRelayDetailsId,
                                                    }) => {
    return (
        <List>
            {relayMatches.map((relayMatch) =>
                (relayMatch.id &&
                        <ListItem
                            button={true}
                            selected={relayMatch.id === relayDetailsId}
                            onClick={() => setRelayDetailsId(relayMatch.id)}
                        >
                            <ListItemIcon>
                                {getIcon(relayMatch.relayType)}
                            </ListItemIcon>
                            <ListItemText primary={relayMatch.nickname}/>
                        </ListItem>
                )
            )}
        </List>
    )
}