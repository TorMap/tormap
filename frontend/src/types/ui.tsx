import {Dialog, Slide} from "@mui/material";
import {styled} from '@mui/material/styles';
import {Statistics} from "./app-state";
import React from "react";
import {TransitionProps} from "@mui/material/transitions";

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

export interface UIProps {
    /**
     * A String array of available days available at the backend
     */
    availableDays: string[]

    /**
     * The value of currently selected day
     */
    sliderValue: number

    /**
     * A callback function to update the selected day
     * @param n the nth entry in availableDays array
     */
    setSliderValue: (n: number) => void

    /**
     * The currently map statistics of the currently rendered information
     */
    statistics?: Statistics
}

//animation for Dialog sliding in
export const Transition = React.forwardRef(function Transition(
    props: TransitionProps & {
        children: React.ReactElement;
    },
    ref: React.Ref<unknown>,
) {
    return <Slide direction="up" ref={ref} {...props} />;
});