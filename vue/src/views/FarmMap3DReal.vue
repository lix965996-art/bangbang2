<template>
  <div class="farm-map-real">
    <!-- 顶部玻璃态导航栏 -->
    <div class="top-glass-bar">
      <div class="brand-section">
        <div class="logo-pulse"></div>
        <div class="title-group">
          <span class="main-title">智慧农场</span>
          <span class="sub-title">Real-Time GIS Monitoring Platform</span>
        </div>
      </div>
      
      <div class="stats-pills">
        <div class="pill-item">
          <i class="el-icon-location"></i>
          <span class="label">地块</span>
          <span class="value">{{ summary.farmCount }}</span>
        </div>
        <div class="pill-item">
          <i class="el-icon-s-grid"></i>
          <span class="label">面积</span>
          <span class="value">{{ summary.totalArea }}<small>亩</small></span>
        </div>
        <div class="pill-item">
          <i class="el-icon-sunny"></i>
          <span class="label">{{ weatherModeText }}</span>
          <span class="value">{{ weather.temp }}°C</span>
        </div>
      </div>

      <div class="time-weather">
        <div class="current-time">{{ currentTime }}</div>
      </div>
    </div>

    <!-- 左侧面板 -->
    <div class="left-panel glass-panel">
      <div class="panel-header">
        <i class="el-icon-s-grid"></i>
        <span>监测点位</span>
        <div class="header-badge">{{ farms.length }}</div>
      </div>
      
      <div class="farm-list">
        <div 
          v-for="(farm, index) in farms" 
          :key="index"
          class="farm-card"
          :class="{ active: selectedFarm && selectedFarm.id === farm.id, warning: getFarmWarning(farm) }"
          @click="handleFarmClick(farm)"
        >
          <div class="farm-icon-wrapper">
            <img :src="getCropIcon(farm.crop)" class="crop-icon" alt="作物" />
            <div v-if="getFarmWarning(farm)" class="warning-dot"></div>
          </div>
          <div class="farm-info">
            <div class="farm-name">{{ farm.farm }}</div>
            <div class="farm-meta">
              <span class="crop-tag">{{ farm.crop }}</span>
              <span class="area-tag">{{ farm.area }}亩</span>
            </div>
            <div class="farm-sensors">
              <span :class="{'danger': Number(farm.temperature) > 30}">
                <i class="el-icon-sunny"></i>{{ farm.temperature }}°
              </span>
              <span :class="{'danger': Number(farm.soilhumidity) < 20}">
                <i class="el-icon-water-cup"></i>{{ farm.soilhumidity }}%
              </span>
            </div>
          </div>
          <div class="farm-status">
            <el-tag 
              :type="getFarmStatusType(farm)" 
              size="mini"
              effect="dark"
            >
              {{ getFarmStatusText(farm) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 地图容器 -->
    <div class="map-container">
      <div id="amap-3d-container" ref="mapContainer"></div>
      
      <!-- 地图控制面板 -->
      <div class="map-controls glass-panel">
        <div class="control-group">
          <el-tooltip content="重置视角" placement="top">
            <div class="control-btn" @click="resetView">
              <i class="el-icon-refresh"></i>
            </div>
          </el-tooltip>
          
          <div class="divider"></div>
          
          <el-tooltip content="3D视角" placement="top">
            <div class="control-btn" :class="{active: map3DEnabled}" @click="toggle3DView">
              <i class="el-icon-view"></i>
            </div>
          </el-tooltip>
          
          <el-tooltip content="卫星图层" placement="top">
            <div class="control-btn" :class="{active: satelliteEnabled}" @click="toggleSatellite">
              <i class="el-icon-picture"></i>
            </div>
          </el-tooltip>
        </div>
        
        <div class="divider-h"></div>
        
        <!-- 天气控制 -->
        <div class="weather-group">
          <div class="weather-label">天气模式</div>
          <div class="weather-buttons">
            <el-tooltip content="晴朗" placement="top">
              <div class="weather-btn" :class="{active: weatherMode==='sunny'}" @click="setWeather('sunny')">
                <img src="@/assets/qing.png" alt="晴" />
              </div>
            </el-tooltip>
            <el-tooltip content="降雨" placement="top">
              <div class="weather-btn" :class="{active: weatherMode==='rain'}" @click="setWeather('rain')">
                <img src="@/assets/xiayu.png" alt="雨" />
              </div>
            </el-tooltip>
            <el-tooltip content="降雪" placement="top">
              <div class="weather-btn" :class="{active: weatherMode==='snow'}" @click="setWeather('snow')">
                <img src="@/assets/snow.png" alt="雪" />
              </div>
            </el-tooltip>
            <el-tooltip content="雷暴" placement="top">
              <div class="weather-btn" :class="{active: weatherMode==='storm'}" @click="setWeather('storm')">
                <img src="@/assets/bingbao.png" alt="暴" />
              </div>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 天气效果层 -->
      <canvas ref="weatherCanvas" class="weather-canvas"></canvas>
    </div>

    <!-- 右侧详情面板 -->
    <transition name="slide-left">
      <div v-if="selectedFarm" class="right-panel glass-panel">
        <div class="panel-header">
          <div class="header-left">
            <i class="el-icon-location-information"></i>
            <span>{{ selectedFarm.farm }}</span>
          </div>
          <el-button type="text" icon="el-icon-close" @click="selectedFarm = null"></el-button>
        </div>

        <!-- 健康评分 -->
        <div class="health-section">
          <div class="health-score">
            <el-progress 
              type="circle" 
              :percentage="farmStatus.health" 
              :width="100"
              :color="customColors"
              :stroke-width="10"
            >
              <template slot="default">
                <span class="score-text">{{ farmStatus.health }}</span>
                <span class="score-label">健康分</span>
              </template>
            </el-progress>
          </div>
          <div class="health-info">
            <div class="crop-badge">
              <span class="crop-name">{{ selectedFarm.crop }}</span>
              <span class="area-info">{{ selectedFarm.area }}亩</span>
            </div>
            <div class="data-source-badge">
              <i class="el-icon-connection"></i>
              {{ selectedFarm.dataSource || 'STM32传感器' }}
            </div>
          </div>
        </div>

        <!-- 环境数据 -->
        <div class="env-data-section">
          <div class="section-title">
            <i class="el-icon-data-line"></i>
            <span>环境监测</span>
          </div>
          
          <div class="env-item">
            <div class="env-label">
              <i class="el-icon-sunny"></i>
              <span>空气温度</span>
            </div>
            <div class="env-value">
              <span class="value-num" :class="{'danger': Number(selectedFarm.temperature) > 30}">
                {{ selectedFarm.temperature }}°C
              </span>
              <el-progress 
                :percentage="normalize(selectedFarm.temperature, 40)" 
                :show-text="false"
                :color="Number(selectedFarm.temperature) > 30 ? '#F56C6C' : '#67C23A'"
              ></el-progress>
            </div>
          </div>

          <div class="env-item">
            <div class="env-label">
              <i class="el-icon-water-cup"></i>
              <span>土壤湿度</span>
            </div>
            <div class="env-value">
              <span class="value-num" :class="{'danger': Number(selectedFarm.soilhumidity) < 20}">
                {{ selectedFarm.soilhumidity }}%
              </span>
              <el-progress 
                :percentage="Number(selectedFarm.soilhumidity)" 
                :show-text="false"
                :color="Number(selectedFarm.soilhumidity) < 20 ? '#F56C6C' : '#67C23A'"
              ></el-progress>
            </div>
          </div>
        </div>

        <!-- AI决策建议 -->
        <div class="ai-advice-section">
          <div class="section-title">
            <img src="@/assets/danao.png" class="ai-icon" alt="AI" />
            <span>智能决策</span>
          </div>
          <div class="advice-content" :class="{'warning': farmStatus.health < 90}">
            <i v-if="farmStatus.health < 90" class="el-icon-warning"></i>
            <i v-else class="el-icon-success"></i>
            <span>{{ farmStatus.msg || '✅ 当前各项指标优异，建议维持现状。' }}</span>
          </div>
        </div>

        <!-- 控制开关 -->
        <div class="control-section">
          <div class="section-title">
            <i class="el-icon-setting"></i>
            <span>智能控制</span>
          </div>
          
          <div class="control-switches">
            <div class="switch-card" :class="{active: switchState.water}">
              <div class="switch-left">
                <div class="switch-icon water">
                  <img src="@/assets/guangai.png" alt="灌溉" />
                </div>
                <div class="switch-info">
                  <span class="switch-name">智能灌溉</span>
                  <span class="switch-status">{{ switchState.water ? '运行中' : '已关闭' }}</span>
                </div>
              </div>
              <el-switch 
                v-model="switchState.water" 
                active-color="#10b981"
                @change="handleSwitchChange('water', $event)"
              ></el-switch>
            </div>

            <div class="switch-card" :class="{active: switchState.fertilizer}">
              <div class="switch-left">
                <div class="switch-icon fertilizer">
                  <img src="@/assets/shifei.png" alt="施肥" />
                </div>
                <div class="switch-info">
                  <span class="switch-name">精准施肥</span>
                  <span class="switch-status">{{ switchState.fertilizer ? '运行中' : '已关闭' }}</span>
                </div>
              </div>
              <el-switch 
                v-model="switchState.fertilizer" 
                active-color="#f59e0b"
                @change="handleSwitchChange('fertilizer', $event)"
              ></el-switch>
            </div>

            <div class="switch-card" :class="{active: switchState.pest}">
              <div class="switch-left">
                <div class="switch-icon pest">
                  <img src="@/assets/xiaosha.png" alt="消杀" />
                </div>
                <div class="switch-info">
                  <span class="switch-name">飞防消杀</span>
                  <span class="switch-status">{{ switchState.pest ? '运行中' : '已关闭' }}</span>
                </div>
              </div>
              <el-switch 
                v-model="switchState.pest" 
                active-color="#3b82f6"
                @change="handleSwitchChange('pest', $event)"
              ></el-switch>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import AMapLoader from '@amap/amap-jsapi-loader';
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';

export default {
  name: 'FarmMap3DReal',
  data() {
    return {
      currentTime: '',
      map: null,
      map3DEnabled: true,
      satelliteEnabled: true,
      
      selectedFarm: null,
      weather: { temp: 26 },
      summary: { farmCount: 0, totalArea: 0 },
      farms: [],
      weatherMode: 'sunny',
      
      // 地图相关
      farmPolygons: [],
      farm3DObjects: [],
      farmMarkers: [],
      
      // 开关状态
      switchState: { water: false, fertilizer: false, pest: false },
      loadingState: { water: false, fertilizer: false, pest: false },
      
      // STM32传感器数据
      stm32Data: {
        device1: { temperature: null, humidity: null },
        device2: { temperature: null, humidity: null, bump: false }
      },
      farmDeviceMap: {},
      
      // 作物配置
      cropConfig: {
        '小麦': { model: '/models/wheat.glb', scale: 0.5, color: '#e6c658', icon: require('@/assets/xiaomai.png') },
        '艾叶': { model: '/models/rhy.glb', scale: 1.0, color: '#8BC34A', icon: require('@/assets/aiye.png') },
        '竹子': { model: '/models/bamboo.glb', scale: 0.02, color: '#4CAF50', icon: require('@/assets/zhuzi.png') },
        '玉米': { model: '/models/maize.glb', scale: 1.0, color: '#FFC107', icon: require('@/assets/yumi.png') },
        '棕树': { model: '/models/tree.glb', scale: 0.2, color: '#388E3C', icon: require('@/assets/zongshu.png') },
        '西红柿': { model: '/models/tomato.glb', scale: 0.01, color: '#FF5722', icon: require('@/assets/xihongshi.png') }
      },
      
      customColors: [
        {color: '#F44336', percentage: 60},
        {color: '#FF9800', percentage: 80},
        {color: '#4CAF50', percentage: 100}
      ],
      
      // 天气效果
      weatherCanvas: null,
      weatherCtx: null,
      weatherParticles: [],
      weatherAnimationId: null,
      
      // 高德地图Key
      amapKey: 'a0dc4534ab26be714e94cef345e480aa'
    };
  },
  
  computed: {
    weatherModeText() {
      const map = { sunny: '晴朗', rain: '小雨', snow: '降雪', storm: '雷暴' };
      return map[this.weatherMode];
    },
    
    farmStatus() {
      if (!this.selectedFarm) return { health: 100, msg: '' };
      
      let tempHigh = Number(this.selectedFarm.temperature) > 30;
      let needsWater = Number(this.selectedFarm.soilhumidity) < 20;
      
      let msg = '';
      let health = 100;
      
      if (tempHigh) { 
        msg = `⚠️ 温度过高警告！当前温度 ${this.selectedFarm.temperature}°C，建议开启降温设备`; 
        health -= 30; 
      } else if (needsWater) { 
        msg = `⚠️ 土壤湿度过低！当前湿度 ${this.selectedFarm.soilhumidity}%，请立即灌溉`; 
        health -= 25; 
      }
      
      return { msg, health: Math.max(0, health) };
    }
  },

  mounted() {
    this.updateTime();
    setInterval(this.updateTime, 1000);
    
    this.initMap();
    this.fetchData();
    
    // 每30秒刷新传感器数据
    this.stm32Timer = setInterval(() => {
      this.fetchSTM32Data();
      this.updateFarmsWithSTM32Data();
    }, 30000);
  },
  
  beforeDestroy() {
    if (this.stm32Timer) clearInterval(this.stm32Timer);
    if (this.weatherAnimationId) cancelAnimationFrame(this.weatherAnimationId);
    if (this.map) this.map.destroy();
  },
  
  methods: {
    updateTime() { 
      this.currentTime = new Date().toLocaleString('zh-CN', { hour12: false }); 
    },
    
    normalize(val, max) { 
      return Math.min((Number(val) / max) * 100, 100); 
    },
    
    getCropIcon(crop) {
      return this.cropConfig[crop]?.icon || require('@/assets/moreng.png');
    },
    
    getFarmWarning(farm) {
      return Number(farm.temperature) > 30 || Number(farm.soilhumidity) < 20;
    },
    
    getFarmStatusType(farm) {
      if (Number(farm.temperature) > 30) return 'danger';
      if (Number(farm.soilhumidity) < 20) return 'warning';
      return 'success';
    },
    
    getFarmStatusText(farm) {
      if (Number(farm.temperature) > 30) return '高温';
      if (Number(farm.soilhumidity) < 20) return '缺水';
      return '正常';
    },

    // ==================== 地图初始化 ====================
    async initMap() {
      try {
        const AMap = await AMapLoader.load({
          key: this.amapKey,
          version: '2.0',
          plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.ControlBar']
        });

        this.map = new AMap.Map('amap-3d-container', {
          zoom: 16,
          center: [110.479191, 29.117096], // 张家界市中心
          viewMode: '3D',
          pitch: 60,
          rotation: 0,
          mapStyle: 'amap://styles/darkblue', // 暗色科技风格
          features: ['bg', 'road', 'building'],
          showLabel: true
        });

        // 添加控件
        this.map.addControl(new AMap.Scale());
        this.map.addControl(new AMap.ToolBar({
          position: { bottom: '20px', right: '20px' }
        }));

        // 添加卫星图层
        const satellite = new AMap.TileLayer.Satellite();
        this.map.add(satellite);

        console.log('✅ 地图初始化成功');
        
        // 初始化天气画布
        this.initWeatherCanvas();
        
      } catch (e) {
        console.error('❌ 地图加载失败:', e);
        this.$message.error('地图加载失败，请检查网络连接');
      }
    },

    // ==================== 数据获取 ====================
    async fetchData() {
      try {
        await this.fetchSTM32Data();
        
        const res = await this.request.get('/statistic/dashboard');
        if (res.code === '200') {
          this.farms = res.data.map(farm => {
            const deviceKey = this.assignDeviceToFarm(farm.id);
            const deviceData = this.getDeviceData(deviceKey);
            
            return {
              ...farm,
              temperature: deviceData.temperature,
              soilhumidity: deviceData.humidity,
              dataSource: deviceKey === 'device1' ? 'STM32-001' : 'STM32-PUMP'
            };
          });
          
          this.summary.farmCount = this.farms.length;
          this.summary.totalArea = this.farms.reduce((acc, cur) => acc + (Number(cur.area) || 0), 0);
          
          if (this.farms.length > 0) {
            this.selectedFarm = this.farms[0];
          }
          
          // 在地图上绘制农田
          this.$nextTick(() => {
            this.renderFarmsOnMap();
          });
        }
      } catch (e) {
        console.error('❌ 数据获取失败:', e);
      }
    },

    async fetchSTM32Data() {
      try {
        const res1 = await this.request.get('/aether/device/status');
        if (res1.code === '200' && res1.data) {
          this.stm32Data.device1.temperature = res1.data.temperature || 25;
          this.stm32Data.device1.humidity = res1.data.humidity || 50;
        }
      } catch (e) {
        this.stm32Data.device1.temperature = 25;
        this.stm32Data.device1.humidity = 50;
      }
      
      try {
        const res2 = await this.request.get('/aether/device/new/status');
        if (res2.code === '200' && res2.data) {
          this.stm32Data.device2.temperature = res2.data.temperature || 26;
          this.stm32Data.device2.humidity = res2.data.humidity || 55;
          this.stm32Data.device2.bump = res2.data.bump || false;
        }
      } catch (e) {
        this.stm32Data.device2.temperature = 26;
        this.stm32Data.device2.humidity = 55;
      }
    },

    assignDeviceToFarm(farmId) {
      if (!this.farmDeviceMap[farmId]) {
        this.farmDeviceMap[farmId] = farmId % 2 === 0 ? 'device1' : 'device2';
      }
      return this.farmDeviceMap[farmId];
    },
    
    getDeviceData(deviceKey) {
      const device = this.stm32Data[deviceKey];
      if (!device) return { temperature: 25, humidity: 50 };
      return {
        temperature: device.temperature || 25,
        humidity: device.humidity || 50
      };
    },

    updateFarmsWithSTM32Data() {
      if (!this.farms || this.farms.length === 0) return;
      
      this.farms = this.farms.map(farm => {
        const deviceKey = this.assignDeviceToFarm(farm.id);
        const deviceData = this.getDeviceData(deviceKey);
        
        return {
          ...farm,
          temperature: deviceData.temperature,
          soilhumidity: deviceData.humidity,
          dataSource: deviceKey === 'device1' ? 'STM32-001' : 'STM32-PUMP'
        };
      });
      
      if (this.selectedFarm) {
        const updated = this.farms.find(f => f.id === this.selectedFarm.id);
        if (updated) this.selectedFarm = updated;
      }
    },

    // ==================== 地图渲染 ====================
    renderFarmsOnMap() {
      if (!this.map) return;
      
      // 清除旧的图层
      this.farmPolygons.forEach(p => this.map.remove(p));
      this.farmMarkers.forEach(m => this.map.remove(m));
      this.farmPolygons = [];
      this.farmMarkers = [];
      
      this.farms.forEach((farm, index) => {
        // 如果有坐标数据，绘制多边形
        if (farm.coordinates) {
          this.renderFarmPolygon(farm);
        } else {
          // 否则使用中心点标记
          this.renderFarmMarker(farm, index);
        }
      });
    },

    renderFarmPolygon(farm) {
      try {
        const coords = typeof farm.coordinates === 'string' 
          ? JSON.parse(farm.coordinates) 
          : farm.coordinates;
        
        const path = coords.map(c => [c.lng, c.lat]);
        const cropConfig = this.cropConfig[farm.crop] || { color: '#8BC34A' };
        
        const polygon = new AMap.Polygon({
          path: path,
          fillColor: cropConfig.color,
          fillOpacity: 0.5,
          strokeColor: '#fff',
          strokeWeight: 2,
          strokeOpacity: 0.8,
          zIndex: 50
        });
        
        polygon.setExtData({ farm });
        
        polygon.on('click', () => {
          this.handleFarmClick(farm);
        });
        
        this.map.add(polygon);
        this.farmPolygons.push(polygon);
        
        // 在中心添加3D模型标记
        if (farm.centerLng && farm.centerLat) {
          this.add3DModelMarker(farm, [farm.centerLng, farm.centerLat]);
        }
        
      } catch (e) {
        console.error('渲染农田多边形失败:', e);
      }
    },

    renderFarmMarker(farm, index) {
      // 如果没有坐标，生成一个临时位置（围绕中心点分布）
      const centerLng = 110.479191;
      const centerLat = 29.117096;
      const offset = 0.002;
      const angle = (index / this.farms.length) * Math.PI * 2;
      
      const lng = centerLng + Math.cos(angle) * offset;
      const lat = centerLat + Math.sin(angle) * offset;
      
      this.add3DModelMarker(farm, [lng, lat]);
    },

    add3DModelMarker(farm, position) {
      const cropConfig = this.cropConfig[farm.crop] || { color: '#8BC34A' };
      
      // 创建自定义HTML标记
      const content = `
        <div class="farm-3d-marker" style="
          background: ${cropConfig.color};
          border: 2px solid #fff;
          border-radius: 50%;
          width: 40px;
          height: 40px;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 4px 12px rgba(0,0,0,0.3);
          cursor: pointer;
          transition: all 0.3s;
        ">
          <img src="${cropConfig.icon}" style="width: 24px; height: 24px; object-fit: contain;" />
        </div>
        <div class="farm-label" style="
          background: rgba(0,0,0,0.7);
          color: white;
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 12px;
          white-space: nowrap;
          margin-top: 4px;
          text-align: center;
        ">${farm.farm}</div>
      `;
      
      const marker = new AMap.Marker({
        position: position,
        content: content,
        offset: new AMap.Pixel(-20, -20),
        zIndex: 100
      });
      
      marker.setExtData({ farm });
      
      marker.on('click', () => {
        this.handleFarmClick(farm);
      });
      
      this.map.add(marker);
      this.farmMarkers.push(marker);
    },

    // ==================== 交互控制 ====================
    handleFarmClick(farm) {
      this.selectedFarm = farm;
      
      // 高亮选中的农田
      this.highlightFarm(farm);
      
      // 移动地图到农田位置
      if (farm.centerLng && farm.centerLat) {
        this.map.setCenter([farm.centerLng, farm.centerLat]);
        this.map.setZoom(18);
      }
      
      this.$message.success(`已选中：${farm.farm}（${farm.crop}）`);
    },

    highlightFarm(farm) {
      // 重置所有农田样式
      this.farmPolygons.forEach(p => {
        p.setOptions({
          strokeWeight: 2,
          strokeColor: '#fff',
          fillOpacity: 0.5
        });
      });
      
      // 高亮选中的农田
      const targetPolygon = this.farmPolygons.find(p => {
        const data = p.getExtData();
        return data && data.farm && data.farm.id === farm.id;
      });
      
      if (targetPolygon) {
        targetPolygon.setOptions({
          strokeWeight: 4,
          strokeColor: '#FFD700',
          fillOpacity: 0.7
        });
      }
    },

    resetView() {
      if (!this.map) return;
      this.map.setZoomAndCenter(16, [110.479191, 29.117096]);
      this.map.setPitch(60);
      this.map.setRotation(0);
    },

    toggle3DView() {
      if (!this.map) return;
      this.map3DEnabled = !this.map3DEnabled;
      this.map.setPitch(this.map3DEnabled ? 60 : 0);
    },

    toggleSatellite() {
      this.satelliteEnabled = !this.satelliteEnabled;
      // 切换地图样式
      if (this.satelliteEnabled) {
        this.map.setMapStyle('amap://styles/darkblue');
      } else {
        this.map.setMapStyle('amap://styles/normal');
      }
    },

    // ==================== 天气效果 ====================
    initWeatherCanvas() {
      this.weatherCanvas = this.$refs.weatherCanvas;
      if (!this.weatherCanvas) return;
      
      this.weatherCanvas.width = window.innerWidth;
      this.weatherCanvas.height = window.innerHeight;
      this.weatherCtx = this.weatherCanvas.getContext('2d');
      
      this.setWeather('sunny');
    },

    setWeather(mode) {
      this.weatherMode = mode;
      
      // 停止之前的动画
      if (this.weatherAnimationId) {
        cancelAnimationFrame(this.weatherAnimationId);
      }
      
      // 清空粒子
      this.weatherParticles = [];
      
      // 根据天气模式初始化粒子
      if (mode === 'rain' || mode === 'storm') {
        this.initRainParticles();
        this.animateRain();
      } else if (mode === 'snow') {
        this.initSnowParticles();
        this.animateSnow();
      }
      
      // 调整地图滤镜
      this.applyWeatherFilter(mode);
    },

    initRainParticles() {
      const count = this.weatherMode === 'storm' ? 500 : 300;
      for (let i = 0; i < count; i++) {
        this.weatherParticles.push({
          x: Math.random() * this.weatherCanvas.width,
          y: Math.random() * this.weatherCanvas.height,
          length: Math.random() * 20 + 10,
          speed: Math.random() * 5 + 5,
          opacity: Math.random() * 0.5 + 0.5
        });
      }
    },

    initSnowParticles() {
      for (let i = 0; i < 200; i++) {
        this.weatherParticles.push({
          x: Math.random() * this.weatherCanvas.width,
          y: Math.random() * this.weatherCanvas.height,
          radius: Math.random() * 3 + 1,
          speed: Math.random() * 1 + 0.5,
          drift: Math.random() * 2 - 1,
          opacity: Math.random() * 0.8 + 0.2
        });
      }
    },

    animateRain() {
      if (!this.weatherCtx) return;
      
      this.weatherCtx.clearRect(0, 0, this.weatherCanvas.width, this.weatherCanvas.height);
      
      this.weatherParticles.forEach(p => {
        this.weatherCtx.beginPath();
        this.weatherCtx.strokeStyle = `rgba(174, 194, 224, ${p.opacity})`;
        this.weatherCtx.lineWidth = 1;
        this.weatherCtx.moveTo(p.x, p.y);
        this.weatherCtx.lineTo(p.x, p.y + p.length);
        this.weatherCtx.stroke();
        
        p.y += p.speed;
        if (p.y > this.weatherCanvas.height) {
          p.y = -p.length;
          p.x = Math.random() * this.weatherCanvas.width;
        }
      });
      
      this.weatherAnimationId = requestAnimationFrame(this.animateRain);
    },

    animateSnow() {
      if (!this.weatherCtx) return;
      
      this.weatherCtx.clearRect(0, 0, this.weatherCanvas.width, this.weatherCanvas.height);
      
      this.weatherParticles.forEach(p => {
        this.weatherCtx.beginPath();
        this.weatherCtx.fillStyle = `rgba(255, 255, 255, ${p.opacity})`;
        this.weatherCtx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        this.weatherCtx.fill();
        
        p.y += p.speed;
        p.x += p.drift;
        
        if (p.y > this.weatherCanvas.height) {
          p.y = -p.radius;
          p.x = Math.random() * this.weatherCanvas.width;
        }
      });
      
      this.weatherAnimationId = requestAnimationFrame(this.animateSnow);
    },

    applyWeatherFilter(mode) {
      if (!this.map) return;
      
      const container = document.getElementById('amap-3d-container');
      if (!container) return;
      
      switch(mode) {
        case 'rain':
          container.style.filter = 'brightness(0.7) contrast(1.1)';
          break;
        case 'snow':
          container.style.filter = 'brightness(1.2) contrast(0.9)';
          break;
        case 'storm':
          container.style.filter = 'brightness(0.4) contrast(1.3)';
          break;
        default:
          container.style.filter = 'none';
      }
    },

    // ==================== 控制开关 ====================
    handleSwitchChange(type, value) {
      if (!this.selectedFarm) {
        this.$message.warning('请先选择一个地块');
        this.switchState[type] = false;
        return;
      }
      
      const actionNames = { water: '智能灌溉', fertilizer: '精准施肥', pest: '飞防消杀' };
      
      if (value) {
        this.handleAction(type);
      } else {
        if (type === 'water') {
          this.controlWaterPump(false);
        }
        this.$message.info(`[${this.selectedFarm.farm}] ${actionNames[type]}已关闭`);
      }
    },

    handleAction(type) {
      if (!this.selectedFarm) return;
      if (this.loadingState[type]) return;
      
      this.loadingState[type] = true;
      
      if (type === 'water') {
        this.$message.success(`[${this.selectedFarm.farm}] 智能喷灌系统已启动`);
        this.controlWaterPump(true);
        setTimeout(() => {
          let val = Number(this.selectedFarm.soilhumidity) + 40;
          if (val > 100) val = 90;
          this.selectedFarm.soilhumidity = val;
          this.syncFarmDataToBackend(this.selectedFarm);
        }, 2000);
      } else if (type === 'fertilizer') {
        this.$message.success(`[${this.selectedFarm.farm}] 无人机正在施肥...`);
        this.syncFarmDataToBackend(this.selectedFarm);
      } else if (type === 'pest') {
        this.$message.success(`[${this.selectedFarm.farm}] 无人机正在进行消杀作业`);
        this.syncFarmDataToBackend(this.selectedFarm);
      }
      
      setTimeout(() => {
        this.loadingState[type] = false;
        this.$message.success('✅ 农事作业完成');
      }, 4000);
    },

    async controlWaterPump(state) {
      try {
        const res = await this.request.post('/aether/device/control/bump', { bump: state });
        if (res.code === '200') {
          console.log('🚿 水泵控制成功:', state ? '开启' : '关闭');
          if (state) {
            setTimeout(() => {
              this.controlWaterPump(false);
              this.switchState.water = false;
            }, 4000);
          }
        }
      } catch (e) {
        console.warn('水泵控制请求:', state ? '开启' : '关闭', e.message || e);
      }
    },

    async syncFarmDataToBackend(farm) {
      try {
        await this.request.post('/statistic', {
          id: farm.id,
          farm: farm.farm,
          crop: farm.crop,
          area: farm.area,
          keeper: farm.keeper,
          temperature: farm.temperature,
          soilhumidity: farm.soilhumidity,
          airhumidity: farm.airhumidity,
          light: farm.light,
          state: farm.state
        });
        console.log('✅ 农田数据已同步到数据库');
      } catch (e) {
        console.error('同步数据失败:', e);
      }
    }
  }
};
</script>

<style scoped>
.farm-map-real {
  width: 100%;
  height: 100vh;
  background: #0a0e27;
  position: relative;
  overflow: hidden;
  font-family: 'Microsoft YaHei', 'Segoe UI', sans-serif;
}

/* ==================== 玻璃态效果 ==================== */
.glass-panel {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.37);
}

/* ==================== 顶部导航栏 ==================== */
.top-glass-bar {
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  height: 70px;
  z-index: 1000;
  background: rgba(10, 14, 39, 0.85);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.brand-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-pulse {
  width: 12px;
  height: 12px;
  background: #00f2f1;
  border-radius: 50%;
  box-shadow: 0 0 20px #00f2f1;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.8; }
}

.title-group {
  display: flex;
  flex-direction: column;
}

.main-title {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  letter-spacing: 2px;
}

.sub-title {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.stats-pills {
  display: flex;
  gap: 16px;
}

.pill-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.pill-item i {
  color: #00f2f1;
  font-size: 18px;
}

.pill-item .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.pill-item .value {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
}

.pill-item small {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: 2px;
}

.time-weather {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-time {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-family: 'Consolas', monospace;
}

/* ==================== 左侧面板 ==================== */
.left-panel {
  position: absolute;
  left: 20px;
  top: 110px;
  bottom: 20px;
  width: 320px;
  z-index: 900;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
  color: #fff;
}

.panel-header i {
  color: #00f2f1;
  font-size: 20px;
}

.header-badge {
  margin-left: auto;
  background: #00f2f1;
  color: #0a0e27;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: bold;
}

.farm-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
}

.farm-list::-webkit-scrollbar {
  width: 6px;
}

.farm-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.farm-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.farm-card:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(4px);
}

.farm-card.active {
  background: rgba(0, 242, 241, 0.15);
  border-color: #00f2f1;
  box-shadow: 0 0 20px rgba(0, 242, 241, 0.3);
}

.farm-card.warning {
  border-color: rgba(245, 108, 108, 0.5);
}

.farm-icon-wrapper {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.crop-icon {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  padding: 4px;
}

.warning-dot {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 12px;
  height: 12px;
  background: #F56C6C;
  border-radius: 50%;
  border: 2px solid #0a0e27;
  animation: pulse 1.5s infinite;
}

.farm-info {
  flex: 1;
  min-width: 0;
}

.farm-name {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 4px;
}

.farm-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
}

.crop-tag, .area-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.farm-sensors {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.farm-sensors span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.farm-sensors span.danger {
  color: #F56C6C;
  font-weight: bold;
}

.farm-sensors i {
  font-size: 14px;
}

.farm-status {
  flex-shrink: 0;
}

/* ==================== 地图容器 ==================== */
.map-container {
  position: absolute;
  left: 360px;
  right: 20px;
  top: 110px;
  bottom: 20px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

#amap-3d-container {
  width: 100%;
  height: 100%;
  transition: filter 0.5s;
}

.weather-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

/* ==================== 地图控制面板 ==================== */
.map-controls {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 100;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s;
  font-size: 18px;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.control-btn.active {
  background: #00f2f1;
  color: #0a0e27;
}

.divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.2);
}

.divider-h {
  height: 1px;
  width: 100%;
  background: rgba(255, 255, 255, 0.2);
  margin: 8px 0;
}

.weather-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.weather-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
}

.weather-buttons {
  display: flex;
  gap: 8px;
}

.weather-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s;
  opacity: 0.6;
}

.weather-btn:hover {
  opacity: 1;
  transform: scale(1.1);
}

.weather-btn.active {
  opacity: 1;
  background: rgba(0, 242, 241, 0.2);
  box-shadow: 0 0 12px rgba(0, 242, 241, 0.5);
}

.weather-btn img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

/* ==================== 右侧详情面板 ==================== */
.right-panel {
  position: absolute;
  right: 20px;
  top: 110px;
  bottom: 20px;
  width: 360px;
  z-index: 900;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.2) transparent;
}

.right-panel::-webkit-scrollbar {
  width: 6px;
}

.right-panel::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.right-panel .panel-header {
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 健康评分区域 */
.health-section {
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.health-score {
  flex-shrink: 0;
}

.score-text {
  display: block;
  font-size: 28px;
  font-weight: bold;
  color: #fff;
}

.score-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
}

.health-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.crop-badge {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.crop-name {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.area-info {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.data-source-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(0, 242, 241, 0.1);
  border: 1px solid rgba(0, 242, 241, 0.3);
  border-radius: 8px;
  font-size: 12px;
  color: #00f2f1;
}

/* 环境数据区域 */
.env-data-section {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.section-title i {
  color: #00f2f1;
  font-size: 16px;
}

.ai-icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.env-item {
  margin-bottom: 16px;
}

.env-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.env-label i {
  font-size: 16px;
  color: #00f2f1;
}

.env-value {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.value-num {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.value-num.danger {
  color: #F56C6C;
}

/* AI决策建议 */
.ai-advice-section {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.advice-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: rgba(103, 194, 58, 0.1);
  border: 1px solid rgba(103, 194, 58, 0.3);
  border-radius: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.6;
}

.advice-content.warning {
  background: rgba(245, 108, 108, 0.1);
  border-color: rgba(245, 108, 108, 0.3);
}

.advice-content i {
  flex-shrink: 0;
  font-size: 16px;
  margin-top: 2px;
}

/* 控制开关区域 */
.control-section {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.control-switches {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.switch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  transition: all 0.3s;
}

.switch-card.active {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: 0 0 16px rgba(16, 185, 129, 0.2);
}

.switch-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.switch-icon.water {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
}

.switch-icon.fertilizer {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
}

.switch-icon.pest {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}

.switch-icon img {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.switch-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.switch-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.switch-status {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.switch-card.active .switch-status {
  color: #10b981;
}

/* ==================== 动画效果 ==================== */
.slide-left-enter-active, .slide-left-leave-active {
  transition: all 0.3s ease;
}

.slide-left-enter, .slide-left-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>

