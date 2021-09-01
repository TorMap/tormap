import {Dialog, withStyles} from "@material-ui/core";

export type SnackbarMessage = {
    message: string,
    severity: "error" | "warning" | "info" | "success",
}

export enum SnackbarMessages {
    ConnectionFailed = "Connection to backend server failed!",
    NoRelaysWithFlags = "There are no relays with the filtered flags!",
    NoFamilyData = "There are no families for this date! This data might be available soon.",
}

export const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100vh',
    },
}))(Dialog);
