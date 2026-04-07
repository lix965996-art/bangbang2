<template>
  <div class="vision-page">
    <section class="console-shell">
      <header class="console-hero">
        <div class="hero-copy">
          <div class="hero-kicker-row">
            <span class="hero-kicker">Agriculture AI Vision</span>
            <span :class="['hero-service', serviceOnline ? 'online' : 'offline']">
              <span class="service-dot"></span>
              {{ serviceOnline ? '模型在线' : '服务离线' }}
            </span>
          </div>

          <h1>果实识别控制台</h1>
          <p class="hero-summary">
            {{ serviceOnline ? '围绕上传、识别、判读和研判写入构建的一体化巡检台。' : 'TomatoDetection 未连接，当前仅可预览画面。' }}
          </p>

          <div class="hero-badges">
            <span class="hero-badge">{{ modeLabel }}</span>
            <span class="hero-badge">{{ detectModeLabel }}</span>
            <span class="hero-badge">{{ selectedFarm ? selectedFarm.farm : '未绑定地块' }}</span>
          </div>
        </div>

        <div class="hero-visual">
          <div class="hero-screen">
            <div class="hero-screen-top">
              <span class="screen-tag">AI 联合识别</span>
              <span class="screen-sub">{{ serviceNote }}</span>
            </div>
            <div class="hero-screen-bottom">
              <div class="screen-metric">
                <span>Crop</span>
                <strong>{{ currentCropLabel }}</strong>
              </div>
              <div class="screen-metric">
                <span>Mode</span>
                <strong>{{ detectMode === 'both' ? 'Dual' : detectMode === 'disease' ? 'Disease' : 'Ripeness' }}</strong>
              </div>
              <div class="screen-metric">
                <span>Status</span>
                <strong>{{ stageStatusText }}</strong>
              </div>
            </div>
          </div>

          <div class="hero-actions">
            <div class="mode-tabs">
              <button
                v-for="item in modeOptions"
                :key="item.value"
                type="button"
                :class="{ active: mode === item.value }"
                @click="mode = item.value"
              >
                {{ item.label }}
              </button>
            </div>

            <div class="hero-ops">
              <el-button class="ghost-btn" size="small" @click="fetchServiceStatus">
                刷新服务
              </el-button>
              <el-button class="primary-btn" size="small" :loading="loading" @click="analyzeCurrentFrame">
                开始识别
              </el-button>
            </div>
          </div>
        </div>
      </header>

      <section class="workspace-layout">
        <article class="workbench-card">
          <div class="workbench-head">
            <div class="title-block">
              <span class="section-kicker">Workspace</span>
              <h2>主任务画布</h2>
            </div>
            <div class="head-status">
              <span class="status-pill">{{ stageStatusText }}</span>
              <span class="status-note">{{ stageModeHint }}</span>
            </div>
          </div>

          <div class="control-strip">
            <el-select v-model="detectMode" size="small" class="control-select">
              <el-option
                v-for="item in detectModeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>

            <el-select
              v-model="cropType"
              size="small"
              class="control-select"
              :disabled="detectMode === 'ripeness'"
            >
              <el-option
                v-for="item in availableCropOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>

            <el-select
              v-model="selectedFarmId"
              size="small"
              class="control-select"
              placeholder="选择地块"
            >
              <el-option
                v-for="farm in farms"
                :key="farm.id"
                :label="farm.farm"
                :value="farm.id"
              />
            </el-select>
          </div>

          <div class="stage-layout">
            <div class="stage-shell" :class="{ processing: loading, dragging: dragActive }">
              <div class="hud-corner top-left"></div>
              <div class="hud-corner top-right"></div>
              <div class="hud-corner bottom-left"></div>
              <div class="hud-corner bottom-right"></div>

              <div v-if="mode === 'upload'" class="stage-block">
                <label
                  class="upload-drop"
                  @dragover.prevent="dragActive = true"
                  @dragleave.prevent="dragActive = false"
                  @drop.prevent="handleDrop"
                >
                  <input type="file" accept="image/*" @change="handleImageChange" />

                  <div v-if="!stageImageSrc" class="empty-preview upload-empty">
                    <div class="empty-copy">
                      <span class="empty-kicker">TomatoDetection</span>
                      <strong>拖拽或点击上传样本</strong>
                      <p>完成上传后直接进入 AI 双检流程</p>
                      <div class="empty-tags">
                        <span>图像巡检</span>
                        <span>智能判读</span>
                        <span>结果入库</span>
                      </div>
                    </div>
                    <div class="empty-reference">
                      <span>成熟样本参考</span>
                    </div>
                  </div>

                  <div v-else class="media-frame">
                    <img :src="stageImageSrc" :alt="stageCaptionTitle" />
                    <div class="media-overlay">
                      <div class="overlay-copy">
                        <span>{{ stageCaptionLabel }}</span>
                        <strong>{{ selectedFarm ? selectedFarm.farm : detectModeLabel }}</strong>
                      </div>
                      <span class="overlay-pill">{{ detectModeLabel }}</span>
                    </div>
                  </div>
                </label>
              </div>

              <div v-else-if="mode === 'video'" class="stage-block">
                <label v-if="!videoUrl" class="upload-drop upload-drop-muted">
                  <input type="file" accept="video/*" @change="handleVideoChange" />
                  <div class="empty-preview compact-empty">
                    <i class="el-icon-video-camera"></i>
                    <strong>导入视频</strong>
                    <p>抽取关键帧后转入图片识别</p>
                  </div>
                </label>

                <div v-else class="video-box">
                  <video ref="videoPlayer" :src="videoUrl" controls preload="metadata"></video>
                  <div class="media-actions">
                    <el-button class="primary-btn" size="small" @click="extractVideoFrame">
                      抽帧
                    </el-button>
                    <el-button class="ghost-btn" size="small" @click="clearVideo">
                      重选
                    </el-button>
                  </div>
                </div>
              </div>

              <div v-else class="stage-block">
                <div class="stream-toolbar">
                  <input
                    v-model.trim="mjpegUrl"
                    type="text"
                    class="stream-input"
                    placeholder="输入 MJPEG 摄像头地址"
                  />
                  <el-button class="primary-btn" size="small" @click="saveCameraUrl">
                    保存
                  </el-button>
                </div>

                <div class="stream-box">
                  <img
                    v-if="streamActive"
                    :src="mjpegUrl"
                    alt="camera stream"
                    @error="handleStreamError"
                  />
                  <div v-else class="empty-preview compact-empty">
                    <i class="el-icon-video-camera-solid"></i>
                    <strong>接入实时画面</strong>
                    <p>连接成功后即可抓拍识别</p>
                  </div>
                </div>

                <div class="media-actions">
                  <el-button class="primary-btn" size="small" @click="startStream">
                    启动
                  </el-button>
                  <el-button class="ghost-btn" size="small" @click="captureFrame">
                    抓拍
                  </el-button>
                  <el-button class="danger-btn" size="small" @click="stopStream">
                    停止
                  </el-button>
                </div>
              </div>

              <div v-if="loading" class="scan-overlay">
                <div class="scan-badge">AI 识别中</div>
              </div>
            </div>

            <aside class="stage-rail">
              <div class="rail-card primary">
                <span class="rail-label">当前状态</span>
                <strong>{{ stageStatusText }}</strong>
                <p>{{ stageModeHint }}</p>
              </div>

              <div class="rail-card">
                <span class="rail-label">任务配置</span>
                <strong>{{ detectModeLabel }}</strong>
                <p>{{ currentCropLabel }} · {{ selectedFarm ? selectedFarm.farm : '未绑定地块' }}</p>
              </div>

              <div v-if="showStageToggle" class="rail-card">
                <span class="rail-label">画面切换</span>
                <div class="mode-tabs compact full-width-tabs">
                  <button
                    type="button"
                    :class="{ active: stageView === 'annotated' }"
                    @click="stageView = 'annotated'"
                  >
                    结果
                  </button>
                  <button
                    type="button"
                    :class="{ active: stageView === 'raw' }"
                    @click="stageView = 'raw'"
                  >
                    原图
                  </button>
                </div>
              </div>

              <div class="rail-card flow-card">
                <span class="rail-label">任务流程</span>
                <div class="flow-list">
                  <div class="flow-item" :class="{ active: inputReady || videoUrl || streamActive }">上传样本</div>
                  <div class="flow-item" :class="{ active: loading || !!analysisResult }">AI 识别</div>
                  <div class="flow-item" :class="{ active: !!analysisResult }">查看结果</div>
                  <div class="flow-item" :class="{ active: canCreateAlert }">写入研判</div>
                </div>
              </div>
            </aside>
          </div>

          <div class="telemetry-strip">
            <div v-for="item in telemetryCards" :key="item.label" class="telemetry-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </article>

        <aside class="analysis-dock">
          <section :class="['analysis-hero', analysisSummary.level]">
            <div class="analysis-hero-top">
              <div class="analysis-copy">
                <span class="section-kicker">Analysis</span>
                <h2>{{ analysisSummary.mainLabel }}</h2>
                <p>{{ analysisSummary.message }}</p>
              </div>
              <div class="confidence-ring" :style="confidenceRingStyle">
                <div class="confidence-core">
                  <span>置信度</span>
                  <strong>{{ analysisSummary.confidence }}</strong>
                </div>
              </div>
            </div>
            <div class="analysis-tags">
              <span class="analysis-tag strong">{{ analysisSummary.levelLabel }}</span>
              <span class="analysis-tag">{{ detectModeLabel }}</span>
              <span class="analysis-tag">{{ selectedFarm ? selectedFarm.farm : '未绑定地块' }}</span>
            </div>
          </section>

          <section class="metric-grid">
            <div v-for="item in summaryMeta" :key="item.label" class="metric-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>

          <section v-if="ripenessStats.total" class="analysis-module">
            <div class="section-head">
              <strong>成熟度趋势</strong>
              <span>{{ ripenessStats.riped_ratio || 0 }}%</span>
            </div>
            <div class="progress-track">
              <div class="progress-value" :style="{ width: ripenessRatioWidth }"></div>
            </div>
            <div class="progress-meta">
              <span class="tiny-pill success">成熟 {{ ripenessStats.riped_count || 0 }}</span>
              <span class="tiny-pill warn">未熟 {{ ripenessStats.unriped_count || 0 }}</span>
            </div>
          </section>

          <section class="analysis-module">
            <div class="section-head">
              <strong>{{ analysisSummary.actionLabel }}</strong>
              <span>{{ compactSuggestions.length }} 条</span>
            </div>
            <div class="suggestion-pills">
              <span
                v-for="item in compactSuggestions"
                :key="item"
                class="suggestion-chip"
              >
                {{ item }}
              </span>
            </div>
          </section>

          <div class="analysis-actions">
            <el-button class="primary-btn" size="small" :disabled="!canCreateAlert" @click="pushToAlertCenter">
              写入研判中心
            </el-button>
            <el-button class="ghost-btn" size="small" @click="$router.push('/business-analysis')">
              查看研判中心
            </el-button>
          </div>

          <section class="analysis-module object-module">
            <div class="section-head">
              <strong>识别对象</strong>
              <span>{{ allDetections.length }} 项</span>
            </div>
            <div v-if="allDetections.length" class="objects-list">
              <div v-for="(item, index) in allDetections" :key="index" class="object-row">
                <div class="object-copy">
                  <strong>{{ displayDetectionName(item) }}</strong>
                  <span>{{ formatBbox(item.bbox) }}</span>
                </div>
                <span class="confidence-pill">{{ formatConfidence(item.confidence) }}</span>
              </div>
            </div>
            <div v-else class="empty-mini">等待识别结果</div>
          </section>
        </aside>
      </section>
    </section>
  </div>
</template>

<script>
const MODE_OPTIONS = [
  { value: 'upload', label: '图片' },
  { value: 'video', label: '视频' },
  { value: 'stream', label: '摄像头' }
];

const DETECT_MODE_OPTIONS = [
  { value: 'both', label: '双检' },
  { value: 'disease', label: '病虫害' },
  { value: 'ripeness', label: '成熟度' }
];

const CROP_OPTIONS = [
  { value: 'tomato', label: '番茄' },
  { value: 'corn', label: '玉米' },
  { value: 'strawberry', label: '草莓' },
  { value: 'rice', label: '水稻' }
];

const MODE_LABELS = {
  upload: '图片上传',
  video: '视频抽帧',
  stream: '摄像头巡检'
};

const DETECT_MODE_LABELS = {
  both: '成熟度 + 病虫害',
  disease: '病虫害识别',
  ripeness: '成熟度分析'
};

export default {
  name: 'FruitDetect',
  data() {
    return {
      mode: 'upload',
      detectMode: 'both',
      cropType: 'tomato',
      stageView: 'annotated',
      mjpegUrl: localStorage.getItem('camera_stream_url') || '',
      streamActive: false,
      imagePreview: '',
      videoUrl: '',
      selectedFile: null,
      loading: false,
      analysisResult: null,
      farms: [],
      selectedFarmId: null,
      serviceOnline: false,
      serviceHintText: '正在检查服务',
      supportedCrops: [],
      dragActive: false,
      healthTimer: null
    };
  },
  computed: {
    modeOptions() {
      return MODE_OPTIONS;
    },
    detectModeOptions() {
      return DETECT_MODE_OPTIONS;
    },
    modeLabel() {
      return MODE_LABELS[this.mode] || '检测输入';
    },
    detectModeLabel() {
      return DETECT_MODE_LABELS[this.detectMode] || 'AI 识别';
    },
    currentCropLabel() {
      const current = CROP_OPTIONS.find(item => item.value === this.cropType);
      return current ? current.label : '未设置';
    },
    serviceNote() {
      return this.serviceOnline ? `支持 ${this.availableCropLabels}` : this.serviceHintText;
    },
    selectedFarm() {
      return this.farms.find(item => item.id === this.selectedFarmId) || null;
    },
    availableCropOptions() {
      if (!this.supportedCrops.length) {
        return CROP_OPTIONS;
      }
      return CROP_OPTIONS.filter(item => this.supportedCrops.includes(item.value));
    },
    availableCropLabels() {
      return this.availableCropOptions.map(item => item.label).join(' / ');
    },
    resultImageSrc() {
      if (!this.analysisResult) {
        return '';
      }
      if (this.analysisResult.result_image) {
        return this.analysisResult.result_image;
      }
      if (this.analysisResult.disease_analysis && this.analysisResult.disease_analysis.result_image) {
        return this.analysisResult.disease_analysis.result_image;
      }
      if (this.analysisResult.ripeness_analysis && this.analysisResult.ripeness_analysis.result_image) {
        return this.analysisResult.ripeness_analysis.result_image;
      }
      return '';
    },
    stageImageSrc() {
      if (this.stageView === 'raw' || !this.resultImageSrc) {
        return this.imagePreview;
      }
      return this.resultImageSrc;
    },
    showStageToggle() {
      return !!this.imagePreview && !!this.resultImageSrc;
    },
    stageCaptionLabel() {
      if (!this.stageImageSrc) {
        return '';
      }
      return this.stageView === 'raw' || !this.resultImageSrc ? '原图' : '识别结果';
    },
    stageCaptionTitle() {
      return this.selectedFarm ? this.selectedFarm.farm : '当前任务';
    },
    inputReady() {
      return !!this.selectedFile;
    },
    stageStatusText() {
      if (this.loading) {
        return '识别中';
      }
      if (this.analysisResult) {
        return '结果已生成';
      }
      if (this.inputReady || this.videoUrl || this.streamActive) {
        return '画面已就绪';
      }
      return '等待输入';
    },
    stageModeHint() {
      if (this.mode === 'upload') {
        return this.inputReady ? '样本已就绪，可直接发起识别' : '上传图片后直接进入识别流程';
      }
      if (this.mode === 'video') {
        return this.videoUrl ? '先抽取关键帧，再转入图片识别' : '导入视频后抽取关键帧';
      }
      return this.streamActive ? '当前可抓拍并写入识别流程' : '连接摄像头后启动巡检';
    },
    ripenessDetections() {
      if (!this.analysisResult) {
        return [];
      }
      if (Array.isArray(this.analysisResult.detections) && this.detectMode === 'ripeness') {
        return this.analysisResult.detections.map(item => Object.assign({}, item, { source: '成熟度' }));
      }
      if (this.analysisResult.ripeness_analysis && Array.isArray(this.analysisResult.ripeness_analysis.detections)) {
        return this.analysisResult.ripeness_analysis.detections.map(item => Object.assign({}, item, { source: '成熟度' }));
      }
      return [];
    },
    diseaseDetections() {
      if (!this.analysisResult) {
        return [];
      }
      if (Array.isArray(this.analysisResult.detections) && this.detectMode === 'disease') {
        return this.analysisResult.detections.map(item => Object.assign({}, item, { source: '病虫害' }));
      }
      if (this.analysisResult.disease_analysis && Array.isArray(this.analysisResult.disease_analysis.detections)) {
        return this.analysisResult.disease_analysis.detections.map(item => Object.assign({}, item, { source: '病虫害' }));
      }
      return [];
    },
    allDetections() {
      return this.detectMode === 'ripeness'
        ? this.ripenessDetections
        : this.ripenessDetections.concat(this.diseaseDetections);
    },
    ripenessStats() {
      if (!this.analysisResult) {
        return {};
      }
      if (this.detectMode === 'ripeness') {
        return this.analysisResult.statistics || {};
      }
      return (this.analysisResult.ripeness_analysis && this.analysisResult.ripeness_analysis.statistics) || {};
    },
    highestConfidenceValue() {
      const values = this.allDetections
        .map(item => this.normalizeConfidence(item.confidence))
        .filter(item => item !== null);
      if (!values.length) {
        return 0;
      }
      return Math.round(Math.max.apply(null, values) * 100);
    },
    summaryMeta() {
      return [
        { label: '对象数量', value: String(this.allDetections.length) },
        { label: '检测时间', value: this.analysisSummary.detectTime },
        { label: '成熟占比', value: this.ripenessStats.total ? `${this.ripenessStats.riped_ratio || 0}%` : '--' },
        { label: '关联地块', value: this.selectedFarm ? this.selectedFarm.farm : '未绑定' }
      ];
    },
    telemetryCards() {
      return [
        { label: '输入源', value: this.modeLabel },
        { label: '识别模式', value: this.detectModeLabel },
        { label: '作物类型', value: this.currentCropLabel },
        { label: '服务状态', value: this.serviceOnline ? '在线' : '离线' }
      ];
    },
    confidenceRingStyle() {
      const degree = Math.max(0, Math.min(this.analysisSummary.confidenceValue, 100)) * 3.6;
      const colorMap = {
        steady: '#1f8a70',
        medium: '#b77934',
        high: '#bf5555'
      };
      const color = colorMap[this.analysisSummary.level] || colorMap.steady;
      return {
        background: `conic-gradient(${color} 0deg ${degree}deg, rgba(209, 220, 214, 0.5) ${degree}deg 360deg)`
      };
    },
    ripenessRatioWidth() {
      const ratio = this.ripenessStats.riped_ratio || 0;
      return `${Math.max(0, Math.min(Number(ratio), 100))}%`;
    },
    compactSuggestions() {
      return (this.analysisSummary.suggestions || []).slice(0, 3);
    },
    analysisSummary() {
      if (!this.analysisResult) {
        return {
          level: 'steady',
          levelLabel: '待识别',
          mainLabel: '等待画面',
          confidence: '--',
          confidenceValue: 0,
          detectTime: '--',
          actionLabel: '准备阶段',
          message: '上传图片或抓拍画面后即可开始识别。',
          suggestions: [
            '先上传样本',
            '绑定识别地块',
            '确认服务在线'
          ]
        };
      }

      const detectTime = this.analysisResult.detect_time || '--';
      const riskyDetections = this.diseaseDetections.filter(item => {
        const name = String(item.class_name || '').toLowerCase();
        return name.indexOf('healthy') === -1 && name.indexOf('health') === -1;
      }).sort((a, b) => {
        const aValue = this.normalizeConfidence(a.confidence) || 0;
        const bValue = this.normalizeConfidence(b.confidence) || 0;
        return bValue - aValue;
      });

      if (riskyDetections.length) {
        const main = riskyDetections[0];
        const confidenceValue = Math.round((this.normalizeConfidence(main.confidence) || 0) * 100);
        const highRisk = confidenceValue >= 80 || riskyDetections.length >= 3;
        return {
          level: highRisk ? 'high' : 'medium',
          levelLabel: highRisk ? '高风险' : '需关注',
          mainLabel: this.displayDetectionName(main),
          confidence: `${confidenceValue}%`,
          confidenceValue,
          detectTime,
          actionLabel: highRisk ? '尽快复核' : '持续观察',
          message: `发现疑似 ${this.displayDetectionName(main)}，建议结合现场情况进行复核。`,
          suggestions: [
            '写入研判中心',
            '安排现场复核',
            '结合环境数据核查'
          ]
        };
      }

      if (this.ripenessStats.total) {
        const ratio = Number(this.ripenessStats.riped_ratio || 0);
        if (ratio >= 75) {
          return {
            level: 'steady',
            levelLabel: '可采收',
            mainLabel: '成熟度较高',
            confidence: `${this.highestConfidenceValue || ratio}%`,
            confidenceValue: this.highestConfidenceValue || ratio,
            detectTime,
            actionLabel: '安排采收',
            message: '当前样本已接近采收窗口，建议安排采收作业。',
            suggestions: [
              '优先采收成熟果',
              '同步记录采收窗口',
              '继续跟踪未熟果'
            ]
          };
        }
        if (ratio >= 40) {
          return {
            level: 'medium',
            levelLabel: '转色期',
            mainLabel: '成熟度上升中',
            confidence: `${this.highestConfidenceValue || ratio}%`,
            confidenceValue: this.highestConfidenceValue || ratio,
            detectTime,
            actionLabel: '继续巡检',
            message: '当前样本处于转色期，建议持续跟踪长势变化。',
            suggestions: [
              '按天巡检',
              '提前安排农事节奏',
              '留意病斑变化'
            ]
          };
        }

        return {
          level: 'steady',
          levelLabel: '生长期',
          mainLabel: '仍在生长',
          confidence: `${this.highestConfidenceValue || ratio}%`,
          confidenceValue: this.highestConfidenceValue || ratio,
          detectTime,
          actionLabel: '维持养护',
          message: '整体成熟度仍在提升，当前未见明显异常。',
          suggestions: [
            '维持水肥节奏',
            '继续积累样本',
            '关注转色情况'
          ]
        };
      }

      return {
        level: 'steady',
        levelLabel: '稳定',
        mainLabel: '未见明显异常',
        confidence: `${this.highestConfidenceValue || 0}%`,
        confidenceValue: this.highestConfidenceValue,
        detectTime,
        actionLabel: '保持巡检',
        message: '当前画面未发现明显风险，建议保持巡检频率。',
        suggestions: [
          '继续积累样本',
          '必要时复拍',
          '持续观察长势'
        ]
      };
    },
    canCreateAlert() {
      return !!this.analysisResult && !!this.selectedFarm;
    }
  },
  mounted() {
    this.initializePage();
  },
  beforeDestroy() {
    this.revokePreview();
    this.revokeVideo();
    if (this.healthTimer) {
      clearInterval(this.healthTimer);
    }
  },
  methods: {
    async initializePage() {
      await Promise.allSettled([
        this.fetchFarms(),
        this.fetchServiceStatus()
      ]);
      this.healthTimer = setInterval(() => {
        this.fetchServiceStatus();
      }, 30000);
    },
    normalizeConfidence(value) {
      if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return null;
      }
      const num = Number(value);
      if (num <= 1) {
        return Math.max(0, Math.min(num, 1));
      }
      return Math.max(0, Math.min(num / 100, 1));
    },
    formatConfidence(value) {
      const normalized = this.normalizeConfidence(value);
      return normalized === null ? '--' : `${Math.round(normalized * 100)}%`;
    },
    formatBbox(bbox) {
      if (!bbox) {
        return '无坐标';
      }
      return `(${bbox.x1}, ${bbox.y1}) - (${bbox.x2}, ${bbox.y2})`;
    },
    displayDetectionName(item) {
      return item.class_name_ch || item.class_name || '未命名对象';
    },
    revokePreview() {
      if (this.imagePreview) {
        URL.revokeObjectURL(this.imagePreview);
        this.imagePreview = '';
      }
    },
    revokeVideo() {
      if (this.videoUrl) {
        URL.revokeObjectURL(this.videoUrl);
        this.videoUrl = '';
      }
    },
    async fetchFarms() {
      const res = await this.request.get('/statistic/page', {
        params: {
          pageNum: 1,
          pageSize: 1000
        }
      });
      if (res.code === '200' && res.data) {
        this.farms = res.data.records || [];
        if (this.farms.length && !this.selectedFarmId) {
          this.selectedFarmId = this.farms[0].id;
        }
      }
    },
    async fetchServiceStatus() {
      try {
        const [healthRes, modelsRes] = await Promise.all([
          this.request.get('/crop-analysis/health'),
          this.request.get('/crop-analysis/models')
        ]);

        this.serviceOnline = healthRes.code === '200';
        this.serviceHintText = this.serviceOnline
          ? '成熟度与病虫害模型可用'
          : (healthRes.msg || '模型服务未启动');

        if (modelsRes.code === '200' && modelsRes.data && modelsRes.data.disease_models) {
          this.supportedCrops = Object.keys(modelsRes.data.disease_models);
        } else if (healthRes.code === '200' && healthRes.data && Array.isArray(healthRes.data.supported_crops)) {
          this.supportedCrops = healthRes.data.supported_crops;
        } else {
          this.supportedCrops = [];
        }

        const available = this.supportedCrops.length
          ? CROP_OPTIONS.filter(item => this.supportedCrops.includes(item.value))
          : CROP_OPTIONS;
        if (available.length && !available.find(item => item.value === this.cropType)) {
          this.cropType = available[0].value;
        }
      } catch (error) {
        this.serviceOnline = false;
        this.serviceHintText = '无法连接 TomatoDetection 服务';
        this.supportedCrops = [];
      }
    },
    saveCameraUrl() {
      if (!this.mjpegUrl) {
        this.$message.warning('请输入摄像头地址');
        return;
      }
      localStorage.setItem('camera_stream_url', this.mjpegUrl);
      this.$message.success('摄像头地址已保存');
    },
    startStream() {
      if (!this.mjpegUrl) {
        this.$message.warning('请输入摄像头地址');
        return;
      }
      this.streamActive = true;
    },
    stopStream() {
      this.streamActive = false;
    },
    handleStreamError() {
      this.streamActive = false;
      this.$message.error('摄像头画面加载失败，请检查地址');
    },
    setImageFile(file) {
      if (!file) {
        return;
      }
      if (!file.type || file.type.indexOf('image/') !== 0) {
        this.$message.warning('请上传图片文件');
        return;
      }
      this.revokePreview();
      this.selectedFile = file;
      this.imagePreview = URL.createObjectURL(file);
      this.analysisResult = null;
      this.stageView = 'raw';
    },
    handleDrop(event) {
      this.dragActive = false;
      const file = event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0];
      this.setImageFile(file);
    },
    handleImageChange(event) {
      const file = event.target.files && event.target.files[0];
      this.setImageFile(file);
      event.target.value = '';
    },
    handleVideoChange(event) {
      const file = event.target.files && event.target.files[0];
      if (!file) {
        return;
      }
      if (!file.type || file.type.indexOf('video/') !== 0) {
        this.$message.warning('请上传视频文件');
        event.target.value = '';
        return;
      }
      this.revokePreview();
      this.revokeVideo();
      this.analysisResult = null;
      this.selectedFile = null;
      this.videoUrl = URL.createObjectURL(file);
      event.target.value = '';
    },
    clearVideo() {
      this.revokeVideo();
      this.selectedFile = null;
      this.analysisResult = null;
    },
    extractVideoFrame() {
      const video = this.$refs.videoPlayer;
      if (!video) {
        return;
      }
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth || 1280;
      canvas.height = video.videoHeight || 720;
      canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
      canvas.toBlob(blob => {
        if (!blob) {
          return;
        }
        const file = new File([blob], `video-frame-${Date.now()}.jpg`, {
          type: 'image/jpeg'
        });
        this.setImageFile(file);
        this.mode = 'upload';
        this.$message.success('已抽取当前帧');
      }, 'image/jpeg', 0.95);
    },
    captureFrame() {
      const streamImage = this.$el.querySelector('.stream-box img');
      if (!streamImage) {
        this.$message.warning('请先启动摄像头巡检');
        return;
      }
      const canvas = document.createElement('canvas');
      canvas.width = streamImage.naturalWidth || 1280;
      canvas.height = streamImage.naturalHeight || 720;
      canvas.getContext('2d').drawImage(streamImage, 0, 0, canvas.width, canvas.height);
      canvas.toBlob(blob => {
        if (!blob) {
          return;
        }
        const file = new File([blob], `stream-capture-${Date.now()}.jpg`, {
          type: 'image/jpeg'
        });
        this.setImageFile(file);
        this.mode = 'upload';
        this.$message.success('已抓拍当前画面');
      }, 'image/jpeg', 0.95);
    },
    buildEndpoint() {
      if (this.detectMode === 'ripeness') {
        return '/crop-analysis/ripeness';
      }
      if (this.detectMode === 'disease') {
        return '/crop-analysis/disease';
      }
      return '/crop-analysis/both';
    },
    async analyzeCurrentFrame() {
      if (!this.selectedFile) {
        this.$message.warning('请先准备检测图片');
        return;
      }

      this.loading = true;
      try {
        const formData = new FormData();
        formData.append('file', this.selectedFile);
        if (this.detectMode !== 'ripeness') {
          formData.append('crop_type', this.cropType);
          formData.append('conf', '0.5');
        }

        const res = await this.request.post(this.buildEndpoint(), formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });

        if (res.code !== '200') {
          throw new Error(res.msg || '识别失败');
        }

        this.analysisResult = res.data || {};
        this.stageView = this.resultImageSrc ? 'annotated' : 'raw';
        this.$message.success('识别完成');
      } catch (error) {
        this.$message.error(error.message || '识别失败，请确认 Python 服务已启动');
      } finally {
        this.loading = false;
      }
    },
    buildAlertPayload() {
      const ripenessRatio = this.ripenessStats.riped_ratio || 0;
      const message = this.detectMode === 'ripeness'
        ? `视觉巡检显示成熟果占比约 ${ripenessRatio}%`
        : `视觉巡检识别到 ${this.analysisSummary.mainLabel}`;
      const suggestion = this.analysisSummary.suggestions[0] || '建议结合现场情况进一步复核';

      return {
        farmlandId: this.selectedFarm.id,
        farmlandName: this.selectedFarm.farm,
        alertType: 'visual',
        alertLevel: this.analysisSummary.level === 'high' ? 'high' : 'medium',
        currentValue: this.detectMode === 'ripeness' ? ripenessRatio : this.analysisSummary.confidenceValue,
        message,
        suggestion
      };
    },
    async pushToAlertCenter() {
      if (!this.canCreateAlert) {
        return;
      }
      try {
        const res = await this.request.post('/alert/manual', this.buildAlertPayload());
        if (res.code !== '200') {
          throw new Error(res.msg || '写入研判中心失败');
        }
        this.$message.success('识别结果已写入研判中心');
      } catch (error) {
        this.$message.error(error.message || '写入研判中心失败');
      }
    }
  }
};
</script>

<style scoped>
.vision-page {
  --page-bg: #f4f8f5;
  --panel-bg: rgba(255, 255, 255, 0.96);
  --panel-soft: #f8fbf9;
  --line: #d9e8de;
  --line-strong: #cfe0d5;
  --text-main: #17332c;
  --text-soft: #6d837a;
  --accent: #1f8a70;
  --accent-deep: #15584a;
  --accent-soft: #e9f7f0;
  --warn: #b9813b;
  --warn-soft: #fbf1e2;
  --danger: #c36363;
  --danger-soft: #fceeed;
  height: calc(100vh - 60px);
  padding: 18px;
  overflow: hidden;
  background:
    radial-gradient(circle at 100% 0%, rgba(31, 138, 112, 0.08), transparent 22%),
    linear-gradient(180deg, #f8fbf8 0%, var(--page-bg) 100%);
  font-family: "PingFang SC", "Microsoft YaHei", "SF Pro Display", sans-serif;
}

.console-shell {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
}

.console-hero,
.workbench-card,
.analysis-dock {
  border-radius: 26px;
  border: 1px solid var(--line);
  background: var(--panel-bg);
  box-shadow: 0 16px 42px rgba(18, 31, 28, 0.05);
}

.console-hero {
  padding: 22px 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  gap: 20px;
}

.hero-copy,
.title-block,
.analysis-copy {
  min-width: 0;
}

.hero-kicker-row,
.hero-badges,
.hero-actions,
.hero-ops,
.control-strip,
.stage-layout,
.telemetry-strip,
.analysis-actions,
.metric-grid,
.progress-meta,
.suggestion-pills,
.analysis-tags {
  display: flex;
}

.hero-kicker-row {
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.hero-kicker,
.section-kicker,
.rail-label,
.empty-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-service {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.hero-service.online {
  color: var(--accent);
  background: #edf8f2;
}

.hero-service.offline {
  color: var(--danger);
  background: var(--danger-soft);
}

.service-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.console-hero h1 {
  margin: 0;
  font-size: 38px;
  line-height: 1;
  letter-spacing: -0.05em;
  color: var(--text-main);
}

.hero-summary {
  margin: 12px 0 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-soft);
}

.hero-badges {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.hero-badge,
.analysis-tag,
.overlay-pill,
.status-pill,
.tiny-pill,
.suggestion-chip,
.confidence-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.hero-badge {
  color: #48675f;
  background: #f3f7f5;
  border: 1px solid var(--line);
}

.hero-visual {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 14px;
}

.hero-screen {
  position: relative;
  min-height: 200px;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background: linear-gradient(135deg, #163f36 0%, #10372f 100%);
}

.hero-screen::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(10, 18, 20, 0.42), rgba(10, 18, 20, 0.12)),
    url('../assets/fruit-detect/console-hero.jpg') center 30% / cover no-repeat;
  opacity: 0.38;
  transform: scale(1.02);
}

.hero-screen::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(95, 190, 160, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(95, 190, 160, 0.08) 1px, transparent 1px);
  background-size: 24px 24px;
  mix-blend-mode: screen;
}
.hero-screen-top,
.hero-screen-bottom,
.workbench-head,
.head-status,
.media-overlay,
.overlay-copy,
.stream-toolbar,
.media-actions,
.section-head,
.object-row,
.object-copy {
  display: flex;
}

.hero-screen-top,
.hero-screen-bottom,
.workbench-head,
.section-head,
.object-row,
.media-overlay {
  align-items: center;
  justify-content: space-between;
}

.hero-screen-top,
.hero-screen-bottom {
  position: relative;
  z-index: 1;
}

.hero-screen-top {
  padding: 20px 22px 0;
}

.screen-tag,
.screen-sub,
.status-note {
  display: inline-flex;
  align-items: center;
}

.screen-tag {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(172, 233, 213, 0.24);
  background: rgba(240, 255, 248, 0.12);
  color: #dcf8eb;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.screen-sub {
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(5, 12, 10, 0.22);
  color: rgba(234, 248, 240, 0.82);
  font-size: 12px;
}

.hero-screen-bottom {
  position: absolute;
  left: 22px;
  right: 22px;
  bottom: 22px;
  gap: 12px;
}

.screen-metric {
  min-width: 0;
  flex: 1;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(186, 241, 224, 0.14);
  background: rgba(7, 17, 15, 0.28);
  backdrop-filter: blur(10px);
}

.screen-metric span {
  display: block;
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(223, 244, 236, 0.62);
}

.screen-metric strong {
  display: block;
  margin-top: 8px;
  font-size: 16px;
  line-height: 1.2;
  color: #f4fff9;
}

.hero-actions {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-ops {
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.mode-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  background: #f1f6f3;
  border: 1px solid rgba(217, 232, 222, 0.92);
}

.mode-tabs button {
  border: 0;
  background: transparent;
  color: var(--text-soft);
  padding: 9px 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.mode-tabs button.active {
  background: #ffffff;
  color: var(--text-main);
  box-shadow: 0 8px 20px rgba(17, 29, 25, 0.08);
}

.mode-tabs.compact {
  width: 100%;
}

.mode-tabs.compact button {
  flex: 1;
  padding: 8px 12px;
  font-size: 12px;
}

.workspace-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.48fr) 360px;
  gap: 16px;
}

.workbench-card,
.analysis-dock {
  min-height: 0;
}

.workbench-card {
  padding: 18px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 14px;
}

.workbench-head {
  gap: 14px;
}

.title-block h2,
.analysis-copy h2 {
  margin: 10px 0 0;
  color: var(--text-main);
  letter-spacing: -0.04em;
}

.title-block h2 {
  font-size: 30px;
  line-height: 1.05;
}

.head-status {
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.status-pill {
  color: var(--accent);
  background: var(--accent-soft);
}

.status-note {
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #f4f8f6;
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 600;
}

.control-strip {
  align-items: center;
  gap: 10px;
}

.control-select {
  min-width: 0;
  flex: 1;
}

.stage-layout {
  min-height: 0;
  gap: 14px;
}

.stage-shell {
  position: relative;
  min-width: 0;
  flex: 1;
  min-height: 0;
  padding: 18px;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid var(--line);
  background:
    radial-gradient(circle at 82% 18%, rgba(62, 182, 145, 0.12), transparent 20%),
    linear-gradient(180deg, #fbfefc 0%, #f2f7f4 100%);
}

.stage-shell::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(31, 138, 112, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(31, 138, 112, 0.04) 1px, transparent 1px);
  background-size: 26px 26px;
  opacity: 0.9;
  pointer-events: none;
}

.stage-shell.processing::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(31, 138, 112, 0.12), transparent);
  transform: translateX(-100%);
  animation: scan 1.25s linear infinite;
  pointer-events: none;
}

.stage-shell.dragging {
  border-color: rgba(31, 138, 112, 0.45);
  box-shadow: inset 0 0 0 1px rgba(31, 138, 112, 0.18);
}

.hud-corner {
  position: absolute;
  width: 28px;
  height: 28px;
  border-color: rgba(31, 138, 112, 0.28);
  border-style: solid;
  pointer-events: none;
}

.hud-corner.top-left {
  top: 12px;
  left: 12px;
  border-width: 2px 0 0 2px;
  border-top-left-radius: 10px;
}

.hud-corner.top-right {
  top: 12px;
  right: 12px;
  border-width: 2px 2px 0 0;
  border-top-right-radius: 10px;
}

.hud-corner.bottom-left {
  left: 12px;
  bottom: 12px;
  border-width: 0 0 2px 2px;
  border-bottom-left-radius: 10px;
}

.hud-corner.bottom-right {
  right: 12px;
  bottom: 12px;
  border-width: 0 2px 2px 0;
  border-bottom-right-radius: 10px;
}

.stage-block,
.video-box,
.stream-box {
  position: relative;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.upload-drop,
.video-box,
.stream-box {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid rgba(207, 224, 213, 0.92);
  background: rgba(255, 255, 255, 0.84);
}

.upload-drop {
  display: flex;
  cursor: pointer;
}

.upload-drop input {
  display: none;
}

.upload-drop-muted {
  border-style: dashed;
}

.empty-preview {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-empty {
  justify-content: space-between;
  gap: 20px;
  padding: 42px;
}

.empty-copy {
  max-width: 360px;
}

.empty-copy strong,
.compact-empty strong {
  display: block;
  margin-top: 12px;
  color: var(--text-main);
  letter-spacing: -0.04em;
}

.empty-copy strong {
  font-size: 34px;
  line-height: 1.1;
}

.empty-copy p,
.compact-empty p {
  margin: 10px 0 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.empty-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.empty-tags span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #f4f8f5;
  color: #527069;
  font-size: 12px;
  font-weight: 700;
}

.empty-reference {
  width: 280px;
  height: 100%;
  min-height: 320px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(217, 232, 222, 0.92);
  background:
    linear-gradient(180deg, rgba(8, 14, 12, 0) 48%, rgba(8, 14, 12, 0.64) 100%),
    url('../assets/fruit-detect/tomato-reference.jpg') center center / cover no-repeat;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.empty-reference span {
  position: absolute;
  left: 18px;
  bottom: 16px;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(8px);
}

.compact-empty {
  flex-direction: column;
  gap: 10px;
  text-align: center;
  color: var(--text-soft);
}

.compact-empty i {
  font-size: 38px;
  color: var(--accent);
}

.compact-empty strong {
  font-size: 28px;
}

.media-frame,
.media-frame img,
.video-box video,
.stream-box img {
  width: 100%;
  height: 100%;
}

.media-frame,
.video-box,
.stream-box {
  background: #f4f8f5;
}

.media-frame {
  position: relative;
}

.media-frame img,
.video-box video,
.stream-box img {
  object-fit: contain;
}

.media-overlay {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(11, 19, 17, 0.08), rgba(11, 19, 17, 0.72));
  color: #ffffff;
}

.overlay-copy {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.overlay-copy span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.74);
}

.overlay-copy strong {
  font-size: 18px;
  line-height: 1.2;
}

.overlay-pill {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.16);
}

.stream-toolbar {
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.stream-input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  outline: 0;
  background: #ffffff;
  color: var(--text-main);
}

.stream-input::placeholder {
  color: #9ab0a7;
}

.media-actions {
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.scan-overlay {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 2;
}

.scan-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(31, 138, 112, 0.12);
}

.stage-rail {
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rail-card,
.analysis-module,
.metric-card {
  border-radius: 20px;
  border: 1px solid var(--line);
  background: #fbfdfc;
}

.rail-card {
  padding: 14px;
  display: grid;
  gap: 8px;
}

.rail-card.primary {
  background: linear-gradient(180deg, #f3fbf7 0%, #eef8f3 100%);
  border-color: rgba(164, 220, 196, 0.72);
}

.rail-card strong {
  font-size: 20px;
  line-height: 1.2;
  color: var(--text-main);
}

.rail-card p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.6;
  font-size: 13px;
}

.full-width-tabs {
  width: 100%;
}

.flow-card {
  flex: 1;
}

.flow-list {
  display: grid;
  gap: 10px;
}

.flow-item {
  position: relative;
  min-height: 42px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  border-radius: 14px;
  border: 1px solid var(--line);
  color: var(--text-soft);
  background: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.flow-item.active {
  border-color: rgba(31, 138, 112, 0.34);
  background: var(--accent-soft);
  color: var(--accent-deep);
}

.telemetry-strip {
  align-items: stretch;
  gap: 10px;
}

.telemetry-card {
  min-width: 0;
  flex: 1;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: #f7faf8;
}

.telemetry-card span,
.metric-card span,
.section-head span,
.object-copy span {
  display: block;
  font-size: 12px;
  color: var(--text-soft);
}

.telemetry-card strong,
.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 17px;
  line-height: 1.3;
  color: var(--text-main);
}

.analysis-dock {
  padding: 18px;
  display: grid;
  grid-template-rows: auto auto auto auto minmax(0, 1fr);
  gap: 12px;
}

.analysis-hero {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, #f8fbf9 0%, #f3f8f5 100%);
}

.analysis-hero.medium {
  background: linear-gradient(180deg, #fffaf2 0%, #fcf5e9 100%);
  border-color: rgba(214, 179, 123, 0.4);
}

.analysis-hero.high {
  background: linear-gradient(180deg, #fff7f7 0%, #fdefef 100%);
  border-color: rgba(212, 132, 132, 0.34);
}

.analysis-hero-top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 108px;
  gap: 14px;
  align-items: center;
}

.analysis-copy h2 {
  font-size: 28px;
  line-height: 1.08;
}

.analysis-copy p {
  margin: 12px 0 0;
  color: #48615a;
  line-height: 1.7;
  font-size: 14px;
}

.confidence-ring {
  position: relative;
  width: 108px;
  height: 108px;
  padding: 8px;
  border-radius: 50%;
}

.confidence-core {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(217, 232, 222, 0.86);
}

.confidence-core span {
  font-size: 11px;
  color: var(--text-soft);
}

.confidence-core strong {
  margin-top: 4px;
  font-size: 20px;
  line-height: 1;
  color: var(--text-main);
}

.analysis-tags {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.analysis-tag {
  color: #4b655e;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(217, 232, 222, 0.92);
}

.analysis-tag.strong {
  color: var(--accent);
  background: var(--accent-soft);
}

.metric-grid {
  gap: 10px;
  flex-wrap: wrap;
}

.metric-card {
  min-width: calc(50% - 5px);
  flex: 1 1 calc(50% - 5px);
  padding: 14px;
}

.analysis-module {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.section-head strong,
.object-copy strong {
  color: var(--text-main);
}

.section-head strong {
  font-size: 15px;
}

.progress-track {
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: #e3ece7;
}

.progress-value {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1f8a70, #58c1a3);
}

.progress-meta,
.suggestion-pills {
  gap: 8px;
  flex-wrap: wrap;
}

.tiny-pill.success {
  color: #1b7c60;
  background: #e5f5ee;
}

.tiny-pill.warn {
  color: #a77530;
  background: #fbf0df;
}

.suggestion-chip {
  color: #4f675f;
  background: #f3f8f5;
  border: 1px solid var(--line);
}

.analysis-actions {
  gap: 10px;
}

.analysis-actions .el-button {
  flex: 1;
}

.object-module {
  min-height: 0;
}

.objects-list {
  min-height: 0;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  padding-right: 2px;
}

.object-row {
  gap: 12px;
  padding: 11px 12px;
  border-radius: 16px;
  border: 1px solid #e4eee8;
  background: #ffffff;
}

.object-copy {
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.object-copy strong {
  font-size: 14px;
}

.confidence-pill {
  color: var(--accent);
  background: var(--accent-soft);
  white-space: nowrap;
}

.empty-mini {
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-soft);
  font-size: 13px;
}

@keyframes scan {
  from {
    transform: translateX(-100%);
  }

  to {
    transform: translateX(100%);
  }
}

::v-deep .el-button {
  border-radius: 14px;
  font-weight: 700;
}

::v-deep .el-button--small {
  padding: 11px 16px;
}

::v-deep .primary-btn,
::v-deep .el-button--primary {
  border-color: #1f8a70;
  background: #1f8a70;
  color: #ffffff;
  box-shadow: 0 10px 22px rgba(31, 138, 112, 0.16);
}

::v-deep .primary-btn:hover,
::v-deep .primary-btn:focus,
::v-deep .el-button--primary:hover,
::v-deep .el-button--primary:focus {
  border-color: #176f5b;
  background: #176f5b;
}

::v-deep .ghost-btn {
  color: #49645d;
  border-color: var(--line);
  background: #ffffff;
}

::v-deep .danger-btn {
  color: #b25151;
  border-color: #f1d3d3;
  background: #fff8f8;
}

::v-deep .el-input__inner {
  height: 40px;
  line-height: 40px;
  border-radius: 14px;
  border-color: var(--line);
  color: var(--text-main);
  background: #ffffff;
}

::v-deep .el-input__inner:focus {
  border-color: rgba(31, 138, 112, 0.46);
}

::v-deep .el-select .el-input .el-select__caret,
::v-deep .el-input__suffix {
  color: #8ba198;
}

@media screen and (max-width: 1480px) {
  .console-hero {
    grid-template-columns: 1fr;
  }

  .workspace-layout {
    grid-template-columns: 1fr;
  }

  .analysis-dock {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: auto auto auto;
  }

  .analysis-hero,
  .object-module {
    grid-column: 1 / -1;
  }
}

@media screen and (max-width: 1240px) {
  .vision-page {
    height: auto;
    min-height: calc(100vh - 60px);
    overflow: auto;
  }

  .stage-layout {
    flex-direction: column;
  }

  .stage-rail {
    width: 100%;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-card {
    grid-column: 1 / -1;
  }

  .upload-empty {
    padding: 32px;
  }

  .empty-reference {
    width: 240px;
    min-height: 280px;
  }
}

@media screen and (max-width: 980px) {
  .vision-page {
    padding: 12px;
  }

  .console-hero,
  .workbench-card,
  .analysis-dock {
    border-radius: 22px;
  }

  .console-hero h1 {
    font-size: 32px;
  }

  .hero-actions,
  .workbench-head,
  .head-status,
  .control-strip,
  .telemetry-strip,
  .analysis-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-ops {
    width: 100%;
    justify-content: stretch;
  }

  .hero-ops .el-button,
  .analysis-actions .el-button {
    flex: 1;
  }

  .control-select,
  .metric-card,
  .telemetry-card {
    width: 100%;
    min-width: 0;
  }

  .upload-empty {
    flex-direction: column;
    align-items: stretch;
  }

  .empty-reference {
    width: 100%;
    height: 200px;
    min-height: 200px;
  }

  .analysis-dock {
    grid-template-columns: 1fr;
  }

  .metric-grid {
    flex-direction: column;
  }
}

@media screen and (max-width: 720px) {
  .console-hero,
  .workbench-card,
  .analysis-dock {
    padding: 14px;
  }

  .hero-screen {
    min-height: 230px;
  }

  .hero-screen-bottom {
    position: static;
    padding: 0 16px 16px;
    flex-direction: column;
  }

  .screen-metric {
    width: 100%;
  }

  .stage-shell {
    min-height: 560px;
    padding: 14px;
  }

  .stage-rail {
    grid-template-columns: 1fr;
  }

  .analysis-hero-top {
    grid-template-columns: 1fr;
  }

  .confidence-ring {
    margin-left: 0;
  }
}
</style>
