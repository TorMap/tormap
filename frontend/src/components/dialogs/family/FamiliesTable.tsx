import React, {FunctionComponent} from "react";
import {Table, TableBody, TableCell, TableHead, TableRow, Typography} from "@mui/material";
import {RelayFamilyIdentifier} from "../../../dto/relay";
import {useSettings} from "../../../context/settings-context";

interface Props {
    /**
     * Family Identifiers
     */
    familyIdentifiers?: RelayFamilyIdentifier[]

    /**
     * Callback for closing the dialog
     */
    closeFamilySelectionDialog: () => void
}

/**
 * A Component to show a Table with Families to select one
 */
export const FamiliesTable: FunctionComponent<Props> = ({
                                                            familyIdentifiers,
                                                            closeFamilySelectionDialog,
                                                        }) => {
    const settings = useSettings().settings
    const setSettings = useSettings().setSettings

    return (
        <>
            {familyIdentifiers ? <Table size={"small"}>
                    <TableHead>
                        <TableRow>
                            <TableCell scope="row">
                                <Typography sx={{fontWeight: "bold",}}>Relay nicknames</Typography>
                            </TableCell>
                            <TableCell scope="" sx={{minWidth: "150px",}}>
                                <Typography sx={{fontWeight: "bold",}}>Member count</Typography>
                            </TableCell>
                            <TableCell scope="row">
                                <Typography sx={{fontWeight: "bold",}}>Autonomous Systems</Typography>
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {familyIdentifiers.map((family) =>
                            <TableRow
                                key={family.id}
                                onClick={() => {
                                    setSettings({...settings, selectedFamily: family.id})
                                    closeFamilySelectionDialog()
                                }}
                                hover={true}
                                sx={{
                                    "&:hover": {
                                        cursor: "pointer",
                                    }
                                }}
                            >
                                <TableCell scope="row">
                                    <Typography>{family.nicknames}</Typography>
                                </TableCell>
                                <TableCell scope="row" sx={{minWidth: "150px",}}>
                                    <Typography>{family.memberCount}</Typography>
                                </TableCell>
                                <TableCell scope="row">
                                    <Typography>
                                        {(family.autonomousSystems) ?
                                            (family.autonomousSystems) :
                                            "data not yet available"}
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
                : <p>We do not have any information about these families for this date.</p>}
        </>
    )
}