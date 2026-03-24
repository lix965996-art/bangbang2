<template>
  <div class="env-monitor-page">
    <!-- 超炫渐变背景 -->
    <div class="bg-gradient"></div>
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>

    <!-- 顶部控制栏 -->
    <div class="page-header glass-effect">
      <div class="left-title">
        <h2>
          <div class="icon-pulse"><i class="el-icon-data-analysis"></i></div>
          智能环境监测指挥中心
        </h2>
        <span class="sub">Environmental Monitoring & AI Decision Platform</span>
        <div class="status-bar">
          <span class="status-dot"></span>
          <span>系统运行正常</span>
        </div>
      </div>
      <div class="header-center">
        <el-select 
          v-model="selectedFarmId" 
          placeholder="选择农田" 
          @change="onFarmSelect"
          size="small"
          style="width: 200px; margin-right: 15px;">
          <el-option
            v-for="farm in farmList"
            :key="farm.id"
            :label="farm.name"
            :value="farm.id">
            <span style="float: left">{{ farm.name }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">{{ farm.crop || '未种植' }}</span>
          </el-option>
        </el-select>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" icon="el-icon-refresh" @click="refreshData" :loading="loading">刷新</el-button>
        <el-button type="success" size="small" icon="el-icon-download" @click="exportData">导出</el-button>
      </div>
    </div>

    <!-- AI 智能建议卡片 -->
    <div class="ai-advice-card glass-effect">
      <div class="ai-header">
        <div class="ai-avatar"><img src="@/assets/ai.png" alt="AI" class="ai-icon" /></div>
        <div class="ai-title">
          <span class="title-text">AI农事智能决策系统</span>
          <el-tag v-if="aiAnalyzing" size="mini" type="info" effect="dark">
            <i class="el-icon-loading"></i> 分析中
          </el-tag>
          <el-tag v-else size="mini" type="success" effect="dark">AI 已连接</el-tag>
        </div>
      </div>
      <div class="ai-content">
        <p class="typing-text">{{ aiText }}<span class="cursor" v-if="typing">|</span></p>
        
        <!-- AI预测结果 -->
        <div class="ai-predictions" v-if="aiPredictions.growthRate > 0">
          <div class="prediction-item">
            <i class="el-icon-data-line"></i>
            <span>生长环境评分：</span>
            <el-progress :percentage="Number(aiPredictions.growthRate)" :color="aiPredictions.growthRate > 80 ? '#67c23a' : aiPredictions.growthRate > 60 ? '#e6a23c' : '#f56c6c'" :stroke-width="6"></el-progress>
          </div>
          <div class="prediction-item">
            <i class="el-icon-date"></i>
            <span>预计收获日期：{{ aiPredictions.harvestDate }}</span>
          </div>
          <div class="prediction-item">
            <i class="el-icon-goods"></i>
            <span>预估产量：{{ aiPredictions.expectedYield }} 吨/亩</span>
          </div>
          <div class="prediction-item" v-if="aiPredictions.marketAnalysis">
            <i class="el-icon-data-board"></i>
            <span>市场行情：{{ aiPredictions.marketAnalysis }}</span>
          </div>
          <div class="prediction-item" v-if="aiPredictions.weatherImpact">
            <i class="el-icon-partly-cloudy"></i>
            <span>天气影响：{{ aiPredictions.weatherImpact }}</span>
          </div>
        </div>
      </div>
      <div class="ai-footer">
        <span><i class="el-icon-lightning"></i> 通义千问智能分析</span>
        <span><i class="el-icon-cpu"></i> 实时数据融合</span>
        <span><i class="el-icon-trophy"></i> 精准产量预测</span>
      </div>
    </div>

    <!-- 农田信息卡片 -->
    <div class="kpi-row">
      <div class="kpi-card" v-for="(kpi, index) in kpiData" :key="index" :class="['kpi-' + index, kpi.status]">
        <div class="kpi-bg-wave"></div>
        <div class="icon-box" :class="kpi.type">
          <i :class="kpi.icon"></i>
          <div class="icon-ring"></div>
        </div>
        <div class="kpi-content">
          <div class="label">
            {{ kpi.label }}
            <el-tag v-if="kpi.status === 'excellent'" size="mini" type="success" effect="dark">优秀</el-tag>
            <el-tag v-if="kpi.status === 'warning'" size="mini" type="warning" effect="dark">关注</el-tag>
          </div>
          <div class="value count-up">
            {{ animatedValues[index] }}
            <span class="unit">{{ kpi.unit }}</span>
          </div>
          <div class="trend" :class="{ up: kpi.trend > 0, down: kpi.trend < 0 }">
            <i :class="kpi.trend > 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
            {{ Math.abs(kpi.trend) }}% {{ kpi.trendText }}
          </div>
          <div class="update-time">最后更新: {{ currentTime }}</div>
        </div>
        <div class="sparkline" :ref="'sparkline' + index"></div>
      </div>
    </div>

  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'DashbordNew',
  data() {
    return {
      loading: false,
      currentTime: '',
      dateRange: '',
      
      // 农场列表（从后端获取）
      farmList: [],
      selectedFarms: [],
      selectedFarmId: null, // 当前选中的农田
      currentFarmData: null, // 当前农田详细数据
      
      // 实时硬件数据
      sensorData: {
        temperature: 0,
        humidity: 0,
        soilMoisture: 0,
        lightIntensity: 0,
        co2Level: 420,
        ph: 6.5,
        led: false,
        deviceOnline: false,
        deviceName: 'STM32-001',
        lastUpdate: ''
      },
      
      // AI 打字效果
      aiText: '',
      typing: false,
      fullAiText: '',
      aiAnalyzing: false,

      
      // 农田信息卡片数据
      kpiData: [
        { label: '预估产量', value: 0, unit: '吨/亩', icon: 'el-icon-goods', type: 'yield', trend: 0, trendText: '较上期', status: 'normal' },
        { label: '预计收入', value: 0, unit: '万元', icon: 'el-icon-coin', type: 'income', trend: 0, trendText: '市场价', status: 'normal' },
        { label: '种植面积', value: 0, unit: '亩', icon: 'el-icon-map-location', type: 'area', trend: 0, trendText: '利用率', status: 'normal' },
        { label: '生长周期', value: 0, unit: '天', icon: 'el-icon-time', type: 'growth', trend: 0, trendText: '剩余', status: 'normal' }
      ],
      animatedValues: [0, 0, 0, 0],
      
      // 历史数据
      historyData: [],
      
      // AI预测和建议
      aiPredictions: {
        growthRate: 0,
        harvestDate: '',
        expectedYield: 0,
        riskLevel: 'low',
        suggestions: [],
        marketAnalysis: '', // 市场行情分析
        weatherImpact: '', // 天气影响评估
        cropStatus: '' // 作物状态
      },
      
      // AI分析状态
      qwenAnalyzing: false,
      
      // 自动控制状态
      autoControl: {
        irrigation: false,
        lighting: false,
        ventilation: false
      },
      
      // 图表实例
      radarChart: null,
      compareChart: null,
      
      // 粒子动画
      particles: [],
      canvas: null,
      ctx: null,
      animationId: null,
      
      // 实时预警数据
      alertData: [],
      
      // 环境阈值设置
      thresholds: {
        tempMin: 15,
        tempMax: 35,
        humiMin: 40,
        humiMax: 80,
        soilMin: 30,
        soilMax: 70
      },
      
      // 数据刷新定时器
      dataTimer: null,
      analysisTimer: null,
      clockTimer: null
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.updateTime();
      this.loadFarmList();  // 加载农田列表
      this.initRadarChart();
      this.initCompareChart();
      this.initParticleCanvas();
      window.addEventListener('resize', this.handleResize);
      
      // 定时更新时间
      this.clockTimer = setInterval(this.updateTime, 1000);
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if (this.clockTimer) clearInterval(this.clockTimer);
    if(this.radarChart && !this.radarChart.isDisposed()) this.radarChart.dispose();
    if(this.compareChart && !this.compareChart.isDisposed()) this.compareChart.dispose();
    if(this.animationId) cancelAnimationFrame(this.animationId);
  },
  methods: {
    // 加载农田列表（从账号管理的农田数据）
    async loadFarmList() {
      try {
        const res = await this.request.get('/statistic/page', {
          params: { pageNum: 1, pageSize: 100, farm: '' }
        });
        if (res.code === '200' && res.data && res.data.records) {
          this.farmList = res.data.records.map(item => ({
            id: item.id,
            name: item.farm || `农田${item.id}`,
            area: item.area || 100, // 面积（亩）
            crop: item.crop || '小麦', // 作物
            plantDate: item.plantDate || '2025-01-01', // 种植日期
            growthDays: item.growthDays || 90, // 生长周期
            temperature: Number(item.temperature || 0),
            humidity: Number(item.soilhumidity || 0)
          }));
          // 默认选中第一个
          if (this.farmList.length > 0) {
            this.selectedFarmId = this.farmList[0].id;
            this.onFarmSelect(this.selectedFarmId);
            this.selectedFarms = this.farmList.slice(0, Math.min(3, this.farmList.length)).map(f => f.id);
            this.initCompareChart();
          }
        }
      } catch (e) {
        console.error('加载农田列表失败:', e);
      }
    },
    
    // 选择农田后的处理
    async onFarmSelect(farmId) {
      if (!farmId) return;
      
      // 找到选中的农田数据
      this.currentFarmData = this.farmList.find(f => f.id === farmId);
      if (!this.currentFarmData) return;
      
      // 清空之前的分析内容
      this.clearPreviousAnalysis();
      
      // 调用通义千问分析
      await this.analyzeWithQwen();
      
      // 更新农田信息KPI卡片
      this.updateFarmKPI();
    },
    
    // 清空之前的分析内容
    clearPreviousAnalysis() {
      this.aiText = '';
      this.fullAiText = '';
      this.typing = false;
      this.aiPredictions = {
        growthRate: 0,
        harvestDate: '',
        expectedYield: 0,
        riskLevel: 'low',
        suggestions: [],
        marketAnalysis: '',
        weatherImpact: '',
        cropStatus: ''
      };
      // 重置KPI数据为初始值
      this.kpiData[0].value = 0;
      this.kpiData[1].value = 0;
      this.kpiData[2].value = 0;
      this.kpiData[3].value = 0;
      this.animatedValues = [0, 0, 0, 0];
    },
    
    // 使用通义千问分析农田数据
    async analyzeWithQwen() {
      // 防止重复调用
      if (this.qwenAnalyzing) {
        return;
      }
      
      this.qwenAnalyzing = true;
      this.aiAnalyzing = true;
      
      try {
        // 获取当前环境数据
        const weatherData = await this.getWeatherData();
        const marketData = await this.getMarketData();
        
        // 构建提示词
        const prompt = `
          请分析以下农田数据并给出预测：
          农田名称：${this.currentFarmData.name}
          种植作物：${this.currentFarmData.crop}
          种植面积：${this.currentFarmData.area}亩
          种植日期：${this.currentFarmData.plantDate}
          当前室内温度：${this.sensorData.temperature}°C
          当前室外温度：${weatherData.temperature}°C
          当前湿度：${this.sensorData.humidity}%
          土壤湿度：${this.sensorData.soilMoisture}%
          未来7天天气：${weatherData.forecast}
          当前${this.currentFarmData.crop}市场价：${marketData.price}元/吨
          
          请给出：
          1. 预估亩产（吨/亩）
          2. 预计总产量（吨）
          3. 预计收入（万元）
          4. 收获日期预测
          5. 天气对产量的影响评估
          6. 市场行情分析
          7. 种植建议
          
          请以JSON格式返回结果。
        `;
        
        // 调用通义千问API
        const response = await this.callQwenAPI(prompt);
        
        // 解析结果
        if (response) {
          this.processQwenResult(response);
        } else {
          // 使用本地模拟分析
          this.performLocalAnalysis();
        }
      } catch (e) {
        console.error('通义千问分析失败:', e);
        // 降级到本地分析
        this.performLocalAnalysis();
      } finally {
        // 延迟重置状态，避免频繁调用
        setTimeout(() => {
          this.qwenAnalyzing = false;
          this.aiAnalyzing = false;
        }, 2000);
      }
    },
    
    // 调用通义千问API（通过后端代理，解决CORS问题）
    async callQwenAPI(prompt) {
      try {
        // 调用后端代理接口
        const response = await this.request.post('/api/chat/qwen-proxy', {
          prompt: prompt,
          systemPrompt: '你是一个农业专家，善于分析农田数据并给出产量和市场预测。请以JSON格式返回结果。'
        });
        
        if (response && response.code === 200 && response.data) {
          // 如果返回的是对象，直接返回
          if (typeof response.data === 'object') {
            return response.data;
          }
          // 如果是字符串，尝试解析为JSON
          if (typeof response.data === 'string') {
            try {
              return JSON.parse(response.data);
            } catch {
              console.error('无法解析通义千问返回的数据:', response.data);
              return null;
            }
          }
        }
        return null;
      } catch (e) {
        console.error('通义千问API调用失败:', e);
        return null;
      }
    },
    
    // 处理通义千问返回结果
    processQwenResult(result) {
      // 确保 result 是有效对象
      if (!result || typeof result !== 'object') {
        this.performLocalAnalysis();
        return;
      }
      
      // 更新AI预测数据（使用安全的默认值）
      const yieldPerAcre = result.yieldPerAcre || 2.5;
      const totalYield = result.totalYield || (yieldPerAcre * (this.currentFarmData.area || 10));
      const suggestions = Array.isArray(result.suggestions) ? result.suggestions : ['适时灌溉', '注意病虫害防治'];
      const marketAnalysis = result.marketAnalysis || '市场价格稳定，需求量较大';
      const weatherImpact = result.weatherImpact || '天气适宜，对产量有积极影响';
      
      this.aiPredictions.expectedYield = yieldPerAcre;
      this.aiPredictions.harvestDate = result.harvestDate || this.calculateHarvestDate();
      this.aiPredictions.marketAnalysis = marketAnalysis;
      this.aiPredictions.weatherImpact = weatherImpact;
      this.aiPredictions.suggestions = suggestions;
      
      // 更新KPI数据
      this.kpiData[0].value = yieldPerAcre; // 亩产
      this.kpiData[1].value = result.expectedIncome || 50; // 预计收入
      
      // 生成AI分析文本（使用安全的变量）
      this.fullAiText = `【通义千问智能分析】基于${this.currentFarmData.name}的实时数据分析，预计亩产可达${yieldPerAcre}吨，总产量约${totalYield}吨。${marketAnalysis}。${weatherImpact}。建议：${suggestions.join('；')}。`;
      
      this.startAITyping();
    },
    
    // 本地模拟分析
    performLocalAnalysis() {
      // 基于农田数据进行分析（不使用传感器数据）
      // 模拟一个基于季节和作物类型的环境评分
      const month = new Date().getMonth() + 1;
      let baseScore = 75;
      
      // 根据季节调整
      if (month >= 3 && month <= 5) baseScore = 85; // 春季
      else if (month >= 6 && month <= 8) baseScore = 70; // 夏季
      else if (month >= 9 && month <= 11) baseScore = 80; // 秋季
      else baseScore = 65; // 冬季
      
      // 根据作物类型调整
      const cropBonus = {
        '小麦': month >= 3 && month <= 5 ? 10 : 0,
        '水稻': month >= 6 && month <= 8 ? 10 : 0,
        '玉米': month >= 4 && month <= 7 ? 10 : 0,
        '大豆': month >= 5 && month <= 8 ? 10 : 0,
        '默认': 5
      };
      
      const envScore = Math.min(100, baseScore + (cropBonus[this.currentFarmData.crop] || cropBonus['默认']));
      
      // 根据作物类型估算产量
      const yieldFactors = {
        '小麦': { base: 0.4, optimal: 0.5 },
        '水稼': { base: 0.6, optimal: 0.75 },
        '玉米': { base: 0.7, optimal: 0.9 },
        '大豆': { base: 0.25, optimal: 0.35 },
        '默认': { base: 0.3, optimal: 0.4 }
      };
      
      const cropFactor = yieldFactors[this.currentFarmData.crop] || yieldFactors['默认'];
      const yieldPerAcre = cropFactor.base + (cropFactor.optimal - cropFactor.base) * (envScore / 100);
      const totalYield = yieldPerAcre * this.currentFarmData.area;
      
      // 估算市场价格
      const marketPrices = {
        '小麦': 2800,
        '水稼': 3200,
        '玉米': 2600,
        '大豆': 5500,
        '默认': 3000
      };
      
      const marketPrice = marketPrices[this.currentFarmData.crop] || marketPrices['默认'];
      const expectedIncome = (totalYield * marketPrice / 10000).toFixed(2); // 万元
      
      // 更新预测数据
      this.aiPredictions.expectedYield = yieldPerAcre.toFixed(2);
      this.aiPredictions.growthRate = envScore;
      this.aiPredictions.harvestDate = this.calculateHarvestDate();
      this.aiPredictions.marketAnalysis = `当前${this.currentFarmData.crop}市场价约${marketPrice}元/吨，价格稳定`;
      this.aiPredictions.weatherImpact = envScore > 80 ? '季节条件优越' : envScore > 60 ? '季节条件适宜' : '需关注季节变化';
      
      // 更新KPI数据
      this.kpiData[0].value = yieldPerAcre.toFixed(2);
      this.kpiData[1].value = expectedIncome;
      this.kpiData[2].value = this.currentFarmData.area;
      this.kpiData[3].value = this.calculateRemainingDays();
      
      // 生成AI文本
      this.fullAiText = `【智能分析】${this.currentFarmData.name}种植${this.currentFarmData.crop}，面积${this.currentFarmData.area}亩。当前环境评分${envScore}分，预计亩产${yieldPerAcre.toFixed(2)}吨，总产量${totalYield.toFixed(0)}吨，预计收入${expectedIncome}万元。${this.aiPredictions.weatherImpact}。`;
      
      // 生成基于季节的建议
      const suggestions = [];
      if (month >= 6 && month <= 8) suggestions.push('夏季高温，注意防暑降温');
      if (month >= 11 || month <= 2) suggestions.push('冬季寒冷，注意保温防冻');
      if (month >= 3 && month <= 4) suggestions.push('春季适宜播种，把握时机');
      this.aiPredictions.suggestions = suggestions;
      
      this.animateKPIValues();
      this.startAITyping();
    },
    
    // 更新农田KPI卡片
    updateFarmKPI() {
      this.kpiData[2].value = this.currentFarmData.area;
      this.kpiData[3].value = this.calculateRemainingDays();
      
      // 更新趋势信息
      const growthRate = Number(this.aiPredictions.growthRate || 0);
      this.kpiData[0].trend = growthRate >= 80 ? 4 : growthRate >= 60 ? 1 : -2;
      this.kpiData[1].trend = this.currentFarmData.area >= 50 ? 3 : 1;
      this.kpiData[2].trend = 0; // 面积不变
      this.kpiData[3].trend = this.calculateRemainingDays() > 0 ? -1 : 0;
      
      this.animateKPIValues();
    },
    
    // 计算收获日期
    calculateHarvestDate() {
      const plantDate = new Date(this.currentFarmData.plantDate);
      const harvestDate = new Date(plantDate);
      harvestDate.setDate(harvestDate.getDate() + (this.currentFarmData.growthDays || 90));
      return harvestDate.toLocaleDateString();
    },
    
    // 计算剩余天数
    calculateRemainingDays() {
      const plantDate = new Date(this.currentFarmData.plantDate);
      const now = new Date();
      const elapsedDays = Math.floor((now - plantDate) / (1000 * 60 * 60 * 24));
      return Math.max(0, (this.currentFarmData.growthDays || 90) - elapsedDays);
    },
    
    // 获取天气数据（模拟）
    async getWeatherData() {
      try {
        const [nowRes, weekRes] = await Promise.all([
          this.request.get('/aether/weather/now'),
          this.request.get('/aether/weather/7d')
        ]);
        const nowData = nowRes?.code === '200' ? nowRes.data?.data : null;
        const weekData = weekRes?.code === '200' ? (weekRes.data?.data?.daily || weekRes.data?.data || []) : [];
        const forecast = Array.isArray(weekData) && weekData.length > 0
          ? weekData.slice(0, 3).map(item => item.textDay || item.text || '晴').join('，')
          : '未来天气以晴到多云为主';

        return {
          temperature: Number(nowData?.temp || this.sensorData.temperature || 24),
          humidity: Number(nowData?.humidity || this.sensorData.humidity || 60),
          forecast
        };
      } catch (e) {
        return {
          temperature: Number(this.sensorData.temperature || 24),
          humidity: Number(this.sensorData.humidity || 60),
          forecast: '未来天气以晴到多云为主'
        };
      }
    },
    
    // 获取市场数据（模拟）
    async getMarketData() {
      const prices = {
        '小麦': 2800,
        '水稻': 3200,
        '玉米': 2600,
        '大豆': 5500
      };
      return {
        price: prices[this.currentFarmData.crop] || 3000
      };
    },
    
    
    // 获取历史数据
    async fetchHistoryData() {
      try {
        const res = await this.request.get('/aether/readings/detail', { params: { days: 7 } });
        if (res.data && res.data.data) {
          this.historyData = res.data.data;
        }
      } catch (e) {
        console.error('获取历史数据失败:', e);
      }
    },
    
    // 检查预警（基于农田数据）
    checkAlerts() {
      if (!this.currentFarmData) return;
      
      const alerts = [];
      const now = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      const remainingDays = this.calculateRemainingDays();
      
      // 收获期预警
      if (remainingDays <= 7) {
        alerts.push({
          time: now,
          sensor: this.currentFarmData.name,
          type: '临近收获',
          value: `剩余${remainingDays}天`,
          handled: false,
          severity: 'high'
        });
      }
      
      // 季节预警
      const month = new Date().getMonth() + 1;
      if (month >= 6 && month <= 8) {
        alerts.push({
          time: now,
          sensor: this.currentFarmData.name,
          type: '夏季高温',
          value: '注意防暑',
          handled: false,
          severity: 'medium'
        });
      } else if (month >= 11 || month <= 2) {
        alerts.push({
          time: now,
          sensor: this.currentFarmData.name,
          type: '冬季低温',
          value: '注意防冻',
          handled: false,
          severity: 'medium'
        });
      }
      
      // 添加新预警到列表
      if (alerts.length > 0) {
        this.alertData = [...alerts, ...this.alertData].slice(0, 10); // 保留最近10条
      }
    },
    
    
    // 计算环境评分（不依赖传感器数据）
    calculateEnvironmentScore() {
      // 基于季节和作物生长周期计算
      const month = new Date().getMonth() + 1;
      let score = 75;
      
      // 季节评分
      if (month >= 3 && month <= 5) score = 85; // 春季
      else if (month >= 6 && month <= 8) score = 70; // 夏季
      else if (month >= 9 && month <= 11) score = 80; // 秋季
      else score = 65; // 冬季
      
      return Math.max(0, Math.min(100, score)).toFixed(1);
    },

    // 更新时间
    updateTime() {
      const now = new Date();
      this.currentTime = now.toLocaleTimeString('zh-CN');
    },
    
    // AI 打字效果
    startAITyping() {
      // 确保先清空之前的内容
      this.aiText = '';
      this.typing = true;
      let index = 0;
      const typeInterval = setInterval(() => {
        if (index < this.fullAiText.length) {
          this.aiText += this.fullAiText[index];
          index++;
        } else {
          this.typing = false;
          clearInterval(typeInterval);
        }
      }, 50);  // 每50ms打一个字
    },
    
    // KPI 数字跳动动画
    animateKPIValues() {
      this.kpiData.forEach((kpi, index) => {
        let current = 0;
        const target = Number(kpi.value);
        const duration = 2000;
        const step = target / (duration / 16);
        
        const animate = () => {
          current += step;
          if (current < target) {
            this.animatedValues[index] = current.toFixed(1);
            requestAnimationFrame(animate);
          } else {
            this.animatedValues[index] = Number(target).toFixed(1);
          }
        };
        animate();
      });
    },
    
    // 刷新数据
    async refreshData() {
      this.loading = true;
      if (this.selectedFarmId) {
        await this.onFarmSelect(this.selectedFarmId);
        this.$message.success('农田数据刷新成功！');
      } else {
        this.$message.warning('请先选择农田');
      }
      this.loading = false;
    },
      // 更新雷达图（基于农田数据）
    updateRadarChart(envScore) {
      if (!this.currentFarmData) return;
      
      // 基于季节和作物类型生成模拟分数
      const seasonScore = Number(envScore) || 75;
      const yieldScore = Math.min(100, this.kpiData[0].value * 100);
      const incomeScore = Math.min(100, this.kpiData[1].value * 2);
      const areaScore = Math.min(100, this.currentFarmData.area / 2);
      const timeScore = Math.min(100, 100 - this.calculateRemainingDays() / 0.9);
      
      const option = {
        radar: {
          indicator: [
            { name: '季节适宜度', max: 100 },
            { name: '预估亩产', max: 100 },
            { name: '预计收益', max: 100 },
            { name: '种植规模', max: 100 },
            { name: '生长进度', max: 100 }
          ],
          radius: '65%',
          splitArea: { areaStyle: { color: ['#f5f7fa', '#fff'] } }
        },
        series: [{
          type: 'radar',
          data: [
            { 
              value: [seasonScore, yieldScore, incomeScore, areaScore, timeScore], 
              name: '农田综合评分', 
              areaStyle: { color: 'rgba(34, 197, 94, 0.3)' }, 
              itemStyle: { color: '#22c55e' }, 
              lineStyle: { width: 2 } 
            },
            { 
              value: [100, 100, 100, 100, 100], 
              name: '标准参考值', 
              lineStyle: { type: 'dashed', color: '#ccc' }, 
              symbol: 'none' 
            }
          ]
        }]
      };
      this.radarChart.setOption(option);
    },

    initRadarChart() {
      if (!this.$refs.radarChart) return; // DOM不存在则跳过
      this.radarChart = echarts.init(this.$refs.radarChart);
      const option = {
        radar: {
          indicator: [
            { name: '温度适宜度', max: 100 },
            { name: '湿度适宜度', max: 100 },
            { name: '光照充足度', max: 100 },
            { name: 'CO2浓度', max: 100 },
            { name: '土壤肥力', max: 100 }
          ],
          radius: '65%',
          splitArea: { areaStyle: { color: ['#f5f7fa', '#fff'] } }
        },
        series: [{
          type: 'radar',
          data: [
            { value: [90, 85, 70, 95, 88], name: '当前环境评分', areaStyle: { color: 'rgba(24, 144, 255, 0.3)' }, itemStyle: { color: '#1890ff' }, lineStyle: { width: 2 } },
            { value: [100, 100, 100, 100, 100], name: '标准参考值', lineStyle: { type: 'dashed', color: '#ccc' }, symbol: 'none' }
          ]
        }]
      };
      if (this.radarChart) this.radarChart.setOption(option);
    },

    initParticleCanvas() {
      this.canvas = this.$refs.particleCanvas;
      if (!this.canvas) return;
      this.ctx = this.canvas.getContext('2d');
      this.resizeCanvas();
      window.addEventListener('resize', this.resizeCanvas);
      this.initParticles();
      this.animate();
    },

    resizeCanvas() {
      if (!this.canvas) return;
      const container = this.canvas.parentElement;
      const { width, height } = container.getBoundingClientRect();
      this.canvas.width = width;
      this.canvas.height = height;
      this.canvasWidth = width;
      this.canvasHeight = height;
    },

    initParticles() {
      const count = 40;
      this.particles = [];
      for (let i = 0; i < count; i++) {
        this.particles.push({
          x: Math.random() * this.canvasWidth,
          y: Math.random() * this.canvasHeight,
          vx: (Math.random() - 0.5) * 0.3,
          vy: (Math.random() - 0.5) * 0.3,
          r: Math.random() * 2 + 1,
          alpha: Math.random() * 0.4 + 0.1
        });
      }
    },

    animate() {
      if (!this.ctx || !this.particles.length) return;
      const ctx = this.ctx;
      const particles = this.particles;
      const width = this.canvasWidth;
      const height = this.canvasHeight;

      ctx.clearRect(0, 0, width, height);

      particles.forEach(p => {
        p.x += p.vx;
        p.y += p.vy;
        if (p.x < 0 || p.x > width) p.vx *= -1;
        if (p.y < 0 || p.y > height) p.vy *= -1;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(37, 99, 235, ${p.alpha * 0.3})`;
        ctx.fill();
      });

      this.animationId = requestAnimationFrame(this.animate);
    },

    // 多地块对比图
    initCompareChart() {
      if (!this.$refs.compareChart) return; // DOM不存在则跳过
      this.compareChart = echarts.init(this.$refs.compareChart);
      const compareFarms = this.farmList.filter(f => this.selectedFarms.includes(f.id));
      const farms = compareFarms.map(f => f.name);
      
      const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { data: ['温度', '湿度'], bottom: 5 },
        grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
        xAxis: { type: 'category', data: farms },
        yAxis: { type: 'value' },
        series: [
          {
            name: '温度',
            type: 'bar',
            data: compareFarms.map(f => Number(f.temperature || 0)),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#ff9a56' },
                { offset: 1, color: '#ff7b39' }
              ])
            }
          },
          {
            name: '湿度',
            type: 'bar',
            data: compareFarms.map(f => Number(f.humidity || 0)),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#4facfe' },
                { offset: 1, color: '#00f2fe' }
              ])
            }
          }
        ]
      };
      this.compareChart.setOption(option);
    },

    handleResize() {
      this.radarChart && this.radarChart.resize();
      this.compareChart && this.compareChart.resize();
    },

    // 控制IoT设备
    async controlDevice(type) {
      if (type === 'led') {
        try {
          const res = await this.request.post('/aether/device/control/led', { 
            led: this.sensorData.led ? 1 : 0 
          });
          if (res.code === '200') {
            this.$message.success(`LED补光灯已${this.sensorData.led ? '开启' : '关闭'}`);
          }
        } catch (e) {
          this.$message.error('控制失败');
          this.sensorData.led = !this.sensorData.led; // 恢复状态
        }
      } else if (type === 'irrigation') {
        this.$message.success(`智能灌溉已${this.autoControl.irrigation ? '开启' : '关闭'}`);
        if (this.autoControl.irrigation) {
          // 自动关闭（30分钟后）
          setTimeout(() => {
            this.autoControl.irrigation = false;
            this.$message.info('灌溉已自动停止');
          }, 30 * 60 * 1000);
        }
      } else if (type === 'ventilation') {
        this.$message.success(`通风系统已${this.autoControl.ventilation ? '开启' : '关闭'}`);
      }
    },
    
    // 处理预警
    handleAlert(index) {
      this.alertData[index].handled = true;
      this.$message.success('预警已处理');
      
      // 根据预警类型自动执行相应操作
      const alert = this.alertData[index];
      if (alert.type === '土壤缺水') {
        this.autoControl.irrigation = true;
        this.controlDevice('irrigation');
      } else if (alert.type === '温度过高') {
        this.autoControl.ventilation = true;
        this.controlDevice('ventilation');
      }
    },

    exportData() {
      this.$message.success('正在生成环境监测报表(PDF)，请稍候...');
      // TODO: 实现导出功能
    },

    getTypeColor(type) {
      if(type.includes('高')) return 'text-danger';
      if(type.includes('低')) return 'text-warning';
      return 'text-info';
    },
    
    getAlertIcon(type) {
      if(type.includes('温度')) return 'el-icon-sunny';
      if(type.includes('湿度')) return 'el-icon-heavy-rain';
      if(type.includes('CO2')) return 'el-icon-wind-power';
      if(type.includes('设备')) return 'el-icon-warning';
      if(type.includes('土壤')) return 'el-icon-view';
      return 'el-icon-info';
    },
    
    getAlertIconClass(type) {
      if(type.includes('高')) return 'danger';
      if(type.includes('低')) return 'warning';
      if(type.includes('超标')) return 'danger';
      if(type.includes('离线')) return 'info';
      return 'warning';
    }
  }
}
</script>

<style scoped>
.env-monitor-page {
  position: relative;
  padding: 16px;
  padding-top: 8px;
  padding-bottom: 40px;
  background: #f4f6f9;
  min-height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
}

.env-monitor-page::-webkit-scrollbar {
  width: 8px;
}

.env-monitor-page::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.env-monitor-page::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.env-monitor-page::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 简洁背景装饰 */
.bg-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, #fafbfc 0%, #f4f6f9 100%);
  opacity: 1;
  z-index: 0;
}

/* 粒子画布 */
.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
  opacity: 0.2;
}

.env-monitor-page > *:not(.particle-canvas):not(.page-header) {
  position: relative;
  z-index: 2;
}

/* 玻璃态效果 */
.glass-effect {
  background: white;
  backdrop-filter: blur(20px);
  border: 1px solid #ffffff;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.03), 0 4px 6px -2px rgba(0, 0, 0, 0.01);
}

/* 头部样式 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 20px;
  border-radius: 16px;
  position: relative;
  z-index: 100;
}

.left-title {
  flex: 1;
}

.left-title h2 {
  margin: 0 0 4px 0;
  color: #1e293b;
  font-size: 20px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-center {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-right {
  flex: 0 0 auto;
  display: flex;
  gap: 10px;
}

.icon-pulse {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7); }
  50% { transform: scale(1.05); box-shadow: 0 0 20px 10px rgba(102, 126, 234, 0); }
}

.icon-pulse i {
  color: #fff;
  font-size: 18px;
}

.left-title .sub {
  font-size: 11px;
  color: #64748b;
  display: block;
  margin-bottom: 4px;
  letter-spacing: 1px;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #10b981;
  margin-top: 2px;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  animation: blink 2s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.divider {
  color: #cbd5e1;
}

.time-update {
  font-family: 'Courier New', monospace;
  color: #2563eb;
  font-weight: 600;
}

.right-actions {
  display: flex;
  gap: 10px;
}

/* AI 建议卡片样式在底部定义 */

.ai-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.3);
}

.ai-icon {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.ai-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-text {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.ai-content {
  background: rgba(124, 58, 237, 0.05);
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 10px;
  border-left: 3px solid #7c3aed;
}

.typing-text {
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
  margin: 0;
}

.cursor {
  animation: cursor-blink 1s step-start infinite;
  color: #7c3aed;
  font-weight: bold;
}

@keyframes cursor-blink {
  50% { opacity: 0; }
}

.ai-footer {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #94a3b8;
}

.ai-footer span {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* AI预测结果 */
.ai-predictions {
  margin-top: 15px;
  padding: 15px;
  background: rgba(124, 58, 237, 0.03);
  border-radius: 10px;
  border: 1px solid rgba(124, 58, 237, 0.1);
}

.prediction-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #475569;
}

.prediction-item:last-child {
  margin-bottom: 0;
}

.prediction-item i {
  color: #7c3aed;
  font-size: 16px;
}

.prediction-item .el-progress {
  flex: 1;
  max-width: 200px;
}

/* KPI 卡片 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.kpi-card {
  position: relative;
  background: white;
  backdrop-filter: blur(20px);
  border: 1px solid #ffffff;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.03), 0 4px 6px -2px rgba(0, 0, 0, 0.01);
}

.kpi-card.excellent {
  border-color: #22c55e;
  background: linear-gradient(135deg, #f0fdf4, #fff);
}

.kpi-card.warning {
  border-color: #e6a23c;
  background: linear-gradient(135deg, #fffbf5, #fff);
}

.kpi-card.danger {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fff5f5, #fff);
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05);
}

.update-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.kpi-bg-wave {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, transparent, rgba(37, 99, 235, 0.2), transparent);
  animation: wave 3s linear infinite;
}

@keyframes wave {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.icon-box {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.icon-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  animation: ring 2s ease-in-out infinite;
}

@keyframes ring {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.5; }
}

.icon-box.temp { background: linear-gradient(135deg, #ff7eb3, #ff758c); }
.icon-box.humi { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.icon-box.soil { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.icon-box.device { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }
.icon-box.yield { background: linear-gradient(135deg, #4ade80, #22c55e); }
.icon-box.income { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.icon-box.area { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.icon-box.growth { background: linear-gradient(135deg, #f472b6, #ec4899); }

.kpi-content {
  flex: 1;
}

.kpi-content .label {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 8px;
}

.kpi-content .value {
  color: #1e293b;
  font-size: 28px;
  font-weight: 800;
  font-family: 'Arial', sans-serif;
}

.unit {
  font-size: 14px;
  font-weight: normal;
  color: #64748b;
  margin-left: 4px;
}

.trend {
  font-size: 12px;
  margin-top: 6px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend.up { color: #10b981; }
.trend.down { color: #ef4444; }

/* 玻璃面板 */
.glass-panel {
  background: white;
  backdrop-filter: blur(20px);
  border: 1px solid #ffffff;
  border-radius: 16px;
  padding: 15px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.03), 0 4px 6px -2px rgba(0, 0, 0, 0.01);
}

/* AI卡片样式 */
.ai-advice-card {
  background: linear-gradient(135deg, white 0%, #fafbfc 100%);
  backdrop-filter: blur(20px);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.03), 0 4px 6px -2px rgba(0, 0, 0, 0.01);
  transition: all 0.3s;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chart-header .title {
  font-weight: 700;
  color: #1e293b;
  font-size: 16px;
}




/* 表格样式 */
.text-danger {
  color: #ef4444;
  font-weight: 700;
}

.text-warning {
  color: #f59e0b;
}

.text-info {
  color: #6b7280;
}

/* Agent 区域 */
.agent-card {
  margin-bottom: 16px;
  padding: 16px;
}

.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.agent-sub {
  margin: 4px 0 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.agent-actions {
  display: flex;
  gap: 8px;
}

.agent-plan {
  margin-top: 12px;
  border: 1px dashed #e2e8f0;
  border-radius: 10px;
  padding: 10px;
}

.agent-advice {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  margin-bottom: 10px;
}

.agent-action-item {
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}

.agent-action-item:last-child {
  border-bottom: none;
}

.action-title {
  font-weight: 700;
  margin-right: 10px;
}

.action-desc {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.action-route {
  color: #2563eb;
  font-weight: 600;
}

.agent-results {
  margin-top: 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}

.results-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: white;
  font-weight: 700;
  font-size: 14px;
}

.results-title i {
  font-size: 16px;
}

.agent-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.3s;
}

.agent-result-item:last-child {
  border-bottom: none;
}

.agent-result-item:hover {
  background: rgba(37, 99, 235, 0.05);
}

.result-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.agent-result-item.success .result-icon {
  background: rgba(22, 163, 74, 0.1);
  color: #16a34a;
}

.agent-result-item.pending-client .result-icon {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.agent-result-item.failed .result-icon {
  background: rgba(220, 38, 38, 0.1);
  color: #dc2626;
}

.result-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-message {
  color: #1e293b;
  font-weight: 600;
  font-size: 14px;
}

.result-route {
  color: #64748b;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
</style>
