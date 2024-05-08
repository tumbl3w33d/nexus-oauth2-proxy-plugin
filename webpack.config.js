const path = require('path');

const TerserPlugin = require('terser-webpack-plugin');

module.exports = {
    mode: 'production',
    devtool: 'source-map',
    entry: './src/frontend/src',
    output: {
        filename: 'nexus-oauth2-proxy-bundle.js',
        path: path.resolve(__dirname, 'target', 'classes', 'static')
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-react'] // Ensure React JSX is appropriately transpiled
                    }
                }
            }
        ]
    },
    externals: {
        '@sonatype/nexus-ui-plugin': 'nxrmUiPlugin',
        axios: 'axios',
        react: 'react'
    },
    resolve: {
        extensions: ['.js', '.jsx']
    },
    optimization: {
        minimize: true,
        minimizer: [
            new TerserPlugin({
                terserOptions: {
                    sourceMap: true
                }
            })
        ]
    }
};
