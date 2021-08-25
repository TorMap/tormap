import {
    CircularProgress,
    Dialog,
    DialogContent,
    DialogTitle,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    makeStyles,
    Table, TableBody, TableCell, TableHead, TableRow,
    Tooltip,
    Typography,
    withStyles
} from "@material-ui/core";
import {DetailsInfo, NodeFamilyIdentifier} from "../types/responses";
import React, {useEffect, useState} from "react";
import CloseIcon from "@material-ui/icons/Close";
import {getIcon, Icon} from "../types/icons";
import {apiBaseUrl} from "../util/Config";

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

const FullHeightDialog = withStyles((theme) => ({
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
     * Families which the user ca view detailed information about
     */
    families: number[]

    familySelectionCallback: (f: number) => void
}

export const FamilySelectionDialog: React.FunctionComponent<Props> = ({
                                                                        showDialog,
                                                                        closeDialog,
                                                                        families,
                                                                          familySelectionCallback
                                                                    }) => {

    const [isLoading, setIsLoading] = useState(true)
    const [familyDetails, setFamilyDetails] = useState<NodeFamilyIdentifier[]>()
    const classes = useStyle()

    useEffect(() => {
        setIsLoading(true)
        setFamilyDetails([])
        //todo: fetch details
        fetch(`${apiBaseUrl}/archive/node/family/identifiers`, {
            headers: {
                'Content-Type': 'application/json',
            },
            method: "post",
            body: JSON.stringify(families),
        })
            .then(response => response.json())
            .then(identifiers => {
                setFamilyDetails(identifiers)
                setIsLoading(false)
                console.log(families)
                console.log(identifiers)
            })
    }, [families])

    function getFamily(familyDetails: NodeFamilyIdentifier[], familyDetailsId: string): NodeFamilyIdentifier | undefined {
        return familyDetails.find((family) => family.id === familyDetailsId)
    }


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
                        {!isLoading ? familyDetails ?
                                <Table size={"small"}>
                                    <TableHead>
                                        <TableRow>
                                            <TableCell scope="" className={classes.valueName}>
                                                <Typography>Family members</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography>Autonomous System</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography>nicknames</Typography>
                                            </TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {familyDetails.map((family) =>
                                            <TableRow onClick={event => familySelectionCallback(+family.id)}>
                                                <TableCell scope="row" className={classes.valueName}>
                                                    <Typography>{family.memberCount}</Typography>
                                                </TableCell>
                                                <TableCell scope="row">
                                                    <Typography>{family.autonomousSystems}</Typography>
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