import {Dialog, withStyles} from "@material-ui/core";

export type SnackbarMessage = {
    message: string
    severity: "error" | "warning" | "info" | "success"
}

export class SnackbarMessages {
    static ConnectionFailed: SnackbarMessage = {
        message: "Connection to backend server failed!",
        severity: "error"
    }
    static NoRelaysWithFlags: SnackbarMessage = {
        message: "There are no relays with the filtered flags!",
        severity: "warning"
    }
    static NoFamilyData: SnackbarMessage = {
        message: "There are no families for this date! This data might be available soon.",
        severity: "warning"
    }
    static NoRelayDetails: SnackbarMessage = {
        message: "Currently we do not have more information about this relay in this month.",
        severity: "warning"
    }
}

export const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100vh',
    },
}))(Dialog);
