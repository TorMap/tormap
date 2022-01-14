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
    DialogTitle,
    Grid,
    IconButton,
    Link,
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
import {Icon} from "@iconify/react";
import TorMapLogo from "../../resources/logo.png";

/**
 * A component for displaying information about TorMap
 */
export const AboutInformation: React.FunctionComponent = () => {
    //Variables for deciding between small and large dialogs
    const theme = useTheme()
    const isLargeScreen = useMediaQuery(theme.breakpoints.up("lg"))
    // AboutDialog specific variables
    const [showDialog, setShowDialog] = useState(false)

    return (
        <div>
            <Button onClick={() => setShowDialog(true)}>
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
                        We developed TorMap for our practical P4-Project at the <Link
                        href={"https://www.tu-darmstadt.de/"} target={"_blank"}>Technical University of
                        Darmstadt</Link> as part of the <Link href={"https://panda-projekt.de/"}
                                                              target={"_blank"}>PANDA project</Link>. It was supervised
                        by <Link
                        href={"mailto:florian.platzer@sit.fraunhofer.de"} target={"_blank"}>Florian
                        Platzer</Link> from <Link href={"https://www.sit.fraunhofer.de/"} target={"_blank"}>Frauenhofer
                        SIT</Link>.
                    </Typography>
                    <Typography variant={"h6"}>How do we get our data?</Typography>
                    <Typography variant={"body1"} gutterBottom>
                        The nonprofit organization <Link href={"https://www.torproject.org/"}
                                                         target={"_blank"}>TorProject</Link> already provides
                        a large <Link href={"https://metrics.torproject.org/collector.html"}
                                      target={"_blank"}>archive</Link> with raw historic data about the network.
                        We regularly process these so called "descriptors" and lookup IPv4 addresses to get geo
                        locations and
                        autonomous systems. If you are interested in our implementation, check out the open source
                        repository
                        on <Link href={"https://github.com/TorMap/tormap"}
                                 target={"_blank"}>GitHub</Link>.<br/>
                        The location and ownership of IP ranges can change over time. Since we
                        only use current IP data, some relays might have been hosted somewhere else, than displayed on
                        our world map.
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
                                    <ListItem button component="a" href={"https://github.com/TorMap/tormap"}
                                              target={"_blank"}>
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
                                    <ListItem button component="a" href={"https://tippin.me/@Julius_Henke"}
                                              target={"_blank"}>
                                        <ListItemIcon>
                                            <Icon icon="mdi:bitcoin" width={26}/>
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
                                    <ListItem button component="a" href="https://github.com/JuliusHenke"
                                              target={"_blank"}>
                                        <ListItemIcon>
                                            <GitHubIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            GitHub
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem button component="a" href="https://juliushenke.com/" target={"_blank"}>
                                        <ListItemIcon>
                                            <LanguageIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Website
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem button component="a" href="https://twitter.com/Julius_Henke"
                                              target={"_blank"}>
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
                                    <ListItem button component="a" href="https://github.com/TimKilb" target={"_blank"}>
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
                </DialogContent>
                <DialogContent
                    dividers
                >
                    <Typography variant={"body2"}>
                        We use IP geolocation data by <Link href={"https://db-ip.com"}
                                                            target={"_blank"}>DB-IP</Link>{", "}
                        Autonomous System data by <Link href={"https://www.maxmind.com/"}
                                                        target={"_blank"}>MaxMind</Link>{" "}
                        and GeoJSON data from <Link href={"https://geojson-maps.ash.ms/"}
                                                    target={"_blank"}>https://geojson-maps.ash.ms/</Link>.
                    </Typography>
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
                        endIcon={<CloseIcon/>}
                    >
                        close
                    </Button>
                </DialogActions> : null}

            </Dialog>
        </div>
    )

}
