import {ThemeOptions} from "@mui/material";

export const TorMapTheme: ThemeOptions = {
    palette: {
        mode: "dark",
    },
    components: {
        MuiTooltip: {
            styleOverrides: {
                tooltip: {
                    fontSize: ".85em",
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
        MuiFormControlLabel: {
            styleOverrides: {
                root: {
                    marginLeft: "0px"
                }
            }
        }
    },
}
