import React from 'react';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import LightningIcon from '@mui/icons-material/FlashOn';
import WalletIcon from '@mui/icons-material/Wallet';
import {Stack} from '@mui/material';
import LaunchIcon from "@mui/icons-material/Launch";

export default function BitcoinCard() {
    const bitcoinAddress = import.meta.env.VITE_DONATION_BITCOIN_ADDRESS
    const nowpaymentsClientApiKey = import.meta.env.VITE_NOWPAYMENTS_CLIENT_API_KEY
    const nowpaymentsDonationUrl = `https://nowpayments.io/donation?api_key=${nowpaymentsClientApiKey}&source=lk_donation&medium=referral`

    const handleCopyAddress = () => {
        navigator.clipboard.writeText(bitcoinAddress);
    };

    const openInWallet = () => {
        window.open(`bitcoin:${bitcoinAddress}`, '_blank');
    };

    return (
        <Card sx={{margin: 'auto', mt: 2, display: 'inline-block'}}>
            <CardContent>
                <Typography gutterBottom variant="subtitle1" component="div">
                    Bitcoin
                </Typography>
                <Stack direction="row">
                    <Typography variant="body2" color="text.secondary">
                        {bitcoinAddress}
                    </Typography>
                    <IconButton size="small" aria-label="copy address" onClick={handleCopyAddress}>
                        <ContentCopyIcon fontSize="small"/>
                    </IconButton>
                </Stack>
            </CardContent>
            <CardActions>
                <Button size="small" startIcon={<WalletIcon/>} onClick={openInWallet}>
                    Open in Wallet
                </Button>
                <a href="https://tippin.me/@TorMapOrg" target="_blank" rel="noreferrer noopener">
                    <Button size="small" startIcon={<LightningIcon/>}>
                        Lightning
                    </Button>
                </a>
                <a href={nowpaymentsDonationUrl} target="_blank" rel="noreferrer noopener">
                    <Button size="small" startIcon={<LaunchIcon/>}>
                        Other Crypto
                    </Button>
                </a>
            </CardActions>
        </Card>
    );
}
