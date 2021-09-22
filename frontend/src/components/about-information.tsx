import React, {useState} from "react";
import {
    Avatar,
    Box,
    Button,
    Card,
    CardHeader,
    Dialog,
    DialogContent,
    DialogTitle,
    Grid,
    IconButton,
    Link,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    makeStyles,
    Typography
} from "@material-ui/core";
import GitHubIcon from '@material-ui/icons/GitHub';
import EmailIcon from '@material-ui/icons/Email';
import TwitterIcon from '@material-ui/icons/Twitter';
import CloseIcon from "@material-ui/icons/Close";
import LanguageIcon from '@material-ui/icons/Language';
import InfoIcon from '@material-ui/icons/Info';
import {Icon} from "@iconify/react";
import TorMapLogo from "../resources/logo.png";

/**
 * Styles according to Material UI doc for components used in AppSettings component
 */
const useStyle = makeStyles(() => ({
    logo: {
        marginRight: "10px",
    },
    about: {
        position: "fixed",
        top: "90px",
        left: "10px",
        color: "white",
    },
    closeButton: {
        position: "absolute",
        right: "10px",
        top: "10px",
    },
    paper: {
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        height: "100%",
    },
    image: {
        display: "block",
        margin: "auto",
        height: "25px",
        textAlign: "center",
    },
    iconLink: {
        marginTop: "5px",
        marginLeft: "15px",
    }
}))

/**
 * A component for displaying information about TorMap
 * @constructor
 */
export const AboutInformation: React.FunctionComponent = () => {
    const [showDialog, setShowDialog] = useState(false)
    const classes = useStyle()

    return (
        <div>
            <Button onClick={() => setShowDialog(true)}>
                <InfoIcon className={classes.about} fontSize={"large"}/>
            </Button>
            <Dialog open={showDialog} fullWidth={true} maxWidth={"md"} onBackdropClick={() => setShowDialog(false)}>
                <DialogTitle>
                    <Box display="flex" alignItems={"center"}>
                        <Avatar className={classes.logo} src={TorMapLogo} alt={"TorMap logo"} />
                        <Typography variant="h6">
                            About TorMap
                        </Typography>
                    </Box>
                    <IconButton aria-label="close" className={classes.closeButton} onClick={() => setShowDialog(false)}>
                        <CloseIcon/>
                    </IconButton>
                </DialogTitle>
                <DialogContent
                    dividers
                >
                    <Typography variant={"body1"} gutterBottom>
                        The Tor network currently consists of thousands of nodes which route anonymous internet traffic
                        daily. The nonprofit organization <Link href={"https://www.torproject.org/"}
                                                                target={"_blank"}>TorProject</Link> already provides
                        a large <Link href={"https://metrics.torproject.org/collector.html"}
                                      target={"_blank"}>archive</Link> with raw historic data about the network.<br/>
                        With our app TorMap we visualize, group and filter public Tor relays on a world
                        map. The state of the network can be viewed for any day between October 2007 and today. Getting
                        details like IP address, contact or Autonomous System info of a relay is as easy as selecting it
                        on the map.<br/>
                        The project was developed by two students for their practical P4-Projekt at the <Link
                        href={"https://www.tu-darmstadt.de/"} target={"_blank"}>Technical University of
                        Darmstadt</Link> as part of the <Link href={"https://panda-projekt.de/"}
                                                              target={"_blank"}>PANDA project</Link> and supervised
                        by <Link
                        href={"mailto:florian.platzer@sit.fraunhofer.de"} target={"_blank"}>Florian Platzer</Link>.<br/>
                        PANDA is a joint project by the <Link href={"https://www.sit.fraunhofer.de/"} target={"_blank"}>Frauenhofer
                        SIT</Link> and the TU
                        Darmstadt which is funded/grant-aided by the <Link href={"https://panda-projekt.de/"}
                                                                           target={"_blank"}>BMBF</Link>.
                        The interdisciplinary team behind the project is researching the Darknet with a view on
                        technical-, social- and philosophical science.
                        The goal of PANDA is to contribute on improving the fight against crime in the Darknet within
                        the
                        framework of civil security research, but without affecting its legitimate uses or even
                        anonymous communication as a whole.
                    </Typography>
                    <Grid container spacing={3}>
                        <Grid item xs={3}>
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
                        <Grid item xs={3}>
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
                        <Grid item xs={3}>
                            <Card elevation={20}>
                                <CardHeader
                                    title="Tim Kilb"
                                />
                                <List dense={true}>
                                    {/* TODO uncomment when ready*/}
                                    {/*<ListItem button component="a" href="https://www.timkilb.com" target={"_blank"}>*/}
                                    {/*    <ListItemIcon>*/}
                                    {/*        <LanguageIcon/>*/}
                                    {/*    </ListItemIcon>*/}
                                    {/*    <ListItemText>*/}
                                    {/*        Website*/}
                                    {/*    </ListItemText>*/}
                                    {/*</ListItem>*/}
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
                        This site includes IP2Location LITE data available from <Link
                        href={"https://www.ip2location.com/"}
                        target={"_blank"}>https://www.ip2location.com/</Link> as
                        well as GeoJSON data from <Link href={"https://geojson-maps.ash.ms/"}
                                                        target={"_blank"}>https://geojson-maps.ash.ms/</Link>.
                    </Typography>
                </DialogContent>
            </Dialog>
        </div>
    )

}
