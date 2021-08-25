import React, {useState} from "react";
import {
    Box, Button, Card, CardContent, CardHeader,
    Dialog,
    DialogContent,
    DialogTitle,
    Grid,
    IconButton,
    Link, List, ListItem, ListItemIcon, ListItemText,
    makeStyles,
    Paper,
    Typography
} from "@material-ui/core";
import GitHubIcon from '@material-ui/icons/GitHub';
import CloseIcon from "@material-ui/icons/Close";
import panda from "../data/about-logos/panda-logo-300x113-white.png";
import tud from "../data/about-logos/tud_logo-white.png";
import sit from "../data/about-logos/Fraunhofer_SIT_Logo.png";
import LanguageIcon from '@material-ui/icons/Language';
import InfoIcon from '@material-ui/icons/Info';


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
        height: "100%"
    },
    image: {
        display: "block",
        margin: "auto",
        width: "80%",
        padding: "10px",
    },
    paddingTop: {
        paddingTop: "20px",
    }
}))

export const AboutInformation: React.FunctionComponent = (

) => {
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
                    <Grid container spacing={3}>
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
                        <Grid item xs={4}>
                            <Link href={"https://www.sit.fraunhofer.de/"} target={"_blank"}>
                                <Paper elevation={20} className={classes.paper}>
                                    <img
                                        src={sit}
                                        alt={"https://www.sit.fraunhofer.de/"}
                                        className={classes.image}
                                        style={{paddingTop: "30px"}}
                                    />
                                </Paper>
                            </Link>
                        </Grid>
                        <Grid item xs={4}>
                            <Link href={"https://www.tu-darmstadt.de/"} target={"_blank"}>
                                <Paper elevation={20} className={classes.paper}>
                                    <img
                                        src={tud}
                                        alt={"https://www.tu-darmstadt.de/"}
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
                    <Typography variant={"body1"} gutterBottom>
                        The TorMap was the result of a P4-Projekt for Students at the Technische Universität Darmstadt as part of the PANDA-Projekt.
                    </Typography>
                    <Typography variant={"body1"} gutterBottom>
                        Goal was to implement an interactiv view on the historic data of the Tor-Network to get insights on the size and distribution of the network itself as well as to more information about relays that make up the network as a whole.
                    </Typography>
                    <Typography variant={"body2"} gutterBottom>
                        PANDA is a joint project by the Frauenhofer SIT and the TU Darmstadt which is funded/grant-aided by the BMBF.
                        The interdisciplinary team behind the project is researching the Darknet with a view on technical-, social- and philosophical sience.
                        Goal of PANDA is to contribute on improving the fight against crime in the Darknet within the framework of civil security research, but without affecting its legitimate uses or even anonymous communication as a whole.
                    </Typography>
                </DialogContent>
                <DialogContent
                    dividers
                >
                    <Typography variant={"body2"} gutterBottom>
                        The implementation of TorMap was done by the students Julius Henke and Tim Kilb from TU-Darmstadt with Florian Platzer as contact person from the PANDA-Project-Team.
                    </Typography>

                    <Grid container className={classes.paddingTop}>
                        <Grid item xs={2}></Grid>
                        <Grid item xs={3}>
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
                                    <ListItem button component="a" href="https://github.com/JuliusHenke" target={"_blank"}>
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
                        <Grid item xs={2}></Grid>
                        <Grid item xs={3}>
                            <Card elevation={20}>
                                <CardHeader
                                    title="Tim Kilb"
                                />
                                <List dense={true}>
                                    <ListItem button component="a" href="https://www.timkilb.de" target={"_blank"}>
                                        <ListItemIcon>
                                            <LanguageIcon/>
                                        </ListItemIcon>
                                        <ListItemText>
                                            Website
                                        </ListItemText>
                                    </ListItem>
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
                        <Grid item xs={2}></Grid>
                    </Grid>

                </DialogContent>
            </Dialog>
        </div>
    )

}