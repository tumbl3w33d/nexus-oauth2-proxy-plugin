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
    const [token, setToken] = React.useState('');
    const [error, setError] = React.useState(false);

    React.useEffect(() => {
        (async () => {
            try {
                const reponse = await Axios.post("/service/rest/oauth2-proxy/user/reset-token");
                setToken(reponse.data);
            } catch (e) {
                setError(true);
                console.error('Failed to reset token:', e);
            }
        })();
    }, []);

    if (error) {
        return <div>Error fetching user data.</div>;
    }

    return <Page>
        <PageHeader>
            <PageTitle icon={faKey} text="OAuth2 Proxy API Token" description="Access your API token for non-interactive access"></PageTitle>
        </PageHeader>
        <ContentBody>
            <Section>
                <p>A new API token has been created. It is only displayed once. Store it in a safe place!</p>
                <p>⚠️ The next time you visit this page, you will automatically reset the token again.</p>
                <p>💡 Make sure no one was watching when displaying this token. If in doubt, just reset it once more.</p>
                <SectionFooter>Your API token: {token}</SectionFooter>
            </Section>
        </ContentBody>
    </Page >
}
