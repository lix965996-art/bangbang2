<template>
  <el-dialog
    title="Farm Location"
    :visible.sync="dialogVisible"
    width="70%"
    top="5vh"
    :close-on-click-modal="false"
    append-to-body
    @opened="initMap"
  >
    <div class="location-selector">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="Search address or place"
          clearable
          @keyup.enter.native="handleSearch"
        />
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">Search</el-button>
        <el-button type="success" icon="el-icon-edit-outline" :disabled="isDrawing" @click="startDraw">
          {{ isDrawing ? 'Drawing...' : 'Draw Area' }}
        </el-button>
        <el-button type="warning" icon="el-icon-delete" @click="clearDraw">Clear</el-button>
      </div>

      <div id="selector-container" class="map-container"></div>

      <div class="status-panel">
        <div class="status-line">
          <span class="label">Address</span>
          <span class="value">{{ selectedLocation.address || 'Click the map or search to choose a location' }}</span>
        </div>
        <div class="status-line">
          <span class="label">District</span>
          <span class="value">{{ calculatedDistrict || '-' }}</span>
        </div>
        <div class="status-line">
          <span class="label">Area</span>
          <span class="value">{{ calculatedArea ? `${calculatedArea} mu` : '-' }}</span>
        </div>
      </div>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">Cancel</el-button>
      <el-button type="primary" @click="confirmSelection">Confirm</el-button>
    </div>
  </el-dialog>
</template>

<script>
import mapConfig from '@/config/map.config.js'

export default {
  name: 'FarmLocationSelector',
  props: {
    visible: { type: Boolean, default: false },
    initialData: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      map: null,
      geocoder: null,
      placeSearch: null,
      mouseTool: null,
      marker: null,
      overlay: null,
      searchKeyword: '',
      isDrawing: false,
      isLoadingMap: false,
      currentPath: [],
      calculatedArea: 0,
      calculatedDistrict: '',
      selectedLocation: {
        lng: null,
        lat: null,
        address: ''
      },
      amapKey: mapConfig.amap.jsKey
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  beforeDestroy() {
    this.destroyMap()
  },
  methods: {
    async initMap() {
      await this.ensureMapSdk()
      this.createMap()
    },
    ensureMapSdk() {
      if (window.AMap && window.AMap.Map && window.AMap.MouseTool) {
        return Promise.resolve()
      }
      if (this.isLoadingMap) {
        return Promise.resolve()
      }

      this.isLoadingMap = true
      return new Promise((resolve, reject) => {
        const existingScript = document.getElementById('farm-location-amap')
        if (existingScript) {
          existingScript.addEventListener('load', () => {
            this.isLoadingMap = false
            resolve()
          }, { once: true })
          existingScript.addEventListener('error', reject, { once: true })
          return
        }

        const script = document.createElement('script')
        script.id = 'farm-location-amap'
        script.src = `https://webapi.amap.com/maps?v=2.0&key=${this.amapKey}&plugin=AMap.ToolBar,AMap.PlaceSearch,AMap.Geocoder,AMap.MouseTool`
        script.onload = () => {
          this.isLoadingMap = false
          resolve()
        }
        script.onerror = (error) => {
          this.isLoadingMap = false
          reject(error)
        }
        document.head.appendChild(script)
      }).catch((error) => {
        console.error('Failed to load AMap SDK:', error)
        this.$message.error('Map loading failed')
      })
    },
    createMap() {
      this.$nextTick(() => {
        const container = document.getElementById('selector-container')
        if (!container || !window.AMap) {
          return
        }

        this.destroyMap()

        this.map = new window.AMap.Map('selector-container', {
          zoom: mapConfig.amap.defaultZoom,
          center: mapConfig.amap.defaultCenter,
          viewMode: '2D',
          resizeEnable: true
        })

        this.map.addControl(new window.AMap.ToolBar())
        this.geocoder = new window.AMap.Geocoder({ radius: 1000, extensions: 'all' })
        this.placeSearch = new window.AMap.PlaceSearch({
          map: this.map,
          autoFitView: true,
          city: '430800'
        })

        if (window.AMap.MouseTool) {
          this.mouseTool = new window.AMap.MouseTool(this.map)
          this.mouseTool.on('draw', (event) => {
            this.isDrawing = false
            if (this.overlay) {
              this.map.remove(this.overlay)
            }
            this.overlay = event.obj

            const path = event.obj.getPath()
            this.currentPath = path.map(point => ({ lng: point.lng, lat: point.lat }))

            if (window.AMap.GeometryUtil) {
              const areaInSquareMeters = window.AMap.GeometryUtil.ringArea(path)
              this.calculatedArea = Number((areaInSquareMeters / 666.67).toFixed(1))
            }

            const center = event.obj.getBounds().getCenter()
            this.updateLocation(center.lng, center.lat)
          })
        }

        this.map.on('click', (event) => {
          if (!this.isDrawing) {
            this.updateLocation(event.lnglat.getLng(), event.lnglat.getLat())
          }
        })

        this.hydrateFromInitialData()
      })
    },
    hydrateFromInitialData() {
      if (!this.initialData) {
        return
      }

      if (this.initialData.centerLng && this.initialData.centerLat) {
        this.selectedLocation = {
          lng: this.initialData.centerLng,
          lat: this.initialData.centerLat,
          address: this.initialData.address || ''
        }
        this.calculatedDistrict = this.initialData.district || ''
        this.calculatedArea = Number(this.initialData.area || 0)
        this.setMarker(this.initialData.centerLng, this.initialData.centerLat)
        this.map.setCenter([this.initialData.centerLng, this.initialData.centerLat])
      }

      if (this.initialData.coordinates) {
        try {
          const coordinates = typeof this.initialData.coordinates === 'string'
            ? JSON.parse(this.initialData.coordinates)
            : this.initialData.coordinates

          if (Array.isArray(coordinates) && coordinates.length > 2) {
            this.currentPath = coordinates
            this.overlay = new window.AMap.Polygon({
              path: coordinates.map(item => [item.lng, item.lat]),
              strokeColor: '#10B981',
              strokeWeight: 3,
              fillColor: '#10B981',
              fillOpacity: 0.2
            })
            this.map.add(this.overlay)
            this.map.setFitView([this.overlay])
          }
        } catch (error) {
          console.error('Failed to parse initial polygon:', error)
        }
      }
    },
    handleSearch() {
      if (!this.searchKeyword || !this.placeSearch) {
        return
      }

      this.placeSearch.search(this.searchKeyword, (status, result) => {
        if (status !== 'complete' || !result.poiList || !result.poiList.pois.length) {
          this.$message.warning('No matching address was found')
          return
        }

        const poi = result.poiList.pois[0]
        if (!poi.location) {
          this.$message.warning('Selected address has no coordinate data')
          return
        }

        this.updateLocation(poi.location.lng, poi.location.lat, poi.name)
        this.map.setZoom(16)
      })
    },
    updateLocation(lng, lat, addressHint = '') {
      this.selectedLocation.lng = lng
      this.selectedLocation.lat = lat
      this.setMarker(lng, lat)
      this.map.setCenter([lng, lat])

      if (!this.geocoder) {
        this.selectedLocation.address = addressHint || `${lng.toFixed(6)}, ${lat.toFixed(6)}`
        return
      }

      this.geocoder.getAddress([lng, lat], (status, result) => {
        if (status === 'complete' && result.regeocode) {
          const addressComponent = result.regeocode.addressComponent || {}
          this.selectedLocation.address = result.regeocode.formattedAddress || addressHint
          this.calculatedDistrict = `${addressComponent.city || ''}${addressComponent.district || ''}` || this.calculatedDistrict
        } else {
          this.selectedLocation.address = addressHint || `${lng.toFixed(6)}, ${lat.toFixed(6)}`
        }
      })
    },
    setMarker(lng, lat) {
      if (!this.map) {
        return
      }

      if (this.marker) {
        this.map.remove(this.marker)
      }

      this.marker = new window.AMap.Marker({
        position: [lng, lat]
      })
      this.map.add(this.marker)
    },
    startDraw() {
      if (!this.mouseTool) {
        this.$message.warning('Drawing tool is not available')
        return
      }

      this.isDrawing = true
      if (this.overlay) {
        this.map.remove(this.overlay)
        this.overlay = null
      }
      this.currentPath = []
      this.calculatedArea = 0
      this.mouseTool.polygon({
        strokeColor: '#10B981',
        strokeWeight: 3,
        fillColor: '#10B981',
        fillOpacity: 0.2
      })
    },
    clearDraw() {
      if (this.overlay) {
        this.map.remove(this.overlay)
        this.overlay = null
      }
      if (this.mouseTool) {
        this.mouseTool.close(true)
      }
      this.currentPath = []
      this.calculatedArea = 0
      this.isDrawing = false
    },
    confirmSelection() {
      if (!this.selectedLocation.lng || !this.selectedLocation.lat) {
        this.$message.warning('Please choose a location first')
        return
      }

      this.$emit('confirm', {
        centerLng: this.selectedLocation.lng,
        centerLat: this.selectedLocation.lat,
        address: this.selectedLocation.address,
        district: this.calculatedDistrict,
        area: this.calculatedArea || this.initialData.area || 0,
        coordinates: this.currentPath.length ? JSON.stringify(this.currentPath) : this.initialData.coordinates || ''
      })
      this.dialogVisible = false
    },
    destroyMap() {
      if (this.map) {
        this.map.destroy()
        this.map = null
      }
      this.marker = null
      this.overlay = null
      this.mouseTool = null
    }
  }
}
</script>

<style scoped>
.location-selector {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: grid;
  grid-template-columns: 1fr auto auto auto;
  gap: 12px;
}

.map-container {
  width: 100%;
  height: 460px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
}

.status-panel {
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
}

.status-line {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px;
  font-size: 14px;
}

.label {
  color: #606266;
  font-weight: 600;
}

.value {
  color: #303133;
  word-break: break-all;
}

@media (max-width: 900px) {
  .toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .map-container {
    height: 360px;
  }
}
</style>
