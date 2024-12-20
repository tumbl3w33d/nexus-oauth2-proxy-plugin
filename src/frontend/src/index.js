import OAuth2ProxyApiTokenComponent from './components/OAuth2ProxyApiTokenComponent';

window.plugins.push({
    id: 'nexus-oauth2-proxy-plugin',

    features: [{
        mode: 'user',
        path: '/oauth2proxy-apitoken',
        view: OAuth2ProxyApiTokenComponent,
        text: 'OAuth2 Proxy API Token',
        description: 'Access OAuth2 proxy API token',
        iconCls: 'x-fa fa-key',
        visibility: {
            requiresUser: true
        }
    }]
});

const toolbarXPath = '//div[contains(@class, "x-toolbar") and contains(@role, "group")]'
const loginButtonXPath = '//a[contains(@id, "signin")]'
const logoutButtonXPath = '//a[contains(@id, "signout")]'

function findFirstElementByXPath(xPath, searchRoot) {
    return document.evaluate(xPath, searchRoot, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

function waitForElement(xPath, searchRoot, elementNameForLogging) {
    elementNameForLogging = (typeof elementNameForLogging === 'undefined') ? xPath : elementNameForLogging;
    return new Promise(resolve => {
        let initElement = findFirstElementByXPath(xPath, searchRoot);
        if(initElement && initElement.checkVisibility()) {
            return resolve(initElement);
        }

        console.log(elementNameForLogging + " not yet visible.");
        const observer = new MutationObserver(mutations => {
            let obsElement = findFirstElementByXPath(xPath, searchRoot);
            if(obsElement && obsElement.checkVisibility()) {
                console.log("Observer found " + elementNameForLogging + ". Unregistering observer and resolving promise");
                observer.disconnect();
                resolve(obsElement)
            }
        });
        observer.observe(searchRoot, {attributes: false, childList:true, subtree: true})
        console.log("Waiting for " + elementNameForLogging + " to become visible...");
    });
}

function triggerPageReloadWhenLogoutButtonIsReplacedWithLoginButton(toolbar) {
    const toolbarObserver = new MutationObserver(mutations => {
        let loginButton = findFirstElementByXPath(loginButtonXPath, toolbar);
        if(loginButton && loginButton.checkVisibility()) {
            console.log("Observer found login button. Either this is on page load or logout was done. Re-Checking in 1 second to make sure...");
            window.setTimeout(() => {
                let stillLoginButton = findFirstElementByXPath(loginButtonXPath, toolbar);
                if(stillLoginButton && stillLoginButton.checkVisibility()) {
                    console.log("Login button still present. Assuming logout was done. Reloading page to retrigger oauth login...");
                    toolbarObserver.disconnect();
                    location.reload();
                } else {
                    console.log("Login button is gone now. Assuming page was still loading. Skipping page reload...");
                }
            }, 1000);
        }
    });
    toolbarObserver.observe(toolbar, {attributes: true, childList:true, subtree: true})
    console.log("Waiting for login button to become visible...");
}

waitForElement(toolbarXPath, document, "toolbar").then(toolbar => {
    waitForElement(logoutButtonXPath, toolbar, "logout button").then(logoutButton => {
            triggerPageReloadWhenLogoutButtonIsReplacedWithLoginButton(toolbar);
    });
});