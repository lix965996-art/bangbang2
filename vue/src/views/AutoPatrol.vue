<template>
  <div v-if="isAgentPreview" class="patrol-preview-page">
    <div class="preview-topline">
      <span class="signal-dot"></span>
      巡检策略台
    </div>
    <section class="preview-hero">
      <div>
        <p class="preview-kicker">巡检规则</p>
        <h1>复核巡检规则与执行记录</h1>
        <p>这里确认灌溉、补光、通知和 AI 分析结果，并把本次处置写入巡检日志。</p>
      </div>
      <div class="preview-score">
        <strong>{{ status.totalPatrols || 0 }}</strong>
        <span>累计巡检</span>
      </div>
    </section>
    <div class="preview-grid">
      <div class="preview-metric">
        <span>土壤湿度联动</span>
        <strong>{{ ruleThresholds.soilHumidityWarn }}%</strong>
      </div>
      <div class="preview-metric">
        <span>高温预警</span>
        <strong>{{ ruleThresholds.temperatureWarn }}°C</strong>
      </div>
      <div class="preview-metric">
        <span>弱光补光</span>
        <strong>{{ ruleThresholds.lightWarn }} lux</strong>
      </div>
    </div>
    <div class="preview-log">
      <div class="preview-log-title">最新处置摘要</div>
      <p v-if="status.latestAiReport">{{ status.latestAiReport }}</p>
      <p v-else>等待本轮巡检生成分析结果。</p>
    </div>
  </div>
  <div v-else class="patrol-page">

    <section class="command-hero">
      <div class="hero-main">
        <div class="hero-kicker">
          <span class="signal-dot" :class="{ paused: !status.enabled }"></span>
          {{ status.enabled ? '巡检运行中' : '巡检已暂停' }}
        </div>
        <h1>巡检与联动</h1>
        <p>按预设阈值查看环境异常、触发设备联动，并保留每次处理记录。</p>
        <div class="hero-meta">
          <span>最近巡检 {{ status.lastPatrolTime || '尚未执行' }}</span>
          <span>巡检间隔 {{ intervalLabel }}</span>
          <span>{{ logs.length }} 条日志</span>
        </div>
      </div>
      <div class="hero-actions">
        <el-button
          class="hero-btn secondary"
          :loading="toggling"
          @click="togglePatrol"
          :icon="status.enabled ? 'el-icon-video-pause' : 'el-icon-video-play'"
        >{{ status.enabled ? '暂停巡检' : '开启巡检' }}</el-button>
        <el-button
          class="hero-btn primary"
          :loading="triggering"
          icon="el-icon-refresh"
          @click="triggerPatrol"
        >立即巡检一次</el-button>
      </div>
    </section>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon green"><i class="el-icon-success"></i></div>
          <div class="stat-body">
            <div class="stat-label">巡检次数</div>
            <div class="stat-value">{{ status.totalPatrols || 0 }}</div>
            <div class="stat-caption">自动与手动巡检合计</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon blue"><i class="el-icon-setting"></i></div>
          <div class="stat-body">
            <div class="stat-label">联动操作</div>
            <div class="stat-value">{{ status.totalActions || 0 }}</div>
            <div class="stat-caption">灌溉、补光、通知等</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon orange"><i class="el-icon-time"></i></div>
          <div class="stat-body">
            <div class="stat-label">最近执行</div>
            <div class="stat-value small">{{ status.lastPatrolTime || '尚未执行' }}</div>
            <div class="stat-caption">最近一次执行记录</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon purple"><i class="el-icon-chat-dot-round"></i></div>
          <div class="stat-body">
            <div class="stat-label">巡检间隔</div>
            <div class="stat-value small">{{ intervalLabel }}</div>
            <div class="stat-caption">系统定时巡检周期</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <section class="agent-console" :class="{ running: agentRunning }">
      <div class="agent-viewport">
        <div class="agent-head">
          <div>
            <div class="agent-kicker">巡检执行台</div>
            <h2>执行过程</h2>
          </div>
          <el-tag size="small" :type="agentRunning ? 'success' : 'info'" effect="plain">
            {{ agentRunning ? '执行中' : '待命' }}
          </el-tag>
        </div>

        <div class="browser-shell">
          <div class="browser-bar">
            <span class="dot red"></span>
            <span class="dot amber"></span>
            <span class="dot green"></span>
            <div class="address-bar">{{ currentAgentPreviewAddress }}</div>
          </div>
          <div class="browser-stage live-browser-stage">
            <div class="agent-frame-tabs">
              <button type="button" :class="{ active: agentViewMode === 'page' }" @click="switchAgentView('page')">页面预览</button>
              <button type="button" :class="{ active: agentViewMode === 'evidence' }" @click="switchAgentView('evidence')">
                执行记录
                <b v-if="agentEvidenceCount">{{ agentEvidenceCount }}</b>
              </button>
              <span class="mode-pill">
                <i></i>
                执行方式：{{ agentModeLabel }}
              </span>
            </div>
            <div
              v-if="agentViewMode === 'page'"
              ref="agentFrameViewport"
              class="agent-frame-viewport"
            >
              <iframe
                class="agent-frame"
                :key="agentFrameKey"
                :src="currentAgentPreviewUrl"
                :title="currentAgentStep.title"
                :style="agentFrameStyle"
                @load="agentFrameLoading = false"
              ></iframe>
            </div>
            <div v-else class="agent-evidence-pane">
              <img v-if="currentAgentEvidenceUrl" :src="currentAgentEvidenceUrl" alt="巡检执行记录" @error="handleAgentEvidenceError" />
              <div v-else class="evidence-empty">
                <i class="el-icon-picture-outline"></i>
                <span>{{ agentEvidenceEmptyText }}</span>
              </div>
            </div>
            <div v-if="agentViewMode === 'page' && agentFrameLoading" class="frame-loading">
              <span></span>
              正在打开 {{ currentAgentStep.module }}
            </div>
            <div v-if="agentViewMode === 'page'" class="agent-live-overlay">
              <div class="scan-line" v-if="agentRunning"></div>
              <div class="stage-chip">{{ currentAgentStep.module }}</div>
              <h3>{{ currentAgentStep.title }}</h3>
              <p>{{ currentAgentStep.detail }}</p>
              <div class="stage-metrics">
                <span v-for="item in currentAgentStep.signals" :key="item">{{ item }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="agent-side">
        <div class="agent-progress-head">
          <span>巡检进度</span>
          <strong>{{ agentProgress }}%</strong>
        </div>
        <div class="agent-progress-track">
          <div class="agent-progress-fill" :style="{ width: agentProgress + '%' }"></div>
        </div>

        <div class="agent-steps">
          <div
            v-for="(step, index) in agentSteps"
            :key="step.key"
            class="agent-step"
            :class="agentStepState(index)"
          >
            <span class="step-index">{{ index + 1 }}</span>
            <div>
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc">{{ step.short }}</div>
            </div>
          </div>
        </div>

        <div class="agent-trace">
          <div class="trace-title">操作轨迹</div>
          <div v-for="item in agentTrace" :key="item.id" class="trace-row">
            <span>{{ item.time }}</span>
            <p>{{ item.text }}</p>
          </div>
          <div v-if="agentTrace.length === 0" class="trace-empty">等待开始巡检</div>
        </div>
      </div>
    </section>

    <section class="insight-grid">
      <!-- AI 最新报告 -->
      <div class="ai-report-card">
        <div class="panel-title">
          <span><i class="el-icon-reading"></i> 最新 AI 巡检报告</span>
          <el-tag size="small" effect="plain" type="success">AI 摘要</el-tag>
        </div>
        <div v-if="status.latestAiReport" class="ai-report-content">{{ status.latestAiReport }}</div>
        <div v-else class="ai-report-empty">暂无 AI 报告，执行一次巡检后将自动生成分析摘要。</div>
      </div>

      <!-- 规则说明 -->
      <div class="rule-panel">
        <div class="panel-title">
          <span><i class="el-icon-connection"></i> 当前生效巡检策略</span>
          <el-button link size="small" @click="ruleExpanded = ruleExpanded.length ? [] : ['rules']">
            {{ ruleExpanded.length ? '收起' : '展开' }}
          </el-button>
        </div>
        <div class="rule-list compact">
          <div class="rule-item"><i class="el-icon-water-cup text-blue"></i><span>土壤湿度低于 <b>{{ ruleThresholds.soilHumidityWarn }}%</b> 自动灌溉</span></div>
          <div class="rule-item"><i class="el-icon-sunny text-orange"></i><span>温度高于 <b>{{ ruleThresholds.temperatureWarn }}°C</b> 推送预警</span></div>
          <div class="rule-item"><i class="el-icon-magic-stick text-yellow"></i><span>白天光照低于 <b>{{ ruleThresholds.lightWarn }} lux</b> 开启补光</span></div>
        </div>
        <el-collapse class="rule-collapse" v-model="ruleExpanded">
          <el-collapse-item title="策略详情" name="rules">
            <div class="rule-tip">策略参数由系统后台统一维护，调整后将在下一轮巡检中生效。</div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </section>

    <!-- 巡检日志表格 -->
    <div class="log-section">
      <div class="log-header">
        <div>
          <div class="log-title"><i class="el-icon-document"></i> 巡检决策日志</div>
          <div class="log-subtitle">记录规则触发、AI 报告和执行结果</div>
        </div>
        <el-button class="refresh-btn" icon="el-icon-refresh" @click="loadLogs" :loading="logsLoading">刷新</el-button>
      </div>

      <el-table
        class="patrol-table"
        :data="logs"
        v-loading="logsLoading"
        size="small"
        style="width:100%"
        :row-class-name="rowClass"
      >
        <el-table-column prop="patrolTime" label="时间" width="160" sortable />
        <el-table-column prop="triggerType" label="触发方式" width="90" align="center">
          <template #default="{row}">
            <el-tag size="small" :type="triggerTagType(row.triggerType)">
              {{ triggerLabel(row.triggerType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="farmName" label="农田" width="110">
          <template #default="{row}">
            <span>{{ row.farmName || '全局' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="actionType" label="操作类型" width="130">
          <template #default="{row}">
            <el-tag size="small" :type="actionTagType(row.actionType)">{{ actionLabel(row.actionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="详情 / 原因" min-width="220">
          <template #default="{row}">
            <div v-if="row.aiReport" class="ai-report-inline">
              <i class="el-icon-reading"></i> {{ row.aiReport }}
            </div>
            <div v-else>
              <div v-if="row.actionDetail" class="detail-text">{{ row.actionDetail }}</div>
              <div v-if="row.reason" class="reason-text">{{ row.reason }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="结果" width="90" align="center">
          <template #default="{row}">
            <el-tag size="small" :type="resultTagType(row.result)">{{ resultLabel(row.result) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="logs.length === 0 && !logsLoading" class="empty-log">
        <i class="el-icon-inbox"></i>
        <p>暂无巡检记录，点击「立即巡检一次」开始</p>
      </div>
    </div>

  </div>
</template>

<script>
export default {
  name: 'AutoPatrol',
  data() {
    return {
      status: {
        enabled: true,
        totalPatrols: 0,
        totalActions: 0,
        lastPatrolTime: null,
        latestAiReport: null,
        ruleThresholds: {}
      },
      logs: [],
      toggling: false,
      triggering: false,
      logsLoading: false,
      ruleExpanded: [],
      intervalLabel: '30 分钟',
      agentRunning: false,
      agentStepIndex: 0,
      agentProgress: 0,
      agentTimer: null,
      agentSessionId: '',
      agentPollTimer: null,
      agentBackendMode: false,
      agentFrameLoading: true,
      agentViewMode: 'page',
      agentUserSelectedView: false,
      agentFrameResizeObserver: null,
      agentFrameViewportWidth: 0,
      agentFrameViewportHeight: 0,
      agentFrameBaseWidth: 1440,
      agentMode: 'business_agent',
      agentEvidenceImageFailed: false,
      agentEvidenceLoading: false,
      agentEvidenceImageSrc: '',
      agentEvidenceObjectUrl: '',
      agentTrace: [],
      agentEvents: [],
      agentSteps: [
        {
          key: 'open-dashboard',
          module: '总控台',
          path: '/unmanned-dashboard',
          title: '打开无人农场总控台',
          short: '读取运行状态',
          detail: '进入农场总览，检查巡检状态、待处理事件和近期决策记录。',
          signals: ['巡检状态', '待审批任务', '决策链']
        },
        {
          key: 'read-sensors',
          module: '环境监测',
          path: '/dashbordnew',
          title: '读取环境监测数据',
          short: '采集温湿度与光照',
          detail: '切换到环境监测看板，读取温度、土壤湿度、空气湿度和光照数据。',
          signals: ['温度', '土壤湿度', '空气湿度', '光照']
        },
        {
          key: 'inspect-vision',
          module: '视觉巡检',
          path: '/fruit-detect',
          title: '查看视觉巡检结果',
          short: '复核图像异常',
          detail: '检查视觉识别结果，确认是否存在病虫害、成熟度异常或图像风险点。',
          signals: ['图像识别', '病虫害风险', '成熟度']
        },
        {
          key: 'check-map',
          module: 'GIS 指挥',
          path: '/farm-map-gaode',
          title: '定位地块与巡检区域',
          short: '确认异常位置',
          detail: '打开 GIS 地块指挥页，将异常事件关联到具体地块和巡检范围。',
          signals: ['地块位置', '巡检轨迹', '区域状态']
        },
        {
          key: 'execute-policy',
          module: '策略执行',
          path: '/auto-patrol',
          title: '执行自主巡检策略',
          short: '触发灌溉/补光/通知',
          detail: '根据阈值和 AI 建议执行策略，并将结果写入巡检日志。',
          signals: ['规则引擎', 'AI 建议', '执行日志']
        }
      ]
    }
  },
  computed: {
    ruleThresholds() {
      return {
        soilHumidityWarn: 25,
        temperatureWarn: 38,
        lightWarn: 500,
        lightStartHour: 6,
        lightEndHour: 18,
        ...(this.status.ruleThresholds || {})
      }
    },
    currentAgentStep() {
      return this.agentSteps[this.agentStepIndex] || this.agentSteps[0]
    },
    currentAgentPreviewRoute() {
      const query = this.currentAgentStep.path === '/auto-patrol' ? { agentPreview: '1' } : {}
      return this.$router.resolve({ path: this.currentAgentStep.path, query })
    },
    currentAgentPreviewUrl() {
      if (typeof window === 'undefined') {
        return this.currentAgentPreviewRoute.href
      }
      return new URL(this.currentAgentPreviewRoute.href, window.location.origin).toString()
    },
    currentAgentPreviewAddress() {
      return this.currentAgentStep.path === '/auto-patrol'
        ? '/auto-patrol · 策略执行视图'
        : this.currentAgentStep.path
    },
    agentEvidenceEvents() {
      return this.agentEvents.filter(item => item.screenshotUrl || item.artifactUrl)
    },
    agentEvidenceCount() {
      return this.agentEvidenceEvents.length
    },
    currentAgentEvidenceEvent() {
      for (let i = this.agentEvents.length - 1; i >= 0; i -= 1) {
        const event = this.agentEvents[i]
        if (event.stepKey === this.currentAgentStep.key && (event.screenshotUrl || event.artifactUrl)) {
          return event
        }
      }
      return this.agentEvidenceEvents[this.agentEvidenceEvents.length - 1] || null
    },
    currentAgentEvidenceRawUrl() {
      const event = this.currentAgentEvidenceEvent
      return event ? (event.artifactUrl || event.screenshotUrl) : ''
    },
    currentAgentEvidenceUrl() {
      return this.agentEvidenceImageSrc
    },
    agentEvidenceEmptyText() {
      if (this.agentEvidenceLoading) return '正在加载执行证据'
      if (this.currentAgentEvidenceEvent && this.agentEvidenceImageFailed) return '后端证据图不可用，已生成事件摘要'
      if (this.currentAgentEvidenceEvent) return '正在准备本步骤执行证据'
      return '等待生成本步骤执行证据'
    },
    agentModeLabel() {
      const map = {
        controlled: '业务流程',
        business_agent: '业务流程',
        playwright: '浏览器实控',
        fallback: '本地执行'
      }
      return map[this.agentMode] || '巡检流程'
    },
    agentFrameScale() {
      const width = this.agentFrameViewportWidth || 1120
      return Math.min(1, Math.max(0.18, width / this.agentFrameBaseWidth))
    },
    agentFrameStyle() {
      const scale = this.agentFrameScale
      const viewportHeight = this.agentFrameViewportHeight || 360
      return {
        width: `${this.agentFrameBaseWidth}px`,
        height: `${Math.max(640, Math.ceil(viewportHeight / scale))}px`,
        transform: `scale(${scale})`
      }
    },
    agentFrameKey() {
      return `${this.currentAgentStep.key}-${this.currentAgentPreviewRoute.href}`
    },
    isAgentPreview() {
      return this.$route && this.$route.query && this.$route.query.agentPreview === '1'
    }
  },
  watch: {
    agentFrameKey() {
      this.agentFrameLoading = true
      this.$nextTick(this.updateAgentFrameViewport)
    },
    currentAgentEvidenceRawUrl() {
      if (this.agentViewMode === 'evidence') {
        this.loadAgentEvidenceImage()
      }
    },
    agentViewMode(mode) {
      if (mode === 'page') {
        this.$nextTick(this.observeAgentFrameViewport)
      } else if (mode === 'evidence') {
        this.loadAgentEvidenceImage()
      }
    }
  },
  created() {
    this.loadStatus()
    this.loadLogs()
  },
  mounted() {
    this.observeAgentFrameViewport()
  },
  beforeUnmount() {
    this.stopAgentPlayback()
    this.stopAgentPolling()
    this.disconnectAgentFrameObserver()
    this.clearAgentEvidenceImage()
  },
  methods: {
    getApiBaseUrl() {
      const configured = import.meta.env.VUE_APP_API_BASE_URL || 'http://localhost:9090'
      if (typeof window === 'undefined') return configured
      try {
        const url = new URL(configured)
        const pageHost = window.location.hostname
        const isLocalApi = url.hostname === 'localhost' || url.hostname === '127.0.0.1'
        const isLocalPage = pageHost === 'localhost' || pageHost === '127.0.0.1'
        if (isLocalApi && isLocalPage) {
          url.hostname = pageHost
        }
        return url.toString().replace(/\/$/, '')
      } catch (e) {
        return configured
      }
    },
    resolveAgentAssetUrl(url) {
      if (!url) return ''
      if (/^(https?:|data:|blob:)/i.test(url)) return url
      if (typeof window === 'undefined') return url
      if (url.startsWith('/api/')) {
        return new URL(url, this.getApiBaseUrl()).toString()
      }
      return new URL(url, window.location.origin).toString()
    },
    getAgentArtifactRequestUrl(url) {
      if (!url) return ''
      if (!/^https?:/i.test(url)) return url
      try {
        const parsed = new URL(url)
        const apiBase = new URL(this.getApiBaseUrl())
        if (parsed.origin === apiBase.origin) {
          return `${parsed.pathname}${parsed.search}`
        }
      } catch (e) {
        return url
      }
      return url
    },
    clearAgentEvidenceImage() {
      if (this.agentEvidenceObjectUrl) {
        URL.revokeObjectURL(this.agentEvidenceObjectUrl)
      }
      this.agentEvidenceObjectUrl = ''
      this.agentEvidenceImageSrc = ''
      this.agentEvidenceImageFailed = false
      this.agentEvidenceLoading = false
    },
    async loadAgentEvidenceImage() {
      const event = this.currentAgentEvidenceEvent
      const rawUrl = this.currentAgentEvidenceRawUrl
      this.clearAgentEvidenceImage()
      if (!event || !rawUrl) return

      this.agentEvidenceLoading = true
      try {
        const blob = await this.request.get(this.getAgentArtifactRequestUrl(rawUrl), { responseType: 'blob' })
        const isImageBlob = blob && blob.size > 0 && (!blob.type || blob.type.includes('image') || blob.type.includes('svg') || blob.type.includes('xml'))
        if (!isImageBlob) throw new Error('证据文件不是图片')
        const objectUrl = URL.createObjectURL(blob)
        this.agentEvidenceObjectUrl = objectUrl
        this.agentEvidenceImageSrc = objectUrl
      } catch (e) {
        this.agentEvidenceImageFailed = true
        this.agentEvidenceImageSrc = this.buildAgentEvidenceFallback(event)
      } finally {
        this.agentEvidenceLoading = false
      }
    },
    buildAgentEvidenceFallback(event) {
      const safe = (value) => String(value || '').replace(/[&<>"']/g, match => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&apos;'
      }[match]))
      const truncate = (value, max = 80) => {
        const text = String(value || '')
        return text.length > max ? `${text.slice(0, max)}...` : text
      }
      const signals = Array.isArray(event.signals) ? event.signals.join(' / ') : ''
      const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="960" height="540" viewBox="0 0 960 540">
          <defs>
            <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
              <stop offset="0" stop-color="#f5faf7"/>
              <stop offset="1" stop-color="#eaf5ee"/>
            </linearGradient>
          </defs>
          <rect width="960" height="540" rx="18" fill="url(#bg)"/>
          <rect x="38" y="34" width="884" height="472" rx="18" fill="#fff" stroke="#d7eadf"/>
          <rect x="38" y="34" width="884" height="88" rx="18" fill="#0f7a4d"/>
          <text x="74" y="88" fill="#dffbea" font-size="22" font-weight="800" font-family="Microsoft YaHei, Arial">执行证据摘要</text>
          <text x="74" y="170" fill="#0f9f66" font-size="16" font-weight="800" font-family="Microsoft YaHei, Arial">${safe(event.module || '巡检步骤')}</text>
          <text x="74" y="218" fill="#10231a" font-size="38" font-weight="800" font-family="Microsoft YaHei, Arial">${safe(truncate(event.title, 22))}</text>
          <foreignObject x="74" y="246" width="812" height="96">
            <div xmlns="http://www.w3.org/1999/xhtml" style="font-family:'Microsoft YaHei',Arial;color:#52645a;font-size:20px;line-height:1.55;">${safe(truncate(event.detail, 120))}</div>
          </foreignObject>
          <rect x="74" y="368" width="812" height="74" rx="12" fill="#f5faf7"/>
          <text x="96" y="398" fill="#6b7d72" font-size="15" font-weight="700" font-family="Microsoft YaHei, Arial">关键信号</text>
          <text x="96" y="425" fill="#24382e" font-size="18" font-family="Microsoft YaHei, Arial">${safe(signals || '暂无')}</text>
          <text x="74" y="478" fill="#7b8b82" font-size="15" font-family="Microsoft YaHei, Arial">原始证据图暂不可用，当前展示后端事件记录摘要 · ${safe(event.time || '')}</text>
        </svg>`
      return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
    },
    handleAgentEvidenceError() {
      this.agentEvidenceImageFailed = true
      if (this.currentAgentEvidenceEvent) {
        this.agentEvidenceImageSrc = this.buildAgentEvidenceFallback(this.currentAgentEvidenceEvent)
      }
    },
    observeAgentFrameViewport() {
      this.disconnectAgentFrameObserver()
      this.updateAgentFrameViewport()
      if (typeof ResizeObserver !== 'undefined' && this.$refs.agentFrameViewport) {
        this.agentFrameResizeObserver = new ResizeObserver(this.updateAgentFrameViewport)
        this.agentFrameResizeObserver.observe(this.$refs.agentFrameViewport)
      } else if (typeof window !== 'undefined') {
        window.addEventListener('resize', this.updateAgentFrameViewport)
      }
    },
    disconnectAgentFrameObserver() {
      if (this.agentFrameResizeObserver) {
        this.agentFrameResizeObserver.disconnect()
        this.agentFrameResizeObserver = null
      }
      if (typeof window !== 'undefined') {
        window.removeEventListener('resize', this.updateAgentFrameViewport)
      }
    },
    updateAgentFrameViewport() {
      const el = this.$refs.agentFrameViewport
      if (!el) return
      const rect = el.getBoundingClientRect()
      this.agentFrameViewportWidth = Math.max(0, rect.width)
      this.agentFrameViewportHeight = Math.max(0, rect.height)
    },
    async loadStatus() {
      try {
        const res = await this.request.get('/api/patrol/status')
        if (res.code === '200') this.status = res.data
      } catch (e) { /* 忽略 */ }
    },
    async loadLogs() {
      this.logsLoading = true
      try {
        const res = await this.request.get('/api/patrol/logs?limit=100')
        if (res.code === '200') this.logs = res.data || []
      } catch (e) { /* 忽略 */ } finally {
        this.logsLoading = false
      }
    },
    async togglePatrol() {
      this.toggling = true
      try {
        const res = await this.request.post('/api/patrol/toggle')
        if (res.code === '200') {
          this.status.enabled = res.data.enabled
          this.$message.success(res.data.message)
        }
      } catch (e) {
        this.$message.error('操作失败：' + e.message)
      } finally {
        this.toggling = false
      }
    },
    async triggerPatrol() {
      this.triggering = true
      try {
        const agentStarted = await this.startBrowserAgentPatrol()
        if (agentStarted) {
          this.$message.success('巡检流程已开始')
          await this.waitForAgentCompletion()
          await this.loadStatus()
          await this.loadLogs()
          return
        }

        this.startAgentPlayback()
        const res = await this.request.post('/api/patrol/trigger')
        if (res.code === '200') {
          this.finishAgentPlayback(res.data)
          this.$message.success(
            `巡检完成：检查了 ${res.data.farmsChecked} 块农田，执行了 ${res.data.actionsExecuted} 项操作`
          )
          await this.loadStatus()
          await this.loadLogs()
        } else {
          this.failAgentPlayback(res.msg || '巡检失败')
          this.$message.error(res.msg || '巡检失败')
        }
      } catch (e) {
        this.failAgentPlayback(e.message)
        this.$message.error('巡检异常：' + e.message)
      } finally {
        this.triggering = false
      }
    },

    async startBrowserAgentPatrol() {
      try {
        const res = await this.request.post('/api/browser-agent/patrol/run')
        if (res && res.code === '200' && res.data && res.data.sessionId) {
          this.agentBackendMode = true
          this.agentSessionId = res.data.sessionId
          this.agentViewMode = 'page'
          this.agentUserSelectedView = false
          this.applyAgentSession(res.data)
          this.startAgentPolling()
          return true
        }
      } catch (e) {
        this.agentBackendMode = false
      }
      return false
    },

    startAgentPolling() {
      this.stopAgentPolling()
      this.agentPollTimer = window.setInterval(() => {
        this.pollAgentSession()
      }, 900)
    },

    stopAgentPolling() {
      if (this.agentPollTimer) {
        window.clearInterval(this.agentPollTimer)
        this.agentPollTimer = null
      }
    },

    async pollAgentSession() {
      if (!this.agentSessionId) return
      try {
        const res = await this.request.get(`/api/browser-agent/sessions/${this.agentSessionId}`)
        if (res && res.code === '200' && res.data) {
          this.applyAgentSession(res.data)
          if (['completed', 'failed'].includes(res.data.status)) {
            this.stopAgentPolling()
          }
        }
      } catch (e) {
        this.stopAgentPolling()
      }
    },

    waitForAgentCompletion() {
      return new Promise(resolve => {
        const startedAt = Date.now()
        const check = async () => {
          await this.pollAgentSession()
          if (!this.agentRunning || Date.now() - startedAt > 30000) {
            resolve()
            return
          }
          window.setTimeout(check, 900)
        }
        check()
      })
    },

    applyAgentSession(session) {
      const events = Array.isArray(session.events) ? session.events : []
      const latest = events[events.length - 1]
      this.agentRunning = session.status === 'queued' || session.status === 'running'
      this.agentMode = session.mode || this.agentMode || 'business_agent'
      this.agentProgress = Number(session.progress) || 0
      if (latest) {
        const index = this.agentSteps.findIndex(step => step.key === latest.stepKey)
        if (index >= 0) this.agentStepIndex = index
      }
      this.agentTrace = events.slice(-5).reverse().map(event => ({
        id: event.id,
        time: event.time,
        text: event.title + (event.detail ? `：${event.detail}` : '')
      }))
      this.agentEvents = events
      if (this.agentViewMode === 'evidence') {
        this.$nextTick(this.loadAgentEvidenceImage)
      }
      if (session.status === 'completed') {
        this.agentRunning = false
        this.agentProgress = 100
        this.agentStepIndex = this.agentSteps.length - 1
      }
      if (session.status === 'failed') {
        this.agentRunning = false
        const message = session.errorMessage || '巡检执行失败'
        if (!this.agentTrace.some(item => item.text.includes(message))) {
          this.pushAgentTrace(`巡检中断：${message}`)
        }
      }
    },

    startAgentPlayback() {
      this.stopAgentPlayback()
      this.stopAgentPolling()
      this.agentBackendMode = false
      this.agentSessionId = ''
      this.agentMode = 'fallback'
      this.agentEvents = []
      this.agentViewMode = 'page'
      this.agentUserSelectedView = false
      this.agentRunning = true
      this.agentStepIndex = 0
      this.agentProgress = 6
      this.agentTrace = []
      this.pushAgentTrace('打开自主巡检任务，准备进入系统页面')

      let tick = 0
      this.agentTimer = window.setInterval(() => {
        tick += 1
        const nextProgress = Math.min(88, 6 + tick * 7)
        this.agentProgress = nextProgress

        const nextIndex = Math.min(
          this.agentSteps.length - 1,
          Math.floor((nextProgress / 100) * this.agentSteps.length)
        )

        if (nextIndex !== this.agentStepIndex) {
          this.agentStepIndex = nextIndex
          const step = this.currentAgentStep
          this.pushAgentTrace(`进入 ${step.module}，${step.short}`)
        }
      }, 650)
    },

    finishAgentPlayback(result = {}) {
      this.stopAgentPlayback(false)
      this.agentRunning = false
      this.agentStepIndex = this.agentSteps.length - 1
      this.agentProgress = 100
      this.pushAgentTrace(`巡检完成：检查 ${result.farmsChecked || 0} 块农田，执行 ${result.actionsExecuted || 0} 项操作`)
    },

    failAgentPlayback(message) {
      this.stopAgentPlayback(false)
      this.agentRunning = false
      this.pushAgentTrace(`巡检中断：${message || '执行失败'}`)
    },

    stopAgentPlayback(resetRunning = true) {
      if (this.agentTimer) {
        window.clearInterval(this.agentTimer)
        this.agentTimer = null
      }
      if (resetRunning) this.agentRunning = false
    },

    pushAgentTrace(text) {
      const now = new Date()
      const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
      this.agentTrace.unshift({
        id: `${Date.now()}-${Math.random()}`,
        time,
        text
      })
      this.agentTrace = this.agentTrace.slice(0, 5)
    },

    agentStepState(index) {
      if (index < this.agentStepIndex || this.agentProgress === 100) return 'done'
      if (index === this.agentStepIndex) return this.agentRunning ? 'active' : 'current'
      return ''
    },

    switchAgentView(mode) {
      this.agentViewMode = mode
      this.agentUserSelectedView = true
      if (mode === 'evidence') {
        this.agentFrameLoading = false
      }
    },

    triggerLabel(type) {
      const map = {
        manual: '手动',
        scheduled: '定时',
        browser_agent: '巡检流程'
      }
      return map[type] || type || '未知'
    },

    triggerTagType(type) {
      const map = {
        manual: 'warning',
        scheduled: 'info',
        browser_agent: 'success'
      }
      return map[type] || 'info'
    },

    actionLabel(type) {
      const map = {
        irrigation_on:     '开启灌溉',
        led_on:            '开启补光灯',
        send_notification: '推送通知',
        agent_decision:    '智能决策',
        ai_analysis:       'AI 报告',
        no_action:         '无需操作'
      }
      return map[type] || type
    },
    actionTagType(type) {
      const map = {
        irrigation_on:     'primary',
        led_on:            'warning',
        send_notification: 'danger',
        agent_decision:    'success',
        ai_analysis:       'success',
        no_action:         'info'
      }
      return map[type] || 'info'
    },
    resultLabel(r) {
      const map = { success: '成功', failed: '失败', skipped: '跳过', no_action: '正常' }
      return map[r] || r
    },
    resultTagType(r) {
      const map = { success: 'success', failed: 'danger', skipped: 'info', no_action: 'info' }
      return map[r] || 'info'
    },
    rowClass({ row }) {
      if (row.actionType === 'ai_analysis') return 'row-ai'
      if (row.result === 'failed') return 'row-failed'
      if (row.actionType === 'no_action') return 'row-normal'
      return ''
    }
  }
}
</script>

<style scoped>
.patrol-page {
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
  padding: 28px 32px 40px;
  color: #12231b;
}

.patrol-preview-page {
  min-height: 100vh;
  padding: 26px;
  background:
    linear-gradient(135deg, rgba(240, 253, 244, 0.96), rgba(255, 255, 255, 0.98)),
    repeating-linear-gradient(90deg, rgba(22, 163, 108, 0.08) 0 1px, transparent 1px 72px);
  color: #12231b;
}

.preview-topline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: #0f7a4d;
  font-size: 13px;
  font-weight: 800;
}

.preview-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(28, 45, 36, 0.1);
}

.preview-kicker {
  margin: 0 0 8px;
  color: #0f9f66;
  font-size: 12px;
  font-weight: 800;
}

.preview-hero h1 {
  margin: 0;
  color: #10231a;
  font-size: 24px;
  line-height: 1.25;
}

.preview-hero p:last-child {
  max-width: 620px;
  margin: 10px 0 0;
  color: #65756c;
  font-size: 14px;
  line-height: 1.7;
}

.preview-score {
  min-width: 116px;
  height: 96px;
  display: grid;
  place-content: center;
  border-radius: 8px;
  background: #ecfdf3;
  color: #0f7a4d;
  text-align: center;
}

.preview-score strong {
  display: block;
  font-size: 32px;
  line-height: 1;
}

.preview-score span {
  margin-top: 7px;
  font-size: 12px;
  font-weight: 800;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.preview-metric,
.preview-log {
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(28, 45, 36, 0.08);
}

.preview-metric {
  min-height: 88px;
  padding: 16px;
}

.preview-metric span {
  display: block;
  margin-bottom: 10px;
  color: #74857b;
  font-size: 12px;
  font-weight: 700;
}

.preview-metric strong {
  color: #14291f;
  font-size: 24px;
}

.preview-log {
  margin-top: 14px;
  padding: 18px;
}

.preview-log-title {
  margin-bottom: 8px;
  color: #14291f;
  font-size: 14px;
  font-weight: 800;
}

.preview-log p {
  margin: 0;
  color: #5c6f64;
  font-size: 14px;
  line-height: 1.7;
}

.command-hero {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 28px;
  min-height: 132px;
  margin-bottom: 18px;
  padding: 22px 26px;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(115deg, rgba(16, 97, 70, 0.98) 0%, rgba(25, 126, 85, 0.94) 58%, rgba(65, 157, 112, 0.86) 100%),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 96px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 72px);
  box-shadow: 0 14px 34px rgba(16, 72, 49, 0.14);
}

.command-hero::after {
  content: "";
  position: absolute;
  inset: auto 26px 0 auto;
  width: 260px;
  height: 92px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), transparent);
  transform: skewX(-22deg);
  opacity: 0.24;
}

.hero-main,
.hero-actions {
  position: relative;
  z-index: 1;
}

.hero-main {
  max-width: 680px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 9px;
  margin-bottom: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  font-weight: 700;
}

.signal-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #70f0ad;
  box-shadow: 0 0 0 5px rgba(112, 240, 173, 0.16);
}

.signal-dot.paused {
  background: #cbd5e1;
  box-shadow: 0 0 0 5px rgba(203, 213, 225, 0.18);
}

.command-hero h1 {
  margin: 0;
  color: #ffffff;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 750;
  letter-spacing: 0;
}

.command-hero p {
  max-width: 640px;
  margin: 8px 0 14px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 14px;
  line-height: 1.65;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.hero-meta span {
  padding: 5px 9px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.84);
  font-size: 12px;
  font-weight: 500;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 230px;
}

.hero-btn {
  height: 36px;
  border-radius: 8px !important;
  padding: 0 14px !important;
  font-weight: 650 !important;
  border: 0 !important;
  transform: none !important;
}

.hero-btn.primary {
  background: #ffffff !important;
  color: #0b7b4e !important;
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.16) !important;
}

.hero-btn.secondary {
  background: rgba(255, 255, 255, 0.14) !important;
  color: #ffffff !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.24) !important;
}

.hero-btn:hover {
  transform: none !important;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 88px;
  min-width: 0;
  padding: 16px 18px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10px 26px rgba(28, 45, 36, 0.07);
}

.stat-card:hover {
  transform: none;
}

.stat-body {
  min-width: 0;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  flex-shrink: 0;
}

.stat-icon.green  { background: #d9fbe8; color: #059669; }
.stat-icon.blue   { background: #e0ecff; color: #2563eb; }
.stat-icon.orange { background: #fff0d8; color: #ea580c; }
.stat-icon.purple { background: #eee7ff; color: #7c3aed; }
.stat-label {
  margin-bottom: 5px;
  color: #6c7a71;
  font-size: 13px;
  font-weight: 600;
}
.stat-value {
  color: #111827;
  font-size: 23px;
  line-height: 1;
  font-weight: 750;
}
.stat-value.small {
  max-width: 100%;
  font-size: 15px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-caption {
  margin-top: 8px;
  color: #9aa7a0;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-console {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: 16px;
  margin-bottom: 16px;
}

.agent-viewport,
.agent-side {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 34px rgba(28, 45, 36, 0.08);
}

.agent-viewport {
  padding: 20px;
}

.agent-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.agent-kicker {
  margin-bottom: 4px;
  color: #0f9f66;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: none;
}

.agent-head h2 {
  margin: 0;
  color: #11241b;
  font-size: 17px;
  font-weight: 750;
}

.browser-shell {
  overflow: hidden;
  border-radius: 8px;
  background: #0f1f18;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.browser-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 12px;
  background: #14291f;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot.red { background: #ff6b6b; }
.dot.amber { background: #f6c453; }
.dot.green { background: #50d890; }

.address-bar {
  flex: 1;
  min-width: 0;
  height: 24px;
  margin-left: 8px;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.browser-stage {
  position: relative;
  min-height: 230px;
  padding: 28px 30px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 18%, rgba(58, 205, 137, 0.22), transparent 28%),
    linear-gradient(135deg, #10231a 0%, #173929 55%, #22583c 100%);
}

.browser-stage::before {
  content: "";
  position: absolute;
  inset: 20px;
  border-radius: 8px;
  background:
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 54px),
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.04) 0 1px, transparent 1px 44px);
  opacity: 0.5;
}

.live-browser-stage {
  height: 410px;
  min-height: 410px;
  padding: 0;
  background: #eef4f0;
}

.live-browser-stage::before {
  display: none;
}

.agent-frame-tabs {
  position: absolute;
  left: 12px;
  top: 10px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px;
  border-radius: 8px;
  background: rgba(11, 30, 22, 0.82);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(8px);
}

.agent-frame-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 9px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.agent-frame-tabs button b {
  min-width: 17px;
  height: 17px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-size: 11px;
  line-height: 1;
}

.agent-frame-tabs button.active {
  background: rgba(112, 240, 173, 0.18);
  color: #9cf7bd;
}

.agent-frame-tabs span {
  padding: 0 8px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 700;
}

.agent-frame-tabs .mode-pill {
  height: 26px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 4px;
  border-left: 1px solid rgba(255, 255, 255, 0.14);
  cursor: default;
  user-select: none;
  opacity: 0.82;
}

.agent-frame-tabs .mode-pill i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #70f0ad;
  box-shadow: 0 0 0 4px rgba(112, 240, 173, 0.12);
}

.agent-frame-viewport {
  position: absolute;
  left: 0;
  right: 0;
  top: 46px;
  bottom: 0;
  overflow: hidden;
  background: #f1f5f9;
}

.agent-frame {
  position: absolute;
  left: 0;
  top: 0;
  border: 0;
  background: #f1f5f9;
  transform-origin: 0 0;
  pointer-events: none;
}

.agent-evidence-pane {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 56px 24px 24px;
  background:
    linear-gradient(135deg, #edf7f1 0%, #f8fbf9 100%),
    repeating-linear-gradient(90deg, rgba(22, 163, 108, 0.06) 0 1px, transparent 1px 64px);
}

.agent-evidence-pane img {
  width: min(100%, 900px);
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 18px 42px rgba(20, 55, 38, 0.18);
  background: #ffffff;
}

.evidence-empty {
  display: grid;
  gap: 8px;
  justify-items: center;
  color: #7b8b82;
  font-size: 13px;
  font-weight: 700;
}

.evidence-empty i {
  font-size: 34px;
  color: #9eb0a5;
}

.agent-live-overlay {
  position: absolute;
  top: 54px;
  right: 14px;
  left: auto;
  bottom: auto;
  z-index: 2;
  width: min(360px, calc(100% - 28px));
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(10, 31, 22, 0.74);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(8px);
  pointer-events: none;
}

.frame-loading {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(15, 31, 24, 0.7);
  color: #dffbea;
  font-size: 13px;
  font-weight: 800;
}

.frame-loading span {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(223, 251, 234, 0.36);
  border-top-color: #8df6b2;
  border-radius: 50%;
  animation: frame-spin 0.8s linear infinite;
}

@keyframes frame-spin {
  to { transform: rotate(360deg); }
}

.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(112, 240, 173, 0.9), transparent);
  animation: agent-scan 2.2s linear infinite;
  z-index: 2;
}

@keyframes agent-scan {
  0% { top: 44px; opacity: 0.2; }
  45% { opacity: 1; }
  100% { top: calc(100% - 36px); opacity: 0.2; }
}

.stage-chip,
.browser-stage h3,
.browser-stage p,
.stage-metrics {
  position: relative;
  z-index: 1;
}

.stage-chip {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(112, 240, 173, 0.16);
  color: #9cf7bd;
  font-size: 11px;
  font-weight: 800;
}

.browser-stage h3 {
  margin: 0 0 6px;
  color: #ffffff;
  font-size: 16px;
  line-height: 1.25;
  font-weight: 800;
}

.browser-stage p {
  max-width: 100%;
  margin: 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 12px;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.stage-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.stage-metrics span {
  padding: 4px 7px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.82);
  font-size: 11px;
  font-weight: 700;
}

.agent-side {
  padding: 20px;
}

.agent-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #25382e;
  font-size: 14px;
  font-weight: 800;
}

.agent-progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf3ef;
}

.agent-progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #16a36c, #64d68e);
  transition: width 0.35s ease;
}

.agent-steps {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.agent-step {
  display: flex;
  gap: 10px;
  min-height: 48px;
  padding: 10px;
  border-radius: 8px;
  background: #f7faf8;
  color: #738279;
}

.agent-step.active {
  background: #ecfdf3;
  color: #103a28;
  box-shadow: inset 0 0 0 1px rgba(22, 163, 108, 0.18);
}

.agent-step.done {
  color: #2f6d4f;
}

.step-index {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #e6eee9;
  color: #587164;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
}

.agent-step.active .step-index,
.agent-step.done .step-index {
  background: #16a36c;
  color: #ffffff;
}

.step-title {
  color: inherit;
  font-size: 13px;
  font-weight: 800;
}

.step-desc {
  margin-top: 3px;
  color: #8a9890;
  font-size: 12px;
}

.agent-trace {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #edf2ee;
}

.trace-title {
  margin-bottom: 10px;
  color: #26382f;
  font-size: 13px;
  font-weight: 800;
}

.trace-row {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  margin-bottom: 8px;
}

.trace-row span {
  color: #9aa79f;
  font-size: 11px;
  font-weight: 700;
}

.trace-row p {
  margin: 0;
  color: #53665b;
  font-size: 12px;
  line-height: 1.45;
}

.trace-empty {
  color: #9aa79f;
  font-size: 12px;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(380px, 0.8fr);
  gap: 16px;
  margin-bottom: 16px;
}

/* AI 报告 */
.ai-report-card {
  min-height: 160px;
  padding: 20px 22px;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(240, 253, 244, 0.96), rgba(255, 255, 255, 0.96)),
    radial-gradient(circle at 100% 0%, rgba(34, 197, 94, 0.12), transparent 32%);
  box-shadow: 0 14px 34px rgba(28, 45, 36, 0.08);
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: #123326;
  font-size: 15px;
  font-weight: 800;
}

.panel-title span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.ai-report-content {
  color: #24382e;
  font-size: 15px;
  line-height: 1.8;
  font-weight: 500;
}

.ai-report-empty {
  color: #8b9b91;
  font-size: 14px;
  line-height: 1.7;
}

/* 规则说明 */
.rule-panel {
  min-height: 160px;
  padding: 20px 22px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(28, 45, 36, 0.08);
}

.rule-list {
  display: grid;
  gap: 10px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f7faf8;
  color: #34453b;
  font-size: 13px;
}

.rule-item i {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  flex-shrink: 0;
}

.text-blue   { color: #2563eb; }
.text-orange { color: #ea580c; }
.text-yellow { color: #d97706; }

.rule-collapse {
  margin-top: 12px;
  border: 0;
}

.rule-collapse :deep(.el-collapse-item__header) {
  height: 34px;
  border: 0;
  color: #6b7d72;
  font-size: 13px;
  background: transparent;
}

.rule-collapse :deep(.el-collapse-item__wrap) {
  border: 0;
  background: transparent;
}

.rule-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.rule-tip {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f6f8f7;
  font-size: 12px;
  color: #7b8b82;
}

/* 日志区域 */
.log-section {
  padding: 18px 20px 22px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 14px 34px rgba(28, 45, 36, 0.08);
  min-height: 320px;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.log-title {
  color: #111827;
  font-size: 16px;
  font-weight: 800;
}

.log-subtitle {
  margin-top: 5px;
  color: #8c9a93;
  font-size: 12px;
}

.refresh-btn {
  height: 34px;
  border: 1px solid #dce6df !important;
  border-radius: 8px !important;
  background: #ffffff !important;
  color: #4d6457 !important;
  box-shadow: none !important;
}

.patrol-table {
  border-radius: 8px;
  overflow: hidden;
}

.patrol-table :deep(.el-table__header th) {
  height: 44px;
  background: #f4f8f5 !important;
  color: #476456 !important;
  font-weight: 800 !important;
  border-bottom: 1px solid #e8efe9 !important;
}

.patrol-table :deep(.el-table__body td) {
  padding: 12px 0 !important;
  border-bottom: 1px solid #eef3ef !important;
}

.patrol-table :deep(.el-table__row:hover > td) {
  background: #f8fbf9 !important;
}

.detail-text {
  color: #26362e;
  font-size: 13px;
  font-weight: 600;
}
.reason-text  {
  margin-top: 3px;
  color: #7d8a83;
  font-size: 12px;
}
.ai-report-inline {
  color: #0f7a4d;
  font-size: 13px;
  line-height: 1.6;
}
.empty-log {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
}
.empty-log i { font-size: 36px; display: block; margin-bottom: 8px; }

@media (max-width: 1100px) {
  .stats-row :deep(.el-col) {
    width: 50%;
    max-width: 50%;
    flex: 0 0 50%;
    margin-bottom: 16px;
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }

  .agent-console {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .patrol-page {
    padding: 16px;
  }

  .command-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 24px;
  }

  .command-hero h1 {
    font-size: 24px;
  }

  .hero-actions {
    justify-content: flex-start;
    min-width: 0;
  }

  .stats-row :deep(.el-col) {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
  }

  .stat-card {
    height: auto;
  }

  .preview-grid,
  .preview-hero {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .live-browser-stage {
    height: 360px;
    min-height: 360px;
  }

  .agent-live-overlay {
    display: none;
  }

  .agent-frame-tabs {
    left: 10px;
    right: 10px;
    width: calc(100% - 20px);
    flex-wrap: wrap;
  }
}
</style>

<style>
.row-ai td { background: #f0fdf4 !important; }
.row-failed td { background: #fff1f2 !important; }
.row-normal td { color: #9ca3af; }
</style>
