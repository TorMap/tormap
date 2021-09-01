import {Dialog, withStyles} from "@material-ui/core";

export type SnackbarMessage = {
    message: string,
    severity: "error" | "warning" | "info" | "success",
}

export enum ErrorMessages {
    ConectionToBackendFailed = "Connection to backend server failed",
    NoRelaysWithFlags = "There are no relays with the filtered flags!",
}

export const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100vh',
    },
}))(Dialog);