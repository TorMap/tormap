import React, {FunctionComponent} from "react";
import {Link} from "@mui/material";

interface Props {
    href: string
    label: string
}

export const ExternalLink: FunctionComponent<Props> = ({href,label}) => {
    return (
        <Link href={href} target={"_blank"} rel={"noopener"}>{label}</Link>
    )
}

export default ExternalLink