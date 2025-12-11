<template>
  <div class="business-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <i class="el-icon-s-marketing" style="color: #7c3aed; margin-right: 8px;"></i>
          AI 经营决策大脑
        </h2>
        <div class="ai-badge">
          <span class="dot-pulse"></span> 智能体模型实时运算中
        </div>
      </div>
      <div class="header-right">
        <el-radio-group v-model="period" size="small" class="custom-radio">
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
          <el-radio-button label="year">全年</el-radio-button>
        </el-radio-group>
        <el-button type="primary" icon="el-icon-download" size="small" class="export-btn" @click="exportReport">
          导出研报
        </el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card theme-blue">
        <div class="card-icon"><i class="el-icon-money"></i></div>
        <div class="card-content">
          <div class="label">总营收预测 (Revenue)</div>
          <div class="value">
            <span class="currency">¥</span>
            <span class="num">{{ formatNumber(totalRevenue) }}</span>
          </div>
          <div class="trend up">
            同比 +12.5% <i class="el-icon-top-right"></i>
          </div>
        </div>
        <div class="card-bg-decoration"></div>
      </div>

      <div class="kpi-card theme-purple">
        <div class="card-icon"><i class="el-icon-wallet"></i></div>
        <div class="card-content">
          <div class="label">净利润 (Net Profit)</div>
          <div class="value">
            <span class="currency">¥</span>
            <span class="num">{{ formatNumber(totalProfit) }}</span>
          </div>
          <div class="trend up">
            利润率 32% <i class="el-icon-top-right"></i>
          </div>
        </div>
        <div class="card-bg-decoration"></div>
      </div>

      <div class="kpi-card theme-green">
        <div class="card-icon"><i class="el-icon-cpu"></i></div>
        <div class="card-content">
          <div class="label">AI 降本增效</div>
          <div class="value">
            <span class="currency">¥</span>
            <span class="num">{{ formatNumber(aiSavedCost) }}</span>
          </div>
          <div class="trend down">
            成本 -8.3% <i class="el-icon-bottom-right"></i>
          </div>
        </div>
        <div class="card-bg-decoration"></div>
      </div>

      <div class="kpi-card theme-orange">
        <div class="card-icon"><i class="el-icon-data-analysis"></i></div>
        <div class="card-content">
          <div class="label">投资回报率 (ROI)</div>
          <div class="value">
            <span class="num">{{ roiDisplay }}</span>
          </div>
          <div class="trend flat">评级：S级优选</div>
        </div>
        <div class="card-bg-decoration"></div>
      </div>
    </div>

    <div class="charts-wrapper">
      <div class="chart-card main-chart-card">
        <div class="card-header">
          <div class="title">
            <i class="el-icon-data-line"></i> 作物市场价格趋势 (AI预测)
          </div>
          <div class="chart-legend">
            <span class="legend-item solid">历史数据</span>
            <span class="legend-item dashed">AI 预测</span>
          </div>
        </div>
        
        <div class="chart-body">
          <div ref="priceChart" style="width: 100%; height: 320px;"></div>
        </div>

        <div class="ai-suggestion-bar">
          
          <div class="ai-text">
            <strong>智能体策略建议：</strong>
            预测未来 15 天草莓价格将达到峰值（约 ¥42/kg）。建议 A2 地块成熟批次暂缓 3 天采摘，等待行情上涨，预计额外获利 15%。
          </div>
        </div>
      </div>

      <div class="side-charts-col">
        <div class="chart-card side-card">
          <div class="card-header">
            <div class="title"><i class="el-icon-aim"></i> 经营风险评估</div>
          </div>
          <div ref="radarChart" style="width: 100%; height: 200px;"></div>
        </div>
        
        <div class="chart-card side-card">
          <div class="card-header">
            <div class="title"><i class="el-icon-pie-chart"></i> 成本结构占比</div>
          </div>
          <div ref="pieChart" style="width: 100%; height: 200px;"></div>
        </div>
      </div>
    </div>

    <div class="chart-card table-card">
      <div class="card-header">
        <div class="title"><i class="el-icon-tickets"></i> 地块盈亏明细表</div>
        <el-button type="text">查看更多 <i class="el-icon-arrow-right"></i></el-button>
      </div>
      <el-table 
        :data="farmlandData" 
        style="width: 100%" 
        :header-cell-style="{background:'#f8fafa', color:'#606266', fontWeight:'600', borderBottom:'none'}"
        :cell-style="{borderBottom:'1px solid #f0f0f0'}"
      >
        <el-table-column prop="farmlandName" label="地块名称" width="180">
          <template slot-scope="scope">
            <span style="font-weight:bold; color:#303133">{{ scope.row.farmlandName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="crop" label="作物">
          <template slot-scope="scope">
            <el-tag size="small" effect="plain">{{ scope.row.crop }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedCost" label="投入成本" align="right">
          <template slot-scope="scope">
            <span style="color:#909399">¥ {{ formatNumber(scope.row.expectedCost) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectedRevenue" label="预计营收" align="right">
          <template slot-scope="scope">¥ {{ formatNumber(scope.row.expectedRevenue) }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="净利润" align="right">
          <template slot-scope="scope">
            <span :style="{color: scope.row.profit > 0 ? '#10b981' : '#ef4444', fontWeight:'bold', fontSize:'15px'}">
              {{ scope.row.profit > 0 ? '+' : '' }} {{ formatNumber(scope.row.profit) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="分析报告" width="120" align="center">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-document">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
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
      loading: false,
      farmlandData: [],
      totalRevenue: 0,
      totalProfit: 0,
      aiSavedCost: 0,
      priceChart: null,
      radarChart: null,
      pieChart: null,
      roiDisplay: '1 : 3.2',
      pieChartData: []
    }
  },
  mounted() {
    this.loadData();
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if(this.priceChart) this.priceChart.dispose();
    if(this.radarChart) this.radarChart.dispose();
    if(this.pieChart) this.pieChart.dispose();
  },
  methods: {
    async loadData() {
      this.loading = true;
      try {
        // 并发获取销售和采购数据
        const [salesRes, purchaseRes] = await Promise.all([
          this.request.get("/sales"),
          this.request.get("/purchase")
        ]);

        const salesData = salesRes.code === '200' ? salesRes.data : [];
        const purchaseData = purchaseRes.code === '200' ? purchaseRes.data : [];

        // 1. 计算 KPI
        this.calculateKPI(salesData, purchaseData);

        // 2. 更新饼图数据 (成本结构)
        this.updatePieChartData(purchaseData);

        // 3. 生成表格数据 (按作物汇总)
        this.generateTableData(salesData, purchaseData);

        this.$nextTick(() => {
          this.initPriceChart();
          this.initRadarChart();
          this.initPieChart();
        });
      } catch (error) {
        console.error("获取经营数据失败:", error);
        // 失败时显示空数据
        this.farmlandData = [];
        this.totalRevenue = 0;
        this.totalProfit = 0;
        this.aiSavedCost = 0;
        this.roiDisplay = '-- : --';
        this.pieChartData = [];
        this.$nextTick(() => {
          this.initPriceChart();
          this.initRadarChart();
          this.initPieChart();
        });
      } finally {
        this.loading = false;
      }
    },

    calculateKPI(salesData, purchaseData) {
      // 计算总营收
      this.totalRevenue = salesData.reduce((sum, item) => {
        return sum + (parseFloat(item.price || 0) * parseFloat(item.number || 0));
      }, 0);

      // 计算总成本
      const totalCost = purchaseData.reduce((sum, item) => {
        return sum + (parseFloat(item.price || 0) * parseFloat(item.number || 0));
      }, 0);

      // 计算净利润
      this.totalProfit = this.totalRevenue - totalCost;

      // 计算 ROI (投资回报率) = 总营收 / 总成本
      const roiRatio = totalCost > 0 ? (this.totalRevenue / totalCost).toFixed(1) : '∞';
      this.roiDisplay = `1 : ${roiRatio}`;

      // 估算 AI 降本增效 (假设 AI 优化了 10% 的成本)
      this.aiSavedCost = totalCost * 0.1;
    },

    updatePieChartData(purchaseData) {
      // 按物资名称汇总成本
      const costMap = {};
      purchaseData.forEach(item => {
        const name = item.product || '未知物资';
        const cost = (parseFloat(item.price || 0) * parseFloat(item.number || 0));
        costMap[name] = (costMap[name] || 0) + cost;
      });

      // 转换为数组并排序
      this.pieChartData = Object.entries(costMap)
        .map(([name, value]) => ({ name, value }))
        .sort((a, b) => b.value - a.value)
        .slice(0, 5); // 取前5名
    },

    generateTableData(salesData, purchaseData) {
      // 按作物汇总营收
      const cropMap = {};
      
      salesData.forEach(item => {
        const crop = item.product || '未知作物';
        const revenue = (parseFloat(item.price || 0) * parseFloat(item.number || 0));
        
        if (!cropMap[crop]) {
          cropMap[crop] = { 
            farmlandName: `${crop}种植区`,
            crop: crop, 
            expectedRevenue: 0, 
            expectedCost: 0 
          };
        }
        cropMap[crop].expectedRevenue += revenue;
      });

      // 将总采购成本按营收比例分摊到各作物
      const totalRevenue = Object.values(cropMap).reduce((sum, item) => sum + item.expectedRevenue, 0);
      const totalRealCost = purchaseData.reduce((sum, item) => sum + (parseFloat(item.price || 0) * parseFloat(item.number || 0)), 0);

      Object.values(cropMap).forEach(item => {
        if (totalRevenue > 0) {
          const ratio = item.expectedRevenue / totalRevenue;
          item.expectedCost = totalRealCost * ratio;
        } else {
          item.expectedCost = 0;
        }
        item.profit = item.expectedRevenue - item.expectedCost;
      });

      this.farmlandData = Object.values(cropMap);
      // 没有数据时保持空数组
    },

    calculateTotals() {
      this.totalRevenue = this.farmlandData.reduce((sum, item) => sum + item.expectedRevenue, 0);
      this.totalProfit = this.farmlandData.reduce((sum, item) => sum + item.profit, 0);
      this.aiSavedCost = 2850; 
    },

    // 1. 折线图优化：更平滑，颜色更透
    initPriceChart() {
      this.priceChart = echarts.init(this.$refs.priceChart);
      const option = {
        tooltip: { 
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          borderColor: '#eee',
          textStyle: { color: '#333' }
        },
        grid: { top: '10%', left: '2%', right: '4%', bottom: '5%', containLabel: true },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: ['11-01', '11-05', '11-10', '11-15', '11-20', '11-25', '11-30', '12-05', '12-10'],
          axisLine: { lineStyle: { color: '#e0e0e0' } },
          axisLabel: { color: '#909399' }
        },
        yAxis: { 
          type: 'value', 
          splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } } 
        },
        series: [
          {
            name: '历史价格',
            type: 'line',
            smooth: true,
            showSymbol: false,
            data: this.totalRevenue > 0 ? [28, 30, 29, 32, 35, 34, null, null, null] : [],
            itemStyle: { color: '#3b82f6' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
                { offset: 1, color: 'rgba(59, 130, 246, 0)' }
              ])
            }
          },
          {
            name: 'AI 预测',
            type: 'line',
            smooth: true,
            lineStyle: { type: 'dashed', width: 2 },
            data: this.totalRevenue > 0 ? [null, null, null, null, null, 34, 38, 42, 40] : [],
            itemStyle: { color: '#8b5cf6' } // 紫色代表 AI
          }
        ]
      };
      this.priceChart.setOption(option);
    },

    // 2. 雷达图优化：颜色填充
    initRadarChart() {
      this.radarChart = echarts.init(this.$refs.radarChart);
      const option = {
        radar: {
          indicator: [
            { name: '市场波动', max: 100 },
            { name: '气候灾害', max: 100 },
            { name: '病虫害', max: 100 },
            { name: '库存积压', max: 100 },
            { name: '资金流', max: 100 }
          ],
          radius: '60%',
          splitArea: { show: false },
          axisLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } }
        },
        series: [{
          type: 'radar',
          data: this.totalRevenue > 0 ? [
            {
              value: [30, 60, 20, 40, 30],
              name: '当前风险',
              areaStyle: { color: 'rgba(16, 185, 129, 0.3)' },
              itemStyle: { color: '#10b981' },
              lineStyle: { width: 2 }
            }
          ] : []
        }]
      };
      this.radarChart.setOption(option);
    },

    // 3. 饼图优化：甜甜圈图
    initPieChart() {
      this.pieChart = echarts.init(this.$refs.pieChart);
      const option = {
        tooltip: { trigger: 'item' },
        legend: { 
          orient: 'vertical', 
          right: 0, 
          top: 'center',
          icon: 'circle',
          itemWidth: 8,
          itemHeight: 8
        },
        series: [
          {
            name: '成本构成',
            type: 'pie',
            radius: ['50%', '70%'], // 环形
            center: ['35%', '50%'], // 左移一点，给 legend 让位
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 5,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: { show: false },
            data: this.pieChartData.length > 0 ? this.pieChartData : []
          }
        ]
      };
      this.pieChart.setOption(option);
    },

    handleResize() {
      this.priceChart && this.priceChart.resize();
      this.radarChart && this.radarChart.resize();
      this.pieChart && this.pieChart.resize();
    },
    formatNumber(num) {
      return num.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    exportReport() {
      this.$message.success('正在生成 AI 经营研报...');
      
      const reportDate = new Date().toLocaleDateString('zh-CN');
      const periodText = this.period === 'week' ? '本周' : this.period === 'month' ? '本月' : '全年';
      
      // 生成 Word 文档 HTML 内容
      const htmlContent = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" 
              xmlns:w="urn:schemas-microsoft-com:office:word" 
              xmlns="http://www.w3.org/TR/REC-html40">
        <head>
          <meta charset="utf-8">
          <title>帮帮农 AI 经营决策研报</title>
          <style>
            body { font-family: '微软雅黑', Arial, sans-serif; padding: 40px; line-height: 1.8; }
            h1 { text-align: center; color: #1f2937; border-bottom: 3px solid #7c3aed; padding-bottom: 10px; }
            h2 { color: #7c3aed; border-left: 4px solid #7c3aed; padding-left: 10px; margin-top: 30px; }
            .meta { text-align: center; color: #666; margin-bottom: 30px; }
            .kpi-box { background: #f8fafc; padding: 20px; border-radius: 8px; margin: 20px 0; }
            .kpi-item { margin: 10px 0; font-size: 16px; }
            .kpi-value { color: #10b981; font-weight: bold; font-size: 18px; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th { background: #7c3aed; color: white; padding: 12px; text-align: left; }
            td { padding: 10px; border-bottom: 1px solid #e5e7eb; }
            tr:hover { background: #f9fafb; }
            .profit-positive { color: #10b981; font-weight: bold; }
            .profit-negative { color: #ef4444; font-weight: bold; }
            .suggestion { background: #faf5ff; border-left: 4px solid #8b5cf6; padding: 15px; margin: 20px 0; }
            .risk-item { margin: 8px 0; }
            .risk-low { color: #10b981; }
            .risk-mid { color: #f59e0b; }
            .footer { text-align: center; color: #9ca3af; margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; }
          </style>
        </head>
        <body>
          <h1>帮帮农 AI 经营决策研报</h1>
          <div class="meta">
            <p>报告日期：${reportDate}  |  分析周期：${periodText}</p>
          </div>
          
          <h2>一、营收概览</h2>
          <div class="kpi-box">
            <div class="kpi-item">总营收预测：<span class="kpi-value">￥ ${this.formatNumber(this.totalRevenue)}</span>    同比 +12.5%</div>
            <div class="kpi-item">净利润：<span class="kpi-value">￥ ${this.formatNumber(this.totalProfit)}</span>    利润率 32%</div>
            <div class="kpi-item">AI 降本增效：<span class="kpi-value">￥ ${this.formatNumber(this.aiSavedCost)}</span>    成本 -8.3%</div>
            <div class="kpi-item">投资回报率 (ROI)：<span class="kpi-value">1 : 3.2</span>    评级：S级优选</div>
          </div>
          
          <h2>二、地块盈亏明细</h2>
          <table>
            <tr>
              <th>地块名称</th>
              <th>作物</th>
              <th>投入成本</th>
              <th>预计营收</th>
              <th>净利润</th>
            </tr>
            ${this.farmlandData.map(item => `
              <tr>
                <td>${item.farmlandName}</td>
                <td>${item.crop}</td>
                <td>￥ ${this.formatNumber(item.expectedCost)}</td>
                <td>￥ ${this.formatNumber(item.expectedRevenue)}</td>
                <td class="${item.profit > 0 ? 'profit-positive' : 'profit-negative'}">
                  ${item.profit > 0 ? '+' : ''}￥ ${this.formatNumber(item.profit)}
                </td>
              </tr>
            `).join('')}
          </table>
          
          <h2>三、智能体策略建议</h2>
          <div class="suggestion">
            <p><strong>建议：</strong>预测未来 15 天草莓价格将达到峰值（约 ￥42/kg）。</p>
            <p>建议 A2 地块成熟批次暂缓 3 天采摘，等待行情上涨，预计额外获利 15%。</p>
          </div>
          
          <h2>四、风险评估</h2>
          <div class="kpi-box">
            <div class="risk-item">市场波动风险: <span class="risk-low">30% (低)</span></div>
            <div class="risk-item">气候灾害风险: <span class="risk-mid">60% (中)</span></div>
            <div class="risk-item">病虫害风险: <span class="risk-low">20% (低)</span></div>
            <div class="risk-item">库存积压风险: <span class="risk-mid">40% (中低)</span></div>
            <div class="risk-item">资金流风险: <span class="risk-low">30% (低)</span></div>
          </div>
          
          <div class="footer">
            <p>报告由 帮帮农 AI 智能体 自动生成</p>
          </div>
        </body>
        </html>
      `;

      // 创建并下载 Word 文档
      const blob = new Blob(['\ufeff', htmlContent], { type: 'application/msword' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `帮帮农_AI经营研报_${reportDate.replace(/\//g, '-')}.doc`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      setTimeout(() => {
        this.$message.success('AI 经营研报已导出！');
      }, 500);
    }
  }
}
</script>

<style scoped>
/* 全局背景：高级灰 */
.business-page {
  padding: 24px;
  background-color: #f5f7fa; /* 这个颜色很重要，不要纯白 */
  min-height: calc(100vh - 60px);
  height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.business-page::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

/* 1. 头部 */
.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
}
.page-title {
  font-size: 22px; color: #1f2937; font-weight: 800; margin: 0; display: flex; align-items: center;
}
.ai-badge {
  display: inline-flex; align-items: center; margin-left: 15px;
  background: #f3e8ff; color: #7c3aed; padding: 4px 12px; border-radius: 20px;
  font-size: 12px; font-weight: 600;
}
.dot-pulse {
  width: 6px; height: 6px; background: #7c3aed; border-radius: 50%; margin-right: 6px;
  animation: pulse 1.5s infinite;
}
@keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.2); } 100% { opacity: 1; transform: scale(1); } }

/* 2. KPI Grid - 卡片优化 */
.kpi-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px;
}
.kpi-card {
  background: white; border-radius: 16px; padding: 20px;
  display: flex; gap: 16px; align-items: flex-start;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  position: relative; overflow: hidden;
}
.kpi-card:hover { transform: none; }

/* 去掉按钮浮动效果 */
.export-btn {
  transition: none !important;
}
.export-btn:hover {
  transform: none !important;
}

.card-icon {
  width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
  font-size: 24px; flex-shrink: 0;
}
.theme-blue .card-icon { background: #eff6ff; color: #3b82f6; }
.theme-purple .card-icon { background: #f5f3ff; color: #8b5cf6; }
.theme-green .card-icon { background: #ecfdf5; color: #10b981; }
.theme-orange .card-icon { background: #fff7ed; color: #f97316; }

/* 装饰背景球 */
.card-bg-decoration {
  position: absolute; right: -20px; bottom: -20px; width: 100px; height: 100px;
  border-radius: 50%; opacity: 0.05; pointer-events: none;
}
.theme-blue .card-bg-decoration { background: #3b82f6; }
.theme-purple .card-bg-decoration { background: #8b5cf6; }
.theme-green .card-bg-decoration { background: #10b981; }
.theme-orange .card-bg-decoration { background: #f97316; }

.card-content { flex: 1; z-index: 1; }
.label { font-size: 13px; color: #6b7280; margin-bottom: 4px; }
.value { font-size: 24px; font-weight: 800; color: #111827; letter-spacing: -0.5px; }
.currency { font-size: 14px; font-weight: 600; margin-right: 2px; }
.trend { margin-top: 8px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 2px; }
.trend.up { color: #10b981; } .trend.down { color: #10b981; } .trend.flat { color: #6b7280; }

/* 3. 图表区域布局 */
.charts-wrapper { display: flex; gap: 24px; margin-bottom: 24px; align-items: stretch; }
.main-chart-card { flex: 2; background: white; border-radius: 16px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); display: flex; flex-direction: column; }
.side-charts-col { flex: 1; display: flex; flex-direction: column; gap: 24px; }
.side-card { flex: 1; background: white; border-radius: 16px; padding: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-header .title { font-size: 16px; font-weight: 700; color: #374151; display: flex; align-items: center; gap: 8px; }
.legend-item { font-size: 12px; color: #6b7280; margin-left: 15px; display: inline-flex; align-items: center; gap: 6px; }
.legend-item::before { content: ''; width: 12px; height: 2px; display: block; }
.legend-item.solid::before { background: #3b82f6; }
.legend-item.dashed::before { border-top: 2px dashed #8b5cf6; height: 0; }

/* AI 建议条 */
.ai-suggestion-bar {
  margin-top: auto; background: #f8fafc; border-radius: 12px; padding: 12px 16px;
  display: flex; align-items: flex-start; gap: 12px; border-left: 4px solid #8b5cf6;
}
.ai-icon-box { font-size: 20px; }
.ai-text { font-size: 13px; color: #4b5563; line-height: 1.6; }

/* 4. 表格优化 */
.table-card { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
</style>