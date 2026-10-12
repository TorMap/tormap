import {Dialog, Slide} from "@mui/material";
import {styled} from '@mui/material/styles';
import {TransitionProps} from "@mui/material/transitions";
import React from "react";

export enum SnackbarMessage {
    ConnectionFailed = "Connection failed! Maybe the server is being upgraded.",
    NoRelaysWithFlags = "There are no relays for the current settings!",
    NoFamilyData = "Currently we have no family information for this date!",
    UpdatedData = "New data needed to be displayed. Please try again now.",
    NoRelayDetails = "Currently we have no details about this relay for this month.",
    HistoricDataProcessing = "Currently historic data is being processed.",
    CountryUsageUnavailable = "The estimated number of users per country is currently not available.",
    CountryBordersFailed = "Could not load the country borders. Please try again.",
}

export const FullHeightDialog = styled(Dialog)(() => ({
    paper: {
        height: '70vh',
    },
}));

export const SlideUpTransition = React.forwardRef(function Transition(
    props: TransitionProps & {
        children: React.ReactElement;
    },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="up" ref={ref} {...props}/>;
});

export const SlideLeftTransition = React.forwardRef(function Transition(
    props: TransitionProps & {
        children: React.ReactElement;
    },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="left" ref={ref} {...props}/>;
});