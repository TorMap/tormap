import React, {useState} from "react";
import {
    Avatar,
    Box,
    Button,
    Card,
    CardHeader,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle, Divider,
    Grid,
    IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Typography,
    useMediaQuery,
    useTheme
} from "@mui/material";
import GitHubIcon from '@mui/icons-material/GitHub';
import EmailIcon from '@mui/icons-material/Email';
import TwitterIcon from '@mui/icons-material/Twitter';
import CloseIcon from "@mui/icons-material/Close";
import LanguageIcon from '@mui/icons-material/Language';
import InfoIcon from '@mui/icons-material/Info';
import TorMapLogo from "../../resources/logo.png";
import {ExternalLink} from "../link/ExternalLink";
import {BitcoinIcon} from "../icons/BitcoinIcon";

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
                        <Avatar sx={{marginRight: "10px"}} src={TorMapLogo} alt={"TorMap logo"}/>
                        <Typography variant="h6">TorMap</Typography>

                    </Box>
                    <IconButton aria-label="close" sx={{
                        position: "absolute",
                        right: "10px",
                        top: "10px",
                    }} onClick={() => setShowDialog(false)}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent
                    dividers
                >
                    <Typography variant={"h6"}>What can I do here?</Typography>
                    <Typography variant={"body1"} gutterBottom>
                        TorMap is a world map displaying approximate locations where Tor relays are being hosted. The
                        Tor network currently consists of thousands of relays which route anonymous internet traffic
                        daily. With our app you can group, filter and analyze Tor relays. The historic state of the
                        network can be viewed for any day between October 2007 and today.<br/>
                        We developed TorMap for our practical P4-Project at the <ExternalLink
                        href={"https://www.tu-darmstadt.de/"} label={"Technical University of Darmstadt"}/> as part of
                        the <ExternalLink href={"https://panda-projekt.de/"} label={"PANDA project"}/>. It was
                        supervised
                        by <ExternalLink href={"mailto:florian.platzer@sit.fraunhofer.de"}
                                         label={"Florian Platzer"}/> from <ExternalLink
                        href={"https://www.sit.fraunhofer.de/"} label={"Frauenhofer SIT"}/>.
                    </Typography>
                    <Typography variant={"h6"}>How do we get our data?</Typography>
                    <Typography variant={"body1"} gutterBottom>
                        The nonprofit organization <ExternalLink href={"https://www.torproject.org/"}
                                                                 label={"TorProject"}/> already provides a
                        large <ExternalLink href={"https://metrics.torproject.org/collector.html"}
                                                               label={"archive"}/> with raw historic data about the
                        network. We regularly process these so called "descriptors" and lookup IPv4 addresses to get geo
                        locations and autonomous systems. If you are interested in our implementation, check out the
                        open source repository on <ExternalLink href={"https://github.com/TorMap/tormap"}
                                         label={"GitHub"}/>.<br/>
                        The location and ownership of IP ranges can change over time. Since we
                        only use current IP data, some relays might have been hosted somewhere else, than displayed on
                        our world map. The location is also not accurate to the street level but rather an approximate of the city.
                    </Typography>
                    <Grid container spacing={3} sx={{
                        paddingTop: "7px",
                    }}>
                        <Grid item xs={12} sm={4}>
                            <Card elevation={20}>
                                <CardHeader
                                    title="TorMap"
                                />
                                <List dense={true}>
                                    <ListItem
                                        button
                                        component="a"
                                        href={"https://github.com/TorMap/tormap"}
                                        target={"_blank"}
                                        rel={"noopener"}
                                    >
                                        <ListItemIcon>
                                            <GitHubIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Github
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem button component="a" href={"mailto:hi@tormap.org"}
                                              target={"_blank"}>
                                        <ListItemIcon>
                                            <EmailIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Email
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem
                                        button component="a"
                                        href={"https://tippin.me/@Julius_Henke"}
                                        target={"_blank"}
                                        rel={"noopener"}
                                    >
                                        <ListItemIcon>
                                            <BitcoinIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Donate
                                        </ListItemText>
                                    </ListItem>
                                </List>
                            </Card>
                        </Grid>
                        <Grid item xs={12} sm={4}>
                            <Card elevation={20}>
                                <CardHeader
                                    title="Julius Henke"
                                />
                                <List dense={true}>
                                    <ListItem
                                        button
                                        component="a"
                                        href="https://github.com/JuliusHenke"
                                        target={"_blank"}
                                        rel={"noopener"}
                                    >
                                        <ListItemIcon>
                                            <GitHubIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            GitHub
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem
                                        button
                                        component="a"
                                        href="https://juliushenke.com/"
                                        target={"_blank"}
                                        rel={"noopener"}
                                    >
                                        <ListItemIcon>
                                            <LanguageIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Website
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem
                                        button component="a"
                                        href="https://twitter.com/Julius_Henke"
                                        target={"_blank"}
                                        rel={"noopener"}
                                    >
                                        <ListItemIcon>
                                            <TwitterIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Twitter
                                        </ListItemText>
                                    </ListItem>
                                </List>
                            </Card>
                        </Grid>
                        <Grid item xs={12} sm={4}>
                            <Card elevation={20}>
                                <CardHeader
                                    title="Tim Kilb"
                                />
                                <List dense={true}>
                                    <ListItem
                                        button
                                        component="a"
                                        href="https://github.com/TimKilb"
                                        target={"_blank"}
                                        rel={"noopener"}
                                    >
                                        <ListItemIcon>
                                            <GitHubIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            GitHub
                                        </ListItemText>
                                    </ListItem>
                                </List>
                            </Card>
                        </Grid>
                    </Grid>
                    <Divider sx={{my: 2}}/>
                    <Typography variant={"body2"}>
                        We use IP geolocation data by <ExternalLink href={"https://db-ip.com"}
                                                                    label={"DB-IP"}/>{", "}
                        Autonomous System data by <ExternalLink href={"https://www.maxmind.com/"}
                                                                label={"MaxMind"}/>{" "}
                        and GeoJSON data from <ExternalLink href={"https://geojson-maps.ash.ms/"}
                                                            label={"https://geojson-maps.ash.ms/"}/>.
                    </Typography>
                    {process.env.REACT_APP_VERSION && <Typography variant={"body2"}>
                        This frontend version is based on TorMap <ExternalLink href={"https://github.com/TorMap/tormap/releases"} label={process.env.REACT_APP_VERSION}/>.
                    </Typography>}
                </DialogContent>
                {!isLargeScreen ? <DialogActions sx={{
                    position: "fixed",
                    bottom: 5,
                    right: 5,
                }}>
                    <Button
                        autoFocus
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
