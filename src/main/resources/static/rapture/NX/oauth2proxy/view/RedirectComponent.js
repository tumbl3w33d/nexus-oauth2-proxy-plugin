(function () {
    console.log("Initializing RedirectComponent");
    window.ReactComponents = window.ReactComponents || {};

    window.ReactComponents.RedirectComponent = function () {
        if (!React || !React.useEffect) {
            console.error('React or React.useEffect is not defined.');
            return null;
        }

        React.useEffect(function () {
            window.location.href = '/apitoken'; // Ensure this URL is correct and reachable
        }, []);

        return null; // This component does not render anything visible
    };
})();