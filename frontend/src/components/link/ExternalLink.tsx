import {Link, SxProps, Theme} from "@mui/material";
import React, {FunctionComponent} from "react";

interface Props {
    href: string
    label: string | React.ReactNode
    /**
     * The accessible name of the link, required if the label is only an icon
     */
    ariaLabel?: string
    sx?: SxProps<Theme>
}

export const ExternalLink: FunctionComponent<Props> = ({href, label, ariaLabel, sx}) => {
    return (
        <Link sx={sx} href={href} target={"_blank"} rel={"noopener"} aria-label={ariaLabel}>{label}</Link>
    )
}

export default ExternalLink
