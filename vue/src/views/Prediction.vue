<template>
  <div class="prediction">
    <h2>智能预测分析</h2>

    <div class="prediction-form">
      <h3>产量预测</h3>
      <div class="form-group">
        <label>作物类型：</label>
        <select v-model="prediction.cropType">
          <option value="tomato">番茄</option>
          <option value="corn">玉米</option>
          <option value="rice">水稻</option>
          <option value="strawberry">草莓</option>
          <option value="cucumber">黄瓜</option>
          <option value="pepper">辣椒</option>
        </select>
      </div>

      <div class="form-group">
        <label>种植面积（亩）：</label>
        <input type="number" v-model="prediction.plantingArea" step="0.1" />
      </div>

      <div class="form-group">
        <label>平均产量（公斤/亩）：</label>
        <input type="number" v-model="prediction.averageYield" step="0.1" />
      </div>

      <div class="form-group">
        <label>肥料使用量（公斤/亩）：</label>
        <input type="number" v-model="prediction.fertilizerUsage" step="0.1" />
      </div>

      <div class="form-group">
        <label>灌溉量（立方米/亩）：</label>
        <input type="number" v-model="prediction.irrigationAmount" step="0.1" />
      </div>

      <div class="form-group">
        <label>温度（℃）：</label>
        <input type="number" v-model="prediction.temperature" step="0.1" />
      </div>

      <div class="form-group">
        <label>湿度（%）：</label>
        <input type="number" v-model="prediction.humidity" step="0.1" />
      </div>

      <div class="form-group">
        <label>生长阶段：</label>
        <select v-model="prediction.growthStage">
          <option value="seedling">幼苗期</option>
          <option value="vegetative">营养生长期</option>
          <option value="flowering">开花期</option>
          <option value="fruiting">结果期</option>
          <option value="harvest">收获期</option>
        </select>
      </div>

      <div class="form-buttons">
        <button @click="predictYield">预测产量</button>
        <button @click="trainModel">训练模型</button>
      </div>
    </div>

    <div v-if="predictionResult" class="result-area">
      <h3>预测结果：</h3>
      <div v-if="predictionResult.predictedYield">
        <p>预测总产量：{{ predictionResult.predictedYield }} 公斤</p>
        <p>预测亩产量：{{ predictionResult.predictedYieldPerMu }} 公斤/亩</p>
        <p>预测准确率：{{ predictionResult.accuracy }}%</p>
        <p>建议：{{ predictionResult.suggestion }}</p>
      </div>
    </div>

    <div class="history-section">
      <h3>历史预测数据</h3>
      <div class="form-group">
        <label>作物类型：</label>
        <select v-model="historyFilter.cropType">
          <option value="">全部</option>
          <option value="tomato">番茄</option>
          <option value="corn">玉米</option>
          <option value="rice">水稻</option>
          <option value="strawberry">草莓</option>
          <option value="cucumber">黄瓜</option>
          <option value="pepper">辣椒</option>
        </select>
      </div>

      <div class="form-group">
        <label>开始日期：</label>
        <input type="date" v-model="historyFilter.startDate" />
      </div>

      <div class="form-group">
        <label>结束日期：</label>
        <input type="date" v-model="historyFilter.endDate" />
      </div>

      <button @click="getPredictionHistory">查询历史数据</button>

      <div v-if="historyData.length > 0" class="history-table">
        <table>
          <thead>
            <tr>
              <th>日期</th>
              <th>作物类型</th>
              <th>种植面积</th>
              <th>预测产量</th>
              <th>实际产量</th>
              <th>准确率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in historyData" :key="item.id">
              <td>{{ item.date }}</td>
              <td>{{ item.cropType }}</td>
              <td>{{ item.plantingArea }}</td>
              <td>{{ item.predictedYield }}</td>
              <td>{{ item.actualYield || 'N/A' }}</td>
              <td>{{ item.accuracy }}%</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="model-info">
      <h3>模型信息</h3>
      <button @click="getModelInfo">获取模型信息</button>
      <div v-if="modelInfo" class="info-details">
        <p>模型名称：{{ modelInfo.modelName }}</p>
        <p>训练数据量：{{ modelInfo.trainingDataSize }} 条</p>
        <p>最后训练时间：{{ modelInfo.lastTrainingTime }}</p>
        <p>平均准确率：{{ modelInfo.averageAccuracy }}%</p>
        <p>支持作物类型：{{ modelInfo.supportedCrops }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import request from '@/utils/request'

export default {
  name: 'Prediction',
  setup() {
    const prediction = ref({
      cropType: 'tomato',
      plantingArea: 10.0,
      averageYield: 5000.0,
      fertilizerUsage: 50.0,
      irrigationAmount: 100.0,
      temperature: 25.0,
      humidity: 60.0,
      growthStage: 'vegetative'
    })

    const predictionResult = ref(null)
    const historyFilter = ref({
      cropType: '',
      startDate: '',
      endDate: ''
    })
    const historyData = ref([])
    const modelInfo = ref(null)

    const predictYield = async () => {
      try {
        const response = await request.post('/api/prediction/yield', prediction.value)
        predictionResult.value = response.data
      } catch (error) {
        console.error('预测失败:', error)
      }
    }

    const getPredictionHistory = async () => {
      try {
        const params = {}
        if (historyFilter.value.cropType) {
          params.cropType = historyFilter.value.cropType
        }
        if (historyFilter.value.startDate) {
          params.startDate = historyFilter.value.startDate
        }
        if (historyFilter.value.endDate) {
          params.endDate = historyFilter.value.endDate
        }

        const response = await request.get('/api/prediction/history', { params })
        historyData.value = response.data
      } catch (error) {
        console.error('获取历史数据失败:', error)
      }
    }

    const getModelInfo = async () => {
      try {
        const response = await request.get('/api/prediction/model-info')
        modelInfo.value = response.data
      } catch (error) {
        console.error('获取模型信息失败:', error)
      }
    }

    const trainModel = async () => {
      try {
        await request.post('/api/prediction/train')
        alert('模型训练开始，请稍后查看结果')
      } catch (error) {
        console.error('训练模型失败:', error)
      }
    }

    return {
      prediction,
      predictionResult,
      historyFilter,
      historyData,
      modelInfo,
      predictYield,
      getPredictionHistory,
      getModelInfo,
      trainModel
    }
  }
}
</script>

<style scoped>
.prediction {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.prediction-form, .history-section, .model-info {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input, .form-group select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.form-buttons button {
  padding: 10px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.result-area {
  margin-top: 20px;
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}

.history-table {
  margin-top: 20px;
  width: 100%;
  border-collapse: collapse;
}

.history-table th, .history-table td {
  padding: 8px;
  border: 1px solid #ddd;
  text-align: left;
}

.history-table th {
  background-color: #f2f2f2;
}

.info-details {
  margin-top: 15px;
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}
</style>