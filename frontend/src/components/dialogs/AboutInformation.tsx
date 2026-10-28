import {Close as CloseIcon, Email as EmailIcon, GitHub as GitHubIcon, Info as InfoIcon, Twitter as TwitterIcon} from "@mui/icons-material";
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
    Stack,
    Typography,
    useMediaQuery,
    useTheme
} from "@mui/material";
import {atom, useAtom} from "jotai";
import React from "react";

import {ContributorCard} from "../card/ContributorCard";
import {ExternalLink} from "../link/ExternalLink";
import BitcoinCard from "../card/BitcoinCard";
import {isFullscreenSupported} from "../../util/fullscreen";
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
    ...(isFullscreenSupported() ? [["F", "Full screen showing only the map, or back"] as [string, string]] : []),
    ["?", "Show this information"],
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
                        TorMap is a world map displaying approximate locations where Tor relays are being hosted.
                        You can group, filter and analyze thousands of Tor relays, which route anonymous internet
                        traffic daily. The historic state of the network can be viewed for any day between October 2007
                        and today.<br/> Initially TorMap was developed by us at the <ExternalLink
                        href={"https://www.tu-darmstadt.de/"} label={"Technical University of Darmstadt"}/> as part of
                        the <ExternalLink href={"https://panda-projekt.de/"} label={"PANDA project"}/>. The project was
                        supervised
                        by <ExternalLink href={"mailto:florian.platzer@sit.fraunhofer.de"}
                                         label={"Florian Platzer"}/> from <ExternalLink
                        href={"https://www.sit.fraunhofer.de/"} label={"Frauenhofer SIT"}/>.
                    </Typography>
                    <h2>How do we get our data?</h2>
                    <Typography variant={"body1"} gutterBottom>
                        The nonprofit organization <ExternalLink href={"https://www.torproject.org/"}
                                                                 label={"TorProject"}/> already provides a
                        large <ExternalLink href={"https://metrics.torproject.org/collector.html"}
                                            label={"archive"}/> with raw historic data about the
                        network. We regularly process these so called &quot;descriptors&quot; and lookup IPv4 addresses
                        to get geo
                        locations and autonomous systems. If you are interested in our implementation, check out the
                        open source repository on <ExternalLink href={"https://github.com/TorMap/tormap"}
                                                                label={"GitHub"}/>.<br/>
                        The location and ownership of IP ranges can change over time. Since we
                        only use current IP data, some relays might have been hosted somewhere else, than displayed on
                        our world map. The location is also not accurate to the street level but rather an approximation
                        of the city.
                    </Typography>
                    <h2>Contributors</h2>
                    <Stack
                        direction={{sm: 'column', md: 'row'}}
                        spacing={{xs: 3, sm: 3}}
                        sx={{justifyContent: "flex-start", alignItems: "flex-start"}}
                    >
                        <ContributorCard
                            avatar={<Avatar src={"https://avatars.githubusercontent.com/u/23460202?s=96&v=4"}/>}
                            name={"Julius Henke"}
                            website={"https://juliushenke.com"}
                            roles={["Maintainer"]}
                        />
                        <ContributorCard
                            avatar={<Avatar src={"https://avatars.githubusercontent.com/u/32802490?s=96&v=4"}/>}
                            name={"Tim Kilb"}
                            website={"https://github.com/TimKilb"}
                            roles={["Developer"]}
                        />
                        <ContributorCard
                            avatar={<Avatar src={"/felix-krauspe.jpeg"}/>}
                            name={"Felix Krauspe"}
                            website={"https://www.linkedin.com/in/felix-krauspe-202bb9218/"}
                            roles={["Server Admin"]}
                        />
                    </Stack>
                    <h2>Donations</h2>
                    <Typography variant={"body1"} gutterBottom>
                        If you like TorMap and want to support us, you can donate to us via Bitcoin or other
                        cryptocurrencies. Your donation helps us to cover server costs and keep the project running ❤️
                    </Typography>
                    <BitcoinCard/>
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
                    <Typography variant={"body2"} sx={{mb: 2}}>
                        The day keys work while nothing else is selected.
                    </Typography>
                    <Divider sx={{my: 2}}/>
                    <Typography variant={"body2"}>
                        We use IP geolocation data by <ExternalLink href={"https://db-ip.com"}
                                                                    label={"DB-IP"}/>{", "}
                        Autonomous System data by <ExternalLink href={"https://www.maxmind.com/"}
                                                                label={"MaxMind"}/>{" "}
                        and GeoJSON data from <ExternalLink href={"https://geojson-maps.ash.ms/"}
                                                            label={"https://geojson-maps.ash.ms/"}/>.
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
