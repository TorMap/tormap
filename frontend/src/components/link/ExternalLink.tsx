import {Link, SxProps, Theme} from "@mui/material";
import React, {FunctionComponent} from "react";

interface Props {
    href: string
    label: string | React.ReactNode
    sx?: SxProps<Theme>
}

export const ExternalLink: FunctionComponent<Props> = ({href,label, sx}) => {
    return (
        <Link sx={sx} href={href} target={"_blank"} rel={"noopener"}>{label}</Link>
    )
}

export default ExternalLink
