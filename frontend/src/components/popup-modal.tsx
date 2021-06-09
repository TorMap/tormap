import React from 'react';
import Popup from 'reactjs-popup';

interface Props {
    /**
     * Whether the popup should be shown or not
     */
    show: boolean
    /**
     * What to perform on popup close
     */
    onClose?: () => void
    /**
     * Content to display
     */
    content: string
}

export const PopupModal: React.FunctionComponent<Props> = ({show, onClose, content}) => (
    <Popup open={show} modal={true} onClose={onClose}>
            <span>{content}</span>
    </Popup>
);
