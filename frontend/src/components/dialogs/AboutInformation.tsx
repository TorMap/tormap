import {
    Close as CloseIcon,
    Email as EmailIcon,
    FavoriteBorder as FavoriteBorderIcon,
    GitHub as GitHubIcon,
    Info as InfoIcon,
    Twitter as TwitterIcon
} from "@mui/icons-material";
import {
    Avatar,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    Grid,
    IconButton,
    Typography,
    useMediaQuery,
    useTheme
} from "@mui/material";
import {atom, useAtom} from "jotai";
import React from "react";

import {ExternalLink} from "../link/ExternalLink";
import {searchShortcut} from "../../util/keyboard";

/**
 * Whether the about dialog is shown. It can also be opened with the "?" key.
 */
export const showAboutDialogAtom = atom(false)

/**
 * Pairs of keys and what they do, for the list of keyboard shortcuts
 */
const keyboardShortcuts = (): [string, string][] => [
    [`${searchShortcut().label} or /`, "Search relays"],
    ["← / →", "Previous or next day"],
    ["Page Down / Page Up", "One month back or forward"],
    ["Home / End", "First or latest day"],
    ["Esc", "Leave the map, so the arrow keys select days instead of moving the map"],
]

/**
 * A component for displaying information about TorMap
 */
export const AboutInformation: React.FunctionComponent = () => {
    // Component state
    const [showDialog, setShowDialog] = useAtom(showAboutDialogAtom)

    // App context
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))

    return (
        <>
            <Button onClick={() => setShowDialog(true)} aria-label={"About TorMap"}>
                <InfoIcon sx={{
                    position: "fixed",
                    top: "90px",
                    left: "10px",
                    color: "white",
                }} fontSize={"large"}/>
            </Button>
            <Dialog
                open={showDialog}
                fullWidth={true}
                maxWidth={"md"}
                onClose={() => setShowDialog(false)}
                fullScreen={!isLargeScreen}
            >
                <DialogTitle>
                    <Box sx={{display: "flex", alignItems: "center"}}>
                        <Avatar sx={{marginRight: "24px"}} src={"android-chrome-192x192.png"} alt={"TorMap logo"}/>
                        <Typography variant="h5">TorMap</Typography>
                        <Grid sx={{flexGrow: 1, paddingLeft: "24px", paddingRight: "30px", paddingTop: "8px"}} container
                              spacing={{xs: 2, sm: 4}}>
                            <Grid>
                                <ExternalLink sx={{color: "white"}} href={"https://github.com/TorMap/tormap"}
                                              ariaLabel={"TorMap on GitHub"} label={<GitHubIcon/>}/>
                            </Grid>
                            <Grid>
                                <ExternalLink sx={{color: "white"}} href={"https://twitter.com/TorMapOrg"}
                                              ariaLabel={"TorMap on Twitter"} label={<TwitterIcon/>}/>
                            </Grid>
                            <Grid>
                                <ExternalLink sx={{color: "white"}} href={"mailto:hi@tormap.org"}
                                              ariaLabel={"Email TorMap"} label={<EmailIcon/>}/>
                            </Grid>
                        </Grid>
                        <IconButton aria-label="close" sx={{
                            position: "absolute",
                            right: "10px",
                        }} onClick={() => setShowDialog(false)}>
                            <CloseIcon/>
                        </IconButton>
                    </Box>
                </DialogTitle>
                <DialogContent
                    dividers
                >
                    <h2>What is TorMap?</h2>
                    <Typography variant={"body1"} gutterBottom>
                        TorMap shows the approximate locations of the thousands of Tor relays, which route anonymous
                        internet traffic. You can view the network for any day since October 2007 and group the
                        relays by family, autonomous system or country, filter them and look up the details of each
                        relay.
                    </Typography>
                    <h2>Where does the data come from?</h2>
                    <Typography variant={"body1"} gutterBottom>
                        The <ExternalLink href={"https://www.torproject.org/"} label={"Tor Project"}/> keeps
                        an <ExternalLink href={"https://metrics.torproject.org/collector.html"} label={"archive"}/> of
                        the descriptors that relays and directory authorities publish. TorMap regularly processes them
                        and looks up the location and autonomous system of each IPv4 address.
                        The <ExternalLink href={"https://github.com/TorMap/tormap"} label={"source code"}/> is open.
                    </Typography>
                    <Typography variant={"body1"} gutterBottom>
                        Locations are accurate to about the city, not the street. Since the lookups use current IP
                        data, relays from past days might have been hosted somewhere else than shown.
                    </Typography>
                    <h2>Who is behind TorMap?</h2>
                    <Box sx={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 1.5,
                        py: 1,
                        px: 1.5,
                        mb: 1,
                        border: 1,
                        borderColor: "divider",
                        borderRadius: 2,
                    }}>
                        <Avatar sx={{width: 40, height: 40}} alt={""}
                                src={"https://avatars.githubusercontent.com/u/23460202?s=80&v=4"}/>
                        <Box>
                            <Typography variant={"body1"}>
                                <ExternalLink href={"https://juliushenke.com"} label={"Julius Henke"}/>
                            </Typography>
                            <Typography variant={"body2"} color={"text.secondary"}>
                                Active maintainer
                            </Typography>
                        </Box>
                    </Box>
                    <Typography variant={"body1"} gutterBottom>
                        Tim Kilb and Julius Henke initially developed TorMap at the <ExternalLink
                        href={"https://www.tu-darmstadt.de/"} label={"Technical University of Darmstadt"}/> as part
                        of the <ExternalLink href={"https://panda-projekt.de/"} label={"PANDA project"}/>, supervised
                        by Florian Platzer from <ExternalLink href={"https://www.sit.fraunhofer.de/"}
                                                              label={"Fraunhofer SIT"}/>. Felix Krauspe hosts and
                        administers the servers.
                    </Typography>
                    <h2>Support TorMap</h2>
                    <Typography variant={"body1"} gutterBottom>
                        TorMap is free, open source and has no ads. Sponsoring helps to cover the server costs and keep
                        the project running.
                    </Typography>
                    <Button
                        href={"https://github.com/sponsors/JuliusHenke"}
                        target={"_blank"}
                        rel={"noopener"}
                        variant={"outlined"}
                        startIcon={<FavoriteBorderIcon/>}
                        sx={{mt: 1}}
                    >
                        Sponsor on GitHub
                    </Button>
                    <Divider sx={{my: 2}}/>
                    <Typography variant={"body2"} component={"h2"} sx={{fontWeight: "bold"}}>
                        Keyboard shortcuts
                    </Typography>
                    <Box component={"dl"} sx={{
                        display: "grid",
                        gridTemplateColumns: "max-content 1fr",
                        columnGap: 2,
                        rowGap: 0.5,
                        mt: 1,
                        typography: "body2",
                        "& dd": {m: 0},
                    }}>
                        {keyboardShortcuts().map(([keys, action]) => (
                            <React.Fragment key={keys}>
                                <dt><kbd>{keys}</kbd></dt>
                                <dd>{action}</dd>
                            </React.Fragment>
                        ))}
                    </Box>
                    <Divider sx={{my: 2}}/>
                    <Typography variant={"body2"}>
                        IP geolocation data by <ExternalLink href={"https://db-ip.com"} label={"DB-IP"}/>,
                        autonomous system data by <ExternalLink href={"https://www.maxmind.com/"} label={"MaxMind"}/>,
                        Tor user estimates by <ExternalLink href={"https://metrics.torproject.org/"}
                                                            label={"Tor Metrics"}/> and country borders
                        from <ExternalLink href={"https://geojson-maps.ash.ms/"} label={"geojson-maps"}/>.
                    </Typography>
                    {APP_VERSION && <Typography variant={"body2"}>
                        This is TorMap version <ExternalLink href={"https://github.com/TorMap/tormap/releases"}
                                                             label={APP_VERSION}/>.
                    </Typography>}
                </DialogContent>
                {!isLargeScreen ? <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        onClick={() => setShowDialog(false)}
                        variant={"contained"}
                        size={"large"}
                    >
                        Back
                    </Button>
                </DialogActions> : null}
            </Dialog>
        </>
    )

}
