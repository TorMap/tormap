import {ThemeOptions} from "@mui/material";

export const TorMapTheme: ThemeOptions = {
    palette: {
        mode: "dark",
        background: {
            //paper: "#424242",
            //default: "#424242",
        },
    },
    components: {
        MuiTooltip: {
            styleOverrides: {
                tooltip: {
                    fontSize: ".85em",
                }
            }
        },
        MuiLink: {
            styleOverrides: {
                root:{
                    color: "rgba(255, 255, 255, 0.7)",
                    fontSize: ".9em",
                }
            }
        },
        MuiAccordionDetails: {
            styleOverrides: {
                root: {
                    padding: "0px"
                }
            }
        },
        MuiFormControlLabel:{
            styleOverrides: {
                root: {
                    marginLeft: "0px"
                }
            }
        }
    },
}