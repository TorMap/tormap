import './index.css';
import "@mui/styles";
import "react-sliding-pane/dist/react-sliding-pane.css";

import {createTheme, ThemeProvider} from "@mui/material";
import {SnackbarProvider} from "notistack";
import React from 'react';
import {createRoot} from "react-dom/client";

import {App} from "./components/App";
import {defaultSettings} from "./config";
import {DateProvider} from "./context/date-context";
import {SettingsProvider} from "./context/settings-context";
import {StatisticsProvider} from "./context/statistics-context";
import {TorMapTheme} from "./types/TorMapTheme";

const theme = createTheme(TorMapTheme)
const root = document.getElementById('root');
if (!root) {
    throw new Error('Root element not found');
}

createRoot(root).render(
    <ThemeProvider theme={theme}>
        <SnackbarProvider
            maxSnack={3}
            autoHideDuration={4000}
            variant={"info"}
            anchorOrigin={{vertical: "top", horizontal: "center"}}
            preventDuplicate={true}
        >
            <SettingsProvider defaultSettings={defaultSettings}>
                <DateProvider>
                    <StatisticsProvider>
                        <App/>
                    </StatisticsProvider>
                </DateProvider>
            </SettingsProvider>
        </SnackbarProvider>
    </ThemeProvider>
)
