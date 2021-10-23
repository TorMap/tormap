import {Dialog, withStyles} from "@material-ui/core";

export enum SnackbarMessage {
    ConnectionFailed = "Connection failed! Maybe the server is being upgraded.",
    NoRelaysWithFlags = "There are no relays with for the current settings!",
    NoFamilyData = "There are no families for this date! This data might be available soon.",
    NoRelayDetails = "Currently we do not have more information about this relay in this month.",
}

export const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100vh',
    },
}))(Dialog);
