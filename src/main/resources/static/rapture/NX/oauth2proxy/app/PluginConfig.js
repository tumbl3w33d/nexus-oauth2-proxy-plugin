Ext.define('NX.oauth2proxy.app.PluginConfig', {

    constructor: function () {
        // Ensure the plugins array is initialized
        window.plugins = window.plugins || [];

        window.plugins.push({
            id: 'nexus-oauth2-proxy-plugin',
            features: [
                {
                    mode: 'user',  // Or other appropriate mode as per your requirements
                    path: '/oauth2proxy-apitoken',
                    //view: 'RedirectComponent',  // Referencing the component by name which you registered globally
                    view: window.ReactComponents.RedirectComponent,
                    text: 'OAuth2 Proxy API Token',
                    description: 'Access OAuth2 proxy API token',
                    iconCls: 'x-fa fa-key',
                    visibility: {
                        requiresUser: true
                    },
                }
            ],
            // You may need to setup more configuration here as per your needs
        });
        console.log('OAuth2 Proxy PluginConfig initialized');
    }
});