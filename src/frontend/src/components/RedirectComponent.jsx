import React from 'react';

function RedirectComponent() {
    React.useEffect(() => {
        window.location.href = '/apitoken';
    }, []);

    return <div>Redirecting...</div>;
}

// Making it available globally if the window object exists and can store React components
/*if (typeof window !== 'undefined') {
    window.ReactComponents = {
        ...window.ReactComponents,
        RedirectComponent  // Register the RedirectComponent globally
    };
}*/
if (typeof window !== 'undefined') {
    window.ReactComponents = window.ReactComponents || {};
    window.ReactComponents.RedirectComponent = RedirectComponent;
}

export default RedirectComponent;