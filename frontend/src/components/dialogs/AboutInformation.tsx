import CloseIcon from "@mui/icons-material/Close";
import EmailIcon from '@mui/icons-material/Email';
import GitHubIcon from '@mui/icons-material/GitHub';
import InfoIcon from '@mui/icons-material/Info';
import TwitterIcon from '@mui/icons-material/Twitter';
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
import React, {useState} from "react";

import {ContributorCard} from "../card/ContributorCard";
import {BitcoinIcon} from "../icons/BitcoinIcon";
import {ExternalLink} from "../link/ExternalLink";

/**
 * A component for displaying information about TorMap
 */
export const AboutInformation: React.FunctionComponent = () => {
    // Component state
    const [showDialog, setShowDialog] = useState(false)

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
                onBackdropClick={() => setShowDialog(false)}
                fullScreen={!isLargeScreen}
            >
                <DialogTitle>
                    <Box display="flex" alignItems={"center"}>
                        <Avatar sx={{marginRight: "24px"}} src={"android-chrome-192x192.png"} alt={"TorMap logo"}/>
                        <Typography variant="h5">TorMap</Typography>
                        <Grid sx={{flexGrow: 1, paddingLeft: "24px", paddingRight: "30px", paddingTop: "8px"}} container
                              spacing={{xs: 2, sm: 4}}>
                            <Grid item>
                                <ExternalLink sx={{color: "white"}} href={"https://github.com/TorMap/tormap"}
                                              label={<GitHubIcon/>}/>
                            </Grid>
                            <Grid item>
                                <ExternalLink sx={{color: "white"}} href={"https://twitter.com/TorMapOrg"}
                                              label={<TwitterIcon/>}/>
                            </Grid>
                            <Grid item>
                                <ExternalLink sx={{color: "white"}} href={"mailto:hi@tormap.org"} label={<EmailIcon/>}/>
                            </Grid>
                            <Grid item>
                                <ExternalLink sx={{color: "white"}} href={"https://tippin.me/@TorMapOrg"}
                                              label={<BitcoinIcon/>}/>
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
                        and today.<br/> We developed TorMap for our practical P4-Project at the <ExternalLink
                        href={"https://www.tu-darmstadt.de/"} label={"Technical University of Darmstadt"}/> as part of
                        the <ExternalLink href={"https://panda-projekt.de/"} label={"PANDA project"}/>. It was
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
                        justifyContent="flex-start"
                        alignItems="flex-start"
                        spacing={{xs: 3, sm: 3}}
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
