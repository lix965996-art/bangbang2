<template>
  <div class="ai-analysis">
    <h2>AI智能分析</h2>

    <div class="analysis-section">
      <h3>果蔬双检分析</h3>
      <div class="upload-area">
        <input type="file" ref="fileInput" @change="handleImageUpload" accept="image/*" />
        <div v-if="selectedImage" class="image-preview">
          <img :src="selectedImage" alt="预览" />
          <button @click="clearImage" class="clear-btn">清除</button>
        </div>
      </div>

      <div class="crop-selection">
        <label>选择作物类型：</label>
        <select v-model="selectedCrop">
          <option value="tomato">番茄</option>
          <option value="corn">玉米</option>
          <option value="rice">水稻</option>
          <option value="strawberry">草莓</option>
          <option value="cucumber">黄瓜</option>
          <option value="pepper">辣椒</option>
        </select>
      </div>

      <div class="analysis-buttons">
        <button @click="analyzeRipeness" :disabled="!selectedFile" :class="{ 'loading': loading }">
          {{ loading && loadingType === 'ripeness' ? '分析中...' : '成熟度检测' }}
        </button>
        <button @click="analyzeDisease" :disabled="!selectedFile" :class="{ 'loading': loading }">
          {{ loading && loadingType === 'disease' ? '分析中...' : '病虫害检测' }}
        </button>
        <button @click="analyzeDual" :disabled="!selectedFile" :class="{ 'loading': loading }">
          {{ loading && loadingType === 'dual' ? '分析中...' : '双检一体化' }}
        </button>
      </div>

      <div v-if="analysisResult" class="result-area">
        <h4>分析结果：</h4>
        <pre>{{ formatResult(analysisResult) }}</pre>
      </div>
    </div>

    <div class="suggestion-section">
      <h3>AI智能建议</h3>
      <div class="suggestion-form">
        <div class="form-group">
          <label>作物类型：</label>
          <select v-model="suggestion.cropType">
            <option value="tomato">番茄</option>
            <option value="corn">玉米</option>
            <option value="rice">水稻</option>
            <option value="strawberry">草莓</option>
            <option value="cucumber">黄瓜</option>
            <option value="pepper">辣椒</option>
          </select>
        </div>

        <div class="form-group">
          <label>温度（℃）：</label>
          <input type="number" v-model="suggestion.temperature" step="0.1" />
        </div>

        <div class="form-group">
          <label>湿度（%）：</label>
          <input type="number" v-model="suggestion.humidity" step="0.1" />
        </div>

        <div class="form-group">
          <label>pH值：</label>
          <input type="number" v-model="suggestion.ph" step="0.1" />
        </div>

        <div class="form-group">
          <label>生长阶段：</label>
          <select v-model="suggestion.growthStage">
            <option value="seedling">幼苗期</option>
            <option value="vegetative">营养生长期</option>
            <option value="flowering">开花期</option>
            <option value="fruiting">结果期</option>
            <option value="harvest">收获期</option>
          </select>
        </div>

        <div class="form-group">
          <label>问题描述：</label>
          <textarea v-model="suggestion.problemDescription" rows="4"></textarea>
        </div>

        <button @click="getAISuggestion">获取AI建议</button>
      </div>

      <div v-if="suggestionResult" class="result-area">
        <h4>AI建议：</h4>
        <pre>{{ suggestionResult }}</pre>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

export default {
  name: 'AIAnalysis',
  setup() {
    const fileInput = ref(null)
    const selectedFile = ref(null)
    const selectedImage = ref(null)
    const selectedCrop = ref('tomato')
    const analysisResult = ref(null)
    const loading = ref(false)
    const loadingType = ref('')

    const suggestion = ref({
      cropType: 'tomato',
      temperature: 25.0,
      humidity: 60.0,
      ph: 6.5,
      growthStage: 'vegetative',
      problemDescription: ''
    })
    const suggestionResult = ref(null)

    const handleImageUpload = (event) => {
      const file = event.target.files[0]
      if (file) {
        selectedFile.value = file
        const reader = new FileReader()
        reader.onload = (e) => {
          selectedImage.value = e.target.result
        }
        reader.readAsDataURL(file)
      }
    }

    const clearImage = () => {
      selectedFile.value = null
      selectedImage.value = null
      if (fileInput.value) {
        fileInput.value.value = ''
      }
    }

    const analyzeRipeness = async () => {
      if (!selectedFile.value) {
        ElMessage.warning('请先选择图片')
        return
      }

      loading.value = true
      loadingType.value = 'ripeness'

      const formData = new FormData()
      formData.append('image', selectedFile.value)
      formData.append('cropType', selectedCrop.value)

      try {
        const response = await request.post('/api/ai/ripeness', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        analysisResult.value = response
        ElMessage.success('成熟度检测完成')
      } catch (error) {
        console.error('成熟度检测失败:', error)
        ElMessage.error('成熟度检测失败: ' + (error.message || '未知错误'))
      } finally {
        loading.value = false
        loadingType.value = ''
      }
    }

    const analyzeDisease = async () => {
      if (!selectedFile.value) {
        ElMessage.warning('请先选择图片')
        return
      }

      loading.value = true
      loadingType.value = 'disease'

      const formData = new FormData()
      formData.append('image', selectedFile.value)
      formData.append('cropType', selectedCrop.value)

      try {
        const response = await request.post('/api/ai/disease', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        analysisResult.value = response
        ElMessage.success('病虫害检测完成')
      } catch (error) {
        console.error('病虫害检测失败:', error)
        ElMessage.error('病虫害检测失败: ' + (error.message || '未知错误'))
      } finally {
        loading.value = false
        loadingType.value = ''
      }
    }

    const analyzeDual = async () => {
      if (!selectedFile.value) {
        ElMessage.warning('请先选择图片')
        return
      }

      loading.value = true
      loadingType.value = 'dual'

      const formData = new FormData()
      formData.append('image', selectedFile.value)
      formData.append('cropType', selectedCrop.value)

      try {
        const response = await request.post('/api/ai/analyze', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        analysisResult.value = response
        ElMessage.success('双检分析完成')
      } catch (error) {
        console.error('双检分析失败:', error)
        ElMessage.error('双检分析失败: ' + (error.message || '未知错误'))
      } finally {
        loading.value = false
        loadingType.value = ''
      }
    }

    const getAISuggestion = async () => {
      try {
        const response = await request.post('/api/ai/suggest', suggestion.value)
        suggestionResult.value = response
        ElMessage.success('AI建议生成完成')
      } catch (error) {
        console.error('获取AI建议失败:', error)
        ElMessage.error('获取AI建议失败: ' + (error.message || '未知错误'))
      }
    }

    const formatResult = (result) => {
      if (typeof result === 'object') {
        return JSON.stringify(result, null, 2)
      }
      return result
    }

    return {
      fileInput,
      selectedFile,
      selectedImage,
      selectedCrop,
      analysisResult,
      suggestion,
      suggestionResult,
      loading,
      loadingType,
      handleImageUpload,
      clearImage,
      analyzeRipeness,
      analyzeDisease,
      analyzeDual,
      getAISuggestion,
      formatResult
    }
  }
}
</script>

<style scoped>
.ai-analysis {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.ai-analysis h2 {
  color: #2c3e50;
  margin-bottom: 30px;
  font-size: 28px;
  text-align: center;
}

.analysis-section, .suggestion-section {
  margin-bottom: 30px;
  padding: 25px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background-color: #fafafa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.analysis-section h3, .suggestion-section h3 {
  color: #4CAF50;
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 22px;
}

.upload-area {
  margin-bottom: 25px;
}

.upload-area input[type="file"] {
  padding: 10px;
  border: 2px dashed #4CAF50;
  border-radius: 8px;
  width: 100%;
  cursor: pointer;
  background-color: #fff;
}

.image-preview {
  margin-top: 20px;
  max-width: 400px;
  position: relative;
}

.image-preview img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  border: 3px solid #4CAF50;
}

.clear-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.clear-btn:hover {
  background-color: #d32f2f;
}

.crop-selection {
  margin-bottom: 25px;
}

.crop-selection label {
  display: block;
  margin-bottom: 10px;
  font-weight: 600;
  color: #333;
  font-size: 16px;
}

.crop-selection select {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  background-color: white;
  cursor: pointer;
}

.crop-selection select:focus {
  outline: none;
  border-color: #4CAF50;
}

.analysis-buttons {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.analysis-buttons button {
  padding: 12px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(76, 175, 80, 0.3);
}

.analysis-buttons button:hover:not(:disabled) {
  background-color: #45a049;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.4);
}

.analysis-buttons button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.analysis-buttons button.loading {
  opacity: 0.7;
  cursor: wait;
}

.result-area {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  white-space: pre-wrap;
  font-family: 'Courier New', monospace;
  border: 1px solid #e0e0e0;
  max-height: 500px;
  overflow-y: auto;
}

.result-area h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
  font-size: 18px;
  border-bottom: 2px solid #4CAF50;
  padding-bottom: 10px;
}

.suggestion-form {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.form-group input, .form-group select, .form-group textarea {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  background-color: white;
  transition: border-color 0.3s ease;
}

.form-group input:focus, .form-group select:focus, .form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
}

.form-group button {
  padding: 12px 30px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.form-group button:hover {
  background-color: #45a049;
  transform: translateY(-2px);
}
</style>