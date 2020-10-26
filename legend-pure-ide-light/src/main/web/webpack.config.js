/**
 * Copyright 2020 Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const sass = require('sass');
const path = require('path');
const fs = require('fs');
const BaseConfig = require('./ide.config.json');
const HtmlWebPackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
const CircularDependencyPlugin = require('circular-dependency-plugin');
const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

const getJavascriptLoaderConfig = ({ isProcessingJSXFiles, isEnvDevelopment }) => ({
  loader: require.resolve('babel-loader'),
  options: {
    cacheDirectory: true,
    // NOTE: Plugin order matters: plugins (top -> bottom) then presets (bottom -> top)
    // See https://babeljs.io/docs/en/plugins/#plugin-options
    presets: [
      ['@babel/preset-env', { debug: false }], // use `debug` option to see the lists of plugins being selected
      ['@babel/preset-react', { development: isProcessingJSXFiles && isEnvDevelopment }], // `development` flag allows accurate source code location
      './dev/babel/preset-studio',
      ['@babel/preset-typescript', {
        // Allow using `declare` in class
        // NOTE: we have to explicit have this before other class modifier plugins like `@babel/plugin-proposal-class-properties`
        // `allowDeclareFields` will be true by default in babel 8
        // See https://babeljs.io/docs/en/babel-preset-typescript#allowdeclarefields
        onlyRemoveTypeImports: true,
        allowDeclareFields: true,
        // We have to enable `isTSX` where appropriate, if not JSX syntax will be mistaken for TS type annotation
        // See https://babeljs.io/docs/en/babel-preset-typescript#istsx
        isTSX: isProcessingJSXFiles,
        allExtensions: isProcessingJSXFiles
      }],
    ],
    plugins: [
      // This plugin provides `react-refresh` capability, but it MUST be DISABLED for PROD
      // NOTE: as of now, this strictly works with React related files so we have to isolate it from non-jsx files
      // as it will throw error while processing with web-workers at runtime
      // See https://github.com/pmmmwh/react-refresh-webpack-plugin/issues/24#issuecomment-672816401
      isProcessingJSXFiles && isEnvDevelopment && 'react-refresh/babel',
    ].filter(Boolean)
  }
});

const rootDirectory = fs.realpathSync(process.cwd());
const resolveApp = relativePath => path.resolve(rootDirectory, relativePath);
const paths = {
  assets: resolveApp('assets'),
  eslintConfig: resolveApp('.eslintrc.js'),
  tsConfig: resolveApp('tsconfig.json'),
  advancedEslintConfig: resolveApp('.eslintrc-advanced.js'),
};
// NOTE: due to routes like `/v1.0.0` (with '.'), to refer to static resources, we move all static content to `/static`
const OUTPUT_STATIC_PATH = 'static';

module.exports = (env, arg) => {
  const isEnvDevelopment = arg.mode === 'development';
  const isEnvProduction = arg.mode === 'production';
  const enableAdvancedMode = Boolean(arg.enableAdvancedMode);
  const enableAsyncTypeCheck = Boolean(arg.enableAsyncTypeCheck);
  const config = {
    mode: arg.mode,
    // WIP: workaround until `webpack-dev-server` watch mode works with webpack@5
    // See https://github.com/webpack/webpack-dev-server/issues/2758#issuecomment-710086019
    target: isEnvDevelopment ? 'web' : 'browserslist',
    bail: isEnvProduction, // fail-fast in production build
    bail: isEnvProduction,
    entry: { main: './src/index.tsx' },
    output: {
      path: path.join(__dirname, `../../../target/classes/${BaseConfig.contentRoute}`),
      assetModuleFilename: `${OUTPUT_STATIC_PATH}/${isEnvDevelopment ? '[name].[ext]' : '[name].[contenthash:8].[ext]'}`,
      publicPath: isEnvDevelopment ? '/' : `/${BaseConfig.contentRoute}/`,
      filename: `${OUTPUT_STATIC_PATH}/${isEnvDevelopment ? '[name].js' : '[name].[contenthash:8].js'}`,
    },
    devtool: isEnvDevelopment
      // NOTE: `eval-cheap-module-source-map` is recommend for dev, but it doesn't report error location accurately
      // See https://github.com/vuejs-templates/webpack/issues/520#issuecomment-356773702
      ? 'cheap-module-source-map'
      : 'source-map',
    watchOptions: {
      poll: 1000,
      ignored: /node_modules/,
    },
    devServer: {
      compress: true, // enable gzip compression for everything served to reduce traffic size
      publicPath: '/',
      open: true,
      port: 3000,
      openPage: BaseConfig.baseRoute,
      // redirect 404s to /index.html
      historyApiFallback: {
        // URL contains dot such as for version (majorV.minV.patchV: 1.0.0) need this rule
        // See https://github.com/bripkens/connect-history-api-fallback#disabledotrule
        disableDotRule: true
      },
      // suppress HMR and WDS messages about updated chunks
      // NOTE: there is a bug that the line '[HMR] Waiting for update signal from WDS...' is not suppressed
      // See https://github.com/webpack/webpack-dev-server/issues/2166
      clientLogLevel: 'warn',
      stats: {
        // Make Webpack Dev Middleware less verbose, consider `quiet` and `noInfo` options as well
        // NOTE: Use custom reporter to output errors and warnings from TS fork checker in `stylish` format. It's less verbose and
        // repetitive. Since we use the custom plugin, we want to mute `errors` and `warnings` from `webpack-dev-middleware`
        // See https://github.com/webpack-contrib/webpack-stylish
        // See https://github.com/TypeStrong/fork-ts-checker-webpack-plugin/issues/119
        all: false,
        colors: true,
        timings: true,
      },
    },
    resolve: {
      extensions: ['.js', '.jsx', '.ts', '.tsx'],
      alias: {
        ServerConfig: path.resolve(__dirname, '../resources/ideLightConfig.json'),
        BaseConfig: path.resolve(__dirname, 'ide.config.json'),
        Const: path.resolve(__dirname, 'src/const'),
        Utilities: path.resolve(__dirname, 'src/utils'),
        Models: path.resolve(__dirname, 'src/models'),
        Stores: path.resolve(__dirname, 'src/stores'),
        Components: path.resolve(__dirname, 'src/components'),
        API: path.resolve(__dirname, 'src/api'),
        Style: path.resolve(__dirname, 'style'),
      },
    },
    module: {
      rules: [
        {
          // Since we have fairly different sets of babel loaders for JSX, we separate the handling of JSX vs. non-JSX
          oneOf: [
            {
              test: /\.(?:js|ts)$/,
              exclude: /node_modules/,
              use: [getJavascriptLoaderConfig({ isEnvDevelopment, isProcessingJSXFiles: false })],
            },
            {
              test: /\.(?:js|ts)x$/,
              exclude: /node_modules/,
              use: [getJavascriptLoaderConfig({ isEnvDevelopment, isProcessingJSXFiles: true })],
            },
          ],
        },
        {
          test: /\.s?css$/,
          use: [
            {
              loader: MiniCssExtractPlugin.loader,
            },
            {
              // Helps resolve @import and url() like import/require()
              loader: require.resolve('css-loader'),
              options: {
                sourceMap: isEnvProduction
              }
            },
            isEnvProduction && {
              // Loads and transforms a CSS/SSS file using PostCSS
              loader: require.resolve('postcss-loader'),
              options: {
                postcssOptions: {
                  plugins: [
                    'autoprefixer',
                    'cssnano' // minification
                  ]
                },
                sourceMap: isEnvProduction
              }
            },
            {
              loader: require.resolve('sass-loader'),
              options: {
                implementation: sass,
                sourceMap: isEnvProduction,
              }
            }
          ].filter(Boolean)
        },
        {
          test: /\.(?:woff2?|ttf|otf|eot|svg|png|gif)$/,
          type: 'asset/resource',
        }
      ]
    },
    plugins: [
      new CleanWebpackPlugin(),
      (enableAdvancedMode || isEnvProduction) && new CircularDependencyPlugin({
        exclude: /node_modules/,
        include: /src.+\.(?:t|j)sx?$/,
        failOnError: true,
        allowAsyncCycles: false, // allow import cycles that include an asyncronous import, e.g. import(/* webpackMode: "weak" */ './file.js')
        cwd: process.cwd(), // set the current working directory for displaying module paths
      }),
      new MonacoWebpackPlugin({
        // Only include what we need to lessen the bundle loads
        // See https://github.com/microsoft/monaco-editor-webpack-plugin
        languages: ['json', 'java', 'markdown'],
        // Here we can choose to also exclude/include features but this really does not
        // significantly affect the bundle size anyhow, but it's also strange that we
        // need to turn off features in `monaco-editor` on creation anyway
        // See https://github.com/microsoft/monaco-editor-webpack-plugin/issues/40
        features: [
          'bracketMatching',
          'clipboard',
          'contextmenu',
          'coreCommands',
          'comment',
          'find',
          'folding',
          'gotoLine',
          'hover',
          'multicursor'
        ]
      }),
      isEnvDevelopment && new ReactRefreshWebpackPlugin(),
      new HtmlWebPackPlugin({
        template: './src/index.html',
        favicon: `${paths.assets}/favicon.ico`,
      }),
      new MiniCssExtractPlugin({
        filename: `${OUTPUT_STATIC_PATH}/${isEnvDevelopment ? '[name].css' : '[name].[contenthash:8].css'}`,
        chunkFilename: `${OUTPUT_STATIC_PATH}/${isEnvDevelopment ? '[id].css' : '[id].[contenthash:8].css'}`,
      }),
      // Webpack plugin that runs TypeScript type checker on a separate process.
      // NOTE: This makes the initial build process slower but allow faster incremental builds
      // See https://www.npmjs.com/package/fork-ts-checker-webpack-plugin#motivation
      // See https://github.com/arcanis/pnp-webpack-plugin#fork-ts-checker-webpack-plugin-integration
      new ForkTsCheckerWebpackPlugin({
        typescript: {
          configFile: paths.tsConfig,
          mode: 'write-references', // recommended mode to improve initial compilation time when using `babel-loader`
          diagnosticsOptions: {
            syntactic: true,
            semantic: true,
            declaration: true,
            global: true,
          },
        },
        // Allow blocking webpack's emit to wait for type checker/linter and to add errors to the webpack's compilation
        // if we turn `async:true` webpack will compile on one thread and type check on another thread so any type
        // error will not cause the build to fail, also error/warning from this plugin will not be captured by webpack
        // so we will have to write our own formatter for the log.
        async: enableAsyncTypeCheck && isEnvDevelopment,
        // We will handle the output here using fork-ts-checker compiler hooks since the lint/error/warning output is not grouped by file
        // See https://github.com/TypeStrong/fork-ts-checker-webpack-plugin/issues/119
        logger: {
          infrastructure: 'silent',
          issues: 'silent',
          devServer: false,
        },
        eslint: {
          enabled: true,
          files: [
            'src/**/*.ts',
            'src/**/*.tsx',
            'style/**/*.ts',
          ],
          options: {
            // See https://eslint.org/docs/developer-guide/nodejs-api#cliengine
            configFile: (isEnvProduction || enableAdvancedMode) ? paths.advancedEslintConfig : paths.eslintConfig,
          }
        },
        formatter: isEnvProduction ? 'codeframe' : undefined
      }),
    ].filter(Boolean)
  };
  if (isEnvProduction) {
    config.optimization = {
      splitChunks: {
        cacheGroups: {
          defaultVendors: {
            test: /node_modules/,
            chunks: 'initial',
            name: 'vendor',
            priority: -10,
            enforce: true
          }
        }
      }
    };
  }
  return config;
};
