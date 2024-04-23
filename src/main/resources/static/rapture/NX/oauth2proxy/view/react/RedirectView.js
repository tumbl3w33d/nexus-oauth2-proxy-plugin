Ext.define('NX.oauth2proxy.view.react.RedirectView', {
    extend: 'Ext.Component',
    alias: 'widget.nx-oauth2proxy-redirect-view',

    listeners: {
        afterrender: function () {
            // Here you may inject your React component after this element is rendered
            ReactDOM.render(React.createElement(YourReactComponent), this.getEl().dom);
        },
        destroy: function () {
            ReactDOM.unmountComponentAtNode(this.getEl().dom);
        }
    }
});