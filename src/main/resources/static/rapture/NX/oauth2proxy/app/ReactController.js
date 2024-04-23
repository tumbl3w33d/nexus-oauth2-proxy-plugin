// Define your Plugin's ExtJS-based Controller
Ext.define('NX.oauth2proxy.app.ReactController', {
  extend: 'NX.app.Controller',

  views: [
    'NX.oauth2proxy.view.react.RedirectView'
  ],

  init: function () {
    this.control({
      'nx-oauth2proxy-redirect-view': {
        render: this.handleRender
      }
    });
  },

  handleRender: function () {
    console.log("React component has been rendered");
    // You could potentially invoke the refresh here or manage other state changes.
    // this.getRedirectView().refresh();
  }
});