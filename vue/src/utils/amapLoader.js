import mapConfig from '@/config/map.config.js'

const PRIMARY_SCRIPT_ID = 'amap-sdk-script'
const LEGACY_SCRIPT_IDS = ['amap-script', 'farm-location-amap']

let loaderPromise = null

const allScriptIds = () => [PRIMARY_SCRIPT_ID, ...LEGACY_SCRIPT_IDS]

const removeKnownScripts = () => {
  allScriptIds().forEach((id) => {
    const script = document.getElementById(id)
    if (script) {
      script.remove()
    }
  })
}

const buildScriptUrl = (key, version) =>
  `https://webapi.amap.com/maps?v=${encodeURIComponent(version)}&key=${encodeURIComponent(key)}`

const ensureSecurityConfig = (securityCode) => {
  if (!securityCode) {
    return
  }

  window._AMapSecurityConfig = {
    ...(window._AMapSecurityConfig || {}),
    securityJsCode: securityCode
  }
}

const ensurePlugins = (plugins) => {
  if (!Array.isArray(plugins) || plugins.length === 0 || !window.AMap || typeof window.AMap.plugin !== 'function') {
    return Promise.resolve(window.AMap)
  }

  return new Promise((resolve, reject) => {
    try {
      window.AMap.plugin(plugins, () => resolve(window.AMap))
    } catch (error) {
      reject(error)
    }
  })
}

export const resetAmapLoader = () => {
  loaderPromise = null
  removeKnownScripts()
}

export const loadAmapSdk = async (options = {}) => {
  const key = options.key || mapConfig.amap.jsKey || process.env.VUE_APP_AMAP_JS_KEY || ''
  const securityCode = options.securityCode || process.env.VUE_APP_AMAP_SECURITY_CODE || ''
  const version = options.version || mapConfig.amap.version || '2.0'
  const plugins = Array.from(new Set([...(options.plugins || [])].filter(Boolean)))

  if (!key) {
    throw new Error('AMAP_KEY_MISSING')
  }

  ensureSecurityConfig(securityCode)

  if (window.AMap && window.AMap.Map) {
    return ensurePlugins(plugins)
  }

  if (!loaderPromise) {
    loaderPromise = new Promise((resolve, reject) => {
      const existingScript = allScriptIds()
        .map((id) => document.getElementById(id))
        .find(Boolean)

      const handleLoad = () => {
        if (window.AMap && window.AMap.Map) {
          resolve(window.AMap)
          return
        }

        loaderPromise = null
        reject(new Error('AMAP_SDK_UNAVAILABLE'))
      }

      const handleError = () => {
        loaderPromise = null
        removeKnownScripts()
        reject(new Error('AMAP_NETWORK_ERROR'))
      }

      if (existingScript) {
        existingScript.addEventListener('load', handleLoad, { once: true })
        existingScript.addEventListener('error', handleError, { once: true })
        return
      }

      const script = document.createElement('script')
      script.id = PRIMARY_SCRIPT_ID
      script.async = true
      script.src = buildScriptUrl(key, version)
      script.addEventListener('load', handleLoad, { once: true })
      script.addEventListener('error', handleError, { once: true })
      document.head.appendChild(script)
    })
  }

  const amap = await loaderPromise
  await ensurePlugins(plugins)
  return amap
}
