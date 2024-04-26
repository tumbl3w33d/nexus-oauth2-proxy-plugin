const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');

module.exports = {
    mode: 'production',
    entry: './src/components/OAuth2ProxyApiTokenComponent.jsx',
    output: {
        path: path.resolve(__dirname, '../../target/classes/static/rapture/'),
        filename: 'bundle.js',
    },
    module: {
        rules: [{
            test: /\.jsx?$/,
            exclude: /node_modules/,
            use: {
                loader: 'babel-loader',
                options: {
                    presets: ['@babel/preset-react']
                }
            }
        }]
    }
};

const { merge } = require('webpack-merge');
const commonConfig = {
    entry: {
        'nexus-oauth2-proxy-plugin': './src/components/OAuth2ProxyApiTokenComponent.jsx'
    },
    output: {
        filename: '[name].js',
        path: path.resolve(__dirname, '../../target/classes/static/rapture/'),
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
            },
            {
                test: /\.s?css$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader',
                    'sass-loader'
                ]
            }
        ]
    },
    plugins: [
        new MiniCssExtractPlugin({
            filename: '[name].css'
        })
    ],
    externals: {
        'react': 'react', // nexus exposes react in this global var
        'react-dom': 'ReactDOM'
    },
    resolve: {
        extensions: ['.js', '.jsx']
    }
};

const prodConfig = {
    mode: 'production',
    devtool: 'source-map',
    optimization: {
        minimize: true,
        minimizer: [
            new CssMinimizerPlugin(),
            new TerserPlugin({
                terserOptions: {
                    keep_fnames: true
                }
            })
        ]
    }
};

module.exports = merge(commonConfig, prodConfig);