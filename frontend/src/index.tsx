import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import "@mui/styles";
import "react-sliding-pane/dist/react-sliding-pane.css";
import {App} from "./components/App";
import {createTheme, ThemeProvider} from "@mui/material";
import {SnackbarProvider} from "notistack";
import {TorMapTheme} from "./types/TorMapTheme";
import {SettingsProvider} from "./context/settings-context";
import {defaultSettings} from "./config";
import {DateProvider} from "./context/date-context";
import {StatisticsProvider} from "./context/statistics-context";

const theme = createTheme(TorMapTheme)

ReactDOM.render(
    <React.StrictMode>
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
    </React.StrictMode>,
    document.getElementById('root')
);
