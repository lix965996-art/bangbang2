<template>
  <div class="lab-page">
    <!-- 顶部标题栏 -->
    <div class="lab-header">
      <div class="header-left">
        <div class="header-icon">🔬</div>
        <div class="header-text">
          <div class="header-title">AI 农产品表型分析与质检工作站</div>
          <div class="header-subtitle">YOLOv8 Deep Learning · Real-time Inference · XAI-Enabled</div>
        </div>
      </div>
      <div class="header-right">
        <div class="info-tag">
          <span class="info-label">推理引擎</span>
          <span class="info-value">{{ inferenceEngine }}</span>
        </div>
        <div class="info-tag">
          <span class="info-label">模型版本</span>
          <span class="info-value">v{{ modelVersion }}</span>
        </div>
        <div class="detect-mode-selector">
          <label>检测模式:</label>
          <select v-model="detectType" class="lab-select">
            <option value="both">双检分析</option>
            <option value="ripeness">成熟度检测</option>
            <option value="disease">病虫害检测</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 三栏布局 -->
    <div class="lab-container">
      <!-- 左侧：样本序列 -->
      <div class="sample-queue-panel">
        <div class="panel-header">
          <h3 class="panel-title">待检样本序列</h3>
          <span class="sample-count">{{ sampleQueue.length }} 样本</span>
        </div>
        <div class="queue-list">
          <div
            v-for="sample in sampleQueue"
            :key="sample.id"
            :class="['queue-card', currentSample && currentSample.id === sample.id ? 'active' : '']"
            @click="selectSample(sample)"
          >
            <div class="card-thumbnail">
              <img v-if="sample.previewUrl" :src="sample.previewUrl" />
              <div v-else class="thumbnail-placeholder">📄</div>
            </div>
            <div class="card-info">
              <div class="sample-id">ID: {{ sample.id }}</div>
              <div class="sample-meta">{{ sample.time }}</div>
              <div :class="['status-badge', sample.status]">
                {{ sample.status === 'pending' ? '待检' : sample.status === 'analyzing' ? '分析中' : '完成' }}
              </div>
            </div>
          </div>
          <div v-if="sampleQueue.length === 0" class="empty-queue">
            <div class="empty-icon">🧪</div>
            <p>暂无样本</p>
          </div>
        </div>
      </div>

      <!-- 中间：工作台 -->
      <div class="workbench-panel">
        <!-- Tab 导航 -->
        <div class="tab-nav">
          <div
            :class="['tab-item', activeTab === 'image' ? 'active' : '']"
            @click="switchTab('image')"
          >
            <span class="tab-icon">🖼️</span>
            <span class="tab-label">图片分析</span>
          </div>
          <div
            :class="['tab-item', activeTab === 'video' ? 'active' : '']"
            @click="switchTab('video')"
          >
            <span class="tab-icon">🎬</span>
            <span class="tab-label">视频分析</span>
          </div>
          <div
            :class="['tab-item', activeTab === 'stream' ? 'active' : '']"
            @click="switchTab('stream')"
          >
            <span class="tab-icon">📡</span>
            <span class="tab-label">实时终端</span>
          </div>
        </div>

        <!-- 工作视窗 -->
        <div class="workbench-viewport">
          <!-- 图片模式 -->
          <div v-if="activeTab === 'image'" class="viewport-content">
            <div
              class="drop-area"
              :class="{ 'drag-over': dropActive }"
              @drop.prevent="onDropImage"
              @dragover.prevent="dropActive = true"
              @dragleave.prevent="dropActive = false"
            >
              <div v-if="!imagePreview" class="upload-prompt">
                <div class="prompt-icon">📤</div>
                <p class="prompt-title">拖拽图片到此区域</p>
                <p class="prompt-subtitle">或点击下方按钮上传</p>
                <input
                  ref="imageInput"
                  type="file"
                  accept="image/*"
                  style="display: none"
                  @change="handleImageUpload"
                />
                <button class="upload-btn" @click="$refs.imageInput.click()">选择图片</button>
              </div>
              <div v-else class="image-display">
                <img ref="mainImage" :src="imagePreview" @load="onImageLoad" />
                <!-- SVG 扫描线 -->
                <svg v-if="analyzing" class="scan-overlay" viewBox="0 0 100 100" preserveAspectRatio="none">
                  <line class="scan-line" x1="0" :y1="scanY" x2="100" :y2="scanY" stroke="#10b981" stroke-width="0.5" opacity="0.8" />
                </svg>
                <!-- 检测框 -->
                <svg
                  v-if="detections.length > 0"
                  class="detection-overlay"
                  :viewBox="`0 0 ${imageWidth} ${imageHeight}`"
                  preserveAspectRatio="xMidYMid meet"
                >
                  <g v-for="(det, idx) in detections" :key="idx">
                    <rect
                      :x="det.x"
                      :y="det.y"
                      :width="det.width"
                      :height="det.height"
                      :stroke="det.type === 'riped' ? '#10b981' : '#f59e0b'"
                      stroke-width="3"
                      fill="none"
                    />
                    <text :x="det.x + 5" :y="det.y + 18" fill="#10b981" font-size="14" font-weight="bold">
                      {{ det.label }} {{ det.confidence }}%
                    </text>
                  </g>
                </svg>
              </div>
            </div>
          </div>

          <!-- 视频模式 -->
          <div v-if="activeTab === 'video'" class="viewport-content">
            <div v-if="!videoSrc" class="upload-prompt">
              <div class="prompt-icon">🎥</div>
              <p class="prompt-title">上传视频文件</p>
              <input
                ref="videoInput"
                type="file"
                accept="video/*"
                style="display: none"
                @change="handleVideoUpload"
              />
              <button class="upload-btn" @click="$refs.videoInput.click()">选择视频</button>
            </div>
            <div v-else class="video-player-wrapper">
              <video ref="videoPlayer" :src="videoSrc" controls></video>
              <button class="extract-btn" @click="extractKeyFrame">📸 提取当前帧分析</button>
            </div>
          </div>

          <!-- 实时流模式 -->
          <div v-if="activeTab === 'stream'" class="viewport-content">
            <div v-if="!streamActive" class="upload-prompt">
              <div class="prompt-icon">📡</div>
              <p class="prompt-title">ESP32-CAM 实时推理</p>
              <input v-model="mjpegUrl" class="stream-url-input" placeholder="输入MJPEG流地址" />
              <button class="upload-btn" @click="startStream">🚀 启动实时终端</button>
            </div>
            <div v-else class="stream-display">
              <img :src="mjpegUrl" @error="handleStreamError" />
              <!-- 实时检测框叠加 -->
              <svg
                v-if="streamDetections.length > 0"
                class="detection-overlay"
                viewBox="0 0 640 480"
                preserveAspectRatio="xMidYMid meet"
              >
                <g v-for="(det, idx) in streamDetections" :key="idx">
                  <rect
                    :x="det.x"
                    :y="det.y"
                    :width="det.width"
                    :height="det.height"
                    stroke="#10b981"
                    stroke-width="2"
                    fill="none"
                  />
                </g>
              </svg>
              <div class="stream-controls">
                <button class="capture-btn" @click="captureFrame">📷 截图分析</button>
                <button class="stop-btn" @click="stopStream">⏹ 停止</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部控制栏 -->
        <div class="workbench-footer">
          <div class="footer-left">
            <label class="xai-toggle">
              <input type="checkbox" v-model="xaiMode" />
              <span class="toggle-label">👁️ 开启热力图 (Grad-CAM)</span>
            </label>
          </div>
          <div class="footer-right">
            <button
              v-if="imagePreview && activeTab === 'image'"
              class="analyze-btn"
              :disabled="analyzing"
              @click="analyzeImage"
            >
              {{ analyzing ? '⏳ 分析中...' : '🔍 开始分析' }}
            </button>
            <button
              v-if="imagePreview && activeTab === 'image'"
              class="clear-btn"
              @click="clearImage"
            >
              🗑️ 清除
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：诊断报告 -->
      <div class="diagnostic-panel">
        <div class="panel-header">
          <h3 class="panel-title">诊断报告</h3>
        </div>

        <div v-if="!detectResult" class="report-empty">
          <div class="empty-icon">📋</div>
          <p>等待分析...</p>
        </div>

        <div v-else class="report-content">
          <!-- 等级印章 -->
          <div class="grade-stamp">
            <div class="stamp-inner">
              <div class="grade-letter">{{ gradeLetter }}</div>
              <div class="grade-text">{{ gradeText }}</div>
            </div>
          </div>

          <!-- 性能面板 -->
          <div class="performance-panel">
            <div class="perf-item">
              <span class="perf-label">Inference:</span>
              <span class="perf-value">{{ inferenceTime || '--' }} ms</span>
            </div>
            <div class="perf-item">
              <span class="perf-label">FPS:</span>
              <span class="perf-value">{{ fps || '--' }}</span>
            </div>
            <div class="perf-item">
              <span class="perf-label">Confidence:</span>
              <span class="perf-value">{{ ripenessRatio || '--' }} %</span>
            </div>
          </div>

          <!-- 检测统计 -->
          <div class="stat-card">
            <div class="stat-title">检测统计</div>
            <div class="stat-row">
              <span>总检测数:</span>
              <span class="stat-num">{{ totalDetections }}</span>
            </div>
            <div class="stat-row">
              <span>成熟果:</span>
              <span class="stat-num stat-riped">{{ ripedCount }}</span>
            </div>
            <div class="stat-row">
              <span>未成熟:</span>
              <span class="stat-num stat-unriped">{{ unripedCount }}</span>
            </div>
          </div>

          <!-- 病害卡片 -->
          <div v-if="diseaseInfo" class="disease-card">
            <div class="disease-header">
              <span class="disease-icon">⚠️</span>
              <span class="disease-name">{{ diseaseInfo.name }}</span>
            </div>
            <div class="disease-body">
              <h4 class="advice-title">💊 防治建议</h4>
              <p class="advice-content">{{ diseaseInfo.advice }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FruitDetect',
  data() {
    return {
      inferenceEngine: 'CUDA 11.8',
      modelVersion: '8.2.0',
      detectType: 'both',
      activeTab: 'image',
      xaiMode: false,
      
      // 图片相关
      imagePreview: '',
      selectedFile: null,
      dropActive: false,
      imageWidth: 640,
      imageHeight: 480,
      detections: [],
      scanY: 0,
      
      // 视频相关
      videoSrc: '',
      
      // 实时流相关
      streamActive: false,
      mjpegUrl: 'http://192.168.137.192/mjpeg/1',
      streamDetections: [],
      
      // 分析结果
      analyzing: false,
      detectResult: null,
      inferenceTime: null,
      fps: null,
      
      // 样本队列
      sampleQueue: [],
      currentSample: null
    }
  },
  computed: {
    ripenessRatio() {
      const stats =
        this.detectResult &&
        this.detectResult.ripeness_analysis &&
        this.detectResult.ripeness_analysis.statistics
      return stats ? stats.riped_ratio : null
    },
    gradeLetter() {
      if (this.ripenessRatio === null) return '-'
      if (this.ripenessRatio >= 80) return 'A'
      if (this.ripenessRatio >= 60) return 'B'
      return 'C'
    },
    gradeText() {
      if (this.ripenessRatio === null) return '未评级'
      if (this.ripenessRatio >= 80) return '特级'
      if (this.ripenessRatio >= 60) return '一级'
      return '二级'
    },
    totalDetections() {
      const r = this.detectResult
      if (!r) return 0
      const a = r.ripeness_analysis?.detections?.length || 0
      const b = r.disease_analysis?.detections?.length || 0
      const c = r.detections?.length || 0
      return a + b || c
    },
    mainDiseaseName() {
      const d = this.getMainDisease()
      return d ? d.name : ''
    },
    diseaseAdvice() {
      const d = this.getMainDisease()
      return d ? this.getDiseaseAdvice(d.originalName) : ''
    },
    ripedCount() {
      if (!this.detectResult || !this.detectResult.ripeness_analysis) return 0
      const dets = this.detectResult.ripeness_analysis.detections || []
      return dets.filter(d => d.class_id === 0).length
    },
    unripedCount() {
      if (!this.detectResult || !this.detectResult.ripeness_analysis) return 0
      const dets = this.detectResult.ripeness_analysis.detections || []
      return dets.filter(d => d.class_id === 1).length
    },
    diseaseInfo() {
      const d = this.getMainDisease()
      if (!d) return null
      return {
        name: d.name,
        advice: this.getDiseaseAdvice(d.originalName)
      }
    }
  },
  methods: {
    triggerFileSelect() {
      if (this.$refs.fileInputRef) {
        this.$refs.fileInputRef.click()
      }
    },
    handleFileChange(event) {
      const files = event.target.files
      if (!files || !files[0]) return
      const file = files[0]
      this.loadFileAsSample(file)
    },
    onDragOver() {
      this.dropActive = true
    },
    onDragLeave() {
      this.dropActive = false
    },
    onDrop(event) {
      this.dropActive = false
      const files = event.dataTransfer && event.dataTransfer.files
      if (!files || !files[0]) return
      const file = files[0]
      this.loadFileAsSample(file)
    },
    loadFileAsSample(file) {
      if (this.previewUrl) {
        URL.revokeObjectURL(this.previewUrl)
      }

      this.selectedFile = file
      this.previewUrl = URL.createObjectURL(file)
      this.detectResult = null
      this.inferenceTime = null

      const sample = {
        id: Date.now().toString(),
        name: file.name,
        time: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit',
        }),
        status: 'pending',
      }

      this.sampleQueue.unshift(sample)
      this.currentSample = sample
    },
    selectSample(sample) {
      this.currentSample = sample
    },
    onImageLoad(event) {
      const img = event.target
      this.imageWidth = img.naturalWidth || 0
      this.imageHeight = img.naturalHeight || 0
    },
    clearImage() {
      if (this.previewUrl) {
        URL.revokeObjectURL(this.previewUrl)
      }
      this.previewUrl = ''
      this.selectedFile = null
      this.detectResult = null
      this.inferenceTime = null
      this.currentSample = null
    },
    async analyzeImage() {
      if (!this.selectedFile) {
        this.$message.warning('请先上传图片')
        return
      }

      this.analyzing = true
      this.inferenceTime = null
      if (this.currentSample) {
        this.currentSample.status = 'analyzing'
      }

      const start = performance.now()

      try {
        const formData = new FormData()
        formData.append('file', this.selectedFile)
        formData.append('crop_type', 'tomato')
        formData.append('conf', '0.5')

        const apiEndpoints = {
          both: 'http://localhost:5000/api/detect/both',
          ripeness: 'http://localhost:5000/api/detect/ripeness',
          disease: 'http://localhost:5000/api/detect/disease',
        }

        const apiUrl = apiEndpoints[this.detectType] || apiEndpoints.both

        const response = await fetch(apiUrl, {
          method: 'POST',
          body: formData,
        })

        const res = await response.json()

        if (res.code === '200') {
          let data = res.data || {}

          if (this.detectType === 'ripeness' && data.statistics) {
            data = {
              result_image: data.result_image,
              detect_time: data.detect_time,
              ripeness_analysis: {
                detections: data.detections,
                statistics: data.statistics,
              },
            }
          }

          if (
            this.detectType === 'disease' &&
            data.detections &&
            !data.disease_analysis
          ) {
            data = {
              result_image: data.result_image,
              detect_time: data.detect_time,
              disease_analysis: {
                detections: data.detections,
                statistics: data.statistics,
              },
            }
          }

          this.detectResult = data
          this.inferenceTime = Math.round(performance.now() - start)
          if (this.currentSample) {
            this.currentSample.status = 'done'
          }
          this.$message.success('分析完成')
        } else {
          this.$message.error(res.msg || '分析失败')
          if (this.currentSample) {
            this.currentSample.status = 'pending'
          }
        }
      } catch (err) {
        console.error(err)
        this.$message.error('分析请求失败，请确认后端服务已启动')
        if (this.currentSample) {
          this.currentSample.status = 'pending'
        }
      } finally {
        this.analyzing = false
      }
    },
    getMainDisease() {
      const analysis = this.detectResult && this.detectResult.disease_analysis
      if (!analysis || !analysis.detections || !analysis.detections.length) {
        return null
      }
      const diseases = analysis.detections.filter((d) => {
        const name = d.class_name || ''
        return !name.includes('Healthy') && !name.toLowerCase().includes('health')
      })
      if (!diseases.length) return null
      const sorted = diseases.slice().sort((a, b) => (b.confidence || 0) - (a.confidence || 0))
      const main = sorted[0]
      return {
        name: main.class_name_ch || main.class_name,
        originalName: main.class_name,
        confidence: main.confidence,
      }
    },
    getDiseaseAdvice(diseaseName) {
      const map = {
        'Early Blight': '建议使用代森锰锌、百菌清等杀菌剂进行喷雾防治；及时摘除病叶，减少菌源；加强通风透光，降低湿度。',
        'Late Blight': '建议选用甲霜灵·锰锌、烯酰吗啉等药剂防治；发现中心病株立即拔除；控制浇水，避免田间积水。',
        'Leaf Miner': '建议使用阿维菌素、灭蝇胺等药剂进行防治；利用黄板诱杀成虫；清洁田园，处理残枝败叶。',
        'Mosaic Virus':
          '主要通过蚜虫传播，建议使用吡虫啉防治蚜虫；发病初期喷洒病毒A、植病灵等抗病毒药剂；拔除病株，防止蔓延。',
      }
      return (
        map[diseaseName] ||
        '建议及时采取防治措施，可使用对应的杀菌剂或杀虫剂进行喷洒处理，并加强田间管理。'
      )
    },
    // Tab切换
    switchTab(tab) {
      this.activeTab = tab
    },
    // 图片处理
    onDropImage(e) {
      this.dropActive = false
      const files = e.dataTransfer?.files
      if (files && files[0]) this.handleImageFile(files[0])
    },
    handleImageUpload(e) {
      const files = e.target.files
      if (files && files[0]) this.handleImageFile(files[0])
    },
    handleImageFile(file) {
      if (this.imagePreview) URL.revokeObjectURL(this.imagePreview)
      this.selectedFile = file
      this.imagePreview = URL.createObjectURL(file)
      this.detections = []
      this.detectResult = null
      
      const sample = {
        id: `#${Date.now().toString().slice(-6)}`,
        name: file.name,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        status: 'pending',
        previewUrl: this.imagePreview
      }
      this.sampleQueue.unshift(sample)
      this.currentSample = sample
    },
    // 视频处理
    handleVideoUpload(e) {
      const file = e.target.files[0]
      if (file) this.videoSrc = URL.createObjectURL(file)
    },
    extractKeyFrame() {
      const video = this.$refs.videoPlayer
      if (!video) return
      
      const canvas = document.createElement('canvas')
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      canvas.getContext('2d').drawImage(video, 0, 0)
      
      canvas.toBlob(blob => {
        this.switchTab('image')
        this.handleImageFile(new File([blob], 'frame.jpg', { type: 'image/jpeg' }))
        this.$message.success('关键帧已提取')
      })
    },
    // 实时流
    startStream() {
      if (!this.mjpegUrl) return this.$message.warning('请输入MJPEG流地址')
      this.streamActive = true
      this.$message.success('实时终端已启动')
    },
    stopStream() {
      this.streamActive = false
      this.streamDetections = []
    },
    handleStreamError() {
      this.$message.error('视频流加载失败')
      this.streamActive = false
    },
    captureFrame() {
      const img = document.querySelector('.stream-display img')
      if (!img) return
      
      const canvas = document.createElement('canvas')
      canvas.width = 640
      canvas.height = 480
      canvas.getContext('2d').drawImage(img, 0, 0, 640, 480)
      
      canvas.toBlob(blob => {
        this.switchTab('image')
        this.handleImageFile(new File([blob], 'stream_capture.jpg', { type: 'image/jpeg' }))
        this.$message.success('截图成功')
      })
    },
    // 图片分析（含Mock数据）
    async analyzeImage() {
      if (!this.selectedFile) return this.$message.warning('请先上传图片')

      this.analyzing = true
      this.startScanAnimation()
      if (this.currentSample) this.currentSample.status = 'analyzing'

      const start = performance.now()

      try {
        const formData = new FormData()
        formData.append('file', this.selectedFile)
        formData.append('crop_type', 'tomato')
        formData.append('conf', '0.5')

        const apiUrl = `http://localhost:5000/api/detect/${this.detectType}`
        const response = await fetch(apiUrl, { method: 'POST', body: formData })
        const res = await response.json()

        if (res.code === '200') {
          this.processDetectionResult(res.data, performance.now() - start)
          this.$message.success('分析完成')
        } else {
          throw new Error(res.msg)
        }
      } catch (err) {
        console.warn('后端未连接，使用Mock数据', err)
        this.generateMockResult(performance.now() - start)
        this.$message.warning('后端未启动，展示模拟数据')
      } finally {
        this.analyzing = false
        if (this.currentSample) this.currentSample.status = 'done'
      }
    },
    processDetectionResult(data, time) {
      this.detectResult = data
      this.inferenceTime = Math.round(time)
      this.fps = Math.round(1000 / time)
      
      this.detections = []
      if (data.ripeness_analysis?.detections) {
        data.ripeness_analysis.detections.forEach(d => {
          if (d.bbox) {
            this.detections.push({
              x: d.bbox[0],
              y: d.bbox[1],
              width: d.bbox[2] - d.bbox[0],
              height: d.bbox[3] - d.bbox[1],
              label: d.class_name_ch || d.class_name,
              confidence: Math.round(d.confidence * 100),
              type: d.class_id === 0 ? 'riped' : 'unriped'
            })
          }
        })
      }
    },
    generateMockResult(time) {
      this.detectResult = {
        ripeness_analysis: {
          detections: [
            { class_id: 0, class_name: 'Riped', confidence: 0.95, bbox: [80, 100, 220, 260] },
            { class_id: 1, class_name: 'Unriped', confidence: 0.88, bbox: [280, 140, 400, 280] }
          ],
          statistics: { riped_ratio: 85 }
        },
        disease_analysis: {
          detections: [{ class_name: 'Healthy', confidence: 0.92 }]
        }
      }
      this.processDetectionResult(this.detectResult, time)
    },
    startScanAnimation() {
      this.scanY = 0
      const animate = () => {
        if (this.analyzing && this.scanY < 100) {
          this.scanY += 2
          requestAnimationFrame(animate)
        } else if (this.analyzing) {
          this.scanY = 0
          requestAnimationFrame(animate)
        }
      }
      animate()
    }
  },
  beforeDestroy() {
    if (this.imagePreview) URL.revokeObjectURL(this.imagePreview)
    if (this.videoSrc) URL.revokeObjectURL(this.videoSrc)
  }
}
</script>

<style scoped>
/* ========== 现代科研实验室风格 ========== */
/* 主色调: 白色 + 浅灰 + 翡翠绿 */

.lab-page {
  height: 100vh;
  background: #f1f5f9;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
}

/* 顶部标题栏 */
.lab-header {
  background: #ffffff;
  border-bottom: 2px solid #e2e8f0;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 32px;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.3px;
}

.header-subtitle {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-tag {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 6px 12px;
}

.info-label {
  font-size: 10px;
  color: #64748b;
  text-transform: uppercase;
}

.info-value {
  font-size: 13px;
  font-weight: 600;
  color: #10b981;
  margin-left: 6px;
}

/* ===== 中间工作台 ===== */
.workbench-panel {
  flex: 1;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.tab-nav {
  display: flex;
  border-bottom: 2px solid #e2e8f0;
  padding: 0 16px;
}

.tab-item {
  padding: 12px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #10b981;
}

.tab-item.active {
  color: #10b981;
  border-bottom-color: #10b981;
}

.tab-icon {
  font-size: 18px;
}

.workbench-viewport {
  flex: 1;
  overflow: hidden;
}

.viewport-content {
  height: 100%;
  padding: 20px;
}

.drop-area {
  height: 100%;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.drop-area.drag-over {
  border-color: #10b981;
  background: #ecfdf5;
}

.upload-prompt {
  text-align: center;
  padding: 40px;
}

.prompt-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.prompt-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.prompt-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 20px;
}

.upload-btn {
  background: #10b981;
  color: #ffffff;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-btn:hover {
  background: #059669;
}

.image-display {
  height: 100%;
  position: relative;
}

.image-display img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.scan-overlay, .detection-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.video-player-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.video-player-wrapper video {
  max-width: 100%;
  max-height: 80%;
  border-radius: 8px;
}

.extract-btn {
  background: #10b981;
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.stream-url-input {
  width: 100%;
  max-width: 400px;
  padding: 10px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 14px;
  margin-bottom: 16px;
}

.stream-display {
  height: 100%;
  position: relative;
}

.stream-display img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.stream-controls {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
}

.capture-btn, .stop-btn {
  background: #10b981;
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.stop-btn {
  background: #ef4444;
}

.workbench-footer {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.xai-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.xai-toggle input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.toggle-label {
  font-size: 13px;
  color: #475569;
}

.footer-right {
  display: flex;
  gap: 12px;
}

.analyze-btn {
  background: #10b981;
  color: #ffffff;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.analyze-btn:hover:not(:disabled) {
  background: #059669;
}

.analyze-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.clear-btn {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

/* ===== 右侧诊断面板 ===== */
.diagnostic-panel {
  width: 320px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.report-empty {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.report-content {
  padding: 16px;
}

.grade-stamp {
  text-align: center;
  margin: 20px 0;
}

.stamp-inner {
  display: inline-block;
  padding: 20px 30px;
  border: 4px double #10b981;
  border-radius: 50%;
  transform: rotate(-12deg);
  background: #ecfdf5;
}

.grade-letter {
  font-size: 48px;
  font-weight: 900;
  color: #10b981;
  line-height: 1;
}

.grade-text {
  font-size: 14px;
  color: #059669;
  margin-top: 4px;
  font-weight: 600;
}

.performance-panel {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.perf-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.perf-item:last-child {
  margin-bottom: 0;
}

.perf-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.perf-value {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  font-family: 'Courier New', monospace;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.stat-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: #475569;
}

.stat-num {
  font-weight: 700;
  color: #1e293b;
  font-family: 'Courier New', monospace;
}

.stat-num.stat-riped {
  color: #10b981;
}

.stat-num.stat-unriped {
  color: #f59e0b;
}

.disease-card {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 16px;
}

.disease-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.disease-icon {
  font-size: 20px;
}

.disease-name {
  font-size: 14px;
  font-weight: 600;
  color: #dc2626;
}

.disease-body {
  background: #ffffff;
  border-radius: 6px;
  padding: 12px;
}

.advice-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.advice-content {
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

/* 滚动条 */
.queue-list::-webkit-scrollbar {
  width: 6px;
}

.queue-list::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.queue-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.queue-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* ===== 左侧样本队列（已添加到上面）===== */
.lab-container {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

.sample-queue-panel {
  width: 280px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.sample-count {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 12px;
}

.queue-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  display: flex;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.queue-card:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.queue-card.active {
  background: #ecfdf5;
  border-color: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.1);
}

.card-thumbnail {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.card-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-placeholder {
  font-size: 24px;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.sample-id {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.sample-meta {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.status-badge {
  display: inline-block;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 4px;
  font-weight: 500;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.analyzing {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge.done {
  background: #d1fae5;
  color: #065f46;
}

.empty-queue {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
</style>