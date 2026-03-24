module.exports = {
  lintOnSave: false,
  publicPath: process.env.NODE_ENV === 'production' ? '/' : '/',
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false,

  devServer: {
    host: '0.0.0.0',
    port: 8080,
    open: true,
    disableHostCheck: true,
    headers: {
      'Access-Control-Allow-Origin': '*'
    },
    proxy: {
      '/api': {
        target: process.env.VUE_APP_API_BASE_URL || 'http://localhost:9094',
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''
        }
      }
    }
  },

  css: {
    loaderOptions: {
      sass: {
        sassOptions: {
          silenceDeprecations: ['legacy-js-api']
        }
      },
      scss: {
        sassOptions: {
          silenceDeprecations: ['legacy-js-api']
        }
      }
    }
  },

  configureWebpack: {
    performance: process.env.NODE_ENV === 'production'
      ? {
          hints: 'warning',
          maxAssetSize: 1024 * 1024,
          maxEntrypointSize: 1536 * 1024,
          assetFilter: assetFilename => /\.(js|css)$/.test(assetFilename)
        }
      : false,
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          libs: {
            name: 'chunk-libs',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'initial'
          },
          elementUI: {
            name: 'chunk-elementUI',
            priority: 20,
            test: /[\\/]node_modules[\\/]_?element-ui(.*)/
          },
          echarts: {
            name: 'chunk-echarts',
            priority: 20,
            test: /[\\/]node_modules[\\/]_?echarts(.*)/
          }
        }
      }
    }
  },

  chainWebpack: config => {
    if (process.env.NODE_ENV === 'production') {
      config.optimization.minimizer('terser').tap(args => {
        args[0].terserOptions = args[0].terserOptions || {}
        args[0].terserOptions.compress = Object.assign({}, args[0].terserOptions.compress, {
          drop_console: true,
          drop_debugger: true
        })
        return args
      })
    }
  }
}
