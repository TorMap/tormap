import {
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    Typography
} from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import React from "react";
import "./node-popup.scss"
import {Relay} from "../../types/relay";

interface Props{
    showNodePopup: boolean,
    setShowNodePopup: () => void,
    relay?: Relay,
}

export const NodePopup: React.FunctionComponent<Props> = ({
    showNodePopup,
    setShowNodePopup,
    relay,
}) => {
    return(
        <Dialog
            open={showNodePopup}
            onClose={setShowNodePopup}
            onBackdropClick={setShowNodePopup}
        >
            <DialogTitle
                className={"dialogfield"}
            >
                <Typography variant="h6">{relay?.nickname}</Typography>
                <IconButton aria-label="close" className={"closeButton"} onClick={setShowNodePopup}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent
                dividers
                className={"dialogfield"}
            >
                <Typography>
                    {JSON.stringify(relay)}
                </Typography>
            </DialogContent>
        </Dialog>
    );
}
