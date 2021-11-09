import {Dialog, withStyles} from "@material-ui/core";

export enum SnackbarMessage {
    ConnectionFailed = "Connection failed! Maybe the server is being upgraded.",
    NoRelaysWithFlags = "There are no relays for the current settings!",
    NoFamilyData = "Currently we have no family information for this date!",
    UpdatedData = "New data needed to be displayed. Please try again now.",
    NoRelayDetails = "Currently we have no details about this relay for this month.",
}

export const FullHeightDialog = withStyles(() => ({
    paper: {
        height: '100%',
    },
}))(Dialog);
