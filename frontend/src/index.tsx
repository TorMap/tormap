import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import "react-sliding-pane/dist/react-sliding-pane.css";
import {App} from "./components/app";
import {createTheme, ThemeProvider} from "@mui/material";
import {SnackbarProvider} from "notistack";
import {TorMapTheme} from "./types/MuiTheme";
import {SettingsProvider} from "./util/SettingsContext";
import {defaultSettings} from "./util/config";

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
                    <App/>
                </SettingsProvider>
            </SnackbarProvider>
        </ThemeProvider>
    </React.StrictMode>,
    document.getElementById('root')
);
