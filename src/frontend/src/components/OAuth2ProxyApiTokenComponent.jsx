import Axios from 'axios';
import React from 'react';
import { faKey } from '@fortawesome/free-solid-svg-icons';

import {
    ContentBody,
    Page,
    PageHeader,
    PageTitle,
    Section,
    SectionFooter
} from '@sonatype/nexus-ui-plugin';

export default function OAuth2ProxyApiTokenComponent() {
    return <Page>
        <PageHeader>
            <PageTitle icon={faKey} text="OAuth2 Proxy API Token" description="Access your API token for non-interactive access"></PageTitle>
        </PageHeader>
        <ContentBody>
            <TokenSection />
        </ContentBody>
    </Page >
}

function TokenSection() {
    const [token, setToken] = React.useState('****************************************');
    const [resetFailed, setResetFailed] = React.useState(false);
    const [resetInProgress, setResetInProgress] = React.useState(false);
    const [tokenFreshlyReset, setTokenFreshlyReset] = React.useState(false);

    const resetToken = React.useCallback(async () => {
        if(resetInProgress) {
            console.log("Still resetting the token, not sending another request now");
        } else {
            setResetInProgress(true);
            Axios.post("/service/rest/oauth2-proxy/user/reset-token")
            .then(response => {
                setToken(response.data);
                setTokenFreshlyReset(true);
                setResetInProgress(false);
            })
            .catch(error => {
                setResetFailed(true);
                console.error('Failed to reset token:' + JSON.stringify(error.toJSON()));
                setResetInProgress(false);
            });
        }
    }, [resetInProgress])

    if(resetFailed) {
        return <Section>
            <p>⛔ Failed to generate a new access token</p>
        </Section>
    }

    if(tokenFreshlyReset) {
        return <Section>
            <p>✔ A new API token has been created. It is only displayed once. Store it in a safe place!</p>
            <p>⚠️ The old API token has been invalidated</p>
            <p>💡 Make sure no one was watching when displaying this token. If in doubt, just reset it once more.</p>
            <TokenFooter resetInProgress={resetInProgress} resetPressed={() => resetToken()} token={token}/>
        </Section>
    }

    return <Section>
        <p>⚠️ Your current API token is hidden. Click the button to generate a new token</p>
        <p>⚠️ When a new token is generated, the old one is invalidated immediately</p>
        <TokenFooter resetInProgress={resetInProgress} resetPressed={() => resetToken()} token={token}/>
    </Section>
}

function TokenFooter({resetInProgress, resetPressed, token}) {
    const buttonStyle = {
        marginRight: '1em',
        minWidth: '10em'
    }
    let buttonText = resetInProgress ? "Generating..." : "Regenerate Token"
    return <SectionFooter>
        <button disabled={resetInProgress} onClick={resetPressed} style={buttonStyle}>{buttonText}</button>
        <span>Your API token: {token}</span>
    </SectionFooter>
}