<template>
  <div class="data-screen-container">
    <div class="header-wrapper">
      <div class="header-content">
        <div class="header-time">{{ timeStr }}</div>
        <div class="header-title">
          <span>智慧农场全域态势感知平台</span>
          <div class="sub-title">INTELLIGENT FARM SITUATIONAL AWARENESS PLATFORM</div>
        </div>
        <div class="header-weather">
          <span class="weather-icon">🌤️</span>
          <span>26°C 适宜农事</span>
          <div class="exit-btn" @click="$router.push('/home')">
            <i class="el-icon-switch-button"></i> 退出
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      
      <div class="panel-column left">
        
        <div class="data-panel">
          <div class="panel-title">
            <i class="el-icon-s-data"></i> 生产概况
            <span class="decoration"></span>
          </div>
          <div class="panel-body">
            <div class="kpi-row">
              <div class="kpi-item">
                <div class="num" style="color: #00f2ff">¥{{ totalRevenue.toLocaleString() }}</div>
                <div class="label">总营收</div>
              </div>
              <div class="kpi-item">
                <div class="num" style="color: #4cd964">{{ profitRate }}</div>
                <div class="label">利润率</div>
              </div>
            </div>
            <div class="chart-box" ref="chartLeft1"></div>
          </div>
        </div>

        <div class="data-panel">
          <div class="panel-title">
            <i class="el-icon-video-camera"></i> 实时视频监控
            <span class="decoration"></span>
            <select v-if="farmlandList.length > 0" v-model="selectedFarmland" class="farmland-select" @change="handleFarmlandChange">
              <option v-for="farm in farmlandList" :key="farm.id" :value="farm.id">{{ farm.name }}</option>
            </select>
          </div>
          <div class="panel-body live-video-body">
            <div class="live-video-container">
              <img 
                :src="currentVideoStreamUrl" 
                alt="实时监控" 
                class="live-video-stream"
                @load="onVideoLoad"
                @error="onVideoError"
              />
              <div class="video-overlay" v-if="videoError">
                <i class="el-icon-video-camera"></i>
                <span>视频流连接失败</span>
              </div>
            </div>
            <div class="camera-info-bar" v-if="currentCamera">
              <div class="camera-name">{{ currentCamera.name }}</div>
              <div class="camera-status live"><span class="live-dot"></span>LIVE</div>
            </div>
          </div>
        </div>

      </div>

      <div class="panel-column center">
        <div class="map-container">
          
          <div class="center-kpi">
            <div class="center-item">
              <div class="label">累计营收</div>
              <div class="value jump-num">¥ {{ totalRevenue.toLocaleString() }}</div>
            </div>
            <div class="center-item">
              <div class="label">净利润</div>
              <div class="value jump-num" :style="{color: totalProfit >= 0 ? '#4cd964' : '#ff4d4f'}">¥ {{ totalProfit.toLocaleString() }}</div>
            </div>
          </div>

          <div class="center-chart" ref="chartCenter"></div>
        </div>
        
        <div class="bottom-stats">
          <div class="stat-item">
            <i class="el-icon-user"></i>
            <span class="stat-label">在线用户</span>
            <span class="stat-value">{{ onlineUsers }}</span>
          </div>
          <div class="stat-item">
            <i class="el-icon-location"></i>
            <span class="stat-label">监测地块</span>
            <span class="stat-value">{{ farmCount }}</span>
          </div>
          <div class="stat-item">
            <i class="el-icon-truck"></i>
            <span class="stat-label">待发货</span>
            <span class="stat-value">{{ pendingShipments }}</span>
          </div>
        </div>
      </div>

      <div class="panel-column right">
        
        <div class="data-panel">
          <div class="panel-title">
            <i class="el-icon-notebook-2"></i> 备忘录
            <span class="decoration"></span>
          </div>
          <div class="panel-body">
            <div class="memo-list">
              <div class="memo-item" v-for="(memo, idx) in memoList" :key="idx">
                <div class="memo-bullet"></div>
                <div class="memo-text">{{ memo }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="data-panel">
          <div class="panel-title">
            <i class="el-icon-pie-chart"></i> 作物销售占比
            <span class="decoration"></span>
          </div>
          <div class="panel-body">
            <div class="chart-box" ref="chartRight2"></div>
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'BigScreen',
  data() {
    return {
      timeStr: '',
      timer: null,
      charts: {},
      // === 真实数据 ===
      totalRevenue: 0,          // 总营收
      totalOrders: 0,           // 订单总数
      totalProfit: 0,           // 净利润
      profitRate: '0%',         // 利润率
      safeRunDays: 0,           // 安全运行天数
      deviceStatus: {           // 设备状态
        temperature: 25.5,
        humidity: 60,
        online: true,
        deviceName: 'STM32-001'
      },
      alerts: [],               // 预警信息
      salesByProduct: [],       // 按产品分类的销售数据
      purchaseCost: 0,          // 采购成本
      inventoryData: [],        // 库存数据
      sensorHistory: [],        // 传感器历史数据
      cropProduction: [],       // 作物产量数据
      // === 新增独特功能数据 ===
      cameraList: [],           // 监控摄像头列表（动态加载）
      farmlandList: [],         // 用户可管理的地块列表
      selectedFarmland: 'all',  // 当前选中的地块
      memoList: [               // 备忘录列表
        '检查 A1 号田灌溉系统',
        '联系供应商采购化肥',
        '下周安排病虫害防治',
        '更新传感器固件版本',
        '准备月度产量报告'
      ],
      onlineUsers: 3,           // 在线用户数
      farmCount: 0,             // 监测地块数
      pendingShipments: 0,      // 待发货数
      hasData: false,            // 是否有数据
      videoError: false,          // 视频流错误状态
      defaultVideoStreamUrl: process.env.VUE_APP_MJPEG_STREAM_URL || 'http://192.168.137.227/mjpeg/1'
    }
  },
  computed: {
    // 当前选中的摄像头
    currentCamera() {
      if (!this.selectedFarmland) return null;
      return this.cameraList.find(cam => cam.farmlandId === this.selectedFarmland);
    },
    currentVideoStreamUrl() {
      if (this.currentCamera && this.currentCamera.streamUrl) {
        return this.currentCamera.streamUrl;
      }
      return this.defaultVideoStreamUrl;
    }
  },
  mounted() {
    this.startClock();
    this.loadAllData();
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeDestroy() {
    this._isDestroyed = true; // 标记组件已销毁
    if (this.timer) clearInterval(this.timer);
    if (this.animateTimer) clearInterval(this.animateTimer); // 停止动画定时器
    if (this.animationId) cancelAnimationFrame(this.animationId);
    // 安全销毁图表实例
    if (this.charts.left1 && !this.charts.left1.isDisposed()) this.charts.left1.dispose();
    if (this.charts.center && !this.charts.center.isDisposed()) this.charts.center.dispose();
    if (this.charts.right2 && !this.charts.right2.isDisposed()) this.charts.right2.dispose();
  },
  methods: {
    startClock() {
      this.updateTime();
      this.timer = setInterval(this.updateTime, 1000);
    },
    updateTime() {
      const now = new Date();
      this.timeStr = now.toLocaleString();
    },
    
    // ========== 加载所有真实数据 ==========
    async loadAllData() {
      try {
        const [salesRes, purchaseRes, inventoryRes, alertRes, sensorRes] = await Promise.all([
          this.request.get("/sales"),
          this.request.get("/purchase"),
          this.request.get("/inventory"),
          this.request.get("/alert/pending").catch(() => ({ code: '200', data: [] })),
          this.request.get("/aether/readings/detail", { params: { days: 1 } }).catch(() => ({ code: '200', data: { data: [] } }))
        ]);

        // 处理销售数据
        const salesData = salesRes.code === '200' ? salesRes.data : [];
        this.processSalesData(salesData);
        
        // 检查是否有数据
        this.hasData = salesData.length > 0 || (purchaseRes.code === '200' && purchaseRes.data && purchaseRes.data.length > 0);
        
        // 加载用户可管理的地块并生成摄像头列表
        await this.loadFarmlandCameras();
        
        // 处理采购数据
        const purchaseData = purchaseRes.code === '200' ? purchaseRes.data : [];
        this.processPurchaseData(purchaseData);
        
        // 处理库存数据
        this.inventoryData = inventoryRes.code === '200' ? inventoryRes.data : [];
        
        // 处理预警数据
        this.alerts = alertRes.code === '200' ? alertRes.data : [];
        
        // 处理传感器历史数据
        const sensorData = sensorRes.code === '200' && sensorRes.data ? sensorRes.data.data : [];
        this.sensorHistory = sensorData || [];
        
        // 计算安全运行天数（基于系统创建时间或固定日期）
        this.calculateSafeRunDays();
        
        // 获取实时设备状态
        this.loadDeviceStatus();
        
        // 加载额外统计数据
        this.loadExtraStats();

      } catch (error) {
        console.error("加载大屏数据失败:", error);
      }
      
      // 初始化图表
      this.$nextTick(() => {
        this.initCharts();
      });
    },
    
    // 加载额外统计数据（独特功能）
    async loadExtraStats() {
      try {
        // 获取农田数量
        const farmRes = await this.request.get("/statistic/page", { params: { pageNum: 1, pageSize: 1 } });
        if (farmRes.code === '200' && farmRes.data) {
          this.farmCount = farmRes.data.total || 0;
        }
        
        // 计算今日订单（简化：使用销售数据长度）
        this.todayOrders = this.totalOrders;
        
        // 计算待发货（简化：库存中数量低于安全库存的品类）
        if (this.inventoryData && Array.isArray(this.inventoryData)) {
          this.pendingShipments = this.inventoryData.filter(item => 
            parseFloat(item.number || 0) < parseFloat(item.safeStock || 10)
          ).length;
        }
        
        // 计算任务进度
        if (this.todayTasks && Array.isArray(this.todayTasks) && this.todayTasks.length > 0) {
          const doneTasks = this.todayTasks.filter(t => t.status === 'done').length;
          this.taskProgress = Math.round((doneTasks / this.todayTasks.length) * 100);
        }
        
      } catch (error) {
        console.warn("加载额外统计失败:", error);
      }
    },
    
    // 视频流加载错误处理
    handleFarmlandChange() {
      this.videoError = false;
    },
    
    onVideoLoad() {
      this.videoError = false;
    },
    
    // 视频流加载错误处理
    onVideoError() {
      this.videoError = true;
    },
    
    // 打开监控画面
    openCamera(cam) {
      if (cam.online) {
        this.$message.success(`正在连接 ${cam.name}...`);
      } else {
        this.$message.warning(`${cam.name} 当前离线`);
      }
    },
    
    // 加载用户可管理的地块并生成摄像头列表
    async loadFarmlandCameras() {
      try {
        // 从 statistic API 获取用户可管理的地块（已有权限过滤）
        const res = await this.request.get("/statistic");
        if (res.code === '200' && res.data && res.data.length > 0) {
          // 生成地块列表（使用 farm 字段作为农田名称）
          this.farmlandList = res.data.map(item => ({
            id: item.id,
            name: item.farm || `农田${item.id}`
          }));
          
          // 根据地块生成摄像头列表（使用农田实际名称）
          this.cameraList = res.data.map(item => ({
            id: item.id,
            farmlandId: item.id,
            name: `${item.farm || '农田' + item.id}`,
            streamUrl: this.defaultVideoStreamUrl,
            online: Boolean(this.defaultVideoStreamUrl)
          }));
          
          this.farmCount = res.data.length;
          // 默认选中第一个地块
          if (this.farmlandList.length > 0) {
            this.selectedFarmland = this.farmlandList[0].id;
          }
        } else {
          this.farmlandList = [];
          this.cameraList = [];
          this.farmCount = 0;
          this.selectedFarmland = '';
        }
      } catch (error) {
        console.warn("加载地块摄像头失败:", error);
        this.farmlandList = [];
        this.cameraList = [];
      }
    },
    
    processSalesData(salesData) {
      // 计算总营收
      this.totalRevenue = salesData.reduce((sum, item) => {
        return sum + (parseFloat(item.price || 0) * parseFloat(item.number || 0));
      }, 0);
      this.totalOrders = salesData.length;
      
      // 按产品分类汇总销售额
      const productMap = {};
      salesData.forEach(item => {
        const name = item.product || '其他';
        const amount = parseFloat(item.price || 0) * parseFloat(item.number || 0);
        productMap[name] = (productMap[name] || 0) + amount;
      });
      
      this.salesByProduct = Object.entries(productMap)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value);
      
      // 按产品统计产量
      const productionMap = {};
      salesData.forEach(item => {
        const name = item.product || '其他';
        productionMap[name] = (productionMap[name] || 0) + parseFloat(item.number || 0);
      });
      this.cropProduction = Object.entries(productionMap)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value);
    },
    
    processPurchaseData(purchaseData) {
      this.purchaseCost = purchaseData.reduce((sum, item) => {
        return sum + (parseFloat(item.price || 0) * parseFloat(item.number || 0));
      }, 0);
      
      // 计算净利润和利润率
      this.totalProfit = this.totalRevenue - this.purchaseCost;
      this.profitRate = this.totalRevenue > 0 
        ? ((this.totalProfit / this.totalRevenue) * 100).toFixed(1) + '%' 
        : '0%';
    },
    
    calculateSafeRunDays() {
      const startDate = new Date('2024-01-01');
      const now = new Date();
      this.safeRunDays = Math.floor((now - startDate) / (1000 * 60 * 60 * 24));
    },
    
    async loadDeviceStatus() {
      try {
        const res = await this.request.get("/aether/device/status");
        if (res.code === '200' && res.data) {
          this.deviceStatus = res.data;
        }
      } catch (error) {
        console.warn("获取设备状态失败:", error);
      }
    },

    initCharts() {
      this.initChartLeft1();  // 作物销量柱状图
      this.initChartCenter(); // 中心雷达图
      this.initChartRight2(); // 销售占比饼图
      // 启动数据模拟跳动
      this.rotateAngle = 0; // 初始化旋转角度
      this.animateTimer = setInterval(this.animateData, 100); // 保存定时器引用以便清理
    },
    animateData() {
      // 检查组件是否已销毁
      if (this._isDestroyed) return;
      
      // 1. KPI 数字脉冲效果 (每秒一次)
      if (this.rotateAngle % 10 === 0) { 
        const kpiValue = document.querySelector('.jump-num');
        if (kpiValue) {
          kpiValue.classList.add('pulse');
          setTimeout(() => kpiValue.classList.remove('pulse'), 500);
        }
      }

      // 2. 雷达图背景环旋转动画 (平滑旋转，这是唯一持续的动态效果)
      if (this.charts.center && !this.charts.center.isDisposed()) {
        this.rotateAngle = (this.rotateAngle + 1) % 360;
        this.charts.center.setOption({
          series: [
            {}, // 雷达图数据已在上文更新，这里占位即可，或者不传
            { 
              startAngle: 90 + this.rotateAngle, 
              endAngle: -270 + this.rotateAngle 
            }, // 内环顺时针旋转
            { 
              startAngle: -this.rotateAngle, 
              endAngle: 360 - this.rotateAngle 
            }  // 外环逆时针旋转
          ]
        });
      }
    },
    resizeCharts() {
      Object.values(this.charts).forEach(chart => {
        if (chart && !chart.isDisposed()) {
          chart.resize();
        }
      });
    },

    // 左1：作物销量柱状图 (真实数据)
    initChartLeft1() {
      const chart = echarts.init(this.$refs.chartLeft1);
      this.charts.left1 = chart;
      
      // 使用真实的作物产量数据
      const productionData = this.cropProduction.slice(0, 5);
      const xData = productionData.map(item => item.name);
      const yData = productionData.map(item => item.value);
      
      chart.setOption({
        grid: { top: 30, bottom: 20, left: 50, right: 10 },
        tooltip: { 
          trigger: 'axis', 
          backgroundColor: 'rgba(0,0,0,0.7)', 
          borderColor: '#00f2ff', 
          textStyle: { color: '#fff' },
          formatter: '{b}: {c} 单位'
        },
        xAxis: { 
          type: 'category', 
          data: this.hasData && xData.length > 0 ? xData : ['暂无数据'],
          axisLine: { lineStyle: { color: '#00f2ff' } },
          axisLabel: { color: '#fff', rotate: xData.length > 4 ? 15 : 0 }
        },
        yAxis: { 
          type: 'value', 
          splitLine: { lineStyle: { color: 'rgba(0,242,255,0.1)' } },
          axisLine: { show: false },
          axisLabel: { color: '#aaa' }
        },
        series: [{
          type: 'bar',
          barWidth: 20,
          itemStyle: {
            borderRadius: [10, 10, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#00f2ff' },
              { offset: 0.5, color: '#00a8ff' },
              { offset: 1, color: 'rgba(0,168,255,0.1)' }
            ]),
            shadowBlur: 10,
            shadowColor: '#00f2ff'
          },
          data: this.hasData && yData.length > 0 ? yData : [],
          animationDuration: 2000,
          animationEasing: 'cubicOut'
        }]
      });
    },

    // 中间：发光雷达图 + 背景科技圆环 (基于真实数据计算评分)
    initChartCenter() {
      const chart = echarts.init(this.$refs.chartCenter);
      this.charts.center = chart;
      
      const center = ['50%', '55%']; // 统一中心点
      
      // 如果没有数据，显示空雷达图
      if (!this.hasData) {
        chart.setOption({
          radar: {
            center: center,
            radius: '60%', 
            indicator: [
              { name: '盈利能力', max: 100 },
              { name: '销售业绩', max: 100 },
              { name: '设备状态', max: 100 },
              { name: '库存管理', max: 100 },
              { name: '安全指数', max: 100 }
            ],
            shape: 'polygon',
            splitNumber: 4,
            axisName: { color: '#00f2ff', fontSize: 14, textShadowBlur: 5, textShadowColor: '#00f2ff' },
            splitLine: { lineStyle: { color: 'rgba(0, 242, 255, 0.3)', width: 1 } },
            splitArea: { show: false },
            axisLine: { lineStyle: { color: 'rgba(0, 242, 255, 0.5)' } }
          },
          series: [{ type: 'radar', data: [] }]
        });
        return;
      }
      
      // 基于真实数据计算各维度评分
      const profitScore = this.totalProfit > 0 ? Math.min(100, 60 + (this.totalProfit / 1000)) : 40;
      const revenueScore = Math.min(100, 50 + (this.totalRevenue / 500));
      const deviceScore = this.deviceStatus.online ? 95 : 50;
      const inventoryScore = this.inventoryData.length > 0 ? Math.min(100, 60 + this.inventoryData.length * 5) : 30;
      const alertScore = this.alerts.length === 0 ? 100 : Math.max(30, 100 - this.alerts.length * 15);

      chart.setOption({
        radar: {
          center: center,
          radius: '60%', 
          indicator: [
            { name: '盈利能力', max: 100 },
            { name: '销售业绩', max: 100 },
            { name: '设备状态', max: 100 },
            { name: '库存管理', max: 100 },
            { name: '安全指数', max: 100 }
          ],
          shape: 'polygon',
          splitNumber: 4,
          axisName: { color: '#00f2ff', fontSize: 14, textShadowBlur: 5, textShadowColor: '#00f2ff' },
          splitLine: { lineStyle: { color: 'rgba(0, 242, 255, 0.3)', width: 1 } },
          splitArea: { show: false },
          axisLine: { lineStyle: { color: 'rgba(0, 242, 255, 0.5)' } }
        },
        series: [
          // 1. 雷达图数据
          {
            type: 'radar',
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: { color: '#ffd700', shadowBlur: 10, shadowColor: '#ffd700' },
            areaStyle: { color: 'rgba(255, 215, 0, 0.3)' },
            lineStyle: { width: 2, color: '#ffd700', shadowBlur: 10, shadowColor: '#ffd700' },
            data: [{ value: [profitScore, revenueScore, deviceScore, inventoryScore, alertScore], name: '农场综合评分' }],
            // 添加快速入场动画 (极速版)
            animationDuration: 900, // 0.5秒完成
            animationEasing: 'elasticOut', // 弹性展开效果
            animationDelay: 0 // 无延迟立即执行
          },
          // 2. 背景装饰环 (内虚线环)
          {
            type: 'gauge',
            radius: '70%',
            center: center,
            startAngle: 90, endAngle: -270,
            axisLine: { show: false },
            axisTick: { show: false },
            splitLine: { show: false },
            axisLabel: { show: false },
            pointer: { show: false },
            detail: { show: false },
            progress: { show: false },
            data: [{ value: 100 }],
            // 使用仪表盘背景画圆
            axisLine: {
              lineStyle: {
                width: 1,
                color: [[1, 'rgba(0, 242, 255, 0.3)']],
                type: 'dashed'
              }
            }
          },
          // 3. 背景装饰环 (外旋转环 - 用仪表盘刻度模拟)
          {
            type: 'gauge',
            radius: '80%',
            center: center,
            startAngle: 0, endAngle: 360,
            axisLine: { show: false },
            splitLine: { show: false },
            axisLabel: { show: false },
            pointer: { show: false },
            detail: { show: false },
            axisTick: {
              length: 2,
              lineStyle: { color: 'rgba(0, 242, 255, 0.5)', width: 2 }
            },
            data: [{ value: 0 }] // 纯装饰
          }
        ]
      });
    },

    // 右2：销售占比饼图 (真实数据)
    initChartRight2() {
      const chart = echarts.init(this.$refs.chartRight2);
      this.charts.right2 = chart;
      
      // 使用真实的销售数据
      const colors = ['#ff6b81', '#ff9f43', '#feca57', '#54a0ff', '#5f27cd', '#00d2d3', '#ff6348'];
      const pieData = this.hasData ? this.salesByProduct.slice(0, 5).map((item, index) => ({
        value: Math.round(item.value),
        name: item.name,
        itemStyle: { color: colors[index % colors.length] }
      })) : [];
      
      chart.setOption({
        tooltip: { 
          trigger: 'item', 
          backgroundColor: 'rgba(0,0,0,0.7)',
          formatter: '{b}: ¥{c} ({d}%)'
        },
        legend: { top: 'bottom', textStyle: { color: '#fff' }, itemGap: 10 },
        series: [{
          name: '销售占比',
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['50%', '45%'],
          roseType: 'radius',
          itemStyle: { borderRadius: 5, borderColor: '#000', borderWidth: 2, shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' },
          label: { show: false },
          data: pieData.length > 0 ? pieData : [{ value: 1, name: '暂无数据', itemStyle: { color: '#666' } }],
          animationType: 'scale',
          animationEasing: 'elasticOut',
          animationDelay: function (idx) { return Math.random() * 200; }
        }]
      });
    }
  }
}
</script>

<style scoped>
/* 引入数字字体 (可选) */
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@700&display=swap');

.data-screen-container {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background-color: #0b1120;
  /* 动态科技背景网格 */
  background-image: 
    linear-gradient(rgba(0, 242, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 242, 255, 0.05) 1px, transparent 1px),
    radial-gradient(circle at 50% 50%, #1e293b 0%, #020617 100%);
  background-size: 30px 30px, 30px 30px, 100% 100%;
  color: #fff;
  z-index: 9999;
  overflow: hidden;
  font-family: "Microsoft YaHei", sans-serif;
  display: flex;
  flex-direction: column;
}

/* 扫描线动画 */
.data-screen-container::after {
  content: "";
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.25) 50%), linear-gradient(90deg, rgba(255, 0, 0, 0.06), rgba(0, 255, 0, 0.02), rgba(0, 0, 255, 0.06));
  background-size: 100% 2px, 6px 100%;
  pointer-events: none;
  z-index: 1;
}

/* --- 头部 --- */
.header-wrapper {
  height: 60px;
  background: url('https://img.alicdn.com/tfs/TB1.8.jQpXXXXbUXpXXXXXXXXXX-1920-100.png') no-repeat center bottom;
  background-size: 100% 100%;
  position: relative;
  flex-shrink: 0;
}
.header-content {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 30px; height: 50px;
}
.header-title { text-align: center; position: absolute; left: 50%; transform: translateX(-50%); top: 2px; }
.header-title span { font-size: 24px; font-weight: bold; letter-spacing: 3px; text-shadow: 0 0 10px #00f2ff; background: linear-gradient(to bottom, #fff, #409EFF); -webkit-background-clip: text; color: transparent; }
.sub-title { font-size: 9px; color: #409EFF; opacity: 0.7; letter-spacing: 2px; }
.header-time, .header-weather { font-family: 'Orbitron', sans-serif; color: #00f2ff; font-size: 14px; width: 220px; }
.header-weather { text-align: right; display: flex; justify-content: flex-end; align-items: center; gap: 12px; }
.exit-btn { cursor: pointer; font-size: 12px; color: #fff; opacity: 0.7; transition: 0.3s; border: 1px solid rgba(255,255,255,0.3); padding: 3px 10px; border-radius: 20px; }
.exit-btn:hover { opacity: 1; background: rgba(255,0,0,0.2); border-color: #ff4d4f; }

/* --- 主体布局 --- */
.main-content {
  flex: 1;
  display: flex;
  padding: 10px 15px;
  gap: 12px;
  min-height: 0; /* 允许收缩 */
}

/* 左右列 */
.panel-column {
  display: flex; flex-direction: column; gap: 10px;
}
.panel-column.left, .panel-column.right { width: 24%; }
.panel-column.center { flex: 1; position: relative; }

/* 通用面板样式 - 磨砂科技感 */
.data-panel {
  flex: 1;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 242, 255, 0.2);
  position: relative;
  display: flex; flex-direction: column;
  padding: 10px;
  box-shadow: 0 0 20px rgba(0, 242, 255, 0.05), inset 0 0 20px rgba(0, 242, 255, 0.05);
  min-height: 0;
  border-radius: 4px;
  transition: all 0.3s;
}
.data-panel:hover {
  box-shadow: 0 0 30px rgba(0, 242, 255, 0.15), inset 0 0 30px rgba(0, 242, 255, 0.1);
  border-color: rgba(0, 242, 255, 0.5);
}

/* 四角装饰 - 呼吸灯效果 */
.data-panel::before { 
  content: ''; position: absolute; top: -1px; left: -1px; width: 10px; height: 10px; 
  border-top: 2px solid #00f2ff; border-left: 2px solid #00f2ff; 
  box-shadow: -2px -2px 10px #00f2ff;
  animation: breathe 3s infinite alternate;
}
.data-panel::after { 
  content: ''; position: absolute; bottom: -1px; right: -1px; width: 10px; height: 10px; 
  border-bottom: 2px solid #00f2ff; border-right: 2px solid #00f2ff; 
  box-shadow: 2px 2px 10px #00f2ff;
  animation: breathe 3s infinite alternate-reverse;
}

@keyframes breathe {
  0% { opacity: 0.5; box-shadow: 0 0 5px #00f2ff; }
  100% { opacity: 1; box-shadow: 0 0 15px #00f2ff; }
}

.panel-title {
  font-size: 14px; color: #fff; 
  border-left: 3px solid #00f2ff; 
  padding-left: 8px; margin-bottom: 8px;
  display: flex; align-items: center; 
  background: linear-gradient(90deg, rgba(0,242,255,0.2), transparent);
  text-shadow: 0 0 5px rgba(0, 242, 255, 0.5);
}

/* 地块选择下拉框 */
.farmland-select {
  margin-left: auto;
  background: rgba(0, 242, 255, 0.1);
  border: 1px solid rgba(0, 242, 255, 0.3);
  color: #00f2ff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  outline: none;
}
.farmland-select:hover {
  background: rgba(0, 242, 255, 0.2);
  border-color: #00f2ff;
}
.farmland-select option {
  background: #1a1a2e;
  color: #fff;
}

.panel-body { flex: 1; position: relative; display: flex; flex-direction: column; min-height: 0; }
.chart-box { flex: 1; min-height: 120px; }

/* 左1 KPI */
.kpi-row { display: flex; justify-content: space-around; margin-bottom: 6px; }
.kpi-item { text-align: center; background: rgba(255,255,255,0.05); padding: 6px; width: 45%; border-radius: 4px; }
.kpi-item .num { font-size: 18px; font-weight: bold; font-family: 'Orbitron'; }
.kpi-item .label { font-size: 10px; color: #aaa; margin-top: 3px; }

/* 左2 摄像头监控 */
.single-camera {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(0,242,255,0.2);
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(0,0,0,0.4);
  margin-bottom: 6px;
}
.single-camera:hover {
  border-color: #00f2ff;
  box-shadow: 0 0 15px rgba(0, 242, 255, 0.2);
}
.camera-preview.large {
  flex: 1;
  width: 100%;
  height: auto;
  min-height: 100px;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  position: relative;
  display: flex; align-items: center; justify-content: center;
}
.center-icon {
  font-size: 48px;
  color: rgba(0, 242, 255, 0.7);
  transition: 0.3s;
}
.single-camera:hover .center-icon {
  color: #00f2ff;
  transform: scale(1.1);
}
.camera-info-bar {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  background: rgba(0, 242, 255, 0.05);
  border-top: 1px solid rgba(0,242,255,0.1);
}
.camera-info-bar .camera-name {
  font-size: 14px;
  font-weight: bold;
  margin: 0;
  color: #fff;
}
.camera-status.online { background: rgba(76,217,100,0.2); color: #4cd964; }
.camera-status.offline { background: rgba(255,77,79,0.2); color: #ff4d4f; }
.camera-status.live {
  background: rgba(255, 0, 0, 0.2);
  color: #ff0000;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 12px;
  animation: liveGlow 1.5s ease-in-out infinite;
}
.live-dot {
  width: 8px;
  height: 8px;
  background: #ff0000;
  border-radius: 50%;
  animation: livePulse 1s ease-in-out infinite;
}
@keyframes livePulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}
@keyframes liveGlow {
  0%, 100% { box-shadow: 0 0 5px rgba(255, 0, 0, 0.3); }
  50% { box-shadow: 0 0 15px rgba(255, 0, 0, 0.6); }
}
.camera-tip { text-align: center; font-size: 10px; color: #666; margin-top: 6px; }

/* 右1 备忘录 */
.memo-list { flex: 1; overflow-y: auto; padding: 5px 0; }
.memo-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 10px 8px;
  border-bottom: 1px solid rgba(0,242,255,0.1); transition: all 0.3s;
}
.memo-item:hover { background: rgba(0,242,255,0.05); }
.memo-item:last-child { border-bottom: none; }
.memo-bullet {
  width: 8px; height: 8px; min-width: 8px;
  background: #00f2ff; border-radius: 50%; margin-top: 4px;
  box-shadow: 0 0 8px rgba(0,242,255,0.5);
}
.memo-text { font-size: 12px; color: #ddd; line-height: 1.5; }

/* 中间区域 */
.map-container {
  flex: 1;
  position: relative;
  border: 1px solid rgba(0, 242, 255, 0.3);
  background: rgba(0,0,0,0.2);
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  min-height: 0;
  padding-top: 40px; /* 给顶部数字留空间 */
}
/* 删除原有的 HTML 动画，改用 ECharts 实现背景环，保证绝对对齐 */

.center-kpi {
  position: absolute; top: 10px; width: 100%; display: flex; justify-content: center; gap: 40px; z-index: 20;
}
.center-item { text-align: center; }
.center-item .label { color: #aaa; font-size: 12px; }
.center-item .value { font-size: 26px; font-weight: bold; color: #ffd700; font-family: 'Orbitron'; text-shadow: 0 0 20px rgba(255, 215, 0, 0.5); }
.center-chart { width: 100%; height: 100%; z-index: 10; }

/* 底部统计指标 */
.bottom-stats {
  height: 50px; margin-top: 8px;
  background: rgba(0, 242, 255, 0.05);
  border: 1px solid rgba(0, 242, 255, 0.2);
  border-radius: 4px;
  display: flex; align-items: center; justify-content: space-around;
  flex-shrink: 0;
}
.stat-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
}
.stat-item i { font-size: 16px; color: #00f2ff; }
.stat-label { font-size: 10px; color: #888; }
.stat-value { font-size: 18px; font-weight: bold; color: #ffd700; font-family: 'Orbitron'; }

/* 实时视频监控样式 */
.live-video-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.live-video-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  border-radius: 6px;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}
.live-video-stream {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 6px;
}
.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #666;
}
.video-overlay i {
  font-size: 48px;
  color: rgba(0, 242, 255, 0.3);
}
.video-overlay span {
  font-size: 12px;
  color: #888;
}
</style>
