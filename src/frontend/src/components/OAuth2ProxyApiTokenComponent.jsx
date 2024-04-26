import Axios from 'axios';
import React from 'react';


function OAuth2ProxyApiTokenComponent() {
    const [token, setToken] = React.useState('');
    const [error, setError] = React.useState(false);

    React.useEffect(() => {
        (async () => {
            try {
                const reponse = await Axios.post("/service/rest/oauth2-proxy-api-token/reset-token");
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

    return <main class="nx-page-main"><div class="nxrm-page-header"><div class="nx-page-title"><h1 class="nx-h1 nx-feature-name"><svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="key" class="svg-inline--fa fa-key fa-w-16 nx-page-title__page-icon nx-icon" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path fill="currentColor" d="M512 176.001C512 273.203 433.202 352 336 352c-11.22 0-22.19-1.062-32.827-3.069l-24.012 27.014A23.999 23.999 0 0 1 261.223 384H224v40c0 13.255-10.745 24-24 24h-40v40c0 13.255-10.745 24-24 24H24c-13.255 0-24-10.745-24-24v-78.059c0-6.365 2.529-12.47 7.029-16.971l161.802-161.802C163.108 213.814 160 195.271 160 176 160 78.798 238.797.001 335.999 0 433.488-.001 512 78.511 512 176.001zM336 128c0 26.51 21.49 48 48 48s48-21.49 48-48-21.49-48-48-48-48 21.49-48 48z"></path></svg><span>OAuth2 Proxy API Token</span></h1><p class="nx-page-title__description nx-feature-description">Generate an API token for non-interactive access</p></div></div><div class="nxrm-content-body"><div class="nxrm-section nx-tile"><div class="nx-tile-content"><p>A new API token has been created. It is only displayed once. Store it in a safe place!</p><p>⚠️ The next time you visit this page, you will automatically reset the token again.</p><p>💡 Make sure no one was watching when displaying this token. If in doubt, just reset it once more.</p><div class="nx-footer"><span>Your API token: {token}</span></div></div></div></div></main>;
}

if (typeof window !== 'undefined') {
    window.ReactComponents = window.ReactComponents || {};
    window.ReactComponents.OAuth2ProxyApiTokenComponent = OAuth2ProxyApiTokenComponent;
}

export default OAuth2ProxyApiTokenComponent;