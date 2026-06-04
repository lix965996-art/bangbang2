<template>
  <div class="unmanned-page">

    <!-- 顶部指挥横幅 -->
    <section class="command-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-left">
          <div class="hero-kicker">
            <span class="signal-dot" :class="{ paused: !patrolEnabled }"></span>
            {{ patrolEnabled ? '巡检运行中' : '巡检已暂停' }}
          </div>
          <h1 class="hero-title">农场总览</h1>
          <p class="hero-desc">查看巡检记录、待办任务、传感器事件和作物生长情况。</p>
        </div>
        <div class="hero-right">
          <el-button class="hero-btn" :loading="loading" icon="el-icon-refresh" @click="loadAll">刷新数据</el-button>
        </div>
      </div>
    </section>

    <!-- KPI 统计卡片 -->
    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-icon green"><i class="el-icon-connection"></i></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.totalChains }}</div>
          <div class="kpi-label">决策记录 <span>最近24h</span></div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon orange"><i class="el-icon-s-claim"></i></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.pendingTasks }}</div>
          <div class="kpi-label">待审批任务 <span>需人工确认</span></div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon red"><i class="el-icon-warning-outline"></i></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.unhandledEvents }}</div>
          <div class="kpi-label">未处理事件 <span>传感器异常</span></div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon blue"><i class="el-icon-finished"></i></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats.autoTasks }}</div>
          <div class="kpi-label">自动执行 <span>低风险自动处理</span></div>
        </div>
      </div>
    </div>

    <!-- 作物生长态势 -->
    <div class="panel growth-panel">
      <div class="panel-head">
        <div class="panel-title"><i class="el-icon-s-data"></i> 作物生长态势</div>
        <el-button link size="small" @click="$router.push('/farmmap3d')">进入3D农场 →</el-button>
      </div>
      <div class="growth-body">
        <div class="growth-kpis">
          <div class="g-kpi">
            <span class="g-kpi-val">{{ growthOverview.totalTrackers || 0 }}</span>
            <span class="g-kpi-lbl">追踪地块</span>
          </div>
          <div class="g-kpi">
            <span class="g-kpi-val">{{ growthOverview.cropTypes || 0 }}</span>
            <span class="g-kpi-lbl">作物类型</span>
          </div>
          <div class="g-kpi harvest">
            <span class="g-kpi-val">{{ growthOverview.nearHarvest || 0 }}</span>
            <span class="g-kpi-lbl">临近采收</span>
          </div>
        </div>
        <div class="growth-list" v-if="growthTrackers.length">
          <div v-for="tracker in growthTrackers" :key="tracker.id" class="growth-item">
            <div class="gi-info">
              <div class="gi-name">{{ tracker.farmName }}</div>
              <div class="gi-meta">{{ tracker.cropName }} · {{ stageLabel(tracker.currentStage) }}</div>
            </div>
            <div class="gi-bar">
              <el-progress :percentage="formatProgress(tracker.progress)" :show-text="false" :stroke-width="6" color="#52c41a"></el-progress>
              <span class="gi-pct">{{ formatProgress(tracker.progress) }}%</span>
            </div>
            <div class="gi-date">{{ tracker.expectedHarvest || '--' }}</div>
          </div>
        </div>
        <div v-else class="empty-hint">
          <i class="el-icon-crop"></i>
          <p>暂无作物生长追踪数据</p>
        </div>
      </div>
    </div>

    <!-- 主内容：左右两栏 -->
    <div class="main-grid">

      <!-- 左栏 -->
      <div class="col">
        <!-- 待审批队列 -->
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title"><i class="el-icon-s-claim"></i> 待审批队列</div>
            <el-tag size="small" type="danger" v-if="approvalTasks.length">{{ approvalTasks.length }} 项</el-tag>
          </div>
          <div class="panel-body">
            <div v-if="approvalTasks.length === 0" class="empty-hint compact">
              <i class="el-icon-circle-check"></i>
              <p>暂无待审批任务</p>
            </div>
            <div v-for="task in approvalTasks" :key="task.id" class="task-card">
              <div class="task-top">
                <el-tag :type="riskTagType(task.riskLevel)" size="small">{{ task.riskLevel }}</el-tag>
                <span class="task-type">{{ taskTypeLabel(task.taskType) }}</span>
                <span class="task-farm">{{ task.farmName || '-' }}</span>
              </div>
              <div class="task-reason">{{ task.reasoning || '无推理说明' }}</div>
              <div class="task-btns">
                <el-button type="success" size="small" @click="approveTask(task.taskId)">批准执行</el-button>
                <el-button type="danger" size="small" @click="rejectTask(task.taskId)">拒绝</el-button>
                <el-button v-if="task.chainId" link size="small" @click="viewChain(task.chainId)">查看推理链</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 传感器事件 -->
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title"><i class="el-icon-warning"></i> 传感器事件</div>
            <el-tag size="small" type="warning" v-if="sensorEvents.length">{{ sensorEvents.length }} 条</el-tag>
          </div>
          <div class="panel-body">
            <div v-if="sensorEvents.length === 0" class="empty-hint compact">
              <i class="el-icon-circle-check"></i>
              <p>暂无未处理事件</p>
            </div>
            <div v-for="event in sensorEvents" :key="event.id" class="event-card">
              <div class="ev-top">
                <el-tag :type="severityTagType(event.severity)" size="small">{{ event.severity }}</el-tag>
                <span class="ev-metric">{{ metricLabel(event.metricName) }}</span>
                <span class="ev-farm">{{ event.farmName }}</span>
              </div>
              <div class="ev-detail">
                当前 <strong>{{ event.currentValue }}</strong> / 阈值 {{ event.thresholdValue }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏 -->
      <div class="col">
        <!-- 决策链时间线 -->
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title"><i class="el-icon-time"></i> 最近决策链</div>
            <el-button link size="small" @click="$router.push('/auto-patrol')">查看全部</el-button>
          </div>
          <div class="panel-body">
            <div v-if="decisionChains.length === 0" class="empty-hint compact">
              <i class="el-icon-document"></i>
              <p>暂无决策记录</p>
            </div>
            <div class="chain-list" v-else>
              <div v-for="chain in decisionChains" :key="chain.id" class="chain-item" @click="viewChain(chain.chainId)">
                <div class="ci-dot" :style="{ background: chainColor(chain.triggerSource) }"></div>
                <div class="ci-body">
                  <div class="ci-head">
                    <el-tag size="small" :type="chainSourceTag(chain.triggerSource)">{{ chainSourceLabel(chain.triggerSource) }}</el-tag>
                    <span class="ci-time">{{ formatTime(chain.createdAt) }}</span>
                  </div>
                  <div class="ci-text">{{ truncate(chain.stepContent, 80) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 已完成任务 -->
        <div class="panel">
          <div class="panel-head">
            <div class="panel-title"><i class="el-icon-finished"></i> 最近完成任务</div>
          </div>
          <div class="panel-body">
            <div v-if="completedTasks.length === 0" class="empty-hint compact">
              <i class="el-icon-document"></i>
              <p>暂无已完成任务</p>
            </div>
            <div v-for="task in completedTasks" :key="task.id" class="done-card">
              <div class="done-top">
                <el-tag type="success" size="small">已完成</el-tag>
                <span class="done-type">{{ taskTypeLabel(task.taskType) }}</span>
                <span class="done-farm">{{ task.farmName || '-' }}</span>
              </div>
              <div class="done-result">{{ task.executionResult || '执行成功' }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 决策链详情抽屉 -->
    <el-drawer title="决策链详情" v-model="drawerVisible" size="55%" destroy-on-close>
      <DecisionChainViewer v-if="currentChainId" :chain-id="currentChainId" />
    </el-drawer>

  </div>
</template>

<script>
import DecisionChainViewer from '@/components/DecisionChainViewer.vue'

export default {
  name: 'UnmannedFarmDashboard',
  components: { DecisionChainViewer },
  data() {
    return {
      loading: false,
      patrolEnabled: true,
      stats: {
        totalChains: 0,
        pendingTasks: 0,
        unhandledEvents: 0,
        autoTasks: 0
      },
      approvalTasks: [],
      sensorEvents: [],
      decisionChains: [],
      completedTasks: [],
      growthOverview: {},
      growthTrackers: [],
      drawerVisible: false,
      currentChainId: ''
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    async loadAll() {
      this.loading = true
      await Promise.all([
        this.loadStats(),
        this.loadApprovalTasks(),
        this.loadSensorEvents(),
        this.loadDecisionChains(),
        this.loadCompletedTasks(),
        this.loadGrowthStatus()
      ])
      this.loading = false
    },

    async loadStats() {
      try {
        const [chainsRes, tasksRes, eventsRes, autoRes] = await Promise.all([
          this.request.get('/api/agent/decision-chains', { params: { limit: 100 } }),
          this.request.get('/api/agent/tasks/pending-approval', { params: { limit: 100 } }),
          this.request.get('/api/agent/sensor-events', { params: { handled: false, limit: 100 } }),
          this.request.get('/api/agent/tasks', { params: { status: 'pending', limit: 100 } })
        ])
        if (chainsRes.code === '200') this.stats.totalChains = (chainsRes.data || []).length
        if (tasksRes.code === '200') this.stats.pendingTasks = (tasksRes.data || []).length
        if (eventsRes.code === '200') this.stats.unhandledEvents = (eventsRes.data || []).length
        if (autoRes.code === '200') {
          const all = autoRes.data || []
          this.stats.autoTasks = all.filter(t => t.autoExecute).length
        }
      } catch (e) { /* 忽略 */ }
    },

    async loadApprovalTasks() {
      try {
        const res = await this.request.get('/api/agent/tasks/pending-approval', { params: { limit: 20 } })
        if (res.code === '200') this.approvalTasks = res.data || []
      } catch (e) { /* 忽略 */ }
    },

    async loadSensorEvents() {
      try {
        const res = await this.request.get('/api/agent/sensor-events', { params: { handled: false, limit: 20 } })
        if (res.code === '200') this.sensorEvents = res.data || []
      } catch (e) { /* 忽略 */ }
    },

    async loadDecisionChains() {
      try {
        const res = await this.request.get('/api/agent/decision-chains', { params: { limit: 10 } })
        if (res.code === '200') this.decisionChains = res.data || []
      } catch (e) { /* 忽略 */ }
    },

    async loadCompletedTasks() {
      try {
        const res = await this.request.get('/api/agent/tasks', { params: { status: 'completed', limit: 10 } })
        if (res.code === '200') this.completedTasks = res.data || []
      } catch (e) { /* 忽略 */ }
    },

    async loadGrowthStatus() {
      try {
        const [overviewRes, trackersRes] = await Promise.all([
          this.request.get('/api/crop-growth/overview'),
          this.request.get('/api/crop-growth/trackers')
        ])
        if (overviewRes.code === '200') this.growthOverview = overviewRes.data || {}
        if (trackersRes.code === '200') {
          this.growthTrackers = (trackersRes.data || [])
            .slice()
            .sort((a, b) => Number(b.progress || 0) - Number(a.progress || 0))
            .slice(0, 6)
        }
      } catch (e) {
        this.growthOverview = {}
        this.growthTrackers = []
      }
    },

    async approveTask(taskId) {
      try {
        const res = await this.request.post(`/api/agent/task/${taskId}/approve`)
        if (res.code === '200') {
          this.$message.success('已批准')
          this.loadAll()
        } else {
          this.$message.error(res.msg || '审批失败')
        }
      } catch (e) {
        this.$message.error('操作失败: ' + e.message)
      }
    },

    async rejectTask(taskId) {
      try {
        const res = await this.request.post(`/api/agent/task/${taskId}/reject`)
        if (res.code === '200') {
          this.$message.success('已拒绝')
          this.loadAll()
        } else {
          this.$message.error(res.msg || '拒绝失败')
        }
      } catch (e) {
        this.$message.error('操作失败: ' + e.message)
      }
    },

    viewChain(chainId) {
      this.currentChainId = chainId
      this.drawerVisible = true
    },

    riskTagType(level) {
      const map = { low: 'success', medium: 'warning', high: 'danger', critical: 'danger' }
      return map[level] || 'info'
    },
    severityTagType(severity) {
      const map = { low: 'info', medium: 'warning', high: 'danger', critical: 'danger' }
      return map[severity] || 'info'
    },
    taskTypeLabel(type) {
      const map = { irrigation: '灌溉', led: '补光', notification: '通知', purchase: '采购', inspection: '巡检' }
      return map[type] || type
    },
    metricLabel(metric) {
      const map = { soil_humidity: '土壤湿度', temperature: '温度', light: '光照', air_humidity: '空气湿度' }
      return map[metric] || metric
    },
    stageLabel(stage) {
      const map = {
        seeding: '播种期', seedling: '苗期', vegetative: '营养生长期',
        flowering: '花期', fruiting: '果期', harvest: '采收期', dormant: '休眠期'
      }
      return map[stage] || stage || '--'
    },
    formatProgress(value) {
      const p = Number(value)
      if (!Number.isFinite(p)) return 0
      return Math.max(0, Math.min(100, Math.round(p)))
    },
    chainSourceLabel(source) {
      const map = { user_chat: '用户对话', auto_patrol: '自主巡检', sensor_event: '传感器事件', scheduled: '定时任务' }
      return map[source] || source
    },
    chainSourceTag(source) {
      const map = { user_chat: 'primary', auto_patrol: 'success', sensor_event: 'warning', scheduled: 'info' }
      return map[source] || ''
    },
    chainColor(source) {
      const map = { user_chat: '#409EFF', auto_patrol: '#52c41a', sensor_event: '#faad14', scheduled: '#8c8c8c' }
      return map[source] || '#409EFF'
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
    },
    truncate(text, len) {
      if (!text) return ''
      return text.length > len ? text.substring(0, len) + '...' : text
    }
  }
}
</script>

<style scoped>
/* ── 页面基础 ── */
.unmanned-page {
  min-height: 100vh;
  padding: 0 0 32px;
  background: radial-gradient(ellipse at 30% 0%, #e8f5e9 0%, transparent 60%),
              linear-gradient(180deg, #f0faf0 0%, #f5f7f5 100%);
}

/* ── 顶部横幅 ── */
.command-hero {
  position: relative;
  padding: 32px 28px 28px;
  overflow: hidden;
  border-bottom: 1px solid #e8f0e8;
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #1a6b3c 0%, #2d8a4e 50%, #1a6b3c 100%);
  opacity: 0.04;
}
.hero-content {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}
.hero-kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #52c41a;
  font-weight: 600;
  margin-bottom: 6px;
}
.signal-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 6px #52c41a;
  animation: pulse 2s ease-in-out infinite;
}
.signal-dot.paused { background: #8c8c8c; box-shadow: none; animation: none; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
.hero-title {
  font-size: 22px;
  font-weight: 750;
  color: #1a1a1a;
  margin: 0 0 4px;
  letter-spacing: 0;
}
.hero-desc {
  font-size: 14px;
  color: #666;
  margin: 0;
}
.hero-btn {
  border-radius: 8px !important;
  font-weight: 600;
}

/* ── KPI 卡片行 ── */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px 28px 0;
  max-width: 1400px;
  margin: 0 auto;
}
.kpi-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  transition: box-shadow 0.2s;
}
.kpi-card:hover { box-shadow: 0 3px 12px rgba(0,0,0,0.08); }
.kpi-icon {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #fff; flex-shrink: 0;
}
.kpi-icon.green  { background: linear-gradient(135deg, #52c41a, #3dad0b); }
.kpi-icon.orange { background: linear-gradient(135deg, #faad14, #d48806); }
.kpi-icon.red    { background: linear-gradient(135deg, #ff4d4f, #cf1322); }
.kpi-icon.blue   { background: linear-gradient(135deg, #1890ff, #096dd9); }
.kpi-value {
  font-size: 26px;
  font-weight: 800;
  color: #1a1a1a;
  line-height: 1.1;
}
.kpi-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 2px;
}
.kpi-label span {
  display: block;
  font-size: 11px;
  color: #bfbfbf;
  margin-top: 1px;
}

/* ── 面板通用 ── */
.panel {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #262626;
}
.panel-title i {
  margin-right: 6px;
  color: #52c41a;
}
.panel-body {
  padding: 16px 20px;
}

/* ── 作物生长面板 ── */
.growth-panel {
  margin: 16px 28px 0;
  max-width: 1400px;
}
@media (min-width: 1460px) { .growth-panel { margin-left: auto; margin-right: auto; } }
.growth-body { padding: 16px 20px 20px; }
.growth-kpis {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.g-kpi {
  background: #f6ffed;
  border: 1px solid #d9f7be;
  border-radius: 8px;
  padding: 14px 16px;
  text-align: center;
}
.g-kpi.harvest {
  background: #fff7e6;
  border-color: #ffe58f;
}
.g-kpi-val {
  display: block;
  font-size: 28px;
  font-weight: 800;
  color: #1a1a1a;
  line-height: 1.1;
}
.g-kpi-lbl {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
.growth-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.growth-item {
  display: grid;
  grid-template-columns: 1fr 180px 100px;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.gi-name {
  font-weight: 700;
  font-size: 14px;
  color: #262626;
}
.gi-meta {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}
.gi-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.gi-bar .el-progress { flex: 1; }
.gi-pct {
  font-size: 13px;
  font-weight: 700;
  color: #52c41a;
  min-width: 36px;
  text-align: right;
}
.gi-date {
  font-size: 12px;
  color: #8c8c8c;
  text-align: right;
}

/* ── 主内容双栏 ── */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 16px 28px 0;
  max-width: 1400px;
  margin: 0 auto;
}
.col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── 任务卡片 ── */
.task-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 10px;
  transition: border-color 0.2s;
}
.task-card:hover { border-color: #52c41a; }
.task-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.task-type { font-weight: 600; font-size: 13px; color: #262626; }
.task-farm { font-size: 12px; color: #8c8c8c; margin-left: auto; }
.task-reason {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
  margin-bottom: 10px;
}
.task-btns { display: flex; gap: 8px; }

/* ── 事件卡片 ── */
.event-card {
  border-left: 3px solid #faad14;
  background: #fffbe6;
  border-radius: 0 8px 8px 0;
  padding: 10px 14px;
  margin-bottom: 8px;
}
.ev-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.ev-metric { font-weight: 600; font-size: 13px; }
.ev-farm { font-size: 12px; color: #8c8c8c; margin-left: auto; }
.ev-detail { font-size: 12px; color: #595959; }

/* ── 决策链列表 ── */
.chain-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.chain-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}
.chain-item:hover { background: #f6ffed; margin: 0 -20px; padding: 10px 20px; border-radius: 6px; }
.chain-item:last-child { border-bottom: none; }
.ci-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 5px;
}
.ci-body { flex: 1; min-width: 0; }
.ci-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.ci-time { font-size: 11px; color: #bfbfbf; }
.ci-text {
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 已完成卡片 ── */
.done-card {
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.done-card:last-child { border-bottom: none; }
.done-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.done-type { font-weight: 600; font-size: 13px; color: #262626; }
.done-farm { font-size: 12px; color: #8c8c8c; margin-left: auto; }
.done-result { font-size: 12px; color: #52c41a; }

/* ── 空状态 ── */
.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: #bfbfbf;
}
.empty-hint i { font-size: 36px; display: block; margin-bottom: 8px; }
.empty-hint p { font-size: 13px; margin: 0; }
.empty-hint.compact { padding: 24px 0; }

/* ── 响应式 ── */
@media (max-width: 1100px) {
  .main-grid { grid-template-columns: 1fr; }
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 700px) {
  .command-hero { padding: 20px 16px; }
  .hero-content { flex-direction: column; align-items: flex-start; }
  .kpi-row, .growth-panel, .main-grid { padding-left: 16px; padding-right: 16px; }
  .kpi-row { grid-template-columns: 1fr; }
  .growth-kpis { grid-template-columns: 1fr; }
  .growth-item { grid-template-columns: 1fr; gap: 8px; }
}
</style>
