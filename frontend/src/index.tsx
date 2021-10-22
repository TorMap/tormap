import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import "react-sliding-pane/dist/react-sliding-pane.css";
import {App} from "./components/app";
import {createMuiTheme, ThemeProvider} from "@material-ui/core";
import {SnackbarProvider} from "notistack";

const theme = createMuiTheme({
    palette: {
        type: "dark",
    },
    overrides: {
        MuiTooltip: {
            tooltip: {
                fontSize: ".85em",
            }
        },
        MuiLink: {
            root: {
                color: "rgba(255, 255, 255, 0.7)",
                fontSize: ".9em"
            }
        }
    }
})

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
                <App/>
            </SnackbarProvider>
        </ThemeProvider>
    </React.StrictMode>,
    document.getElementById('root')
);
