import {Dialog} from "@mui/material";
import {styled} from '@mui/material/styles';

export enum SnackbarMessage {
    ConnectionFailed = "Connection failed! Maybe the server is being upgraded.",
    NoRelaysWithFlags = "There are no relays for the current settings!",
    NoFamilyData = "Currently we have no family information for this date!",
    UpdatedData = "New data needed to be displayed. Please try again now.",
    NoRelayDetails = "Currently we have no details about this relay for this month.",
}

export const FullHeightDialog = styled(Dialog)(() => ({
    paper: {
        height: '100%',
    },
}));
