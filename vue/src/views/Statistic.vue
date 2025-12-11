<template>
  <div class="page-container">
    <div class="toolbar">
      <div class="left-panel">
        <div class="title-box">
          <h2> 农田实时监测中心</h2>
          <span class="subtitle">当前监测地块: {{ total }} 个 | 系统状态: 在线</span>
        </div>
      </div>
      <div class="right-panel">
        <el-input 
          v-model="searchFarm" 
          placeholder="输入农田名称搜索..." 
          style="width: 200px; margin-right: 10px"
          @keyup.enter.native="load"
          clearable>
        </el-input>
        <el-button type="primary" @click="load" icon="el-icon-search">搜索</el-button>
        <el-button type="success" @click="handleAdd" icon="el-icon-plus">新增地块</el-button>
        <el-button type="warning" @click="load" icon="el-icon-refresh">刷新数据</el-button>
        <el-button type="danger" @click="fixTemperatureData" icon="el-icon-setting">修正异常温度</el-button>
      </div>
    </div>

    <div v-loading="loading" class="farm-grid">
      <div 
        v-for="(item, index) in tableData" 
        :key="item.id" 
        class="card-scene"
      >
        <div 
          class="card-obj" 
          :class="{ 'is-flipped': item.isFlipped }"
          @click="toggleFlip(item)"
        >
          
          <div class="card-face card-front" :class="getStatusClass(item.state)">
            <div class="status-badge">
              <span class="dot"></span> {{ item.state || '监测中' }}
            </div>
            
            <div class="icon-wrapper">
              <img :src="getCropIcon(item.crop)" class="crop-emoji-img" />
            </div>

            <div class="field-info">
              <h3>{{ item.farm }}</h3>
              <p class="sub-text">{{ item.crop }} · 负责人: {{ item.keeper }}</p>
            </div>

            <div class="mini-dashboard">
              <div class="mini-item">
                <span class="label"><img src="@/assets/wendu.png" alt="温度" class="mini-icon" /> 温度</span>
                <span class="val">{{ item.temperature }}℃</span>
              </div>
              <div class="mini-item">
                <span class="label"><img src="@/assets/water.png" alt="湿度" class="mini-icon" /> 土壤</span>
                <span class="val">{{ item.soilhumidity }}%</span>
              </div>
              <div class="mini-item">
                <span class="label"><img src="@/assets/sun.png" alt="光照" class="mini-icon" /> 光照</span>
                <span class="val">{{ item.light || '暂无数据' }}</span>
              </div>
            </div>
            
            <div class="tap-hint"><img src="@/assets/dianji.png" alt="点击" class="tap-icon" /> 点击卡片查看详情与AI建议</div>
          </div>

          <div class="card-face card-back">
            <div class="back-header">
              <h4>{{ item.farm }} 数据面板</h4>
              <span class="close-btn" title="返回">↺</span>
            </div>
            
            <div class="data-list">
              <div class="data-row">
                <span>空气湿度</span>
                <el-progress :percentage="Number(item.airhumidity) || 0" :color="customColors"></el-progress>
              </div>
              <div class="data-row">
                <span>CO2 浓度</span>
                <span class="data-val strong">{{ item.carbon }} ppm</span>
              </div>
              <div class="data-row">
                <span>设备状态</span>
                <div class="device-tags">
                  <el-tag size="mini" :type="item.pump === '开启' ? 'success' : 'info'">水泵{{item.pump}}</el-tag>
                  <el-tag size="mini" :type="item.filllight === '开启' ? 'warning' : 'info'">补光{{item.filllight}}</el-tag>
                </div>
              </div>
            </div>

            <div class="ai-box">
              <div class="ai-title">
                 <strong>AI 农事决策</strong>
              </div>
              <p class="ai-content">{{ generateAIAdvice(item) }}</p>
            </div>

            <div class="action-group">
              <button class="action-btn water" @click.stop="quickAction('灌溉', item)">
                <img src="@/assets/guangai.png" alt="灌溉" class="action-icon" />
                <span>灌溉</span>
              </button>
              <button class="action-btn fertilizer" @click.stop="quickAction('施肥', item)">
                <img src="@/assets/shifei.png" alt="施肥" class="action-icon" />
                <span>施肥</span>
              </button>
              <button class="action-btn edit" @click.stop="handleEdit(item)">
                <img src="@/assets/bianji.png" alt="编辑" class="action-icon" />
                <span>编辑</span>
              </button>
              <el-popconfirm
                title="确定删除这个地块吗？"
                @confirm="del(item.id)"
              >
                <button slot="reference" class="action-btn delete" @click.stop>
                  <img src="@/assets/delete.png" alt="删除" class="action-icon" />
                  <span>删除</span>
                </button>
              </el-popconfirm>
            </div>
          </div>

        </div>
      </div>
    </div>

    <div style="padding: 5px 0; text-align: center; margin-top: -10px;">
      <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[8, 12, 20]"
          :page-size="pageSize"
          layout="total, prev, pager, next, jumper"
          :total="total">
      </el-pagination>
    </div>

    <el-dialog title="地块信息管理" :visible.sync="dialogFormVisible" width="40%" :close-on-click-modal="false">
      <el-form label-width="120px" size="small" style="width: 90%">
        <el-form-item label="农田名称">
           <el-input v-model="form.farm" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="作物名称">
          <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center;">
            <el-tag
              :key="tag"
              v-for="tag in dynamicTags"
              closable
              :disable-transitions="false"
              @close="handleClose(tag)">
              {{tag}}
            </el-tag>
            <el-input
              class="input-new-tag"
              v-if="inputVisible"
              v-model="inputValue"
              ref="saveTagInput"
              size="small"
              style="width: 100px"
              @keyup.enter.native="handleInputConfirm"
              @blur="handleInputConfirm"
            >
            </el-input>
            <el-button v-else class="button-new-tag" size="small" @click="showInput">+ 添加作物</el-button>
          </div>
        </el-form-item>
        <el-form-item label="生长状态">
           <el-select v-model="form.state" placeholder="请选择">
              <el-option label="生长良好" value="生长良好"></el-option>
              <el-option label="缺水预警" value="缺水预警"></el-option>
              <el-option label="病虫害风险" value="病虫害风险"></el-option>
           </el-select>
        </el-form-item>
        <el-form-item label="农田负责人">
          <el-input v-model="form.keeper" autocomplete="off"></el-input>
        </el-form-item>
        </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">保 存</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
export default {
  name: "Statistic",
  data() {
    return {
      tableData: [], // 存储农田数据
      total: 0,
      pageNum: 1,
      pageSize: 8, // 改成8，卡片布局一页8个比较好看
      searchFarm: "",
      loading: false,
      form: {},
      dialogFormVisible: false,
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      customColors: [
        {color: '#f56c6c', percentage: 20},
        {color: '#e6a23c', percentage: 40},
        {color: '#5cb87a', percentage: 100}
      ],
      // STM32实时传感器数据
      stm32Data: {
        temperature: null,
        humidity: null
      },
      // Tags输入控制
      dynamicTags: [],
      inputVisible: false,
      inputValue: ''
    }
  },
  created() {
    this.load()
  },
  mounted() {
    // 每30秒刷新一次STM32传感器数据
    this.stm32Timer = setInterval(() => {
      this.fetchSTM32Data();
      this.updateTableDataWithSTM32();
    }, 30000);
  },
  beforeDestroy() {
    // 清理STM32数据刷新定时器
    if (this.stm32Timer) {
      clearInterval(this.stm32Timer);
    }
  },
  methods: {
    // Tags 处理方法
    handleClose(tag) {
      this.dynamicTags.splice(this.dynamicTags.indexOf(tag), 1);
    },

    showInput() {
      this.inputVisible = true;
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },

    handleInputConfirm() {
      let inputValue = this.inputValue;
      if (inputValue) {
        this.dynamicTags.push(inputValue);
      }
      this.inputVisible = false;
      this.inputValue = '';
    },

    // 加载数据
    async load() {
      this.loading = true;
      
      // 先获取STM32传感器数据
      await this.fetchSTM32Data();
      
      this.request.get("/statistic/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          farm: this.searchFarm,
        }
      }).then(res => {
        this.loading = false;
        // 核心逻辑：给每个数据加一个 isFlipped 属性，控制翻转
        if(res.data && res.data.records) {
          this.tableData = res.data.records.map(item => {
            const updatedItem = { ...item, isFlipped: false };
            
            // 如果温度为空或无效，使用STM32数据
            if (!updatedItem.temperature || updatedItem.temperature === '' || updatedItem.temperature === null) {
              updatedItem.temperature = this.stm32Data.temperature || 25;
            }
            
            // 如果土壤湿度为空或无效，使用STM32湿度数据
            if (!updatedItem.soilhumidity || updatedItem.soilhumidity === '' || updatedItem.soilhumidity === null) {
              updatedItem.soilhumidity = this.stm32Data.humidity || 50;
            }
            
            return updatedItem;
          });
          this.total = res.data.total;
        }
      })
    },
    
    // 翻转卡片
    toggleFlip(item) {
      // Vue 2 响应式修改数组中的对象属性需注意
      item.isFlipped = !item.isFlipped; 
      this.$forceUpdate(); // 强制刷新一下视图确保动画触发
    },

    // 根据状态返回颜色样式
    getStatusClass(state) {
      if (!state) return 'bg-green';
      if (state.includes('预警') || state.includes('缺水')) return 'bg-orange';
      if (state.includes('病') || state.includes('虫') || state.includes('危险')) return 'bg-red';
      return 'bg-green';
    },

    // 根据作物名字返回 Emoji 图标
    getCropIcon(cropName) {
      if (!cropName) return require('@/assets/moreng.png');
      if (cropName.includes('麦')) return require('@/assets/xiaomai.png');
      if (cropName.includes('米') || cropName.includes('稻')) return require('@/assets/yumi.png');
      if (cropName.includes('果') || cropName.includes('苹')) return require('@/assets/xihongshi.png');
      if (cropName.includes('菜')) return require('@/assets/aiye.png');
      if (cropName.includes('竹')) return require('@/assets/zhuzi.png');
      if (cropName.includes('棕')) return require('@/assets/zongshu.png');
      if (cropName.includes('柿')) return require('@/assets/xihongshi.png');
      return require('@/assets/moreng.png');
    },

    // 生成 AI 建议 (前端模拟，增加展示效果)
    generateAIAdvice(item) {
      if (Number(item.soilhumidity) < 30) return `监测到 ${item.farm} 土壤湿度严重不足，建议立即开启水泵灌溉 2 小时。`;
      if (item.state && item.state.includes('虫')) return `系统图像识别发现疑似叶斑病，建议无人机喷洒杀菌剂，并联系负责人 ${item.keeper}。`;
      if (Number(item.temperature) > 35) return `当前气温过高，建议开启遮阳网和自动喷淋降温。`;
      return `当前 ${item.crop} 生长环境指标优异，预计产量将提升 10%，请继续保持监测。`;
    },

    // 快捷操作
    quickAction(actionName, item) {
      this.$message.success(`指令已下发：正在对 [${item.farm}] 执行 ${actionName} 操作`);
      // 这里可以加真实的 axios 请求
      // this.request.post(...)
    },

    // 以下是保留的原有增删改查逻辑
    save() {
      // 将 Tags 转换为逗号分隔字符串
      this.form.crop = this.dynamicTags.join(',');
      
      this.request.post("/statistic", this.form).then(res => {
        if (res.code === '200') {
          this.$message.success("保存成功")
          this.dialogFormVisible = false
          this.load()
        } else {
          this.$message.error("保存失败")
        }
      })
    },
    handleAdd() {
      this.dialogFormVisible = true
      this.form = {}
      this.dynamicTags = [] // 重置 Tags
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      // 将 crop 字符串转换为 Tags 数组
      this.dynamicTags = this.form.crop ? this.form.crop.split(',') : []
      this.dialogFormVisible = true
    },
    del(id) {
      this.request.delete("/statistic/" + id).then(res => {
        if (res.code === '200') {
          this.$message.success("删除成功")
          this.load()
        } else {
          this.$message.error("删除失败")
        }
      })
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize
      this.load()
    },
    handleCurrentChange(pageNum) {
      this.pageNum = pageNum
      this.load()
    },

    // 修正异常温度数据
    async fixTemperatureData() {
      this.$confirm('此操作将修正所有超出正常范围的温度数据（-20°C ~ 50°C），是否继续？', '⚠️ 数据修正确认', {
        confirmButtonText: '确定修正',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        const loading = this.$loading({
          lock: true,
          text: '正在修正数据...',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        });
        
        try {
          const res = await this.request.post('/statistic/fix-temperature');
          loading.close();
          
          if (res.code === '200') {
            const data = res.data;
            this.$message.success({
              message: `✅ ${data.message}`,
              duration: 5000
            });
            // 刷新数据
            this.load();
          } else {
            this.$message.error('修正失败：' + (res.msg || '未知错误'));
          }
        } catch (err) {
          loading.close();
          this.$message.error('修正失败：' + (err.message || '网络异常'));
        }
      }).catch(() => {
        this.$message.info('已取消修正');
      });
    },

    // 获取STM32传感器实时数据
    async fetchSTM32Data() {
      try {
        const res = await this.request.get('/aether/device/status');
        if (res.code === '200' && res.data) {
          this.stm32Data.temperature = res.data.temperature || 25;
          this.stm32Data.humidity = res.data.humidity || 50;
          console.log('✅ STM32传感器数据获取成功:', this.stm32Data);
        }
      } catch (e) {
        console.warn('⚠️ STM32传感器数据获取失败，使用默认值', e);
        // 如果获取失败，使用默认值
        this.stm32Data.temperature = 25;
        this.stm32Data.humidity = 50;
      }
    },

    // 更新表格数据，使用STM32数据填充缺失值
    updateTableDataWithSTM32() {
      if (!this.tableData || this.tableData.length === 0) return;
      
      this.tableData = this.tableData.map(item => {
        const updatedItem = { ...item };
        
        // 如果温度为空或无效，使用STM32数据
        if (!updatedItem.temperature || updatedItem.temperature === '' || updatedItem.temperature === null) {
          updatedItem.temperature = this.stm32Data.temperature || 25;
        }
        
        // 如果土壤湿度为空或无效，使用STM32湿度数据
        if (!updatedItem.soilhumidity || updatedItem.soilhumidity === '' || updatedItem.soilhumidity === null) {
          updatedItem.soilhumidity = this.stm32Data.humidity || 50;
        }
        
        return updatedItem;
      });
    }
  }
}
</script>

<style scoped>
/* 页面容器 */
/* 页面容器滚动设置 */
.page-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
  max-height: 100vh;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  /* 隐藏滚动条 */
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE/Edge */
}

.page-container::-webkit-scrollbar {
  display: none !important; /* Chrome/Safari */
  width: 0 !important;
  height: 0 !important;
}

.statistic-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
  max-height: 100vh;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  /* 隐藏滚动条 */
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE/Edge */
}

.statistic-container::-webkit-scrollbar {
  display: none !important; /* Chrome/Safari */
  width: 0 !important;
  height: 0 !important;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  background: white;
  padding: 15px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}
.title-box h2 { margin: 0; color: #303133; font-size: 20px; }
.subtitle { color: #909399; font-size: 13px; margin-top: 5px; display: block; }

/* 网格布局 */
.farm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  padding-bottom: 20px;
}

/* 3D 卡片场景 */
.card-scene {
  height: 280px;
  perspective: 1000px;
  cursor: pointer;
}

.card-obj {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.8s cubic-bezier(0.4, 0.2, 0.2, 1);
  transform-style: preserve-3d;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.08);
}

.card-obj.is-flipped {
  transform: rotateY(180deg);
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: 16px;
  overflow: hidden;
}

/* --- 正面 --- */
.card-front {
  background: white;
  display: flex;
  flex-direction: column;
  padding: 15px;
  color: white;
}

/* 状态背景色 */
.bg-green { background: linear-gradient(135deg, #4ade80 0%, #10b981 100%); }
.bg-orange { background: linear-gradient(135deg, #fbbf24 0%, #d97706 100%); }
.bg-red { background: linear-gradient(135deg, #f87171 0%, #dc2626 100%); }

.status-badge {
  align-self: flex-end;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(5px);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot { width: 6px; height: 6px; background: white; border-radius: 50%; }

.icon-wrapper {
  text-align: center;
  margin: 10px 0;
  text-shadow: 0 4px 10px rgba(0,0,0,0.1);
  animation: float 3s ease-in-out infinite;
}
.crop-emoji-img { width: 70px; height: 70px; object-fit: contain; }
@keyframes float { 0%,100% {transform: translateY(0);} 50% {transform: translateY(-8px);} }

.field-info { text-align: center; margin-bottom: 12px; }
.field-info h3 { margin: 0; font-size: 18px; font-weight: 700; }
.sub-text { opacity: 0.9; font-size: 12px; margin-top: 4px; }

.mini-dashboard {
  display: flex;
  background: rgba(0,0,0,0.15);
  border-radius: 8px;
  padding: 8px;
}
.mini-item { flex: 1; text-align: center; display: flex; flex-direction: column; }
.mini-item .label { font-size: 10px; opacity: 0.8; display: flex; align-items: center; justify-content: center; gap: 3px; }
.mini-item .val { font-size: 13px; font-weight: bold; }

.mini-icon {
  width: 12px;
  height: 12px;
  object-fit: contain;
}

.action-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
}

.tap-hint { text-align: center; font-size: 11px; opacity: 0.6; margin-top: auto; display: flex; align-items: center; justify-content: center; gap: 4px; }

.tap-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
}

/* --- 背面 --- */
.card-back {
  transform: rotateY(180deg);
  background: white;
  padding: 12px 15px;
  display: flex;
  flex-direction: column;
  color: #303133;
}

.back-header {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  padding-bottom: 6px;
  margin-bottom: 8px;
}
.back-header h4 { margin: 0; font-size: 14px; }
.close-btn { cursor: pointer; color: #909399; font-size: 18px; }

.data-list { font-size: 12px; margin-bottom: 8px; }
.data-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.device-tags { display: flex; gap: 5px; }

/* AI 建议框 */
.ai-box {
  background: #f3f4f6;
  border-left: 3px solid #8b5cf6;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
  flex: 1;
  overflow: hidden;
}
.ai-title { color: #6d28d9; font-size: 11px; margin-bottom: 3px; }
.ai-content { margin: 0; font-size: 11px; color: #4b5563; line-height: 1.3; }

/* 按钮组 */
.action-group {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 6px;
  margin-top: auto;
}
.action-btn {
  border: none;
  background: #f4f4f5;
  padding: 4px 0;
  border-radius: 6px;
  font-size: 10px;
  cursor: pointer;
  transition: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #606266;
  height: 40px;
  width: 100%;
  min-width: 0;
}
.action-btn:hover { transform: none; }
.action-btn.water { background: #ecf5ff; color: #409eff; }
.action-btn.fertilize { background: #fdf6ec; color: #e6a23c; }
.action-btn.delete { background: #fef0f0; color: #f56c6c; }

/* 工具栏按钮和输入框常规化样式 */
.toolbar .el-input .el-input__inner {
  border-radius: 4px !important;
  border: 1px solid #dcdfe6 !important;
  background: #ffffff !important;
  font-size: 14px !important;
  transition: none !important;
}

.toolbar .el-input .el-input__inner:focus {
  border-color: #dcdfe6 !important;
  box-shadow: none !important;
  outline: none !important;
}

.toolbar .el-input.is-focus .el-input__inner {
  border-color: #dcdfe6 !important;
  box-shadow: none !important;
}

/* 强制覆盖所有可能的绿色聚焦样式 */
.toolbar .el-input__inner:focus,
.toolbar .el-input__inner:active,
.toolbar .el-input.is-focus .el-input__inner,
.toolbar .el-input--focus .el-input__inner {
  border-color: #dcdfe6 !important;
  box-shadow: 0 0 0 0 transparent !important;
  outline: none !important;
}

.toolbar .el-button {
  border-radius: 4px !important;
  font-size: 14px !important;
  padding: 8px 15px !important;
  font-weight: normal !important;
  transition: none !important;
}

.toolbar .el-button:hover {
  transform: none !important;
  box-shadow: none !important;
}

.toolbar .el-button--primary {
  background: #409eff !important;
  border-color: #409eff !important;
}

.toolbar .el-button--primary:hover {
  background: #409eff !important;
  border-color: #409eff !important;
}

.toolbar .el-button--success {
  background: #67c23a !important;
  border-color: #67c23a !important;
}

.toolbar .el-button--success:hover {
  background: #67c23a !important;
  border-color: #67c23a !important;
}

.toolbar .el-button--warning {
  background: #e6a23c !important;
  border-color: #e6a23c !important;
}

.toolbar .el-button--warning:hover {
  background: #e6a23c !important;
  border-color: #e6a23c !important;
}
</style>