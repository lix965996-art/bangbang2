<template>
  <div class="command-dashboard" v-loading="loading">
    <section class="command-hero">
      <div class="hero-copy">
        <div class="eyebrow"><span class="live-dot"></span> FARM INTELLIGENCE BOARD</div>
        <h1>环境监测分析看板</h1>
        <p>{{ selectedFarmName }} · 地块环境数据、设备遥测与智能研判建议</p>
        <div class="source-pill">数据来源：{{ sensorSourceLabel }}</div>
      </div>
      <div class="hero-tools">
        <el-select
          v-model="selectedFarmId"
          placeholder="切换监测地块"
          @change="onFarmSelect"
          size="small"
          class="command-select farm-select">
          <el-option v-for="farm in farmList" :key="farm.id" :label="farm.name" :value="farm.id">
            <span style="float: left; font-weight: 700;">{{ farm.name }}</span>
            <span style="float: right; color: #5eead4; font-size: 12px">{{ farm.crop || '未种植' }}</span>
          </el-option>
        </el-select>
        <el-button class="command-refresh" size="small" icon="el-icon-refresh" @click="refreshData" :loading="loading">
          同步遥测
        </el-button>
      </div>
    </section>

    <section class="telemetry-strip">
      <div v-for="(kpi, index) in kpiData" :key="index" class="telemetry-cell" :class="kpi.colorClass">
        <div class="cell-top">
          <i :class="kpi.icon"></i>
          <span>{{ kpi.label }}</span>
        </div>
        <strong>{{ kpi.value }}<em>{{ kpi.unit }}</em></strong>
        <small>{{ kpi.note }}</small>
      </div>
    </section>

    <section class="command-grid">
      <div class="command-panel twin-panel">
        <div class="panel-head">
          <div>
            <span class="section-kicker">FIELD STATUS</span>
            <h2>地块生命体征</h2>
          </div>
          <span class="risk-badge" :class="riskLevel.className">{{ riskLevel.label }}</span>
        </div>
        <div class="twin-stage">
          <div class="scan-orbit orbit-one"></div>
          <div class="scan-orbit orbit-two"></div>
          <div class="field-core">
            <span>{{ selectedFarmName }}</span>
            <strong>{{ envScore }}</strong>
            <small>环境综合分</small>
          </div>
          <div class="radial-label label-top">AI</div>
          <div class="radial-label label-right">IoT</div>
          <div class="radial-label label-bottom">巡检</div>
          <div class="radial-label label-left">GIS</div>
        </div>
        <div class="sensor-grid">
          <div v-for="sensor in sensorTiles" :key="sensor.name" class="sensor-tile" :class="sensor.state">
            <span>{{ sensor.name }}</span>
            <strong>{{ sensor.value }}<em>{{ sensor.unit }}</em></strong>
            <small>{{ sensor.status }}</small>
          </div>
        </div>
      </div>

      <div class="command-panel ai-panel">
        <div class="panel-head">
          <div>
            <span class="section-kicker">DECISION SUPPORT</span>
            <h2>AI 研判建议</h2>
          </div>
          <span class="engine-state" :class="{ loading: qwenAnalyzing }">
            {{ qwenAnalyzing ? '分析中' : '在线' }}
          </span>
        </div>
        <div class="ai-console">
          <div class="ai-avatar">
            <img src="@/assets/ai.png" alt="AI" @error="handleImgError" />
          </div>
          <p>{{ aiText }}<span class="cursor" v-if="typing">|</span></p>
        </div>
        <div class="command-chain">
          <div v-for="item in commandActions" :key="item.title" class="chain-item">
            <span class="chain-dot"></span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.detail }}</small>
            </div>
            <em>{{ item.state }}</em>
          </div>
        </div>
      </div>

      <div class="command-panel radar-panel">
        <div class="panel-head">
          <div>
            <span class="section-kicker">SENSOR FUSION</span>
            <h2>五维环境雷达</h2>
          </div>
          <strong class="score-text">{{ envScore }}</strong>
        </div>
        <div class="chart-container" ref="radarChart"></div>
      </div>

      <div class="command-panel alert-panel">
        <div class="panel-head">
          <div>
            <span class="section-kicker">EVENT REVIEW</span>
            <h2>异常研判队列</h2>
          </div>
        </div>
        <div class="event-stream">
          <div v-for="item in anomalyItems" :key="item.title" class="event-item" :class="item.level">
            <span class="event-time">{{ item.time }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.detail }}</small>
            </div>
          </div>
        </div>
      </div>

      <div class="command-panel compare-panel">
        <div class="panel-head compare-head">
          <div>
            <span class="section-kicker">FIELD TELEMETRY</span>
            <h2>跨地块遥测对比</h2>
          </div>
          <div class="field-switcher">
            <el-checkbox-group v-model="selectedFarms" @change="onFarmsChange" size="small">
              <el-checkbox v-for="farm in farmList" :key="farm.id" :label="farm.id" border>
                {{ farm.name }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
        <div class="field-matrix">
          <div v-for="field in fieldMatrix" :key="field.id" class="field-chip" :class="field.state">
            <span>{{ field.name }}</span>
            <strong>{{ field.score }}</strong>
            <small>{{ field.copy }}</small>
          </div>
        </div>
        <div class="chart-container large" ref="compareChart"></div>
      </div>
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'DashbordNew',
  data() {
    return {
      loading: false,
      farmList: [],
      selectedFarms: [],
      selectedFarmId: null, 
      currentFarmData: null,

      // 真实遥测数据：优先来自 OneNET 设备接口，其次来自地块档案字段。
      sensorData: {
        temperature: null, humidity: null, soilMoisture: null, lightIntensity: null, co2Level: null
      },
      sensorSource: '等待数据',
      deviceOnline: false,

      // AI 状态
      aiText: '正在初始化农事分析模型...',
      typing: false,
      qwenAnalyzing: false,
      typingTimeout: null,
      aiTimer: null,
      dataTimer: null,
      _isDestroyed: false,

      kpiData: [
        { label: '环境综合分', value: '--', unit: '', note: '按真实环境字段计算', icon: 'el-icon-data-analysis', colorClass: 'tone-cyan' },
        { label: '有效遥测项', value: 0, unit: ' 项', note: '温湿度/土壤/光照/CO2', icon: 'el-icon-cpu', colorClass: 'tone-green' },
        { label: '风险指标', value: 0, unit: ' 项', note: '仅进入研判，不自动执行', icon: 'el-icon-warning-outline', colorClass: 'tone-amber' },
        { label: '建议联动', value: 0, unit: ' 条', note: '需人工确认或接入控制接口', icon: 'el-icon-connection', colorClass: 'tone-violet' }
      ],

      aiPredictions: {
        growthRate: 0, harvestDate: '--', expectedYield: 0, marketAnalysis: '--'
      },

      radarChart: null,
      compareChart: null,
      envScore: '--',
      fallbackAiImg: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="%23AF52DE"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z"/></svg>'
    }
  },
  computed: {
    selectedFarmName() {
      return this.currentFarmData?.name || '全域农场'
    },
    sensorSourceLabel() {
      const state = this.deviceOnline ? '设备在线' : '设备未直连'
      return `${this.sensorSource} / ${state}`
    },
    validSensorCount() {
      return Object.values(this.sensorData).filter(value => this.hasValue(value)).length
    },
    hasTelemetry() {
      return this.validSensorCount > 0
    },
    riskLevel() {
      const score = Number(this.envScore)
      if (!Number.isFinite(score)) return { label: '等待数据', className: 'watch' }
      if (score >= 82) return { label: '低风险运行', className: 'stable' }
      if (score >= 68) return { label: '重点观测', className: 'watch' }
      return { label: '需要处置', className: 'danger' }
    },
    sensorTiles() {
      const temp = this.toFiniteNumber(this.sensorData.temperature)
      const humidity = this.toFiniteNumber(this.sensorData.humidity)
      const soil = this.toFiniteNumber(this.sensorData.soilMoisture)
      const light = this.toFiniteNumber(this.sensorData.lightIntensity)
      const co2 = this.toFiniteNumber(this.sensorData.co2Level)
      return [
        {
          name: '冠层温度',
          value: this.formatSensorValue(temp, 1),
          unit: '℃',
          status: this.hasValue(temp) ? (temp > 28 ? '偏高，建议通风' : '温度稳定') : '暂无真实值',
          state: this.getSensorState(temp, value => value > 28)
        },
        {
          name: '空气湿度',
          value: this.formatSensorValue(humidity, 0),
          unit: '%',
          status: this.hasValue(humidity) ? (humidity < 45 ? '偏干，建议补湿' : '湿度适宜') : '暂无真实值',
          state: this.getSensorState(humidity, value => value < 45)
        },
        {
          name: '土壤水分',
          value: this.formatSensorValue(soil, 0),
          unit: '%',
          status: this.hasValue(soil) ? (soil < 42 ? '建议补水复核' : '根区水分正常') : '暂无真实值',
          state: this.getSensorState(soil, value => value < 42, 'danger')
        },
        {
          name: '光照强度',
          value: this.formatSensorValue(light, 0),
          unit: 'lx',
          status: this.hasValue(light) ? (light > 3000 ? '强光，建议遮阳' : '光照可控') : '暂无真实值',
          state: this.getSensorState(light, value => value > 3000)
        },
        {
          name: 'CO2 浓度',
          value: this.formatSensorValue(co2, 0),
          unit: 'ppm',
          status: this.hasValue(co2) ? (co2 > 900 ? '偏高，建议换气' : '气体环境稳定') : '暂无真实值',
          state: this.getSensorState(co2, value => value > 900)
        }
      ]
    },
    commandActions() {
      const soil = this.toFiniteNumber(this.sensorData.soilMoisture)
      const temp = this.toFiniteNumber(this.sensorData.temperature)
      const soilLow = this.hasValue(soil) && soil < 42
      const tempHigh = this.hasValue(temp) && temp > 28
      return [
        {
          title: '巡检复核建议',
          detail: this.hasTelemetry ? `${this.selectedFarmName} 重点复核异常指标对应区域` : '等待真实遥测后生成巡检建议',
          state: this.hasTelemetry ? '建议' : '待数据'
        },
        {
          title: '自动滴灌策略',
          detail: soilLow ? '根区水分偏低，建议人工确认后执行滴灌' : '未检测到补水触发条件',
          state: soilLow ? '需确认' : '待命'
        },
        {
          title: '温室微气候控制',
          detail: tempHigh ? '温度偏高，建议检查风机或遮阳策略' : '未检测到高温触发条件',
          state: tempHigh ? '需确认' : '稳定'
        }
      ]
    },
    anomalyItems() {
      const soil = this.toFiniteNumber(this.sensorData.soilMoisture)
      const temp = this.toFiniteNumber(this.sensorData.temperature)
      const co2 = this.toFiniteNumber(this.sensorData.co2Level)
      const soilLow = this.hasValue(soil) && soil < 42
      const tempHigh = this.hasValue(temp) && temp > 28
      const co2High = this.hasValue(co2) && co2 > 900
      if (!this.hasTelemetry) {
        return [
          {
            time: '待采集',
            title: '暂无真实遥测数据',
            detail: '请检查地块环境字段或 OneNET 设备接口',
            level: 'notice'
          }
        ]
      }
      return [
        {
          time: '当前',
          title: soilLow ? '根区水分跌破舒适区' : '根区水分保持稳定',
          detail: soilLow ? '生成补水复核建议，尚未自动执行设备控制' : '暂不需要补水干预',
          level: soilLow ? 'danger' : 'stable'
        },
        {
          time: '当前',
          title: tempHigh ? '冠层温度偏高' : '冠层温度正常',
          detail: tempHigh ? '建议检查通风策略并复核设备状态' : '热区分布平稳',
          level: tempHigh ? 'warning' : 'stable'
        },
        {
          time: '当前',
          title: co2High ? 'CO2 浓度上行' : '气体环境正常',
          detail: co2High ? '生成换气建议，需接入控制接口后执行' : '无需额外处置',
          level: co2High ? 'warning' : 'notice'
        }
      ]
    },
    fieldMatrix() {
      return this.farmList.slice(0, 6).map((farm) => {
        const score = this.calculateFarmEnvironmentScore(farm)
        return {
          id: farm.id,
          name: farm.name,
          score: this.hasValue(score) ? score : '--',
          copy: farm.crop || '未配置作物',
          state: this.hasValue(score) && score >= 88 ? 'excellent' : this.hasValue(score) && score >= 78 ? 'normal' : 'watch'
        }
      })
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.loadFarmList();
      
      this.debouncedResize = this.debounce(this.handleResize, 200);
      window.addEventListener('resize', this.debouncedResize);

      this.startAiAnalysis();
      this.dataTimer = setInterval(() => { this.refreshSensorData(); }, 10000);
    });
  },
  beforeUnmount() {
    this._isDestroyed = true;
    if(this.aiTimer) clearInterval(this.aiTimer);
    if(this.dataTimer) clearInterval(this.dataTimer);
    if(this.typingTimeout) clearTimeout(this.typingTimeout);
    window.removeEventListener('resize', this.debouncedResize);
    
    if(this.radarChart) this.radarChart.dispose();
    if(this.compareChart) this.compareChart.dispose();
  },
  methods: {
    toFiniteNumber(value) {
      if (value === null || value === undefined || value === '') return null;
      const num = Number(value);
      return Number.isFinite(num) ? num : null;
    },
    hasValue(value) {
      return value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value));
    },
    formatSensorValue(value, digits = 0) {
      return this.hasValue(value) ? Number(value).toFixed(digits) : '--';
    },
    getSensorState(value, warnTester, warnState = 'warning') {
      if (!this.hasValue(value)) return 'empty';
      return warnTester(Number(value)) ? warnState : 'stable';
    },
    normalizeMetric(value, ideal, tolerance) {
      if (!this.hasValue(value)) return null;
      return Math.max(0, Math.min(100, 100 - Math.abs(Number(value) - ideal) * tolerance));
    },
    calculateFarmEnvironmentScore(farm) {
      if (!farm) return null;
      const scores = [
        this.normalizeMetric(farm.temperature, 25, 5),
        this.hasValue(farm.airhumidity) ? Math.max(0, Math.min(100, Number(farm.airhumidity))) : null,
        this.hasValue(farm.soilhumidity) ? Math.max(0, Math.min(100, Number(farm.soilhumidity))) : null,
        this.hasValue(farm.light) ? Math.max(0, Math.min(100, Number(farm.light) / 30)) : null,
        this.normalizeMetric(farm.carbon, 600, 0.08)
      ].filter(value => value !== null);
      if (!scores.length) return null;
      return Number((scores.reduce((sum, value) => sum + value, 0) / scores.length).toFixed(1));
    },
    applyFarmTelemetry(farm) {
      this.sensorData = {
        temperature: this.toFiniteNumber(farm?.temperature),
        humidity: this.toFiniteNumber(farm?.airhumidity),
        soilMoisture: this.toFiniteNumber(farm?.soilhumidity),
        lightIntensity: this.toFiniteNumber(farm?.light),
        co2Level: this.toFiniteNumber(farm?.carbon)
      };
      this.sensorSource = this.validSensorCount ? '地块档案' : '暂无真实遥测';
      this.deviceOnline = false;
    },
    handleImgError(e) {
      e.target.src = this.fallbackAiImg;
    },
    debounce(func, wait) {
      let timeout;
      return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
      };
    },
    getScoreColor(score) {
      if (score >= 80) return '#34C759'; 
      if (score >= 60) return '#FF9500'; 
      return '#FF3B30'; 
    },
    async loadFarmList() {
      this.loading = true;
      try {
        const res = await this.request.get('/statistic/page', { params: { pageNum: 1, pageSize: 100, farm: '' } });
        if (res.code === '200' && res.data && res.data.records) {
          this.farmList = res.data.records.map(item => ({
            id: item.id,
            name: item.farm || `农田${item.id}`,
            area: item.area || 100,
            crop: item.crop || '小麦',
            growthDays: item.growthDays || 90,
            temperature: item.temperature,
            airhumidity: item.airhumidity,
            soilhumidity: item.soilhumidity,
            light: item.light,
            carbon: item.carbon,
            ph: item.ph
          }));
          
          if (this.farmList.length > 0) {
            this.selectedFarmId = this.farmList[0].id;
            this.selectedFarms = this.farmList.slice(0, 3).map(f => f.id);
            this.onFarmSelect(this.selectedFarmId);
            this.initCompareChart();
            this.refreshSensorData();
          }
        }
      } catch (e) {
        this.$message.error('地块数据加载失败');
      } finally {
        this.loading = false;
      }
    },
    async onFarmSelect(farmId) {
      this.currentFarmData = this.farmList.find(f => f.id === farmId);
      if (this.currentFarmData) {
        this.applyFarmTelemetry(this.currentFarmData);
        this.envScore = this.calculateEnvironmentScore();
        this.updateKPIs();
        this.generateAiAnalysis();
        this.initRadarChart();
      }
    },
    onFarmsChange() {
      this.initCompareChart();
    },
    async refreshData() {
      this.loading = true;
      try {
        await this.refreshSensorData();
        this.generateAiAnalysis();
        this.$message.success(this.hasTelemetry ? '数据已同步' : '暂无可用真实遥测');
      } catch (error) {
        this.$message.error('刷新失败');
      } finally {
        this.loading = false;
      }
    },
    startAiAnalysis() {
      this.aiTimer = setInterval(() => {
        if (this.currentFarmData) this.generateAiAnalysis();
      }, 30000);
    },
    generateAiAnalysis() {
      this.qwenAnalyzing = true;
      this.aiText = '模型正在分析当前地块数据...';
      this.typing = false;
      clearTimeout(this.typingTimeout);

      setTimeout(() => {
        const month = new Date().getMonth() + 1;
        const crop = this.currentFarmData.crop;
        
        let suggestion = this.hasTelemetry
          ? '已基于当前真实环境字段完成研判，暂未发现必须自动处置的风险。'
          : '当前缺少真实遥测数据，系统仅展示地块档案与待采集状态，不生成执行类策略。';
        if (month >= 3 && month <= 5) suggestion = '春季生长窗口已打开，建议提高土壤水分采样频率，并让无人车优先复核根区状态。';
        else if (month >= 6 && month <= 8) suggestion = '系统检测到高温季风险，建议启用“通风 + 脉冲滴灌”联动策略，并缩短巡检间隔。';
        else if (month >= 9 && month <= 11) suggestion = '成熟期数据趋势良好，建议联动视觉巡检模型复核成熟度和病斑风险。';

        if (crop === '水稻') suggestion += ' 水稻处于分蘖期，建议保持浅水层。';
        else if (crop === '小麦') suggestion += ' 小麦拔节期，需重点关注氮肥施用情况。';

        const growthRate = Number(this.envScore) || 0;
        const expectedYield = this.currentFarmData.area || 0;
        const harvestDate = new Date(Date.now() + (90 - this.currentFarmData.growthDays) * 86400000);

        this.aiPredictions = {
          growthRate,
          harvestDate: harvestDate.toLocaleDateString('zh-CN'),
          expectedYield: parseFloat(expectedYield),
          marketAnalysis: this.hasTelemetry ? '环境控制策略可保持当前参数' : '缺少真实遥测，暂不生成控制策略'
        };

        this.updateKPIs();
        this.aiText = suggestion;
        this.typeAiText();
        this.qwenAnalyzing = false;
      }, 1500);
    },
    typeAiText() {
      this.typing = true;
      const fullText = this.aiText;
      let index = 0;

      const type = () => {
        if (this._isDestroyed) return;
        if (index < fullText.length) {
          this.aiText = fullText.substring(0, index + 1);
          index++;
          this.typingTimeout = setTimeout(type, 30); // 稍微加快打字速度
        } else {
          this.typing = false;
        }
      };
      type();
    },
    updateKPIs() {
      if (!this.currentFarmData) return;
      const score = Number(this.envScore);
      const riskFields = this.sensorTiles.filter(item => item.state === 'warning' || item.state === 'danger').length;
      this.kpiData[0].value = Number.isFinite(score) ? score.toFixed(1) : '--';
      this.kpiData[1].value = this.validSensorCount;
      this.kpiData[2].value = riskFields;
      this.kpiData[3].value = this.commandActions.filter(item => item.state === '需确认' || item.state === '建议').length;
    },
    calculateEnvironmentScore() {
      const farmScore = this.calculateFarmEnvironmentScore({
        temperature: this.sensorData.temperature,
        airhumidity: this.sensorData.humidity,
        soilhumidity: this.sensorData.soilMoisture,
        light: this.sensorData.lightIntensity,
        carbon: this.sensorData.co2Level
      });
      return this.hasValue(farmScore) ? Number(farmScore).toFixed(1) : '--';
    },
    async refreshSensorData() {
      if (this.currentFarmData) {
        this.applyFarmTelemetry(this.currentFarmData);
      }
      try {
        const res = await this.request.get('/aether/device/status');
        if (res.code === '200' && res.data) {
          const data = res.data;
          const temperature = this.toFiniteNumber(data.temperature);
          const humidity = this.toFiniteNumber(data.humidity);
          this.sensorData = {
            ...this.sensorData,
            temperature: this.hasValue(temperature) ? temperature : this.sensorData.temperature,
            humidity: this.hasValue(humidity) ? humidity : this.sensorData.humidity
          };
          this.sensorSource = data.source ? `设备接口(${data.source})` : '设备接口';
          this.deviceOnline = Boolean(data.online);
        }
      } catch (error) {
        this.deviceOnline = false;
      }
      this.envScore = this.calculateEnvironmentScore();
      this.updateKPIs();
      this.initRadarChart();
    },

    initRadarChart() {
      if (!this.$refs.radarChart) return;
      if (this.radarChart) this.radarChart.dispose();
      this.radarChart = echarts.init(this.$refs.radarChart);

      const data = [
        this.toFiniteNumber(this.sensorData.temperature) || 0,
        this.toFiniteNumber(this.sensorData.humidity) || 0,
        this.toFiniteNumber(this.sensorData.soilMoisture) || 0,
        (this.toFiniteNumber(this.sensorData.lightIntensity) || 0) / 30,
        (this.toFiniteNumber(this.sensorData.co2Level) || 0) / 10
      ];

      const color = this.getScoreColor(this.envScore);

      this.radarChart.setOption({
        backgroundColor: 'transparent',
        title: this.hasTelemetry ? undefined : {
          text: '暂无真实遥测',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#94a3b8', fontSize: 14, fontWeight: 600 }
        },
        radar: {
          indicator: [
            { name: '温度', max: 40 },
            { name: '空气湿度', max: 100 },
            { name: '土壤湿度', max: 100 },
            { name: '光照(/30)', max: 100 },
            { name: 'CO2(x10)', max: 100 }
          ],
          radius: '65%',
          center: ['50%', '55%'],
          splitNumber: 4,
          axisName: { color: '#52705e', fontSize: 12, fontWeight: 700 },
          splitLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.18)', width: 1 } },
          splitArea: { show: true, areaStyle: { color: ['rgba(236, 253, 245, 0.9)', 'rgba(255, 255, 255, 0.9)'] } },
          axisLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.16)' } }
        },
        series: [{
          type: 'radar',
          data: [{
            value: data,
            name: '实时环境',
            itemStyle: { color: color },
            areaStyle: { color: echarts.color.modifyAlpha(color, 0.22) },
            lineStyle: { width: 2.5 }
          }]
        }]
      });
    },

    initCompareChart() {
      if (!this.$refs.compareChart) return;
      if (this.compareChart) this.compareChart.dispose();
      this.compareChart = echarts.init(this.$refs.compareChart);
      
      const farms = this.farmList.filter(f => this.selectedFarms.includes(f.id));
      if(farms.length === 0) return;

      const farmNames = farms.map(f => f.name);
      
      const tempDatas = farms.map(f => this.toFiniteNumber(f.temperature));
      const humiDatas = farms.map(f => this.toFiniteNumber(f.airhumidity));

      this.compareChart.setOption({
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(16, 185, 129, 0.08)' } },
          backgroundColor: 'rgba(255, 255, 255, 0.96)',
          borderColor: 'rgba(16, 185, 129, 0.25)',
          textStyle: { color: '#163525' }
        },
        legend: {
          data: ['平均温度 (℃)', '平均湿度 (%)'],
          bottom: 0,
          textStyle: { color: '#52705e', fontWeight: 600 },
          icon: 'roundRect'
        },
        grid: { left: '2%', right: '2%', bottom: '17%', top: '14%', containLabel: true },
        xAxis: { 
          type: 'category', 
          data: farmNames, 
          axisLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.18)' } },
          axisTick: { show: false },
          axisLabel: { color: '#52705e', fontWeight: 700, margin: 12 }
        },
        yAxis: { 
          type: 'value', 
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: 'rgba(16, 185, 129, 0.12)', type: 'dashed' } },
          axisLabel: { color: '#789180' }
        },
        series: [
          {
            name: '平均温度 (℃)', type: 'bar', barWidth: '15%', barGap: '20%',
            itemStyle: {
              borderRadius: [6, 6, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#f97316' },
                { offset: 1, color: 'rgba(249, 115, 22, 0.12)' }
              ])
            },
            data: tempDatas
          },
          {
            name: '平均湿度 (%)', type: 'bar', barWidth: '15%',
            itemStyle: {
              borderRadius: [6, 6, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#10b981' },
                { offset: 1, color: 'rgba(16, 185, 129, 0.12)' }
              ])
            },
            data: humiDatas
          }
        ]
      });
    },
    handleResize() {
      if(this.radarChart) this.radarChart.resize();
      if(this.compareChart) this.compareChart.resize();
    }
  }
}
</script>

<style scoped>
.command-dashboard {
  min-height: calc(100vh - 90px);
  padding: 22px;
  color: #e5f8ff;
  background:
    linear-gradient(rgba(94, 234, 212, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(94, 234, 212, 0.05) 1px, transparent 1px),
    linear-gradient(135deg, #07111f 0%, #0f172a 46%, #101827 100%);
  background-size: 34px 34px, 34px 34px, cover;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

.command-hero {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  min-height: 164px;
  padding: 26px 28px;
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid rgba(94, 234, 212, 0.24);
  border-radius: 8px;
  background:
    linear-gradient(120deg, rgba(20, 184, 166, 0.22), transparent 42%),
    linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(15, 23, 42, 0.76));
  box-shadow: 0 22px 60px rgba(2, 8, 23, 0.32);
}

.command-hero::after {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #5eead4, #f59e0b, transparent);
}

.hero-copy,
.hero-tools {
  position: relative;
  z-index: 1;
}

.eyebrow,
.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #5eead4;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 16px rgba(34, 197, 94, 0.9);
  animation: blink 1.4s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero-copy h1 {
  margin: 10px 0 8px;
  font-size: 30px;
  line-height: 1.15;
  color: #f8fafc;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-copy p {
  margin: 0;
  color: #a7f3d0;
  font-size: 14px;
}

.hero-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

:deep(.command-select .el-select__wrapper),
:deep(.command-select .el-input__wrapper) {
  min-height: 38px;
  border-radius: 8px !important;
  background: rgba(2, 6, 23, 0.62) !important;
  box-shadow: inset 0 0 0 1px rgba(94, 234, 212, 0.34) !important;
}

:deep(.command-select .el-input__inner) {
  color: #e5f8ff !important;
  font-weight: 700;
}

.command-refresh {
  height: 38px;
  color: #07111f !important;
  border: 0 !important;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #5eead4, #facc15) !important;
  font-weight: 800 !important;
}

.telemetry-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.telemetry-cell {
  position: relative;
  min-height: 118px;
  padding: 16px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.72);
}

.telemetry-cell::before {
  content: "";
  position: absolute;
  inset: 0;
  border-left: 3px solid var(--tone);
  opacity: 0.9;
}

.cell-top {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #c7d2fe;
  font-size: 13px;
  font-weight: 800;
}

.cell-top i {
  color: var(--tone);
  font-size: 18px;
}

.telemetry-cell strong {
  display: block;
  margin-top: 13px;
  color: #f8fafc;
  font-size: 28px;
  line-height: 1;
  font-weight: 900;
}

.telemetry-cell em {
  margin-left: 3px;
  color: #94a3b8;
  font-size: 13px;
  font-style: normal;
}

.telemetry-cell small {
  display: block;
  margin-top: 10px;
  color: #8fb8c3;
  font-size: 12px;
}

.tone-cyan { --tone: #22d3ee; }
.tone-green { --tone: #34d399; }
.tone-amber { --tone: #f59e0b; }
.tone-violet { --tone: #a78bfa; }

.command-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(420px, 1.15fr) minmax(320px, 0.9fr);
  gap: 16px;
  align-items: stretch;
}

.command-panel {
  min-width: 0;
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: rgba(8, 16, 30, 0.78);
  box-shadow: 0 18px 44px rgba(2, 8, 23, 0.26);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 5px 0 0;
  color: #f8fafc;
  font-size: 17px;
  font-weight: 900;
  letter-spacing: 0;
}

.risk-badge,
.engine-state {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.risk-badge.stable,
.engine-state {
  color: #86efac;
  background: rgba(34, 197, 94, 0.12);
}

.risk-badge.watch,
.engine-state.loading {
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.14);
}

.risk-badge.danger {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.16);
}

.twin-panel {
  grid-row: span 2;
}

.twin-stage {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 280px;
  margin: 8px 0 16px;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(94, 234, 212, 0.1) 1px, transparent 1px),
    linear-gradient(rgba(94, 234, 212, 0.1) 1px, transparent 1px),
    rgba(2, 6, 23, 0.45);
  background-size: 28px 28px;
}

.scan-orbit {
  position: absolute;
  border: 1px solid rgba(94, 234, 212, 0.36);
  border-radius: 50%;
  animation: rotateScan 18s linear infinite;
}

.orbit-one {
  width: 210px;
  height: 210px;
}

.orbit-two {
  width: 148px;
  height: 148px;
  border-color: rgba(250, 204, 21, 0.38);
  animation-duration: 12s;
  animation-direction: reverse;
}

.scan-orbit::after {
  content: "";
  position: absolute;
  top: -4px;
  left: 50%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5eead4;
  box-shadow: 0 0 18px rgba(94, 234, 212, 0.9);
}

@keyframes rotateScan {
  to { transform: rotate(360deg); }
}

.field-core {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 128px;
  height: 128px;
  border: 1px solid rgba(94, 234, 212, 0.42);
  border-radius: 50%;
  background: rgba(8, 16, 30, 0.92);
  box-shadow: inset 0 0 28px rgba(34, 211, 238, 0.16), 0 0 30px rgba(20, 184, 166, 0.18);
}

.field-core span,
.field-core small {
  color: #8fb8c3;
  font-size: 11px;
  font-weight: 800;
}

.field-core strong {
  color: #5eead4;
  font-size: 34px;
  line-height: 1;
}

.radial-label {
  position: absolute;
  display: grid;
  place-items: center;
  width: 46px;
  height: 26px;
  color: #c7d2fe;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.82);
  font-size: 11px;
  font-weight: 900;
}

.label-top { top: 28px; }
.label-right { right: 34px; }
.label-bottom { bottom: 28px; }
.label-left { left: 34px; }

.sensor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sensor-tile {
  min-height: 86px;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.58);
}

.sensor-tile span,
.sensor-tile small {
  display: block;
  color: #93c5fd;
  font-size: 12px;
  font-weight: 700;
}

.sensor-tile strong {
  display: block;
  margin: 7px 0 5px;
  color: #f8fafc;
  font-size: 22px;
  line-height: 1;
}

.sensor-tile em {
  margin-left: 3px;
  color: #94a3b8;
  font-size: 12px;
  font-style: normal;
}

.sensor-tile.warning { border-color: rgba(245, 158, 11, 0.36); }
.sensor-tile.danger { border-color: rgba(239, 68, 68, 0.38); }

.ai-panel {
  min-height: 342px;
}

.ai-console {
  display: flex;
  gap: 12px;
  min-height: 112px;
  padding: 14px;
  border: 1px solid rgba(94, 234, 212, 0.16);
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.42);
}

.command-dashboard .ai-avatar {
  width: 44px;
  height: 44px;
  padding: 6px;
  flex-shrink: 0;
  border: 1px solid rgba(94, 234, 212, 0.28);
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.86);
}

.command-dashboard .ai-avatar img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.ai-console p {
  margin: 0;
  color: #dffcf6;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 600;
}

.command-dashboard .cursor {
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  background: #5eead4;
  animation: blink 1s infinite;
}

.command-chain {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.chain-item {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.chain-item:last-child {
  border-bottom: 0;
}

.chain-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22d3ee;
  box-shadow: 0 0 14px rgba(34, 211, 238, 0.88);
}

.chain-item strong,
.event-item strong {
  display: block;
  color: #f8fafc;
  font-size: 13px;
}

.chain-item small,
.event-item small {
  display: block;
  margin-top: 4px;
  color: #8fb8c3;
  font-size: 12px;
}

.chain-item em {
  color: #facc15;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.radar-panel,
.alert-panel {
  min-height: 342px;
}

.command-dashboard .score-text {
  color: #5eead4;
  font-size: 26px;
  font-weight: 900;
}

.command-dashboard .chart-container {
  width: 100%;
  min-height: 238px;
}

.event-stream {
  display: grid;
  gap: 10px;
}

.event-item {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 12px;
  min-height: 76px;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.52);
}

.event-time {
  color: #a7f3d0;
  font-size: 12px;
  font-weight: 900;
}

.event-item.warning { border-color: rgba(245, 158, 11, 0.32); }
.event-item.danger { border-color: rgba(239, 68, 68, 0.35); }
.event-item.notice { border-color: rgba(34, 211, 238, 0.24); }

.compare-panel {
  grid-column: span 2;
}

.compare-head {
  align-items: center;
}

.field-switcher {
  max-width: 58%;
  overflow-x: auto;
  padding-bottom: 2px;
}

:deep(.field-switcher .el-checkbox.is-bordered) {
  height: 30px;
  padding: 5px 10px;
  margin-right: 6px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
}

:deep(.field-switcher .el-checkbox__input) {
  display: none;
}

:deep(.field-switcher .el-checkbox__label) {
  padding-left: 0;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
}

:deep(.field-switcher .el-checkbox.is-checked) {
  border-color: rgba(94, 234, 212, 0.58);
  background: rgba(20, 184, 166, 0.18);
}

:deep(.field-switcher .el-checkbox.is-checked .el-checkbox__label) {
  color: #dffcf6;
}

.field-matrix {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 8px;
}

.field-chip {
  min-height: 72px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.32);
}

.field-chip span,
.field-chip small {
  display: block;
  overflow: hidden;
  color: #8fb8c3;
  font-size: 11px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-chip strong {
  display: block;
  margin: 5px 0;
  color: #f8fafc;
  font-size: 22px;
  line-height: 1;
}

.field-chip.excellent { border-color: rgba(52, 211, 153, 0.36); }
.field-chip.watch { border-color: rgba(245, 158, 11, 0.34); }

.command-dashboard .chart-container.large {
  height: 300px;
  min-height: 300px;
  padding: 0;
}

@media (max-width: 1320px) {
  .command-grid {
    grid-template-columns: 1fr 1fr;
  }

  .twin-panel {
    grid-row: auto;
  }

  .compare-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 920px) {
  .command-dashboard {
    padding: 14px;
  }

  .command-hero,
  .hero-tools,
  .compare-head {
    align-items: stretch;
    flex-direction: column;
  }

  .telemetry-strip,
  .command-grid,
  .sensor-grid,
  .field-matrix {
    grid-template-columns: 1fr;
  }

  .field-switcher {
    max-width: 100%;
  }

  .farm-select {
    width: 100%;
  }
}

/* 与主系统统一的浅绿色科技风格覆盖 */
.command-dashboard {
  color: #1f3528;
  background:
    linear-gradient(rgba(22, 163, 74, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(22, 163, 74, 0.045) 1px, transparent 1px),
    #f4faf6;
  background-size: 34px 34px, 34px 34px, cover;
}

.command-hero {
  border-color: #cfe8da;
  background:
    linear-gradient(120deg, rgba(220, 252, 231, 0.92), rgba(255, 255, 255, 0.96) 52%),
    linear-gradient(135deg, rgba(16, 185, 129, 0.14), rgba(255, 255, 255, 0.84));
  box-shadow: 0 18px 42px rgba(16, 185, 129, 0.1);
}

.command-hero::after {
  background: linear-gradient(90deg, transparent, #16a34a, #eab308, transparent);
}

.eyebrow,
.section-kicker {
  color: #0f9f6e;
}

.hero-copy h1,
.panel-head h2,
.telemetry-cell strong,
.field-core strong,
.sensor-tile strong,
.chain-item strong,
.event-item strong,
.field-chip strong {
  color: #163525;
}

.hero-copy p,
.source-pill,
.telemetry-cell small,
.sensor-tile small,
.field-core span,
.field-core small,
.chain-item small,
.event-item small,
.field-chip span,
.field-chip small {
  color: #5f7669;
}

.source-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  margin-top: 14px;
  border: 1px solid #cfe8da;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 800;
}

:deep(.command-select .el-select__wrapper),
:deep(.command-select .el-input__wrapper) {
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #bfe3d0 !important;
}

:deep(.command-select .el-input__inner) {
  color: #163525 !important;
}

.command-refresh {
  color: #ffffff !important;
  background: linear-gradient(135deg, #16a34a, #0f9f6e) !important;
}

.telemetry-cell,
.command-panel,
.sensor-tile,
.ai-console,
.event-item,
.field-chip {
  border-color: #d7eadf;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 12px 30px rgba(22, 101, 52, 0.06);
}

.twin-stage {
  background:
    linear-gradient(90deg, rgba(16, 185, 129, 0.08) 1px, transparent 1px),
    linear-gradient(rgba(16, 185, 129, 0.08) 1px, transparent 1px),
    rgba(247, 253, 249, 0.92);
  background-size: 28px 28px;
}

.field-core,
.radial-label {
  border-color: rgba(16, 185, 129, 0.26);
  background: rgba(255, 255, 255, 0.92);
}

.radial-label,
.cell-top,
.sensor-tile span {
  color: #35644c;
}

.scan-orbit {
  border-color: rgba(16, 185, 129, 0.28);
}

.orbit-two {
  border-color: rgba(234, 179, 8, 0.32);
}

.chain-item {
  border-bottom-color: #e4f1e9;
}

.chain-item em,
.event-time {
  color: #0f9f6e;
}

.risk-badge.stable,
.engine-state {
  color: #047857;
  background: #dcfce7;
}

.risk-badge.watch,
.engine-state.loading {
  color: #b45309;
  background: #fef3c7;
}

.risk-badge.danger {
  color: #b91c1c;
  background: #fee2e2;
}

.event-item.warning,
.sensor-tile.warning,
.field-chip.watch {
  border-color: rgba(245, 158, 11, 0.34);
  background: #fffbeb;
}

.event-item.danger,
.sensor-tile.danger {
  border-color: rgba(239, 68, 68, 0.32);
  background: #fff1f2;
}

.sensor-tile.empty {
  border-style: dashed;
  background: #f8fafc;
}

.field-chip.excellent {
  border-color: rgba(22, 163, 74, 0.34);
  background: #f0fdf4;
}

.field-switcher {
  scrollbar-color: #bfe3d0 transparent;
}

:deep(.field-switcher .el-checkbox.is-bordered) {
  border-color: #d7eadf;
  background: #ffffff;
}

:deep(.field-switcher .el-checkbox__label) {
  color: #5f7669;
}

:deep(.field-switcher .el-checkbox.is-checked) {
  border-color: #10b981;
  background: #ecfdf5;
}

:deep(.field-switcher .el-checkbox.is-checked .el-checkbox__label) {
  color: #047857;
}

.command-dashboard .score-text {
  color: #0f9f6e;
}
</style>
