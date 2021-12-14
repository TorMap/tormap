import {CircularProgress} from "@mui/material";
import React, {FunctionComponent} from "react";

/**
 * A Component for a loading Animation
 */
export const LoadingAnimation: FunctionComponent = () => {
    return <CircularProgress
        color={"inherit"}
        sx={{
            position: "fixed",
            left: "calc(50% - 25px)",
            top: "calc(50% - 25px)",
            margin: "auto",
            backgroundColor: "transparent",
            color: "rgba(255,255,255,.6)",
            zIndex: 1000,
        }}/>
}