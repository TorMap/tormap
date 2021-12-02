import React, {FunctionComponent} from "react";
import {List, ListItem, ListItemIcon, ListItemText, Tooltip} from "@mui/material";
import {getIcon} from "../../types/icons";
import {getRelayType} from "../../util/aggregate-relays";
import {RelayLocationDto, RelayIdentifierDto} from "../../types/responses";

interface Props {
    /**
     * Relays which the user can view detailed information about
     */
    relays: RelayLocationDto[]

    /**
     * Relay Identifiers
     */
    relayIdentifiers: RelayIdentifierDto[]

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
 * @param relayIdentifiers
 * @param relayDetailsId
 * @param setRelayDetailsId
 * @constructor
 */
export const RelayList: FunctionComponent<Props> = ({
                                                        relays,
                                                        relayIdentifiers,
                                                        relayDetailsId,
                                                        setRelayDetailsId,
                                                    }) => {

    const findIcon = (identifier: RelayIdentifierDto) => {
        const relay = relays.find(
            (relay) => relay.detailsId === identifier.id
        )
        return relay ? getIcon(getRelayType(relay)) : null
    }

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
                            selected={identifier.id === relayDetailsId}
                            onClick={() => setRelayDetailsId(identifier.id)}
                        >
                            <ListItemIcon>
                                {findIcon(identifier)}
                            </ListItemIcon>
                            <ListItemText primary={identifier.nickname}/>
                        </ListItem>
                    </Tooltip>
                )
            )}
        </List>
    )
}