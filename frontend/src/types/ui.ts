import {Dialog, withStyles} from "@material-ui/core";

export type SnackbarMessage = {
    message: string,
    severity: "error" | "warning" | "info" | "success",
}

export const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100vh',
    },
}))(Dialog);