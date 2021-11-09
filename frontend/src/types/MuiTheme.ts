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
        MuiLink: {
            styleOverrides: {
                root:{
                    color: "rgba(255, 255, 255, 0.7)",
                    fontSize: ".9em",
                }
            }
        }
    },}