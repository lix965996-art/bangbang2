<template>
  <div class="fruit-detect-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <i class="el-icon-picture-outline title-icon"></i>
          <div class="title-text">
            <h2>果蔬双检智能分析</h2>
            <p>基于YOLOv8深度学习模型，同时识别果蔬成熟度和病虫害状态</p>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧上传区域 -->
      <div class="upload-section">
        <div class="section-card analysis-panel">
          <div class="card-header">
            <i class="el-icon-s-operation"></i>
            <span>分析配置</span>
          </div>

          <!-- 检测类型选择 -->
          <div class="detect-type-selector">
            <el-radio-group v-model="detectType" size="small">
              <el-radio-button label="both">
                <i class="el-icon-s-data"></i> 双检分析
              </el-radio-button>
              <el-radio-button label="ripeness">
                <i class="el-icon-sunny"></i> 成熟度检测
              </el-radio-button>
              <el-radio-button label="disease">
                <i class="el-icon-first-aid-kit"></i> 病虫害检测
              </el-radio-button>
            </el-radio-group>
          </div>

          <!-- 模式切换 Tabs -->
          <el-tabs v-model="detectMode" type="card" class="mode-tabs" @tab-click="handleTabClick">
            <!-- 图片检测 -->
            <el-tab-pane name="image">
              <span slot="label"><i class="el-icon-picture"></i> 图片检测</span>
              <div class="tab-content">
                <el-upload
                  class="image-uploader"
                  drag
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleFileChange"
                  accept="image/*"
                >
                  <div v-if="!previewUrl" class="upload-placeholder">
                    <i class="el-icon-upload upload-icon"></i>
                    <div class="upload-text">拖拽图片或<em>点击上传</em></div>
                    <div class="upload-tip">支持 JPG、PNG 格式</div>
                  </div>
                  <div v-else class="preview-container">
                    <img :src="previewUrl" class="preview-image" />
                    <div class="preview-overlay">
                      <i class="el-icon-refresh-right"></i>
                      <span>点击更换</span>
                    </div>
                  </div>
                </el-upload>

                <div class="action-buttons">
                  <el-button 
                    type="primary" 
                    icon="el-icon-search"
                    :loading="analyzing"
                    :disabled="!selectedFile"
                    @click="analyzeImage"
                  >
                    {{ analyzing ? '分析中...' : (detectResult ? '重新分析' : '开始分析') }}
                  </el-button>
                  <el-button 
                    icon="el-icon-delete"
                    :disabled="!selectedFile && !previewUrl && !detectResult"
                    @click="clearImage"
                  >
                    清除
                  </el-button>
                </div>
              </div>
            </el-tab-pane>

            <!-- 视频检测 -->
            <el-tab-pane name="video">
              <span slot="label"><i class="el-icon-video-camera"></i> 视频检测</span>
              <div class="tab-content">
                <el-upload
                  class="video-uploader"
                  drag
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleVideoChange"
                  accept="video/*"
                >
                  <div v-if="!videoUrl" class="upload-placeholder">
                    <i class="el-icon-video-camera upload-icon"></i>
                    <div class="upload-text">拖拽视频或<em>点击上传</em></div>
                  </div>
                  <div v-else class="preview-container">
                    <video :src="videoUrl" class="preview-video" controls></video>
                  </div>
                </el-upload>

                <div class="action-buttons">
                  <el-button 
                    type="primary" 
                    icon="el-icon-video-play"
                    :loading="analyzing"
                    :disabled="!selectedVideo"
                    @click="analyzeVideo"
                  >
                    {{ analyzing ? '处理中...' : '开始处理' }}
                  </el-button>
                  <el-button 
                    icon="el-icon-delete"
                    :disabled="!selectedVideo"
                    @click="clearVideo"
                  >
                    清除
                  </el-button>
                </div>
              </div>
            </el-tab-pane>

            <!-- 实时检测 -->
            <el-tab-pane name="camera">
              <span slot="label"><i class="el-icon-camera"></i> 实时检测</span>
              <div class="tab-content">
                <div class="camera-container">
                  <img 
                    v-if="cameraActive" 
                    :src="mjpegStreamUrl" 
                    class="camera-video"
                    @error="handleStreamError"
                  />
                  <div v-else class="camera-placeholder">
                    <i class="el-icon-video-camera"></i>
                    <p>点击下方按钮开启实时监控</p>
                  </div>
                  <canvas ref="cameraCanvas" style="display: none;"></canvas>
                </div>

                <div class="action-buttons">
                  <el-button 
                    v-if="!cameraActive"
                    type="primary" 
                    icon="el-icon-video-camera"
                    @click="startMjpegStream"
                  >
                    开启实时监控
                  </el-button>
                  <template v-else>
                    <el-button 
                      type="success" 
                      icon="el-icon-camera"
                      :loading="analyzing"
                      @click="captureAndAnalyze"
                    >
                      截图检测
                    </el-button>
                    <el-button 
                      type="danger"
                      icon="el-icon-video-pause"
                      @click="stopMjpegStream"
                    >
                      关闭
                    </el-button>
                  </template>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 使用说明 (简化版) -->
        <div class="section-card tips-card">
          <div class="tips-mini">
            <i class="el-icon-info"></i>
            <span>提示：选择检测类型与作物，上传图片即可快速获取AI分析报告。</span>
          </div>
        </div>
      </div>

      <!-- 右侧结果区域 -->
      <div class="result-section">
        <div class="section-card result-card">
          <div class="card-header">
            <i class="el-icon-data-analysis"></i>
            <span>分析结果</span>
            <span v-if="detectResult" class="detect-time">{{ detectResult.detect_time }}</span>
          </div>

          <div v-if="!detectResult && !analyzing" class="empty-result">
            <i class="el-icon-picture-outline"></i>
            <p>上传图片后查看分析结果</p>
          </div>

          <div v-else-if="analyzing" class="analyzing-state">
            <div class="analyzing-animation">
              <i class="el-icon-loading"></i>
            </div>
            <p>AI正在分析图片，请稍候...</p>
          </div>

          <div v-else class="result-content">
            <!-- 分页切换按钮 -->
            <div class="result-tabs">
              <div 
                class="result-tab" 
                :class="{ active: resultTab === 'image' }"
                @click="resultTab = 'image'"
              >
                <i class="el-icon-picture"></i> 检测图片
              </div>
              <div 
                v-if="detectType === 'both' || detectType === 'ripeness'"
                class="result-tab" 
                :class="{ active: resultTab === 'ripeness' }"
                @click="resultTab = 'ripeness'"
              >
                <i class="el-icon-cherry"></i> 成熟度
              </div>
              <div 
                v-if="detectType === 'both' || detectType === 'disease'"
                class="result-tab" 
                :class="{ active: resultTab === 'disease' }"
                @click="resultTab = 'disease'"
              >
                <i class="el-icon-warning"></i> 病虫害
              </div>
            </div>

            <!-- 检测图片 -->
            <div v-show="resultTab === 'image'" class="result-panel">
              <div class="result-image-container">
                <img :src="detectResult.result_image" class="result-image" />
              </div>
            </div>

            <!-- 成熟度分析 -->
            <div v-show="resultTab === 'ripeness'" class="result-panel" v-if="detectResult.ripeness_analysis">
              <!-- 统计卡片 -->
              <div class="compact-stats-row">
                <div class="mini-stat">
                  <span class="mini-value">{{ detectResult.ripeness_analysis.statistics.total }}</span>
                  <span class="mini-label">检测总数</span>
                </div>
                <div class="mini-stat green">
                  <span class="mini-value">{{ detectResult.ripeness_analysis.statistics.riped_count }}</span>
                  <span class="mini-label">成熟</span>
                </div>
                <div class="mini-stat orange">
                  <span class="mini-value">{{ detectResult.ripeness_analysis.statistics.unriped_count }}</span>
                  <span class="mini-label">未成熟</span>
                </div>
              </div>

              <!-- 成熟度进度条 -->
              <div class="progress-section">
                <div class="progress-header">
                  <span>成熟度指标</span>
                  <span class="progress-value">{{ detectResult.ripeness_analysis.statistics.riped_ratio }}%</span>
                </div>
                <el-progress 
                  :percentage="detectResult.ripeness_analysis.statistics.riped_ratio" 
                  :color="getProgressColor(detectResult.ripeness_analysis.statistics.riped_ratio)"
                  :stroke-width="16"
                  :show-text="false"
                ></el-progress>
              </div>

              <!-- 采收建议 -->
              <div class="advice-card">
                <div class="advice-header">
                  <i class="el-icon-s-opportunity"></i>
                  <span>采收建议</span>
                </div>
                <div class="advice-content">
                  <template v-if="detectResult.ripeness_analysis.statistics.riped_ratio >= 80">
                    <p class="advice-text success">果实成熟度高，建议尽快采收，避免过熟腐烂。</p>
                  </template>
                  <template v-else-if="detectResult.ripeness_analysis.statistics.riped_ratio >= 50">
                    <p class="advice-text warning">部分果实已成熟，可分批采收成熟果实。</p>
                  </template>
                  <template v-else>
                    <p class="advice-text info">果实成熟度较低，建议继续培育，加强水肥管理。</p>
                  </template>
                </div>
              </div>

              <!-- 检测详情列表 -->
              <div class="detail-list" v-if="detectResult.ripeness_analysis.detections && detectResult.ripeness_analysis.detections.length > 0">
                <div class="detail-header">检测详情</div>
                <div class="detail-items">
                  <el-tag 
                    v-for="(item, index) in detectResult.ripeness_analysis.detections.slice(0, 8)" 
                    :key="index"
                    :type="item.class_id === 0 ? 'success' : 'warning'"
                    size="small"
                    class="detail-tag"
                  >
                    {{ item.class_name_ch || item.class_name }} {{ item.confidence }}%
                  </el-tag>
                </div>
              </div>
            </div>

            <!-- 病虫害分析 -->
            <div v-show="resultTab === 'disease'" class="result-panel" v-if="detectResult.disease_analysis">
              <!-- 重点病害名称 -->
              <div class="main-disease-card" v-if="getMainDisease(detectResult.disease_analysis)">
                <div class="main-disease-icon">
                  <i class="el-icon-warning-outline"></i>
                </div>
                <div class="main-disease-info">
                  <div class="main-disease-name">{{ getMainDisease(detectResult.disease_analysis).name }}</div>
                  <div class="main-disease-conf">置信度: {{ getMainDisease(detectResult.disease_analysis).confidence }}%</div>
                </div>
              </div>

              <!-- 健康状态 -->
              <div class="health-status-card success" v-if="!getMainDisease(detectResult.disease_analysis)">
                <div class="status-icon">
                  <i class="el-icon-circle-check"></i>
                </div>
                <div class="status-info">
                  <h4>作物健康状况良好</h4>
                  <p>未检测到明显的病虫害症状，请继续保持良好的田间管理。</p>
                </div>
              </div>

              <!-- 防治建议 -->
              <div class="advice-card disease-advice" v-if="getMainDisease(detectResult.disease_analysis)">
                <div class="advice-header">
                  <i class="el-icon-first-aid-kit"></i>
                  <span>防治方案</span>
                </div>
                <div class="advice-content">
                  <p class="advice-text warning">{{ getDiseaseAdvice(getMainDisease(detectResult.disease_analysis).originalName) }}</p>
                </div>
              </div>

              <!-- 检测统计 -->
              <div class="disease-stats" v-if="detectResult.disease_analysis.statistics">
                <div class="stat-item">
                  <span class="stat-value">{{ detectResult.disease_analysis.statistics.total_detections || detectResult.disease_analysis.detections.length }}</span>
                  <span class="stat-label">检测数量</span>
                </div>
              </div>

              <!-- 检测详情 -->
              <div class="detail-list" v-if="detectResult.disease_analysis.detections && detectResult.disease_analysis.detections.length > 0">
                <div class="detail-header">所有检测结果</div>
                <div class="detail-items">
                  <el-tag 
                    v-for="(item, index) in detectResult.disease_analysis.detections.slice(0, 8)" 
                    :key="index"
                    :type="item.class_name && (item.class_name.includes('Healthy') || item.class_name.includes('health')) ? 'success' : 'danger'"
                    size="medium"
                    class="detail-tag"
                  >
                    {{ item.class_name_ch || item.class_name }} {{ item.confidence }}%
                  </el-tag>
                </div>
              </div>

              <!-- 日常管理建议 -->
              <div class="tips-card">
                <div class="tips-header">
                  <i class="el-icon-info"></i>
                  <span>日常管理建议</span>
                </div>
                <ul class="tips-list">
                  <li>定期巡查田间，及时发现病虫害</li>
                  <li>保持田间通风透光，降低湿度</li>
                  <li>合理施肥，增强作物抗病能力</li>
                </ul>
              </div>
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
      detectMode: 'image', // image, video, camera
      detectType: 'both', // both, ripeness, disease
      selectedFile: null,
      previewUrl: null,
      selectedVideo: null,
      videoUrl: null,
      cameraActive: false,
      cameraStream: null,
      analyzing: false,
      detectResult: null,
      selectedCrop: 'tomato', // 默认选择番茄
      confidence: 0.5, // 默认置信度
      resultTab: 'image', // 结果分页：image, ripeness, disease
      mjpegStreamUrl: 'http://192.168.137.192/mjpeg/1' // MJPEG视频流地址
    }
  },
  mounted() {
  },
  beforeDestroy() {
    // 组件销毁时关闭监控
    this.stopMjpegStream()
  },
  methods: {
    // 处理文件选择
    handleFileChange(file) {
      this.selectedFile = file.raw
      this.previewUrl = URL.createObjectURL(file.raw)
      this.detectResult = null
    },

    // 清除图片
    clearImage() {
      // 清除所有状态
      this.selectedFile = null
      this.previewUrl = null
      this.detectResult = null
      this.analyzing = false
      
      // 重置文件上传组件
      const uploadInput = document.querySelector('.el-upload__input')
      if (uploadInput) {
        uploadInput.value = ''
      }
      
      this.$message.success('已清除')
    },

    // 分析图片
    async analyzeImage() {
      if (!this.selectedFile) {
        this.$message.warning('请先上传图片')
        return
      }

      if (!this.selectedCrop) {
        this.$message.warning('请选择作物类型')
        return
      }

      this.analyzing = true
      this.detectResult = null

      try {
        const formData = new FormData()
        formData.append('file', this.selectedFile)
        formData.append('crop_type', this.selectedCrop)
        formData.append('conf', this.confidence.toString())
        
        // 根据检测类型选择不同的接口
        const apiEndpoints = {
          both: 'http://localhost:5000/api/detect/both',
          ripeness: 'http://localhost:5000/api/detect/ripeness',
          disease: 'http://localhost:5000/api/detect/disease'
        }
        const apiUrl = apiEndpoints[this.detectType]
        
        const response = await fetch(apiUrl, {
          method: 'POST',
          body: formData
        })
        
        const res = await response.json()

        if (res.code === '200') {
          // 统一数据格式
          let result = res.data
          
          // 单独成熟度检测时，将数据包装成统一格式
          if (this.detectType === 'ripeness' && result.statistics) {
            result = {
              result_image: result.result_image,
              detect_time: result.detect_time,
              ripeness_analysis: {
                detections: result.detections,
                statistics: result.statistics
              }
            }
          }
          
          // 单独病虫害检测时，将数据包装成统一格式
          if (this.detectType === 'disease' && result.detections && !result.disease_analysis) {
            result = {
              result_image: result.result_image,
              detect_time: result.detect_time,
              disease_analysis: {
                detections: result.detections,
                statistics: result.statistics
              }
            }
          }
          
          this.detectResult = result
          
          // 自动切换到对应的结果标签页
          if (this.detectType === 'ripeness') {
            this.resultTab = 'ripeness'
          } else if (this.detectType === 'disease') {
            this.resultTab = 'disease'
          } else {
            this.resultTab = 'image'
          }
          
          const typeNames = { both: '双检分析', ripeness: '成熟度检测', disease: '病虫害检测' }
          this.$message.success(`${typeNames[this.detectType]}完成!`)
        } else {
          this.$message.error(res.msg || '分析失败')
        }
      } catch (e) {
        console.error(e)
        this.$message.error('分析请求失败，请确保Flask服务已启动 (python integrated_api_server.py)')
      } finally {
        this.analyzing = false
      }
    },

    // 获取进度条颜色
    getProgressColor(percentage) {
      if (percentage >= 80) return '#67C23A'
      if (percentage >= 50) return '#E6A23C'
      return '#F56C6C'
    },

    // 获取主要病害（置信度最高的非健康检测）
    getMainDisease(diseaseAnalysis) {
      if (!diseaseAnalysis || !diseaseAnalysis.detections || diseaseAnalysis.detections.length === 0) {
        return null
      }
      
      // 过滤掉健康的检测结果，找出真正的病害
      const diseases = diseaseAnalysis.detections.filter(d => {
        const name = d.class_name || ''
        return !name.includes('Healthy') && !name.includes('health')
      })
      
      if (diseases.length === 0) {
        return null
      }
      
      // 按置信度排序，取最高的
      const sorted = diseases.sort((a, b) => b.confidence - a.confidence)
      const main = sorted[0]
      
      return {
        name: main.class_name_ch || main.class_name,
        originalName: main.class_name,
        confidence: main.confidence
      }
    },

    // 获取病害防治建议
    getDiseaseAdvice(diseaseName) {
      const adviceMap = {
        'Early Blight（早疫病）': '建议使用代森锰锌、百菌清等杀菌剂进行喷雾防治；及时摘除病叶，减少菌源；加强通风透光，降低湿度。',
        'Late Blight（晚疫病）': '建议选用甲霜灵·锰锌、烯酰吗啉等药剂防治；发现中心病株立即拔除；控制浇水，避免田间积水。',
        'Leaf Miner（潜叶病）': '建议使用阿维菌素、灭蝇胺等药剂进行防治；利用黄板诱杀成虫；清洁田园，处理残枝败叶。',
        'Leaf Mold（叶霉病）': '建议喷洒多菌灵、甲基托布津等药剂；加强温湿度管理，适时通风；选用抗病品种。',
        'Mosaic Virus（花叶病毒）': '主要通过蚜虫传播，建议使用吡虫啉防治蚜虫；发病初期喷洒病毒A、植病灵等抗病毒药剂；拔除病株，防止蔓延。',
        'Septoria（壳针孢属）': '建议使用苯醚甲环唑、代森锰锌等药剂防治；实行轮作倒茬；及时清除田间病残体。',
        'Spider Mites（蜘蛛螨）': '建议使用哒螨灵、阿维菌素等杀螨剂；清除田边杂草；保护利用天敌，如捕食螨。',
        'Yellow Leaf Curl Virus（黄化卷叶病毒）': '主要由烟粉虱传播，重点防治烟粉虱；使用防虫网阻隔；选用抗病毒品种。',
        'Brown_Spot（褐斑病）': '建议使用三环唑、多菌灵等药剂；增施磷钾肥，提高抗病力；合理灌溉，避免深水漫灌。',
        'Rice_Blast（稻瘟病）': '建议使用三环唑、富士一号等特异性药剂；选用抗病品种；科学施肥，避免氮肥过量。',
        'Bacterial_Blight（细菌性叶枯病）': '建议使用叶枯唑、农用链霉素等药剂；浅水勤灌，适时晒田；严禁带菌稻草还田。',
        'Gray Mold（灰霉病）': '建议使用腐霉利、异菌脲等药剂；控制浇水，降低湿度；及时摘除病叶病果。',
        'Powdery Mildew Leaf（白粉病叶）': '建议使用三唑酮、粉锈宁等药剂；及时清除病叶；保持田间清洁。'
      }
      
      return adviceMap[diseaseName] || '建议及时采取防治措施，可使用对应的杀菌剂或杀虫剂进行喷洒处理，并加强田间管理。'
    },

    // 切换检测模式 Tab
    handleTabClick(tab) {
      this.detectMode = tab.name
      this.clearAll()
      if (tab.name !== 'camera') {
        this.stopCamera()
      }
    },

    // 处理视频文件选择
    handleVideoChange(file) {
      this.selectedVideo = file.raw
      this.videoUrl = URL.createObjectURL(file.raw)
      this.detectResult = null
    },

    // 清除视频
    clearVideo() {
      this.selectedVideo = null
      this.videoUrl = null
      this.detectResult = null
      this.analyzing = false
      
      const uploadInput = document.querySelector('.el-upload__input')
      if (uploadInput) {
        uploadInput.value = ''
      }
      
      this.$message.success('已清除')
    },

    // 清除所有
    clearAll() {
      this.selectedFile = null
      this.previewUrl = null
      this.selectedVideo = null
      this.videoUrl = null
      this.detectResult = null
      this.analyzing = false
    },

    // 分析视频（提取关键帧）
    async analyzeVideo() {
      if (!this.selectedVideo) {
        this.$message.warning('请先上传视频')
        return
      }

      this.analyzing = true
      this.detectResult = null

      try {
        // 创建视频元素提取第一帧
        const video = document.createElement('video')
        video.src = this.videoUrl
        
        await new Promise((resolve) => {
          video.onloadeddata = resolve
        })

        // 跳到视频中间位置
        video.currentTime = video.duration / 2

        await new Promise((resolve) => {
          video.onseeked = resolve
        })

        // 创建canvas提取帧
        const canvas = document.createElement('canvas')
        canvas.width = video.videoWidth
        canvas.height = video.videoHeight
        const ctx = canvas.getContext('2d')
        ctx.drawImage(video, 0, 0)

        // 将canvas转为blob
        const blob = await new Promise((resolve) => {
          canvas.toBlob(resolve, 'image/jpeg', 0.95)
        })

        // 发送到API
        const formData = new FormData()
        formData.append('file', blob, 'video_frame.jpg')

        const detectApiUrl = process.env.VUE_APP_DETECT_API_URL || 'http://localhost:5000'
        const response = await fetch(`${detectApiUrl}/api/detect`, {
          method: 'POST',
          body: formData
        })

        const res = await response.json()

        if (res.code === '200') {
          this.detectResult = res.data
          this.$message.success('视频分析完成!')
        } else {
          this.$message.error(res.msg || '分析失败')
        }
      } catch (e) {
        console.error(e)
        this.$message.error('视频处理失败: ' + e.message)
      } finally {
        this.analyzing = false
      }
    },

    // 关闭摄像头（保留兼容）
    stopCamera() {
      this.stopMjpegStream()
    },

    // 开启MJPEG视频流
    startMjpegStream() {
      this.cameraActive = true
      this.$message.success('实时监控已开启')
    },

    // 关闭MJPEG视频流
    stopMjpegStream() {
      this.cameraActive = false
    },

    // 处理视频流加载错误
    handleStreamError() {
      this.$message.error('视频流加载失败，请检查摄像头地址是否正确')
      this.cameraActive = false
    },

    // 截图并分析
    async captureAndAnalyze() {
      if (!this.cameraActive) {
        this.$message.warning('请先开启实时监控')
        return
      }

      this.analyzing = true
      this.detectResult = null

      try {
        // 从MJPEG流截取当前帧
        const img = document.querySelector('.camera-video')
        const canvas = this.$refs.cameraCanvas
        
        canvas.width = img.naturalWidth || 640
        canvas.height = img.naturalHeight || 480
        
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height)

        // 将canvas转为blob
        const blob = await new Promise((resolve) => {
          canvas.toBlob(resolve, 'image/jpeg', 0.95)
        })

        // 发送到Flask API进行双检分析
        const formData = new FormData()
        formData.append('file', blob, 'stream_capture.jpg')
        formData.append('crop_type', this.selectedCrop)
        formData.append('conf', this.confidence.toString())

        const response = await fetch('http://localhost:5000/api/detect/both', {
          method: 'POST',
          body: formData
        })

        const res = await response.json()

        if (res.code === '200') {
          this.detectResult = res.data
          this.$message.success('双检分析完成!')
        } else {
          this.$message.error(res.msg || '检测失败')
        }
      } catch (e) {
        console.error(e)
        this.$message.error('截图检测失败: ' + e.message)
      } finally {
        this.analyzing = false
      }
    },

    // 格式化置信度显示
    formatConfidence(val) {
      return `${(val * 100).toFixed(0)}%`
    },

    // 获取作物中文名称
    getCropName(cropType) {
      const cropNames = {
        'tomato': '番茄',
        'corn': '玉米',
        'rice': '水稻',
        'strawberry': '草莓'
      }
      return cropNames[cropType] || cropType
    },

    // 获取检测列表
    getDetections() {
      if (!this.detectResult) return []
      
      if (this.detectType === 'both' && this.detectResult.ripeness_analysis) {
        // 双检分析：合并成熟度和病虫害检测结果
        const ripenessDetections = this.detectResult.ripeness_analysis.detections || []
        const diseaseDetections = this.detectResult.disease_analysis.detections || []
        return [...ripenessDetections, ...diseaseDetections]
      } else if (this.detectResult.detections) {
        // 单独检测
        return this.detectResult.detections
      }
      return []
    },

    // 获取检测项名称
    getDetectionName(item) {
      if (item.class_name_ch) {
        return item.class_name_ch
      } else if (item.class_name) {
        return item.class_name
      }
      return '未知'
    },

    // 获取检测项样式类
    getDetectionClass(item) {
      if (this.detectType === 'disease' || this.detectType === 'both') {
        // 病虫害检测
        if (item.class_name && item.class_name.includes('Healthy')) {
          return 'healthy'
        } else {
          return 'disease'
        }
      } else {
        // 成熟度检测
        return item.class_id === 0 ? 'riped' : 'unriped'
      }
    }
  }
}
</script>

<style scoped>
.fruit-detect-page {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, #67C23A 0%, #529b2e 100%);
  border-radius: 16px;
  padding: 25px 30px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title-icon {
  font-size: 40px;
  color: rgba(255, 255, 255, 0.9);
}

.title-text h2 {
  margin: 0;
  color: white;
  font-size: 24px;
  font-weight: 600;
}

.title-text p {
  margin: 5px 0 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

/* 主内容区 */
.main-content {
  display: flex;
  gap: 12px;
  height: calc(100% - 60px);
}

.upload-section {
  width: 480px;
  min-width: 480px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 模式选择器 */
.mode-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mode-selector ::v-deep .el-radio {
  margin-right: 0;
  padding: 12px 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s;
}

.mode-selector ::v-deep .el-radio:hover {
  border-color: #67C23A;
  background: #f0f9ff;
}

.mode-selector ::v-deep .el-radio.is-checked {
  border-color: #67C23A;
  background: #f0f9ff;
}

.mode-selector ::v-deep .el-radio__label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.mode-selector ::v-deep .el-radio__label i {
  font-size: 18px;
}

.result-section {
  flex: 1;
  max-width: 600px;
}

/* 卡片样式 */
.section-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.analysis-panel {
  padding-bottom: 10px;
}

/* 配置工具栏 */
.config-toolbar {
  margin-bottom: 15px;
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.slider-row {
  background: #f8f9fa;
  padding: 10px 15px;
  border-radius: 8px;
  margin-bottom: 0;
}

.slider-row .label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.custom-divider {
  margin: 15px 0 20px 0;
}

.config-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

/* 检测类型选择器 */
.detect-type-selector {
  margin-bottom: 15px;
}

.detect-type-selector ::v-deep .el-radio-group {
  display: flex;
  width: 100%;
}

.detect-type-selector ::v-deep .el-radio-button {
  flex: 1;
}

.detect-type-selector ::v-deep .el-radio-button__inner {
  width: 100%;
  padding: 10px 8px;
  font-size: 13px;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.detect-type-selector ::v-deep .el-radio-button:first-child .el-radio-button__inner {
  border-radius: 8px 0 0 8px;
}

.detect-type-selector ::v-deep .el-radio-button:last-child .el-radio-button__inner {
  border-radius: 0 8px 8px 0;
}

.detect-type-selector ::v-deep .el-radio-button__orig-radio:checked + .el-radio-button__inner {
  background: linear-gradient(135deg, #67C23A 0%, #529B2E 100%);
  border-color: #67C23A;
  box-shadow: none;
}

.detect-type-selector ::v-deep .el-radio-button__inner i {
  font-size: 14px;
}

/* Tabs 样式 */
.mode-tabs ::v-deep .el-tabs__header {
  margin-bottom: 20px;
}

.mode-tabs ::v-deep .el-tabs__item {
  font-size: 14px;
  height: 40px;
  line-height: 40px;
}

.mode-tabs ::v-deep .el-tabs__item.is-active {
  color: #67C23A;
  font-weight: bold;
}

.tab-content {
  padding: 5px 0;
}

/* 简化的 Tips */
.tips-mini {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
}

.tips-mini i {
  color: #67C23A;
  font-size: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header i {
  color: #67C23A;
  font-size: 20px;
}

.detect-time {
  margin-left: auto;
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

/* 上传区域 */
.image-uploader {
  width: 100%;
}

::v-deep .el-upload-dragger {
  width: 435px;
  height: 220px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

::v-deep .el-upload-dragger:hover {
  border-color: #67C23A;
}

.upload-placeholder {
  text-align: center;
}

.upload-icon {
  font-size: 50px;
  color: #c0c4cc;
  margin-bottom: 10px;
}

.upload-text {
  color: #606266;
  font-size: 14px;
}

.upload-text em {
  color: #67C23A;
  font-style: normal;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.preview-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 10px;
}

.preview-container:hover .preview-overlay {
  opacity: 1;
}

.preview-overlay i {
  font-size: 30px;
  margin-bottom: 10px;
}

/* 摄像头样式 */
.camera-container {
  width: 100%;
  height: 280px;
  background: #000;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
  text-align: center;
}

.camera-placeholder i {
  font-size: 50px;
  color: #999;
  margin-bottom: 10px;
}

.camera-placeholder p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

/* 视频预览 */
.preview-video {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.video-uploader {
  width: 100%;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.action-buttons .el-button {
  flex: 1;
}

/* 去掉按钮浮动效果 */
::v-deep .action-buttons .el-button {
  transition: none !important;
  transform: none !important;
}

::v-deep .action-buttons .el-button:hover,
::v-deep .action-buttons .el-button:focus,
::v-deep .action-buttons .el-button:active {
  transform: none !important;
  box-shadow: none !important;
}

/* 使用说明 */
.tips-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.tip-num {
  width: 24px;
  height: 24px;
  background: #67C23A;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}


/* 结果区域 */
.result-card {
  min-height: 400px;
}

.empty-result, .analyzing-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
}

.empty-result i, .analyzing-state i {
  font-size: 40px;
  margin-bottom: 8px;
  color: #dcdfe6;
}

.analyzing-animation i {
  font-size: 60px;
  color: #67C23A;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 结果图片 */
.result-image-container {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 12px;
  margin-bottom: 20px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-image {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
}

/* 统计卡片 */
.statistics-row {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  padding: 15px;
  border-radius: 12px;
  text-align: center;
}

.stat-card.total {
  background: linear-gradient(135deg, #409EFF 0%, #337ecc 100%);
  color: white;
}

.stat-card.riped {
  background: linear-gradient(135deg, #67C23A 0%, #529b2e 100%);
  color: white;
}

.stat-card.unriped {
  background: linear-gradient(135deg, #E6A23C 0%, #cf9236 100%);
  color: white;
}

.stat-card.ratio {
  background: linear-gradient(135deg, #909399 0%, #73767a 100%);
  color: white;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-label {
  font-size: 12px;
  margin-top: 5px;
  opacity: 0.9;
}

/* 成熟度进度条 */
.maturity-bar {
  background: #f8f9fa;
  padding: 15px 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.bar-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.bar-value {
  font-weight: bold;
  color: #67C23A;
}

/* 检测详情列表 */
.detection-list {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
}

.list-header {
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.detection-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 200px;
  overflow-y: auto;
}

.detection-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 15px;
  border-radius: 8px;
  font-size: 13px;
}

.detection-item.riped {
  background: rgba(103, 194, 58, 0.1);
  border: 1px solid rgba(103, 194, 58, 0.3);
}

.detection-item.unriped {
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.3);
}

.item-index {
  color: #909399;
  font-weight: 500;
}

.item-class {
  font-weight: 600;
}

.detection-item.riped .item-class {
  color: #67C23A;
}

.detection-item.unriped .item-class {
  color: #E6A23C;
}

.item-conf {
  color: #909399;
  font-size: 12px;
}

/* 检测类型选择器 */
.type-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.type-selector ::v-deep .el-radio {
  margin-right: 0;
  padding: 12px 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s;
}

.type-selector ::v-deep .el-radio:hover {
  border-color: #409EFF;
  background: #f0f9ff;
}

.type-selector ::v-deep .el-radio.is-checked {
  border-color: #409EFF;
  background: #f0f9ff;
}

.type-selector ::v-deep .el-radio__label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.type-selector ::v-deep .el-radio__label i {
  font-size: 18px;
}

/* 作物选择器 */
.crop-selector {
  margin-top: 20px;
  margin-bottom: 15px;
}

.selector-label {
  display: block;
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

/* 置信度滑块 */
.confidence-slider {
  margin-top: 15px;
}

.slider-label {
  display: block;
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
  font-weight: 500;
}

/* 双检分析结果 */
.dual-analysis-result {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.analysis-section {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.section-title i {
  font-size: 18px;
}

.section-title i.el-icon-cherry {
  color: #67C23A;
}

.section-title i.el-icon-warning {
  color: #E6A23C;
}

/* 病虫害统计 */
.disease-stats {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.disease-stat-card {
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  transition: all 0.3s;
}

.disease-stat-card.healthy {
  background: linear-gradient(135deg, #67C23A 0%, #529b2e 100%);
  color: white;
}

.disease-stat-card.has-disease {
  background: linear-gradient(135deg, #E6A23C 0%, #cf9236 100%);
  color: white;
}

.crop-type-info {
  text-align: center;
  font-size: 14px;
  color: #666;
  padding: 10px;
  background: white;
  border-radius: 8px;
}

/* 单独分析结果 */
.single-analysis-result {
  margin-bottom: 20px;
}

/* 病虫害检测项样式 */
.detection-item.disease {
  background: rgba(230, 162, 60, 0.1);
  border: 1px solid rgba(230, 162, 60, 0.3);
}

.detection-item.healthy {
  background: rgba(103, 194, 58, 0.1);
  border: 1px solid rgba(103, 194, 58, 0.3);
}

.detection-item.disease .item-class {
  color: #E6A23C;
}

.detection-item.healthy .item-class {
  color: #67C23A;
}

/* 结果分页标签 */
.result-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  background: #f5f7fa;
  padding: 6px;
  border-radius: 10px;
}

.result-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.result-tab:hover {
  background: rgba(103, 194, 58, 0.1);
  color: #67C23A;
}

.result-tab.active {
  background: #67C23A;
  color: white;
  font-weight: 500;
}

.result-tab i {
  font-size: 15px;
}

.result-panel {
  min-height: 200px;
}

/* 紧凑统计行 */
.compact-stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.mini-stat {
  flex: 1;
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
  text-align: center;
}

.mini-stat.green {
  background: rgba(103, 194, 58, 0.1);
}

.mini-stat.orange {
  background: rgba(230, 162, 60, 0.1);
}

.mini-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.mini-stat.green .mini-value {
  color: #67C23A;
}

.mini-stat.orange .mini-value {
  color: #E6A23C;
}

.mini-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 进度条区域 */
.progress-section {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.progress-value {
  font-weight: bold;
  color: #67C23A;
}

/* 主要病害卡片 - 重点突出 */
.main-disease-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 25px;
  border-radius: 12px;
  margin-bottom: 15px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(238, 90, 90, 0.3);
}

.main-disease-icon {
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-disease-icon i {
  font-size: 32px;
  color: white;
}

.main-disease-info {
  flex: 1;
}

.main-disease-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 5px;
}

.main-disease-conf {
  font-size: 14px;
  opacity: 0.9;
}

/* 病虫害统计 */
.disease-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.disease-stats .stat-item {
  flex: 1;
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
  text-align: center;
}

.disease-stats .stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.disease-stats .stat-label {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

/* 病虫害防治建议 */
.disease-advice {
  background: #fff8e6;
  border: 1px solid #ffeeba;
}

.disease-advice .advice-header {
  color: #856404;
}

.disease-advice .advice-header i {
  color: #e6a23c;
}

/* 健康状态卡片 */
.health-status-card.success {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(82, 155, 46, 0.15) 100%);
  border: 1px solid rgba(103, 194, 58, 0.3);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.health-status-card .status-icon i {
  font-size: 40px;
  color: #67C23A;
}

.health-status-card .status-info h4 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 16px;
}

.health-status-card .status-info p {
  margin: 0;
  color: #666;
  font-size: 13px;
}

/* 病虫害结果卡片 */
.disease-result-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 25px;
  border-radius: 12px;
  margin-bottom: 15px;
}

.disease-result-card.healthy {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1) 0%, rgba(82, 155, 46, 0.15) 100%);
  border: 1px solid rgba(103, 194, 58, 0.3);
}

.disease-result-card.has-disease {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.1) 0%, rgba(207, 146, 54, 0.15) 100%);
  border: 1px solid rgba(230, 162, 60, 0.3);
}

.disease-result-card i {
  font-size: 40px;
}

.disease-result-card.healthy i {
  color: #67C23A;
}

.disease-result-card.has-disease i {
  color: #E6A23C;
}

.disease-info {
  display: flex;
  flex-direction: column;
}

.disease-count {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.disease-label {
  font-size: 14px;
  color: #606266;
}

.detection-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 建议卡片 */
.advice-card {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}

.advice-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.advice-header i {
  color: #67C23A;
  font-size: 16px;
}

.advice-content {
  padding-left: 24px;
}

.advice-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}

.advice-text.success {
  color: #67C23A;
}

.advice-text.warning {
  color: #E6A23C;
}

.advice-text.info {
  color: #909399;
}

/* 健康状态卡片 */
.health-status-card {
  display: flex;
  align-items: center;
  gap: 15px;
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.08) 0%, rgba(82, 155, 46, 0.12) 100%);
  border: 1px solid rgba(103, 194, 58, 0.2);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 15px;
}

.status-icon {
  width: 50px;
  height: 50px;
  background: rgba(103, 194, 58, 0.15);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon i {
  font-size: 28px;
  color: #67C23A;
}

.status-info h4 {
  margin: 0 0 5px 0;
  font-size: 15px;
  color: #333;
}

.status-info p {
  margin: 0;
  font-size: 13px;
  color: #606266;
}

/* 详情列表 */
.detail-list {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}

.detail-header {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.detail-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-tag {
  margin: 0;
}

/* 提示卡片 */
.tips-card {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.tips-header i {
  color: #409EFF;
  font-size: 16px;
}

.tips-list {
  margin: 0;
  padding-left: 24px;
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.tips-list li {
  margin-bottom: 5px;
}
</style>
