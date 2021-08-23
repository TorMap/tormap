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
    Table, TableCell, TableRow,
    Tooltip,
    Typography,
    withStyles
} from "@material-ui/core";
import {DetailsInfo, NodeFamilyIdentifier} from "../types/responses";
import React, {useEffect, useState} from "react";
import CloseIcon from "@material-ui/icons/Close";
import {getIcon, Icon} from "../types/icons";

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
        height: '100%'
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
}

export const FamilyDetailsDialog: React.FunctionComponent<Props> = ({
                                                                        showDialog,
                                                                        closeDialog,
                                                                        families,
                                                                    }) => {

    const [isLoading, setIsLoading] = useState(true)
    const [familyDetails, setFamilyDetails] = useState<NodeFamilyIdentifier[]>()
    const [familyDetailsId, setFamilyDetailsId] = useState<string | undefined>()
    const [family, setFamily] = useState<NodeFamilyIdentifier | undefined>()
    const [detailsInfo, setDetailsInfo] = useState<DetailsInfo[] | undefined>()
    const classes = useStyle()

    useEffect(() => {
        setIsLoading(true)
        setFamilyDetails([])
        setFamilyDetailsId(undefined)
        //todo: fetch details
    }, [])

    // get data for selected family
    useEffect(() => {
        if (familyDetailsId && familyDetails) {
            let family = getFamily(familyDetails, familyDetailsId)
            if (!family) return setDetailsInfo(undefined)
            setFamily(family)
            setDetailsInfo([
                {name: "Family ID", value: family.id},
                {name: "Family members", value: family.memberCount},
                {name: "Autonomous System", value: family.autonmousSystems},
                {name: "Fingerprints", value: family.fingerprints},
                {name: "Nicknames", value: family.nicknames},
            ])
        }
    }, [familyDetailsId])

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
                {!isLoading && <DialogTitle className={families.length > 1 ? classes.infoPadding : undefined}>
                    <Typography
                        variant="h6">{familyDetailsId ? (familyDetailsId) : (`No information`)}</Typography>
                    <IconButton aria-label="close" className={classes.closeButton} onClick={closeDialog}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>}
                <DialogContent
                    dividers
                    className={families.length > 1 ? classes.infoPadding : undefined}>
                    <div>
                        {detailsInfo ?
                                <Table size={"small"}>
                                    {detailsInfo.map((row) =>
                                        row.value &&
                                        <TableRow>
                                            <TableCell scope="row" className={classes.valueName}>
                                                <Typography>{row.name}</Typography>
                                            </TableCell>
                                            <TableCell scope="row">
                                                <Typography>{row.value}</Typography>
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </Table>
                                : <p>We do not have any information about these families for this date.</p>}
                    </div>
                </DialogContent>
               {families.length > 1 && <Drawer
                    className={classes.drawer}
                    PaperProps={{
                        style: {
                            position: "absolute",
                            width: "250px",
                        }
                    }}
                    anchor={"left"}
                    variant={"permanent"}>
                    <List>
                        {familyDetails ? familyDetails.map((family) =>
                            (family.id &&
                                <Tooltip title={family.id} arrow={true} classes={{tooltip: classes.noMaxWidth}}>
                                    <div>
                                        <ListItem button key={family.id}
                                                  selected={family.id === familyDetailsId}
                                                  onClick={() => setFamilyDetailsId(family.id)}>
                                            <ListItemIcon>{getIcon(Icon.FamilyCount)}</ListItemIcon>
                                            <ListItemText primary={family.id + "" + family.memberCount}/>
                                        </ListItem>
                                    </div>
                                </Tooltip>)) : null}
                    </List>
                </Drawer>}
            </div>
        </FullHeightDialog>
    )
}