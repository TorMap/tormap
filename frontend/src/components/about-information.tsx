import React, {useState} from "react";
import {
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
    Paper,
    Typography
} from "@material-ui/core";
import GitHubIcon from '@material-ui/icons/GitHub';
import TwitterIcon from '@material-ui/icons/Twitter';
import CloseIcon from "@material-ui/icons/Close";
import panda from "../data/about-logos/panda-logo-300x113-white.png";
import LanguageIcon from '@material-ui/icons/Language';
import InfoIcon from '@material-ui/icons/Info';

/**
 * Styles according to Material UI doc for components used in AppSettings component
 */
const useStyle = makeStyles(() => ({
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
        width: "80%",
        padding: "10px",
        textAlign: "center",
    },
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
                    <Typography
                        variant="h6">About TorMap</Typography>
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
                        technical-, social- and philosophical since.
                        The goal of PANDA is to contribute on improving the fight against crime in the Darknet within
                        the
                        framework of civil security research, but without affecting its legitimate uses or even
                        anonymous communication as a whole.
                    </Typography>
                </DialogContent>
                <DialogContent
                    dividers
                >
                    <Grid container spacing={3}>
                        <Grid item xs={4}>
                            <Card elevation={20}>
                                <CardHeader
                                    title="Julius Henke"
                                />
                                <List dense={true}>
                                    <ListItem button component="a" href="https://juliushenke.com/" target={"_blank"}>
                                        <ListItemIcon>
                                            <LanguageIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Website
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem button component="a" href="https://github.com/JuliusHenke"
                                              target={"_blank"}>
                                        <ListItemIcon>
                                            <GitHubIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            GitHub
                                        </ListItemText>
                                    </ListItem>
                                    <ListItem button component="a" href="https://twitter.com/Julius_Henke"
                                              target={"_blank"}>
                                        <ListItemIcon>
                                            <TwitterIcon />
                                        </ListItemIcon>
                                        <ListItemText>
                                            Twitter
                                        </ListItemText>
                                    </ListItem>
                                </List>
                            </Card>
                        </Grid>
                        <Grid item xs={4}>
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
                        <Grid item xs={4}>
                            <Link href={"https://panda-projekt.de/"} target={"_blank"}>
                                <Paper elevation={20} className={classes.paper}>
                                    <img
                                        src={panda}
                                        alt={"https://panda-projekt.de/"}
                                        className={classes.image}
                                    />
                                </Paper>
                            </Link>
                        </Grid>
                    </Grid>
                </DialogContent>
                <DialogContent
                    dividers
                >
                    <Typography variant={"body2"}>This site includes IP2Location LITE data available from <Link
                        href={"https://www.ip2location.com/"} target={"_blank"}>https://www.ip2location.com/</Link> as
                        well as GeoJSON data from <Link href={"https://geojson-maps.ash.ms/"}
                                                        target={"_blank"}>https://geojson-maps.ash.ms/</Link>.</Typography>
                </DialogContent>
            </Dialog>
        </div>
    )

}
