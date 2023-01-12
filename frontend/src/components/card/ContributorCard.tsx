import * as React from 'react';
import {FunctionComponent, ReactNode} from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import {Chip, Stack} from "@mui/material";
import ExternalLink from "../link/ExternalLink";

interface Props {
    name: string
    avatar: ReactNode
    roles: string[]
    website?: string
}

export const ContributorCard: FunctionComponent<Props> = ({name, avatar, roles, website}) => {
    return (
        <Card sx={{minWidth: 250}}>
            <CardContent>
                <Typography sx={{margin: "8px"}} variant="h5" component="div">
                    <Stack direction="row"
                           justifyContent="flex-start"
                           alignItems="center"
                           spacing={2}>
                        {avatar} {website ? <ExternalLink href={website} label={name}/> : <span>{name}</span>}
                    </Stack>
                </Typography>
                {roles.map(role => <Chip key={role} label={role} variant="outlined"/>)}
            </CardContent>
        </Card>
    );
}
