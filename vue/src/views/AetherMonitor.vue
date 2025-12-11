<template>
  <div class="aether-monitor">
    <!-- 绿色粒子背景 -->
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    
    <!-- 顶部标题栏 -->
    <div class="header">
      <div class="title">AI 视觉识别监控</div>
      <div class="status-group">
        <el-tag :type="deviceOnline ? 'success' : 'danger'" size="small">
          {{ deviceOnline ? '设备在线' : '设备离线' }}
        </el-tag>
      </div>
    </div>

    <!-- 关键指标卡片 -->
    <el-row :gutter="12" class="kpi-row">
      <!-- 室外温度 -->
      <el-col :span="6">
        <el-card class="kpi-card weather-card">
          <div class="kpi-label">室外温度</div>
          <div class="kpi-value">
            {{ weatherNow && weatherNow.temp ? weatherNow.temp + '℃' : '--' }}
          </div>
          <div class="kpi-desc">
            {{ weatherNow && weatherNow.text || '加载中...' }}
          </div>
        </el-card>
      </el-col>

      <!-- 室内温度 -->
      <el-col :span="6">
        <el-card class="kpi-card">
          <div class="kpi-label">室内温度</div>
          <div class="kpi-value">{{ temperature === null ? '--' : temperature + '℃' }}</div>
          <div class="kpi-desc">STM32 实时检测</div>
        </el-card>
      </el-col>

      <!-- 室内湿度 -->
      <el-col :span="6">
        <el-card class="kpi-card">
          <div class="kpi-label">室内湿度</div>
          <div class="kpi-value">{{ humidity === null ? '--' : humidity + '%' }}</div>
          <div class="kpi-desc">STM32 实时检测</div>
        </el-card>
      </el-col>

      <!-- 光照灯控制 -->
      <el-col :span="6">
        <el-card class="kpi-card lamp-card">
          <div class="kpi-label">光照灯</div>
          <div class="lamp-control">
            <img src="@/assets/sun.png" class="lamp-icon" :class="{ active: ledOn }" alt="光照灯" />
            <el-switch
              v-model="ledOn"
              @change="toggleLamp"
              active-color="#13ce66"
              inactive-color="#dcdfe6"
            ></el-switch>
          </div>
          <div class="kpi-desc">{{ ledOn ? '已开启' : '已关闭' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 天气预报和预警 -->
    <el-row :gutter="12" class="weather-row" style="max-height: 240px;">
      <!-- 天气预报 -->
      <el-col :span="16">
        <el-card class="weather-forecast-card" style="max-height: 240px; overflow: hidden;">
          <div slot="header" class="card-header">
            <img src="@/assets/yubao.png" class="header-icon" alt="天气预报" /><span>天气预报</span>
            <!-- 现代化分段控制器 -->
            <div class="segment-control">
              <button 
                :class="['segment-btn', { active: activeWeatherTab === '24h' }]" 
                @click="activeWeatherTab = '24h'"
              >24小时</button>
              <button 
                :class="['segment-btn', { active: activeWeatherTab === '7d' }]" 
                @click="activeWeatherTab = '7d'"
              >4天</button>
            </div>
          </div>
          <!-- 24小时预报 -->
          <div class="forecast-rail" v-if="activeWeatherTab === '24h'">
            <template v-if="weather24h.length > 0">
              <button class="scroll-btn scroll-left" @click="scrollForecast(-200)">‹</button>
              <div class="forecast-scroll-wrapper">
                <div class="forecast-scroll" ref="forecastScroll24h">
                  <!-- SVG 温度趋势线 -->
                  <svg class="temp-trend-svg" :width="weather24h.length * 74" height="130" preserveAspectRatio="none">

                    <!-- 渐变定义 -->
                    <defs>
                      <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" style="stop-color:rgba(5,150,105,0.4)"/>
                        <stop offset="50%" style="stop-color:rgba(5,150,105,0.7)"/>
                        <stop offset="100%" style="stop-color:rgba(5,150,105,0.4)"/>
                      </linearGradient>
                      <filter id="glow">
                        <feGaussianBlur stdDeviation="1.5" result="coloredBlur"/>
                        <feMerge>
                          <feMergeNode in="coloredBlur"/>
                          <feMergeNode in="SourceGraphic"/>
                        </feMerge>
                      </filter>
                    </defs>
                    <!-- 平滑曲线 -->
                    <path 
                      :d="getTrendPath()" 
                      fill="none" 
                      stroke="url(#lineGradient)" 
                      stroke-width="2"
                      stroke-linecap="round"
                      filter="url(#glow)"
                    />
                    <!-- 数据点 -->
                    <circle 
                      v-for="(item, index) in weather24h" 
                      :key="'dot-' + index"
                      :cx="index * 74 + 37"
                      :cy="getTempY(item.temp)"
                      r="4"
                      fill="#059669"
                      stroke="rgba(5,150,105,0.3)"
                      stroke-width="2"
                    />
                  </svg>
                  <!-- 天气卡片 -->
                  <div 
                    v-for="(item, index) in weather24h" 
                    :key="index" 
                    class="forecast-capsule"
                  >
                    <div class="capsule-time">{{ formatTime(item.fxTime) }}</div>
                    <div class="capsule-icon">{{ getWeatherIcon(item.text, item.fxTime) }}</div>
                    <div class="capsule-temp">{{ item.temp }}°</div>
                    <div class="capsule-wind">{{ item.windDir || '微风' }}</div>
                  </div>
                </div>
              </div>
              <button class="scroll-btn scroll-right" @click="scrollForecast(200)">›</button>
            </template>
            <el-empty v-else description="暂无数据" :image-size="60"></el-empty>
          </div>
          <!-- 4天预报 -->
          <div class="forecast-rail" v-else>
            <div v-if="weather7d.length > 0" class="forecast-scroll">
              <div 
                v-for="(item, index) in weather7d" 
                :key="index" 
                class="forecast-capsule"
              >
                <div class="capsule-time">{{ formatDate(item.fxDate) }}</div>
                <div class="capsule-icon">{{ getWeatherIcon(item.textDay) }}</div>
                <div class="capsule-temp">{{ item.tempMin }}°<span class="temp-divider">/</span>{{ item.tempMax }}°</div>
              </div>
            </div>
            <el-empty v-else description="暂无数据" :image-size="60"></el-empty>
          </div>
        </el-card>
      </el-col>

      <!-- 预警信息 -->
      <el-col :span="8">
        <el-card class="alert-card" style="max-height: 240px; overflow: hidden;">
          <div slot="header" class="card-header">
            <span>⚠️ 预警信息</span>
          </div>
          <div class="alert-list">
            <!-- 天气预警 -->
            <div v-if="weatherAlerts.length > 0">
              <div v-for="(alert, index) in weatherAlerts" :key="'weather-' + index" class="alert-item warning">
                <i class="el-icon-warning"></i>
                <div class="alert-content">
                  <div class="alert-title">{{ alert.title }}</div>
                  <div class="alert-desc">{{ alert.text }}</div>
                </div>
              </div>
            </div>
            <!-- 温湿度预警 -->
            <div v-if="tempAlert" class="alert-item danger">
              <i class="el-icon-warning"></i>
              <div class="alert-content">
                <div class="alert-title">温度异常</div>
                <div class="alert-desc">{{ tempAlert }}</div>
              </div>
            </div>
            <div v-if="humiAlert" class="alert-item warning">
              <i class="el-icon-warning"></i>
              <div class="alert-content">
                <div class="alert-title">湿度异常</div>
                <div class="alert-desc">{{ humiAlert }}</div>
              </div>
            </div>
            <el-empty v-if="!weatherAlerts.length && !tempAlert && !humiAlert" description="暂无预警" :image-size="60"></el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史数据图表 -->
    <el-row :gutter="12" class="chart-row">
      <el-col :span="12">
        <el-card>
          <div slot="header" class="card-header">
            <img src="@/assets/qushi.png" class="header-icon" alt="温度趋势" /><span>温度趋势</span>
          </div>
          <div ref="tempChart" style="height: 160px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div slot="header" class="card-header">
            <span>💧 湿度趋势</span>
          </div>
          <div ref="humiChart" style="height: 160px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'AetherMonitor',
  data() {
    return {
      // 设备状态
      deviceOnline: false,
      temperature: null,
      humidity: null,
      ledOn: false,
      
      // 天气数据
      weatherNow: null,
      weather24h: [],
      weather7d: [],
      weatherAlerts: [],
      activeWeatherTab: '24h',
      
      // 预警
      tempAlert: '',
      humiAlert: '',
      
      // 阈值
      tempThresholdHigh: 30,
      tempThresholdLow: 10,
      humiThresholdHigh: 75,
      humiThresholdLow: 25,
      
      // 历史数据
      historyData: [],
      
      // 图表实例
      tempChartInstance: null,
      humiChartInstance: null,
      
      // 轮询定时器
      pollingTimer: null,
      weatherTimer: null,
      
      // 粒子动画相关
      particles: [],
      canvas: null,
      ctx: null,
      animationId: null,
      canvasWidth: 0,
      canvasHeight: 0,
      mouseX: 0,
      mouseY: 0,
      mouseMoving: false
    }
  },
  
  mounted() {
    this.initParticleCanvas();
    this.initCharts();
    this.fetchDeviceStatus();
    this.fetchWeatherData();
    this.fetchHistoryData();
    
    // 启动轮询
    this.pollingTimer = setInterval(() => {
      this.fetchDeviceStatus();
      this.fetchHistoryData(); // 实时刷新图表数据
    }, 2000); // 每2秒更新一次设备状态和图表
    
    this.weatherTimer = setInterval(() => {
      this.fetchWeatherData();
    }, 300000); // 每5分钟更新一次天气
  },
  
  beforeDestroy() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
    if (this.weatherTimer) {
      clearInterval(this.weatherTimer);
    }
    if (this.tempChartInstance) {
      this.tempChartInstance.dispose();
    }
    if (this.humiChartInstance) {
      this.humiChartInstance.dispose();
    }
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
    }
    window.removeEventListener('resize', this.resizeCanvas);
  },
  
  methods: {
    // 滚动天气预报
    scrollForecast(delta) {
      const el = this.$refs.forecastScroll24h;
      if (el) {
        el.scrollBy({ left: delta, behavior: 'smooth' });
      }
    },
    
    // 获取温度对应的Y坐标
    getTempY(temp) {
      if (!this.weather24h || this.weather24h.length === 0) return 45;
      const temps = this.weather24h.map(item => parseInt(item.temp));
      const minTemp = Math.min(...temps);
      const maxTemp = Math.max(...temps);
      const range = maxTemp - minTemp || 1;
      // Y坐标范围: 30 (顶部) 到 60 (底部)
      return 60 - ((parseInt(temp) - minTemp) / range) * 30;
    },
    
    // 生成平滑贝塞尔曲线路径
    getTrendPath() {
      if (!this.weather24h || this.weather24h.length < 2) return '';
      
      const points = this.weather24h.map((item, index) => ({
        x: index * 74 + 37,
        y: this.getTempY(item.temp)
      }));
      
      // 使用 Catmull-Rom 样条转贝塞尔曲线
      let path = `M ${points[0].x} ${points[0].y}`;
      
      for (let i = 0; i < points.length - 1; i++) {
        const p0 = points[Math.max(0, i - 1)];
        const p1 = points[i];
        const p2 = points[i + 1];
        const p3 = points[Math.min(points.length - 1, i + 2)];
        
        // 计算控制点
        const cp1x = p1.x + (p2.x - p0.x) / 6;
        const cp1y = p1.y + (p2.y - p0.y) / 6;
        const cp2x = p2.x - (p3.x - p1.x) / 6;
        const cp2y = p2.y - (p3.y - p1.y) / 6;
        
        path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
      }
      
      return path;
    },
    
    // 获取设备状态
    async fetchDeviceStatus() {
      try {
        const res = await this.request.get('/aether/device/status');
        
        if (res.code === '200' && res.data) {
          const data = res.data;
          this.deviceOnline = data.online;
          this.temperature = data.temperature;
          this.humidity = data.humidity;
          this.ledOn = data.led === 1;
          
          // 检查阈值预警
          this.checkThresholds();
        }
      } catch (error) {
        console.error('获取设备状态失败:', error);
        this.deviceOnline = false;
      }
    },
    
    // 控制LED灯
    async toggleLamp() {
      try {
        const res = await this.request.post('/aether/device/control/led', {
          led: this.ledOn ? 1 : 0
        });
        
        if (res.code === '200') {
          this.$message.success(this.ledOn ? '灯已开启' : '灯已关闭');
        } else {
          this.$message.error('控制失败');
          this.ledOn = !this.ledOn; // 恢复状态
        }
      } catch (error) {
        console.error('控制LED失败:', error);
        this.$message.error('控制失败');
        this.ledOn = !this.ledOn;
      }
    },
    
    // 获取天气数据
    async fetchWeatherData() {
      try {
        // 获取实时天气
        const nowRes = await this.request.get('/aether/weather/now');
        if (nowRes.code === '200' && nowRes.data && nowRes.data.data) {
          this.weatherNow = nowRes.data.data;
        }
        
        // 尝试获取24小时天气，如果失败则从7d接口获取
        try {
          const hourlyRes = await this.request.get('/aether/weather/24h');
          if (hourlyRes.code === '200' && hourlyRes.data && hourlyRes.data.data) {
            this.weather24h = hourlyRes.data.data || [];
          }
        } catch (e) {
          console.log('24h接口不可用，将从7d接口获取hourly数据');
        }
        
        // 获取7天天气
        const weekRes = await this.request.get('/aether/weather/7d');
        if (weekRes.code === '200' && weekRes.data && weekRes.data.data) {
          this.weather7d = weekRes.data.data.daily || weekRes.data.data || [];
          // 如果24h数据为空，使用7d接口的hourly数据
          if (!this.weather24h || this.weather24h.length === 0) {
            this.weather24h = weekRes.data.data.hourly || [];
          }
        }
        
        // 获取预警
        const alertRes = await this.request.get('/aether/weather/alerts');
        if (alertRes.code === '200' && alertRes.data) {
          this.weatherAlerts = alertRes.data.data || [];
        }
      } catch (error) {
        console.error('获取天气数据失败:', error);
      }
    },
    
    // 获取历史数据
    async fetchHistoryData() {
      try {
        const res = await this.request.get('/aether/readings/detail?days=30');
        
        if (res.code === '200' && res.data && res.data.data) {
          this.historyData = res.data.data || [];
          this.updateCharts();
        }
      } catch (error) {
        console.error('获取历史数据失败:', error);
      }
    },
    
    // 检查阈值
    checkThresholds() {
      this.tempAlert = '';
      this.humiAlert = '';
      
      if (this.temperature !== null) {
        if (this.temperature > this.tempThresholdHigh) {
          this.tempAlert = `当前温度 ${this.temperature}℃ 超过上限 ${this.tempThresholdHigh}℃`;
        } else if (this.temperature < this.tempThresholdLow) {
          this.tempAlert = `当前温度 ${this.temperature}℃ 低于下限 ${this.tempThresholdLow}℃`;
        }
      }
      
      if (this.humidity !== null) {
        if (this.humidity > this.humiThresholdHigh) {
          this.humiAlert = `当前湿度 ${this.humidity}% 超过上限 ${this.humiThresholdHigh}%`;
        } else if (this.humidity < this.humiThresholdLow) {
          this.humiAlert = `当前湿度 ${this.humidity}% 低于下限 ${this.humiThresholdLow}%`;
        }
      }
    },
    
    // 初始化图表
    initCharts() {
      // 确保DOM元素存在
      if (!this.$refs.tempChart || !this.$refs.humiChart) {
        console.warn('图表DOM元素未找到，延迟初始化');
        this.$nextTick(() => {
          if (this.$refs.tempChart && this.$refs.humiChart) {
            this.tempChartInstance = echarts.init(this.$refs.tempChart);
            this.humiChartInstance = echarts.init(this.$refs.humiChart);
          }
        });
        return;
      }
      
      this.tempChartInstance = echarts.init(this.$refs.tempChart);
      this.humiChartInstance = echarts.init(this.$refs.humiChart);
      
      window.addEventListener('resize', () => {
        this.tempChartInstance && this.tempChartInstance.resize();
        this.humiChartInstance && this.humiChartInstance.resize();
      });
    },
    
    // 更新图表
    updateCharts() {
      if (!this.historyData || this.historyData.length === 0) return;
      
      // 按时间排序，确保从早到晚的顺序
      const sortedData = [...this.historyData].sort((a, b) => new Date(a.date) - new Date(b.date));
      
      const times = sortedData.map(item => {
        const date = new Date(item.date);
        return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
      });
      const temps = sortedData.map(item => item.temp);
      const humis = sortedData.map(item => item.humi);
      
      // 温度图表
      const tempOption = {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: times,
          boundaryGap: false
        },
        yAxis: {
          type: 'value',
          name: '温度(℃)'
        },
        series: [{
          name: '温度',
          type: 'line',
          smooth: true,
          data: temps,
          itemStyle: {
            color: '#409EFF'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(64, 158, 255, 0.3)'
              }, {
                offset: 1, color: 'rgba(64, 158, 255, 0.05)'
              }]
            }
          }
        }]
      };
      
      // 湿度图表
      const humiOption = {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: times,
          boundaryGap: false
        },
        yAxis: {
          type: 'value',
          name: '湿度(%)'
        },
        series: [{
          name: '湿度',
          type: 'line',
          smooth: true,
          data: humis,
          itemStyle: {
            color: '#67C23A'
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, color: 'rgba(103, 194, 58, 0.3)'
              }, {
                offset: 1, color: 'rgba(103, 194, 58, 0.05)'
              }]
            }
          }
        }]
      };
      
      this.tempChartInstance.setOption(tempOption);
      this.humiChartInstance.setOption(humiOption);
    },
    
    // 格式化时间
    formatTime(timeStr) {
      const date = new Date(timeStr);
      return `${date.getHours()}:00`;
    },
    
    // 格式化日期
    formatDate(dateStr) {
      const date = new Date(dateStr);
      return `${date.getMonth() + 1}/${date.getDate()}`;
    },
    
    // 获取天气图标
    getWeatherIcon(text, timeStr) {
      let isNight = false;
      if (timeStr) {
        const hour = new Date(timeStr).getHours();
        isNight = hour >= 18 || hour < 6;
      }

      const iconMap = {
        '晴': isNight ? '🌙' : '☀️',
        '多云': isNight ? '☁️' : '⛅',
        '阴': '☁️',
        '雨': '🌧️',
        '雪': '❄️',
        '雷': '⚡'
      };
      
      for (let key in iconMap) {
        if (text && text.includes(key)) {
          return iconMap[key];
        }
      }
      return isNight ? '🌙' : '🌤️';
    },
    
    // ========== 粒子动画方法 ==========
    
    initParticleCanvas() {
      this.$nextTick(() => {
        this.canvas = this.$refs.particleCanvas;
        if (!this.canvas) return;
        
        this.ctx = this.canvas.getContext('2d');
        this.resizeCanvas();
        window.addEventListener('resize', this.resizeCanvas);
        this.initParticles();
        this.animate();
      });
    },
    
    resizeCanvas() {
      if (!this.canvas) return;
      const container = this.canvas.parentElement;
      if (container) {
        const { width, height } = container.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        this.canvas.width = width * dpr;
        this.canvas.height = height * dpr;
        this.ctx.scale(dpr, dpr);
        this.canvas.style.width = `${width}px`;
        this.canvas.style.height = `${height}px`;
        this.canvasWidth = width;
        this.canvasHeight = height;
        if (this.particles.length === 0) {
          this.initParticles(Math.floor(width * height / 10000));
        }
      }
    },
    
    initParticles(count = 120) {
      const particles = [];
      const width = this.canvasWidth;
      const height = this.canvasHeight;
      for (let i = 0; i < count; i++) {
        const type = Math.random() > 0.85 ? 'special' : (Math.random() > 0.7 ? 'bright' : 'normal');
        const angle = Math.random() * Math.PI * 2;
        const speed = 0.1 + Math.random() * 0.15;
        particles.push({
          x: Math.random() * width,
          y: Math.random() * height,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          r: type === 'special' ? 4 : (type === 'bright' ? 3 : 2),
          alpha: type === 'special' ? 0.9 : (type === 'bright' ? 0.75 : 0.6),
          color: this.getRandomGreenColor(),
          type: type
        });
      }
      this.particles = particles;
    },
    
    getRandomGreenColor() {
      const colors = [[76, 175, 80], [102, 187, 106], [129, 199, 132], [165, 214, 167], [144, 238, 144], [50, 205, 50]];
      return colors[Math.floor(Math.random() * colors.length)];
    },
    
    animate() {
      if (!this.ctx) return;
      const ctx = this.ctx;
      const particles = this.particles;
      const width = this.canvasWidth;
      const height = this.canvasHeight;
      
      ctx.clearRect(0, 0, width, height);
      const bgGradient = ctx.createLinearGradient(0, 0, width, height);
      bgGradient.addColorStop(0, "rgba(232, 236, 241, 0.3)");
      bgGradient.addColorStop(0.5, "rgba(222, 228, 235, 0.2)");
      bgGradient.addColorStop(1, "rgba(232, 236, 241, 0.3)");
      ctx.fillStyle = bgGradient;
      ctx.fillRect(0, 0, width, height);
      
      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];
        p.x = (p.x + p.vx + width) % width;
        p.y = (p.y + p.vy + height) % height;
        const speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy);
        if (speed > 1) {
          p.vx *= 0.98;
          p.vy *= 0.98;
        }
        
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${p.color[0]}, ${p.color[1]}, ${p.color[2]}, ${p.alpha})`;
        ctx.fill();
        
        if (p.type === 'special' || p.type === 'bright') {
          const gradient = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 5);
          gradient.addColorStop(0, `rgba(${p.color[0]}, ${p.color[1]}, ${p.color[2]}, ${p.alpha * 0.5})`);
          gradient.addColorStop(0.5, `rgba(${p.color[0]}, ${p.color[1]}, ${p.color[2]}, ${p.alpha * 0.2})`);
          gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
          ctx.fillStyle = gradient;
          ctx.fillRect(p.x - p.r * 5, p.y - p.r * 5, p.r * 10, p.r * 10);
        }
        
        for (let j = i + 1; j < particles.length; j++) {
          const p2 = particles[j];
          const dx = p.x - p2.x;
          const dy = p.y - p2.y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          if (distance < 150) {
            ctx.beginPath();
            ctx.moveTo(p.x, p.y);
            ctx.lineTo(p2.x, p2.y);
            const opacity = (1 - distance / 150) * 0.4;
            ctx.strokeStyle = `rgba(76, 175, 80, ${opacity})`;
            ctx.lineWidth = 1;
            ctx.stroke();
          }
        }
      }
      this.animationId = requestAnimationFrame(this.animate);
    }
  }
}
</script>

<style scoped>
.aether-monitor {
  position: relative;
  padding: 12px;
  min-height: 100vh;
  max-height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(to bottom, #E8ECF1 0%, #DEE4EB 100%);
  /* 隐藏滚动条 */
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE/Edge */
}

.aether-monitor::-webkit-scrollbar {
  display: none !important; /* Chrome/Safari */
  width: 0 !important;
  height: 0 !important;
}

/* 粒子画布 */
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* 确保内容在粒子之上 */
.aether-monitor > *:not(.particle-canvas) {
  position: relative;
  z-index: 1;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.08), 0 2px 6px -1px rgba(0, 0, 0, 0.04);
  border: none;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #1F2937;
}

.kpi-row {
  margin-bottom: 8px;
}

.kpi-card {
  text-align: center;
  background: rgba(255, 255, 255, 0.55) !important;
  backdrop-filter: blur(12px) !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px -2px rgba(76, 175, 80, 0.2), 0 2px 6px -1px rgba(0, 0, 0, 0.06) !important;
  border: 2px solid rgba(76, 175, 80, 0.5) !important;
  height: 120px !important;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.kpi-label {
  font-size: 12px;
  color: #6B7280;
  margin-bottom: 4px;
  font-weight: 500;
}

.kpi-value {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
  margin: 4px 0;
}

.kpi-desc {
  font-size: 11px;
  color: #9CA3AF;
  margin-top: 3px;
}

.weather-card .kpi-value {
  color: #E6A23C;
}

.lamp-card .lamp-control {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}

.lamp-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
  opacity: 0.4;
  filter: grayscale(100%);
}

.lamp-icon.active {
  opacity: 1;
  filter: grayscale(0%);
}

.weather-row {
  margin-bottom: 4px;
  display: flex;
  align-items: stretch;
}

.weather-row .el-col {
  display: flex;
}

.weather-row .el-card {
  flex: 1;
}

.chart-row {
  margin-bottom: 4px;
}

.card-header {
  font-weight: 600;
  font-size: 14px;
  color: #1F2937;
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

/* 分段控制器 */
.segment-control {
  margin-left: auto;
  display: flex;
  background: #F3F4F6;
  border-radius: 20px;
  padding: 3px;
  gap: 2px;
}

.segment-btn {
  padding: 5px 14px;
  border: none;
  background: transparent;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: #6B7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.segment-btn.active {
  background: #FFFFFF;
  color: #059669;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  font-weight: 600;
}

.segment-btn:hover:not(.active) {
  color: #374151;
}

/* 天气预报滚动轨道 */
.forecast-rail {
  overflow: visible;
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 滚动容器包装器 - 裁剪SVG超出部分 */
.forecast-scroll-wrapper {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.scroll-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(5, 150, 105, 0.1);
  color: #059669;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.scroll-btn:hover {
  background: rgba(5, 150, 105, 0.2);
  transform: none;
}

.forecast-scroll {
  position: relative;
  display: flex;
  gap: 4px;
  overflow: hidden;
  overflow-x: auto;
  padding: 10px 4px 10px 4px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  min-height: 130px;
  max-height: 130px;
}

/* 隐藏滚动条 */
.forecast-scroll::-webkit-scrollbar {
  height: 0;
  display: none;
}

/* SVG 温度趋势线 */
.temp-trend-svg {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

/* 天气胶囊卡片 */
.forecast-capsule {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 12px 8px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(229, 231, 235, 0.6);
  border-radius: 12px;
  min-width: 68px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  z-index: 2;
}

.forecast-capsule:hover {
  transform: none;
  background: #FFFFFF;
  box-shadow: none;
  border-color: #e5e7eb;
}

.capsule-time {
  font-size: 11px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
  font-family: 'SF Mono', 'Roboto Mono', monospace;
}

.capsule-icon {
  font-size: 20px;
  margin: 2px 0 4px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.capsule-temp {
  font-size: 13px;
  font-weight: 700;
  color: #059669;
  font-family: 'SF Mono', 'Roboto Mono', monospace;
  letter-spacing: -0.5px;
  margin-bottom: 2px;
}

.capsule-wind {
  font-size: 9px;
  font-weight: 500;
  color: #9CA3AF;
  margin-top: 1px;
}

.temp-divider {
  color: #9CA3AF;
  margin: 0 2px;
  font-weight: 400;
}

.alert-list {
  min-height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  padding: 6px;
  margin-bottom: 4px;
  border-radius: 6px;
  background: #FEF0F0;
  border-left: 3px solid #F56C6C;
}

.alert-item.warning {
  background: #FDF6EC;
  border-left-color: #E6A23C;
}

.alert-item i {
  font-size: 20px;
  margin-right: 10px;
  color: #F56C6C;
}

.alert-item.warning i {
  color: #E6A23C;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-weight: 600;
  font-size: 13px;
  color: #1F2937;
  margin-bottom: 4px;
}

.alert-desc {
  font-size: 11px;
  color: #4B5563;
}

/* 所有卡片统一背景 */
.weather-forecast-card,
.alert-card,
.chart-row .el-card {
  background: #ffffff !important;
  backdrop-filter: none !important;
  border-radius: 12px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid #e5e7eb !important;
}

/* 预警卡片高度与天气预报一致 */
.alert-card {
  height: 100%;
}

.alert-card .el-card__body {
  padding: 10px !important;
  height: calc(100% - 44px);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Element UI 卡片样式 */
.el-card {
  background: #ffffff !important;
  backdrop-filter: none !important;
  border-radius: 12px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid #e5e7eb !important;
}

.weather-forecast-card .el-card__header,
.alert-card .el-card__header {
  background: #fafafa !important;
  backdrop-filter: none !important;
  padding: 3px 6px !important;
  border-bottom: 1px solid #e5e7eb !important;
  border-radius: 12px 12px 0 0 !important;
}

.el-card__body {
  background: #ffffff !important;
  padding: 8px !important;
}

/* 只针对天气预报卡片调整内边距 */
.weather-forecast-card .el-card__body {
  padding: 6px !important;
}

.alert-card .el-card__body {
  padding: 6px !important;
}

/* 去掉所有浮动和动态效果 */
* {
  transition: none !important;
  animation: none !important;
}

.weather-forecast-card:hover,
.alert-card:hover,
.el-card:hover {
  transform: none !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06) !important;
}

.forecast-capsule {
  transition: none !important;
}

.forecast-capsule:hover {
  transform: none !important;
  box-shadow: none !important;
  background: rgba(255, 255, 255, 0.85) !important;
  border-color: rgba(229, 231, 235, 0.6) !important;
}

.scroll-btn {
  transition: none !important;
}

.scroll-btn:hover {
  transform: none !important;
}

.segment-btn {
  transition: none !important;
}

.kpi-card:hover {
  transform: none !important;
}

.dot-pulse {
  animation: none !important;
}

/* 隐藏所有滚动条 */
.aether-monitor {
  overflow: hidden !important;
}

.aether-monitor ::-webkit-scrollbar {
  display: none !important;
  width: 0 !important;
}

.aether-monitor * {
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

.alert-list {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.alert-list::-webkit-scrollbar {
  display: none;
}
</style>

<style>
/* 全局隐藏滚动条 */
.el-main {
  overflow: hidden !important;
}
.el-main::-webkit-scrollbar {
  display: none !important;
}
html, body {
  overflow: hidden !important;
}

/* KPI卡片边框样式 - 非scoped覆盖Element UI */
.aether-monitor .kpi-row .el-card {
  border: 2px solid rgba(76, 175, 80, 0.5) !important;
  background: rgba(255, 255, 255, 0.55) !important;
}
.aether-monitor .kpi-row .el-card .el-card__body {
  background: transparent !important;
}
</style>
