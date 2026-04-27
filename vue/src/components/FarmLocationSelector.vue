<template>
  <el-dialog
    title="地块位置标记"
    :visible="visible"
    @opened="initMap"
    @close="$emit('update:visible', false)"
    width="70%"
    top="5vh"
    :close-on-click-modal="false"
    append-to-body
  >
    <div class="location-selector">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索地址或地点"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        <el-button type="success" icon="el-icon-edit-outline" :disabled="isDrawing" @click="startDraw">
          {{ isDrawing ? '绘制中...' : '绘制区域' }}
        </el-button>
        <el-button type="warning" icon="el-icon-delete" @click="clearDraw">清除</el-button>
      </div>

      <div id="selector-container" class="map-container"></div>

      <div class="status-panel">
        <div class="status-line">
          <span class="label">地址</span>
          <span class="value">{{ selectedLocation.address || '点击地图选择位置，或搜索地址' }}</span>
        </div>
        <div class="status-line">
          <span class="label">所属区县</span>
          <span class="value">{{ calculatedDistrict || '-' }}</span>
        </div>
        <div class="status-line">
          <span class="label">面积</span>
          <span class="value">{{ calculatedArea ? `${calculatedArea} 亩` : '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" @click="confirmSelection">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import mapConfig from '@/config/map.config.js'
import { loadAmapSdk } from '@/utils/amapLoader'

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
      }
    }
  },
  computed: {},
  watch: {
    visible(val) {
      console.log('[FarmLocationSelector] visible 变化为:', val)
    }
  },
  beforeDestroy() {
    this.destroyMap()
  },
  methods: {
    async initMap() {
      console.log('[FarmLocationSelector] initMap 被调用, window.AMap:', !!window.AMap)
      try {
        await this.ensureMapSdk()
        console.log('[FarmLocationSelector] SDK 加载完成，开始创建地图')
        this.createMap()
      } catch (error) {
        console.error('[FarmLocationSelector] 地图初始化失败:', error)
      }
    },
    async ensureMapSdk() {
      if (this.isLoadingMap) {
        console.log('[FarmLocationSelector] SDK 正在加载中，跳过重复请求')
        return
      }

      this.isLoadingMap = true
      console.log('[FarmLocationSelector] 开始加载 AMap SDK...')
      try {
        await loadAmapSdk({
          plugins: ['AMap.ToolBar', 'AMap.PlaceSearch', 'AMap.Geocoder', 'AMap.MouseTool', 'AMap.GeometryUtil']
        })
        console.log('[FarmLocationSelector] AMap SDK 加载成功')
      } catch (error) {
        console.error('[FarmLocationSelector] AMap SDK 加载失败:', error)
        this.$message.error('高德地图加载失败，请检查网络或 Key 配置')
        throw error
      } finally {
        this.isLoadingMap = false
      }
    },
    createMap() {
      console.log('[FarmLocationSelector] createMap 被调用')
      this.$nextTick(() => {
        const container = document.getElementById('selector-container')
        console.log('[FarmLocationSelector] 容器元素:', !!container, 'AMap:', !!window.AMap)
        if (!container || !window.AMap) {
          console.warn('[FarmLocationSelector] 无法创建地图 - 容器或AMap不存在')
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

        console.log('[FarmLocationSelector] 插件可用性 - Geocoder:', !!window.AMap.Geocoder, 'PlaceSearch:', !!window.AMap.PlaceSearch, 'MouseTool:', !!window.AMap.MouseTool)

        if (window.AMap.Geocoder) {
          this.geocoder = new window.AMap.Geocoder({ radius: 1000, extensions: 'all' })
        } else {
          console.warn('[FarmLocationSelector] Geocoder 插件不可用')
        }

        if (window.AMap.PlaceSearch) {
          this.placeSearch = new window.AMap.PlaceSearch({
            map: this.map,
            autoFitView: true,
            city: '430800'
          })
        } else {
          console.warn('[FarmLocationSelector] PlaceSearch 插件不可用')
        }

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
      console.log('[FarmLocationSelector] handleSearch 被调用, keyword:', this.searchKeyword)
      console.log('[FarmLocationSelector] placeSearch 对象:', !!this.placeSearch)
      if (!this.searchKeyword || !this.placeSearch) {
        console.warn('[FarmLocationSelector] 搜索条件不满足 - keyword:', !!this.searchKeyword, 'placeSearch:', !!this.placeSearch)
        return
      }

      this.placeSearch.search(this.searchKeyword, (status, result) => {
        console.log('[FarmLocationSelector] 搜索结果 - status:', status, 'result:', result)
        if (status !== 'complete' || !result.poiList || !result.poiList.pois.length) {
          this.$message.warning('未找到匹配的地址')
          return
        }

        const poi = result.poiList.pois[0]
        if (!poi.location) {
          this.$message.warning('所选地址没有坐标数据')
          return
        }

        console.log('[FarmLocationSelector] 搜索到POI:', poi.name, poi.location)
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
        this.$message.warning('绘图工具不可用')
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
        this.$message.warning('请先选择位置')
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
      this.$emit('update:visible', false)
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
