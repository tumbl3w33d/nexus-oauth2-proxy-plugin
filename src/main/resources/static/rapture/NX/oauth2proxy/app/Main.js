Ext.define('NX.oauth2proxy.app.Main', {
    extend: 'Ext.app.Controller',

    init: function () {
        this.callParent(arguments);
        console.log('OAuth2 Proxy Plugin Initialized');

        // Plugin initialization logic here
        // For a simple redirect, most logic will be in the React component
    }
});