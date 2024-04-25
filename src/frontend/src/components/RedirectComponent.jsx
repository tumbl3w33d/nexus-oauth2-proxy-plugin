import Axios from 'axios';
import React from 'react';

/* like nexus-public/components/nexus-ui-plugin/src/frontend/src/interface/ExtJS.js */
function checkPermission(permission) {
    return NX.Permissions.check(permission);
}

function RedirectComponent() {
    const [userId, setUserId] = React.useState(null);
    const [token, setToken] = React.useState('');
    const [error, setError] = React.useState(false);

    React.useEffect(() => {
        (async () => {
            try {
                const user = NX.State.getUser();
                if (user && user.id) {
                    setUserId(user.id);
                    // this only works with admin privileges
                    /*Axios.put("/service/rest/v1/security/users/" + encodeURIComponent(user.id) + "/change-password",
                        "new-secret-password",
                        {
                            headers: { 'Content-Type': 'text/plain' },
                        })*/
                    //console.log("url: /service/rest/v1/security/users/" + encodeURIComponent(user.id) + "/change-password");

                    // a redirect
                    //window.location.href = "/apitoken?userId=" + user.id;

                    // reset the token and show it to the user
                    const reponse = await Axios.post("/service/rest/oauth2-proxy-api-token/reset-token")
                    setToken(reponse.data);
                } else {
                    throw new Error('User not found');
                }
            } catch (e) {
                setError(true);
                console.error('Failed to fetch user:', e);
            }
        })();
    }, []);

    if (error) {
        return <div>Error fetching user data.</div>;
    }

    return <div>Here is the generated token for {userId}: {token}</div>;
}

if (typeof window !== 'undefined') {
    window.ReactComponents = window.ReactComponents || {};
    window.ReactComponents.RedirectComponent = RedirectComponent;
}

export default RedirectComponent;