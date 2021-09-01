import {
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    makeStyles,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
    withStyles
} from "@material-ui/core";
import {NodeFamilyIdentifier} from "../types/responses";
import React, {useEffect, useState} from "react";
import CloseIcon from "@material-ui/icons/Close";
import {apiBaseUrl} from "../util/config";

const useStyle = makeStyles(() => ({
    closeButton: {
        position: "absolute",
        right: "10px",
        top: "10px",
    },
    drawer: {
        width: "250px",
    },
    infoPadding: {
        paddingLeft: "270px",
    },
    valueName: {
        minWidth: "150px",
    },
    noMaxWidth: {
        maxWidth: "none",
    },
}))

const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100%',
    },
}))(Dialog);

interface Props {
    /**
     * Whether the modal should currently be visible
     */
    showDialog: boolean

    /**
     * Hide the modal
     */
    closeDialog: () => void

    /**
     * Families which the user can view detailed information about
     */
    families: number[]

    familySelectionCallback: (f: number) => void
}

/**
 * A Dialog to select a Family from multiple Families
 * @param showDialog
 * @param closeDialog
 * @param families - The familyIDs available
 * @param familySelectionCallback - the callback function for selecting a family
 */
export const FamilySelectionDialog: React.FunctionComponent<Props> = ({
                                                                        showDialog,
                                                                        closeDialog,
                                                                        families,
                                                                          familySelectionCallback
                                                                    }) => {

    const [isLoading, setIsLoading] = useState(true)
    const [familyIdentifiers, setFamilyIdentifiers] = useState<NodeFamilyIdentifier[]>()
    const classes = useStyle()

    /**
     * Query more information about the Families specified in "families" parameter
     */
    useEffect(() => {
        setFamilyIdentifiers([])
        if (families.length > 0) {
            setIsLoading(true)
            fetch(`${apiBaseUrl}/archive/node/family/identifiers`, {
                headers: {
                    'Content-Type': 'application/json',
                },
                method: "post",
                body: JSON.stringify(families),
            })
                .then(response => response.json())
                .then((identifiers: NodeFamilyIdentifier[]) => {
                    setFamilyIdentifiers(identifiers)
                    setIsLoading(false)
                    console.log(families)
                    console.log(identifiers)
                })
        }
    }, [families])

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={families.length > 1 ? "lg" : "md"}
            fullWidth={true}
        >
            <div>
                <DialogTitle>
                    <Typography
                        variant="h6">Select a family</Typography>
                    <IconButton aria-label="close" className={classes.closeButton} onClick={closeDialog}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent
                    dividers
                >
                    <div>
                        {!isLoading ? familyIdentifiers ?
                                <Table size={"small"}>
                                    <TableHead>
                                        <TableRow>
                                            <TableCell scope="" className={classes.valueName}>
                                                <Typography>Family members</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography>Autonomous Systems</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography>Relay Nicknames</Typography>
                                            </TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {familyIdentifiers.map((family) =>
                                            <TableRow
                                                key={family.id}
                                                onClick={() => familySelectionCallback(family.id)}
                                                hover={true}
                                            >
                                                <TableCell scope="row" className={classes.valueName}>
                                                    <Typography>{family.memberCount}</Typography>
                                                </TableCell>
                                                <TableCell scope="row">
                                                    <Typography>
                                                        {(family.autonomousSystems.length >= 0) ?
                                                            (family.autonomousSystems) :
                                                            "This data is not available yet."
                                                        }
                                                    </Typography>
                                                </TableCell>
                                                <TableCell scope="row">
                                                    <Typography>{family.nicknames}</Typography>
                                                </TableCell>
                                            </TableRow>
                                        )}
                                    </TableBody>
                                </Table>
                                : <p>We do not have any information about these families for this date.</p>
                        : <p>loading...</p>}
                    </div>
                </DialogContent>
            </div>
        </FullHeightDialog>
    )
}
