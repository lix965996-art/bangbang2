<template>
  <div class="fintech-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <span class="title-icon">🧠</span>
          AI 经营决策大脑
        </h1>
        <div class="ai-status-badge">
          <span class="pulse-dot"></span>
          <span>智能体模型实时运算中</span>
        </div>
      </div>
      <div class="header-right">
        <button class="period-btn" :class="{ active: period === 'week' }" @click="period = 'week'">本周</button>
        <button class="period-btn" :class="{ active: period === 'month' }" @click="period = 'month'">本月</button>
        <button class="period-btn" :class="{ active: period === 'year' }" @click="period = 'year'">全年</button>
        <button class="export-btn" @click="exportReport">
          📥 导出研报
        </button>
      </div>
    </div>

    <!-- KPI 指标卡片（带 CountUp 和 Sparkline） -->
    <div class="kpi-cards-grid">
      <div class="kpi-card profit-card">
        <div class="kpi-icon">💰</div>
        <div class="kpi-content">
          <div class="kpi-label">总营收预测</div>
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number" ref="revenueNumber">{{ formatNumber(totalRevenue) }}</span>
          </div>
          <div class="kpi-trend up">
            <span>↗</span> 同比 +12.5%
          </div>
        </div>
        <svg class="sparkline" viewBox="0 0 100 30">
          <polyline :points="sparklineRevenue" fill="none" stroke="rgba(16, 185, 129, 0.3)" stroke-width="1.5"/>
        </svg>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon">💎</div>
        <div class="kpi-content">
          <div class="kpi-label">净利润</div>
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number">{{ formatNumber(totalProfit) }}</span>
          </div>
          <div class="kpi-trend up">
            <span>↗</span> 利润率 32%
          </div>
        </div>
        <svg class="sparkline" viewBox="0 0 100 30">
          <polyline :points="sparklineProfit" fill="none" stroke="rgba(99, 102, 241, 0.3)" stroke-width="1.5"/>
        </svg>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon">🤖</div>
        <div class="kpi-content">
          <div class="kpi-label">AI 降本增效</div>
          <div class="kpi-value">
            <span class="currency">¥</span>
            <span class="number">{{ formatNumber(aiSavedCost) }}</span>
          </div>
          <div class="kpi-trend down">
            <span>↘</span> 成本 -8.3%
          </div>
        </div>
        <svg class="sparkline" viewBox="0 0 100 30">
          <polyline :points="sparklineSaving" fill="none" stroke="rgba(244, 63, 94, 0.3)" stroke-width="1.5"/>
        </svg>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon">📊</div>
        <div class="kpi-content">
          <div class="kpi-label">投资回报率 (ROI)</div>
          <div class="kpi-value roi-value">{{ roiDisplay }}</div>
          <div class="kpi-trend flat">
            评级：S级优选
          </div>
        </div>
      </div>
    </div>

    <!-- ===== AI 推演沙盘 (核心功能) ===== -->
    <div class="simulator-panel">
      <div class="panel-header">
        <h2 class="panel-title">
          <span class="icon">🎯</span>
          AI 推演沙盘 (Scenario Simulator)
        </h2>
        <div class="panel-hint">拖动因子滑块，实时预测利润变化</div>
      </div>
      
      <div class="simulator-body">
        <!-- 左侧：关键因子控制台 -->
        <div class="factor-console">
          <div class="console-title">关键因子控制台</div>
          
          <div class="factor-item">
            <div class="factor-header">
              <span class="factor-name">成本波动率</span>
              <span class="factor-value">{{ costFluctuation }}%</span>
            </div>
            <input 
              type="range" 
              class="factor-slider" 
              v-model.number="costFluctuation" 
              min="-30" 
              max="30" 
              step="1"
            />
            <div class="factor-scale">
              <span>-30%</span>
              <span>0</span>
              <span>+30%</span>
            </div>
          </div>

          <div class="factor-item">
            <div class="factor-header">
              <span class="factor-name">产量预期</span>
              <span class="factor-value">{{ yieldExpectation }}%</span>
            </div>
            <input 
              type="range" 
              class="factor-slider yield" 
              v-model.number="yieldExpectation" 
              min="-20" 
              max="40" 
              step="1"
            />
            <div class="factor-scale">
              <span>-20%</span>
              <span>0</span>
              <span>+40%</span>
            </div>
          </div>

          <div class="ai-prediction-box">
            <div class="prediction-label">AI 预测结果</div>
            <div class="prediction-value">利润变化: <strong :class="{ positive: predictedProfit >= 0, negative: predictedProfit < 0 }">{{ predictedProfit >= 0 ? '+' : '' }}{{ predictedProfit.toFixed(1) }}%</strong></div>
          </div>
        </div>

        <!-- 右侧：实时推演图表 -->
        <div class="simulator-chart">
          <div ref="simulatorChart" class="chart-container"></div>
        </div>
      </div>
    </div>

    <!-- ===== 资金流向桑基图 ===== -->
    <div class="charts-row">
      <div class="chart-panel sankey-panel">
        <div class="panel-header">
          <h3 class="panel-title">
            <span class="icon">💸</span>
            资金流向桑基图 (Cost Flow)
          </h3>
          <div class="panel-hint">可视化资金流动路径</div>
        </div>
        <div ref="sankeyChart" class="chart-container"></div>
      </div>

      <div class="chart-panel">
        <div class="panel-header">
          <h3 class="panel-title">
            <span class="icon">⚠️</span>
            经营风险评估
          </h3>
        </div>
        <div ref="radarChart" class="chart-container"></div>
      </div>
    </div>

    <!-- ===== 智能决策卡片（消息流）===== -->
    <div class="insights-panel">
      <div class="panel-header">
        <h2 class="panel-title">
          <span class="icon">💡</span>
          智能决策建议 (Actionable Insights)
        </h2>
      </div>

      <div class="insights-stream">
        <div class="insight-card warning">
          <div class="insight-header">
            <span class="badge">AI 预警</span>
            <span class="time">2分钟前</span>
          </div>
          <div class="insight-body">
            <p class="insight-text">
              检测到<strong>玉米期货价格倒挂</strong>（现货 ¥2.8/kg，期货 ¥2.6/kg），预计 7 天内价差收窄，存在<span class="highlight-risk">价格下跌风险 15%</span>。
            </p>
            <div class="insight-actions">
              <button class="action-btn primary" @click="handleAction('adjust_plan')">
                📉 调整种植计划
              </button>
              <button class="action-btn secondary" @click="handleAction('lock_contract')">
                🔒 锁定远期合同
              </button>
            </div>
          </div>
        </div>

        <div class="insight-card success">
          <div class="insight-header">
            <span class="badge">AI 机会</span>
            <span class="time">15分钟前</span>
          </div>
          <div class="insight-body">
            <p class="insight-text">
              预测<strong>草莓价格峰值</strong>将在 15 天后到达（约 ¥42/kg），建议 A2 地块成熟批次<span class="highlight-profit">暂缓采摘 3 天</span>，预计额外获利 15%。
            </p>
            <div class="insight-actions">
              <button class="action-btn primary" @click="handleAction('delay_harvest')">
                ⏱️ 延迟采摘计划
              </button>
              <button class="action-btn secondary" @click="handleAction('view_detail')">
                📊 查看详细分析
              </button>
            </div>
          </div>
        </div>

        <div class="insight-card info">
          <div class="insight-header">
            <span class="badge">AI 建议</span>
            <span class="time">1小时前</span>
          </div>
          <div class="insight-body">
            <p class="insight-text">
              根据气象局数据，未来 10 天<strong>降水量偏多 30%</strong>，建议提前采购排水设备，并调整灌溉计划，预防<span class="highlight-warning">涝害风险</span>。
            </p>
            <div class="insight-actions">
              <button class="action-btn primary" @click="handleAction('adjust_irrigation')">
                💧 调整灌溉计划
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'BusinessAnalysis',
  data() {
    return {
      period: 'month',
      totalRevenue: 46025,
      totalProfit: 14728,
      aiSavedCost: 3850,
      roiDisplay: '1 : 3.2',
      
      // AI 推演沙盘参数
      costFluctuation: 0,
      yieldExpectation: 0,
      predictedProfit: 0,
      
      // Sparkline 数据
      sparklineRevenue: '0,20 15,12 30,18 45,8 60,15 75,5 90,10 100,0',
      sparklineProfit: '0,15 15,8 30,20 45,5 60,12 75,18 90,3 100,8',
      sparklineSaving: '0,25 15,20 30,15 45,18 60,10 75,12 90,8 100,5',
      
      // ECharts 实例
      simulatorChart: null,
      sankeyChart: null,
      radarChart: null
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initSimulatorChart();
      this.initSankeyChart();
      this.initRadarChart();
      this.startCountUp();
    });
    window.addEventListener('resize', this.handleResize);
  },
  watch: {
    costFluctuation() {
      this.updatePrediction();
      this.updateSimulatorChart();
    },
    yieldExpectation() {
      this.updatePrediction();
      this.updateSimulatorChart();
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if(this.simulatorChart && !this.simulatorChart.isDisposed()) this.simulatorChart.dispose();
    if(this.sankeyChart && !this.sankeyChart.isDisposed()) this.sankeyChart.dispose();
    if(this.radarChart && !this.radarChart.isDisposed()) this.radarChart.dispose();
  },
  methods: {
    // Count-up 动画
    startCountUp() {
      const animateNumber = (start, end, duration, callback) => {
        const startTime = performance.now();
        const step = (currentTime) => {
          const elapsed = currentTime - startTime;
          const progress = Math.min(elapsed / duration, 1);
          const current = start + (end - start) * this.easeOutCubic(progress);
          callback(current);
          if (progress < 1) {
            requestAnimationFrame(step);
          }
        };
        requestAnimationFrame(step);
      };

      // 这里可以添加数字动画逻辑，但由于 Vue 2 的限制，我们保持简单
    },
    
    easeOutCubic(t) {
      return 1 - Math.pow(1 - t, 3);
    },

    // 更新预测结果
    updatePrediction() {
      // 简单的线性模型：利润变化 = 产量影响 - 成本影响
      const yieldImpact = this.yieldExpectation * 0.8;
      const costImpact = this.costFluctuation * 0.6;
      this.predictedProfit = yieldImpact - costImpact;
    },

    // 初始化推演图表
    initSimulatorChart() {
      this.simulatorChart = echarts.init(this.$refs.simulatorChart);
      this.updateSimulatorChart();
    },

    // 更新推演图表（响应滑块变化）
    updateSimulatorChart() {
      const baseProfit = [32, 35, 33, 36, 38, 40, 42, 45];
      const factor = 1 + this.predictedProfit / 100;
      const predictedData = baseProfit.map(v => v * factor);

      const option = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#e5e7eb',
          borderWidth: 1,
          textStyle: { color: '#111827', fontSize: 12 },
          padding: [8, 12]
        },
        grid: { top: '10%', left: '3%', right: '3%', bottom: '8%', containLabel: true },
        xAxis: {
          type: 'category',
          data: ['Q1', 'Q2', 'Q3', 'Q4', 'Q5', 'Q6', 'Q7', 'Q8'],
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          axisLabel: { color: '#6b7280', fontSize: 11 },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          name: '利润 (万元)',
          nameTextStyle: { color: '#6b7280', fontSize: 11, padding: [0, 0, 0, -10] },
          splitLine: { lineStyle: { type: 'dashed', color: '#f3f4f6' } },
          axisLabel: { color: '#6b7280', fontSize: 11 },
          axisLine: { show: false },
          axisTick: { show: false }
        },
        series: [
          {
            name: '历史利润',
            type: 'line',
            data: baseProfit,
            smooth: false,
            itemStyle: { color: '#94a3b8' },
            lineStyle: { width: 2, color: '#94a3b8' },
            symbol: 'circle',
            symbolSize: 4,
            showSymbol: false
          },
          {
            name: 'AI 预测利润',
            type: 'line',
            data: predictedData,
            smooth: false,
            lineStyle: { 
              type: [5, 5], 
              width: 2.5,
              color: this.predictedProfit >= 0 ? '#059669' : '#dc2626'
            },
            itemStyle: { 
              color: this.predictedProfit >= 0 ? '#059669' : '#dc2626'
            },
            symbol: 'circle',
            symbolSize: 5
          }
        ]
      };
      this.simulatorChart.setOption(option, true);
    },

    // 初始化桑基图
    initSankeyChart() {
      this.sankeyChart = echarts.init(this.$refs.sankeyChart);
      const option = {
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#e5e7eb',
          borderWidth: 1,
          textStyle: { color: '#111827', fontSize: 12 },
          padding: [8, 12],
          formatter: function(params) {
            if (params.dataType === 'edge') {
              return `${params.data.source} → ${params.data.target}<br/>金额: ¥${params.data.value.toLocaleString()}`;
            }
            return `${params.name}`;
          }
        },
        series: [
          {
            type: 'sankey',
            layout: 'none',
            emphasis: { focus: 'adjacency' },
            nodeWidth: 20,
            nodeGap: 12,
            data: [
              { name: '总营收', itemStyle: { color: '#3b82f6' } },
              { name: '种子成本', itemStyle: { color: '#f59e0b' } },
              { name: '农药成本', itemStyle: { color: '#dc2626' } },
              { name: '人工成本', itemStyle: { color: '#ec4899' } },
              { name: '设备折旧', itemStyle: { color: '#8b5cf6' } },
              { name: '其他支出', itemStyle: { color: '#6b7280' } },
              { name: '净利润', itemStyle: { color: '#059669' } }
            ],
            links: [
              { source: '总营收', target: '种子成本', value: 8200 },
              { source: '总营收', target: '农药成本', value: 6500 },
              { source: '总营收', target: '人工成本', value: 12000 },
              { source: '总营收', target: '设备折旧', value: 4500 },
              { source: '总营收', target: '其他支出', value: 2100 },
              { source: '总营收', target: '净利润', value: 14728 }
            ],
            lineStyle: { 
              color: 'gradient', 
              curveness: 0.5, 
              opacity: 0.4
            },
            label: { 
              fontSize: 11, 
              color: '#374151',
              fontWeight: 500
            }
          }
        ]
      };
      this.sankeyChart.setOption(option);
    },

    // 初始化雷达图
    initRadarChart() {
      this.radarChart = echarts.init(this.$refs.radarChart);
      const option = {
        tooltip: {
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#e5e7eb',
          borderWidth: 1,
          textStyle: { color: '#111827', fontSize: 12 },
          padding: [8, 12]
        },
        radar: {
          indicator: [
            { name: '市场波动', max: 100 },
            { name: '气候灾害', max: 100 },
            { name: '病虫害', max: 100 },
            { name: '库存积压', max: 100 },
            { name: '资金流', max: 100 }
          ],
          radius: '65%',
          splitArea: { show: false },
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          splitLine: { lineStyle: { color: '#f3f4f6' } },
          name: { textStyle: { color: '#6b7280', fontSize: 11 } }
        },
        series: [{
          type: 'radar',
          data: [
            {
              value: [30, 60, 20, 40, 30],
              name: '当前风险',
              areaStyle: { color: 'rgba(5, 150, 105, 0.15)' },
              itemStyle: { color: '#059669' },
              lineStyle: { width: 2, color: '#059669' }
            }
          ]
        }]
      };
      this.radarChart.setOption(option);
    },

    handleResize() {
      this.simulatorChart && this.simulatorChart.resize();
      this.sankeyChart && this.sankeyChart.resize();
      this.radarChart && this.radarChart.resize();
    },

    // 处理操作按钮点击
    handleAction(action) {
      const messages = {
        'adjust_plan': '📋 正在调整种植计划...',
        'lock_contract': '🔒 正在锁定远期合同...',
        'delay_harvest': '⏱️ 已延迟采摘计划 3 天',
        'view_detail': '📊 正在加载详细分析...',
        'adjust_irrigation': '💧 灌溉计划已调整'
      };
      this.$message.success(messages[action] || '指令已下发');
    },
    formatNumber(num) {
      return num.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    exportReport() {
      this.$message.success('正在生成 AI 经营研报...');
      
      setTimeout(() => {
        this.$message.success('AI 经营研报已导出！');
      }, 500);
    }
  }
}
</script>

<style scoped>
/* ========== Modern Eco-Tech 风格 ========== */
/* 配色：白底 + 翡翠绿(#10b981) + 珊瑚红(#f43f5e) + 靛青蓝(#6366f1) */

.fintech-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 20px;
}

.ai-status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #059669;
  border: 1px solid #d1fae5;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.period-btn {
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #64748b;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.period-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.period-btn.active {
  background: #10b981;
  color: #ffffff;
  border-color: #10b981;
}

.export-btn {
  background: #3b82f6;
  color: #ffffff;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.export-btn:hover {
  background: #4f46e5;
}

/* ===== KPI 卡片（带 Sparkline）===== */
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.kpi-card {
  background: #ffffff;
  border-radius: 4px;
  padding: 12px 16px;
  box-shadow: none;
  border: 1px solid #e5e7eb;
  position: relative;
  overflow: hidden;
  transition: all 0.2s;
}

.kpi-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.kpi-icon {
  display: none;
}

.kpi-content {
  position: relative;
  z-index: 2;
}

.kpi-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
  margin-bottom: 4px;
  font-family: 'Roboto Mono', 'Courier New', monospace;
}

.kpi-value .currency {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  margin-right: 2px;
}

.kpi-value .number {
  font-family: 'Courier New', monospace;
}

.roi-value {
  color: #3b82f6;
  font-size: 24px;
  font-family: 'Roboto Mono', 'Courier New', monospace;
}

.kpi-trend {
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kpi-trend.up {
  color: #10b981;
}

.kpi-trend.down {
  color: #f43f5e;
}

.kpi-trend.flat {
  color: #64748b;
}

.sparkline {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 100%;
  height: 30px;
  opacity: 0.4;
  pointer-events: none;
}

/* ===== AI 推演沙盘 ===== */
.simulator-panel {
  background: #ffffff;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: none;
  border: 1px solid #e5e7eb;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-title .icon {
  font-size: 16px;
}

.panel-hint {
  font-size: 11px;
  color: #9ca3af;
}

.simulator-body {
  display: flex;
  gap: 24px;
}

.factor-console {
  width: 280px;
  background: #fafbfc;
  border-radius: 4px;
  padding: 14px;
  border: 1px solid #e5e7eb;
}

.console-title {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
  text-align: left;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.factor-item {
  margin-bottom: 16px;
}

.factor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.factor-name {
  font-size: 12px;
  font-weight: 500;
  color: #4b5563;
}

.factor-value {
  font-size: 14px;
  font-weight: 700;
  color: #059669;
  font-family: 'Roboto Mono', 'Courier New', monospace;
}

.factor-slider {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: #e5e7eb;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
}

.factor-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #dc2626;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  border: 2px solid #ffffff;
}

.factor-slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #dc2626;
  cursor: pointer;
  border: 2px solid #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.factor-slider.yield::-webkit-slider-thumb {
  background: #059669;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.factor-slider.yield::-moz-range-thumb {
  background: #059669;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.factor-scale {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.ai-prediction-box {
  background: #ffffff;
  border-radius: 4px;
  padding: 12px;
  margin-top: 16px;
  border: 1px solid #dbeafe;
}

.prediction-label {
  font-size: 11px;
  color: #1e40af;
  font-weight: 500;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.prediction-value {
  font-size: 13px;
  color: #374151;
}

.prediction-value strong {
  font-size: 16px;
  font-weight: 700;
  font-family: 'Roboto Mono', 'Courier New', monospace;
}

.prediction-value strong.positive {
  color: #10b981;
}

.prediction-value strong.negative {
  color: #f43f5e;
}

.simulator-chart {
  flex: 1;
}

.chart-container {
  width: 100%;
  height: 320px;
}

/* ===== 图表行 ===== */
.charts-row {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.chart-panel {
  background: #ffffff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: none;
  border: 1px solid #e5e7eb;
}

.sankey-panel .chart-container {
  height: 380px;
}

/* ===== 智能决策卡片 ===== */
.insights-panel {
  background: #ffffff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: none;
  border: 1px solid #e5e7eb;
}

.insights-stream {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.insight-card {
  background: #ffffff;
  border-radius: 0;
  padding: 12px 14px;
  border-left: 3px solid;
  border: 1px solid #e5e7eb;
  border-left-width: 3px;
  transition: all 0.15s;
}

.insight-card:hover {
  border-color: #d1d5db;
  border-left-color: inherit;
}

.insight-card.warning {
  border-left-color: #dc2626;
  background: #ffffff;
}

.insight-card.success {
  border-left-color: #059669;
  background: #ffffff;
}

.insight-card.info {
  border-left-color: #2563eb;
  background: #ffffff;
}

.insight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.insight-header .badge {
  background: #1e293b;
  color: #ffffff;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 2px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.insight-card.warning .badge {
  background: #dc2626;
}

.insight-card.success .badge {
  background: #059669;
}

.insight-card.info .badge {
  background: #2563eb;
}

.insight-header .time {
  font-size: 11px;
  color: #9ca3af;
}

.insight-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  margin: 0 0 10px 0;
}

.insight-text strong {
  color: #111827;
  font-weight: 600;
}

.highlight-risk {
  color: #dc2626;
  font-weight: 600;
}

.highlight-profit {
  color: #059669;
  font-weight: 600;
}

.highlight-warning {
  color: #f59e0b;
  font-weight: 700;
}

.insight-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  border: none;
}

.action-btn.primary {
  background: #059669;
  color: #ffffff;
}

.action-btn.primary:hover {
  background: #047857;
}

.action-btn.secondary {
  background: #f9fafb;
  color: #4b5563;
  border: 1px solid #d1d5db;
}

.action-btn.secondary:hover {
  background: #f3f4f6;
}

@media screen and (max-width: 1280px) {
  .fintech-page {
    min-height: calc(100vh - 60px);
    padding: 14px;
  }

  .page-header {
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 12px;
  }

  .header-right {
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .kpi-cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .simulator-body {
    flex-direction: column;
    gap: 16px;
  }

  .factor-console {
    width: 100%;
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 768px) {
  .fintech-page {
    padding: 12px;
  }

  .header-left,
  .header-right,
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
    gap: 10px;
  }

  .period-btn,
  .export-btn {
    width: 100%;
    text-align: center;
  }

  .kpi-cards-grid {
    grid-template-columns: 1fr;
  }

  .kpi-card,
  .simulator-panel,
  .chart-panel,
  .insights-panel {
    padding: 14px;
  }

  .chart-container,
  .sankey-panel .chart-container {
    height: 280px;
  }

  .insight-header,
  .insight-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .insight-actions {
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }
}

@media screen and (max-width: 480px) {
  .page-title {
    font-size: 18px;
  }

  .kpi-value,
  .roi-value {
    font-size: 20px;
  }

  .prediction-value strong {
    font-size: 15px;
  }
}
</style>
