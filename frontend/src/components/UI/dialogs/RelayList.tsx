import React, {FunctionComponent} from "react";
import {List, ListItem, ListItemIcon, ListItemText, Tooltip} from "@mui/material";
import {getIcon} from "../../../types/icons";
import {getRelayType} from "../../../util/aggregate-relays";
import {GeoRelayView, NodeIdentifier} from "../../../types/responses";

interface Props {
    /**
     * Relays which the user can view detailed information about
     */
    relays: GeoRelayView[]

    /**
     * Relay Identifiers
     */
    relayIdentifiers: NodeIdentifier[]

    /**
     * ID of currently selected Relay
     */
    nodeDetailsId?: number

    /**
     * Setter for nodeDetailsId
     */
    setNodeDetailsId: (id: number) => void
}

/**
 * A List with Relays to select one
 * @param relays
 * @param relayIdentifiers
 * @param nodeDetailsId
 * @param setNodeDetailsId
 * @constructor
 */
export const RelayList: FunctionComponent<Props> = ({
                                                        relays,
                                                        relayIdentifiers,
                                                        nodeDetailsId,
                                                        setNodeDetailsId,
                                                    }) => {
    return (
        <List>
            {relayIdentifiers.map((identifier) =>
                (identifier.id &&
                    <Tooltip
                        key={identifier.id}
                        title={identifier.fingerprint}
                        arrow={true}
                        sx={{maxWidth: "none",}}
                    >
                        <ListItem
                            button={true}
                            selected={identifier.id === nodeDetailsId}
                            onClick={() => setNodeDetailsId(identifier.id)}
                        >
                            <ListItemIcon>
                                {getIcon(getRelayType(relays.find(
                                    (relay) => relay.detailsId === identifier.id)
                                ))}
                            </ListItemIcon>
                            <ListItemText primary={identifier.nickname}/>
                        </ListItem>
                    </Tooltip>
                )
            )}
        </List>
    )
}