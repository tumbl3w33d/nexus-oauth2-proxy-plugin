import React from 'react';

function RedirectComponent() {
    React.useEffect(() => {
        window.location.href = '/apitoken';
    }, []);

    return <div>Redirecting...</div>;
}


if (typeof window !== 'undefined') {
    window.ReactComponents = window.ReactComponents || {};
    window.ReactComponents.RedirectComponent = RedirectComponent;
}

export default RedirectComponent;