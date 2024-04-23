Ext.define('NX.oauth2proxy.app.PluginConfig', {

    constructor: function () {
        window.plugins = window.plugins || [];

        window.plugins.push({
            id: 'nexus-oauth2-proxy-plugin',
            features: [
                {
                    mode: 'user',
                    path: '/oauth2proxy-apitoken',
                    view: window.ReactComponents.RedirectComponent,
                    text: 'OAuth2 Proxy API Token',
                    description: 'Access OAuth2 proxy API token',
                    iconCls: 'x-fa fa-key',
                    visibility: {
                        requiresUser: true
                    },
                }
            ],
        });
    }
});