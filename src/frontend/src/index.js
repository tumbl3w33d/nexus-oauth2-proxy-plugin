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