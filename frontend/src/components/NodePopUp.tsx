import {
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    makeStyles,
    Typography
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import React from "react";

interface Props{
    showNodePopup: boolean,
    setShowNodePopup: () => void,
    nodePopupContent: string,
}

export const NodePopUp: React.FunctionComponent<Props> = ({
    showNodePopup,
    setShowNodePopup,
    nodePopupContent,
}) => {
    const css= makeStyles({
        closeButton: {
            position: 'absolute',
            right: "10px",
            top: "10px",
            color: "grey",
        },
        dialogfield:{
            backgroundColor: "darkgray",
        }
    });
    const style = css();

    return(
        <Dialog
            open={showNodePopup}
            onClose={setShowNodePopup}
            onBackdropClick={setShowNodePopup}
        >
            <DialogTitle
                className={style.dialogfield}
            >
                <Typography variant="h6">Test Dialog</Typography>
                <IconButton aria-label="close" className={style.closeButton} onClick={setShowNodePopup}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent
                dividers
                className={style.dialogfield}
            >
                <Typography>
                    {nodePopupContent}
                </Typography>
            </DialogContent>
        </Dialog>
    );
}