import {
    DialogContent,
    DialogTitle,
    IconButton,
    makeStyles,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography
} from "@material-ui/core";
import {NodeFamilyIdentifier} from "../types/responses";
import React, {useEffect, useState} from "react";
import CloseIcon from "@material-ui/icons/Close";
import {FullHeightDialog, SnackbarMessage} from "../types/ui";
import {backend} from "../util/util";
import {useSnackbar} from "notistack";

/**
 * Styles according to Material UI doc for components used in AppSettings component
 */
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
    tableRow: {
        "&:hover": {
            cursor: "pointer",
        }
    },
    tableHead: {
        fontWeight: "bold",
    },
}))

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
     * Trigger download of the current day
     */
    refreshDayData: () => void

    /**
     * Families which the user can view detailed information about
     */
    familyIds: number[]

    /**
     * Callback for setting a family as selected
     * @param f the family ID
     */
    familySelectionCallback: (f: number) => void
}

/**
 *
 * A Dialog to select a Family from multiple Families
 * @param showDialog - Whether the family selection dialog should be displayed
 * @param closeDialog - Event handler for closing the dialog
 * @param families - The familyIDs available to select
 * @param familySelectionCallback - the callback function for selecting a family
 */
export const FamilySelectionDialog: React.FunctionComponent<Props> = ({
                                                                          showDialog,
                                                                          closeDialog,
                                                                          refreshDayData,
                                                                          familyIds,
                                                                          familySelectionCallback,
                                                                      }) => {

    const [isLoading, setIsLoading] = useState(true)
    const [familyIdentifiers, setFamilyIdentifiers] = useState<NodeFamilyIdentifier[]>()

    const classes = useStyle()
    const { enqueueSnackbar } = useSnackbar();

    /**
     * Query more information about the Families specified in "families" parameter
     */
    useEffect(() => {
        setFamilyIdentifiers([])
        if (familyIds.length > 0) {
            setIsLoading(true)
            backend.post<NodeFamilyIdentifier[]>(
                '/relay/details/family/identifiers',
                familyIds
            ).then(response => {
                const identifiers = response.data
                if (identifiers.length > 0) {
                    setFamilyIdentifiers(response.data)
                } else {
                    closeDialog()
                    enqueueSnackbar(SnackbarMessage.UpdatedData, {variant: "success"})
                    refreshDayData()
                }
                setIsLoading(false)
            }).catch(() => {
                enqueueSnackbar(SnackbarMessage.ConnectionFailed, {variant: "error"})
                setIsLoading(false)
            })
        }
    }, [closeDialog, enqueueSnackbar, familyIds, refreshDayData])

    return (
        <FullHeightDialog
            open={showDialog}
            onClose={closeDialog}
            onBackdropClick={closeDialog}
            maxWidth={familyIds.length > 1 ? "lg" : "md"}
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
                                            <TableCell scope="row">
                                                <Typography className={classes.tableHead}>Relay nicknames</Typography>
                                            </TableCell>
                                            <TableCell scope="" className={classes.valueName}>
                                                <Typography className={classes.tableHead}>Member count</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography className={classes.tableHead}>Autonomous Systems</Typography>
                                            </TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {familyIdentifiers.map((family) =>
                                            <TableRow
                                                key={family.id}
                                                onClick={() => familySelectionCallback(family.id)}
                                                hover={true}
                                                className={classes.tableRow}
                                            >
                                                <TableCell scope="row">
                                                    <Typography>{family.nicknames}</Typography>
                                                </TableCell>
                                                <TableCell scope="row" className={classes.valueName}>
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
                                : <p>We do not have any information about these families for this date.</p>
                            : <p>loading...</p>}
                    </div>
                </DialogContent>
            </div>
        </FullHeightDialog>
    )
}
