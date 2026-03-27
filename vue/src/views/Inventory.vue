<template>
  <div class="jd-logistics-page">
    <!-- 顶部 KPI 看板 -->
    <div class="kpi-dashboard">
      <div class="kpi-card primary">
        <div class="kpi-icon">📦</div>
        <div class="kpi-content">
          <div class="kpi-label">物资总数</div>
          <div class="kpi-value">{{ total }}</div>
          <div class="kpi-trend">库存管理</div>
        </div>
      </div>
      
      <div class="kpi-card warning">
        <div class="kpi-icon"></div>
        <div class="kpi-content">
          <div class="kpi-label">库存预警</div>
          <div class="kpi-value">{{ lowStockCount }}</div>
          <div class="kpi-trend">低于安全库存</div>
        </div>
      </div>
      
      <div class="kpi-card success">
        <div class="kpi-icon">✓</div>
        <div class="kpi-content">
          <div class="kpi-label">充足库存</div>
          <div class="kpi-value">{{ sufficientStockCount }}</div>
          <div class="kpi-trend">库存健康</div>
        </div>
      </div>
      
      <div class="kpi-card info">
        <div class="kpi-icon">🌡</div>
        <div class="kpi-content">
          <div class="kpi-label">仓库环境</div>
          <div class="kpi-value">{{ warehouseTemp.toFixed(1) }}°C</div>
          <div class="kpi-trend">湿度 {{ warehouseHumidity.toFixed(0) }}%</div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 数据表格卡片 -->
      <div class="data-table-card">
        <!-- 工具栏 -->
        <div class="table-toolbar">
          <div class="toolbar-left">
            <h3 class="table-title">库存列表</h3>
            <div class="data-count">共 {{ total }} 项物资</div>
          </div>
          <div class="toolbar-right">
            <div class="search-box">
              <input 
                type="text" 
                class="jd-input" 
                v-model="produce" 
                placeholder="搜索物资名称"
                @keyup.enter="load"
              />
              <button class="jd-btn" @click="load">搜索</button>
            </div>
            <button class="jd-btn primary" @click="handleAdd">
              <span>+ 入库登记</span>
            </button>
            <button class="jd-btn" @click="exp">
              <span>↓ 导出报表</span>
            </button>
          </div>
        </div>

        <!-- 数据表格 -->
        <div v-if="loading" class="table-loading">
          <div class="loading-spinner"></div>
          <div>加载中...</div>
        </div>
        
        <div v-else class="inventory-table">
          <!-- 表格头部 -->
          <div class="table-header">
            <div class="th" style="width: 50px">序号</div>
            <div class="th" style="width: 200px">物资名称</div>
            <div class="th" style="width: 100px">仓库位置</div>
            <div class="th" style="width: 150px">库存状态</div>
            <div class="th" style="width: 80px">当前库存</div>
            <div class="th" style="width: 100px">库存占比</div>
            <div class="th" style="width: 80px">保管员</div>
            <div class="th" style="flex: 1">备注</div>
            <div class="th" style="width: 150px">操作</div>
          </div>
          
          <!-- 表格体 -->
          <div class="table-body">
            <div 
              v-for="(item, index) in tableData" 
              :key="item.id" 
              class="table-row"
            >
              <div class="td" style="width: 50px">{{ (pageNum - 1) * pageSize + index + 1 }}</div>
              <div class="td" style="width: 200px">
                <div class="item-name-cell">
                  <img :src="getItemImage(item.produce)" class="item-thumb" :alt="item.produce" />
                  <span class="item-name-text">{{ item.produce }}</span>
                </div>
              </div>
              <div class="td" style="width: 100px">
                <div class="location-cell">
                  <div>{{ item.warehouse }}</div>
                  <div class="location-sub">{{ item.region }}</div>
                </div>
              </div>
              <div class="td" style="width: 150px">
                <div class="stock-bar-cell">
                  <div class="stock-bar-bg">
                    <div 
                      class="stock-bar-fill" 
                      :class="getStockLevelClass(item)"
                      :style="{ width: getStockPercentage(item) + '%' }"
                    ></div>
                  </div>
                  <span class="stock-percent">{{ getStockPercentage(item) }}%</span>
                </div>
              </div>
              <div class="td" style="width: 80px">
                <span class="stock-number" :class="getStockLevelClass(item)">{{ item.number }}</span>
              </div>
              <div class="td" style="width: 100px">
                <span class="stock-ratio">{{ item.number }}/{{ item.maxStock }}</span>
              </div>
              <div class="td" style="width: 80px">{{ item.keeper }}</div>
              <div class="td" style="flex: 1">{{ item.remark || '-' }}</div>
              <div class="td" style="width: 150px">
                <div class="action-btns">
                  <button class="action-link" @click="handleEdit(item)">编辑</button>
                  <span class="action-sep">|</span>
                  <button class="action-link danger" @click="del(item.id)">报废</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 入库对话框 -->
    <el-dialog title="📦 物资入库登记" :visible.sync="dialogFormVisible" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px" size="small" :model="form">
        <el-form-item label="物资名称">
          <el-input v-model="form.produce" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="所属仓库">
          <el-select v-model="form.warehouse" placeholder="请选择" style="width:100%">
            <el-option label="1号综合农资库" value="1号仓"></el-option>
            <el-option label="2号设备仓" value="2号仓"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="存放区域">
          <el-input v-model="form.region" placeholder="例如：A-01货架"></el-input>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="form.number" :min="1" style="width:100%"></el-input-number>
        </el-form-item>
        <el-form-item label="最大库存">
          <el-input-number v-model="form.maxStock" :min="1" placeholder="100" style="width:100%"></el-input-number>
        </el-form-item>
        <el-form-item label="管理员">
          <el-input v-model="form.keeper"></el-input>
        </el-form-item>
        <el-form-item label="备注">
          <el-input type="textarea" v-model="form.remark"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import apiConfig from '@/config/api.config';

export default {
  name: "Inventory",
  data() {
    return {
      apiConfig, // 引入API配置
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 12,
      produce: "",
      form: {},
      dialogFormVisible: false,
      multipleSelection: [],
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      loading: false,
      hoveredItem: null,
      // 仓库环境数据
      warehouseTemp: 24,
      warehouseHumidity: 45,
      expiringCount: 2
    }
  },
  computed: {
    // 低库存物资数量
    lowStockCount() {
      return this.tableData.filter(item => {
        const percentage = this.getStockPercentage(item)
        return percentage < 20
      }).length
    },
    // 充足库存物资数量
    sufficientStockCount() {
      return this.tableData.filter(item => {
        const percentage = this.getStockPercentage(item)
        return percentage >= 50
      }).length
    }
  },
  created() {
    this.load()
    this.startEnvironmentMonitor()
  },
  beforeDestroy() {
    if (this.envTimer) {
      clearInterval(this.envTimer)
    }
  },
  methods: {
    // Mock 数据生成（仅在 API 失败时使用）
    generateMockData() {
      console.warn('使用 Mock 数据演示，请检查后端 API 连接')
      const materials = [
        { name: '复合肥(NPK 15-15-15)', maxStock: 500, current: 450 },
        { name: '尿素(含氮46%)', maxStock: 300, current: 180 },
        { name: '草甘膦除草剂', maxStock: 200, current: 35 },
        { name: '吡虫啉杀虫剂', maxStock: 150, current: 120 },
        { name: '锄头(加厚型)', maxStock: 50, current: 8 },
        { name: '喷雾器(电动)', maxStock: 30, current: 25 },
        { name: '有机肥(腐熟鸡粪)', maxStock: 400, current: 380 },
        { name: '水溶肥(速效型)', maxStock: 100, current: 12 },
        { name: '地膜(0.01mm)', maxStock: 200, current: 190 },
        { name: '滴灌管(PE)', maxStock: 500, current: 420 },
        { name: '种子(玉米)', maxStock: 100, current: 5 },
        { name: '防虫网(40目)', maxStock: 150, current: 135 }
      ]
      
      const warehouses = ['1号仓', '2号仓']
      const regions = ['A-01', 'A-02', 'B-01', 'B-02', 'C-01']
      const keepers = ['张三', '李四', '王五', '赵六']
      
      return materials.map((mat, index) => ({
        id: index + 1,
        produce: mat.name,
        warehouse: warehouses[index % 2],
        region: regions[index % regions.length] + '货架',
        number: mat.current,
        maxStock: mat.maxStock,
        keeper: keepers[index % keepers.length],
        remark: mat.current < mat.maxStock * 0.2 ? '需要补货' : '库存充足'
      }))
    },
    
    // 数据加载 - 优先使用真实 API
    load() {
      this.loading = true;
      
      this.request.get("/inventory/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          produce: this.produce
        }
      }).then(res => {
        this.loading = false;
        
        if (res.data && res.data.records) {
          // 使用真实数据
          this.tableData = res.data.records.map(item => ({
            ...item,
            maxStock: item.maxStock || 100 // 设置默认最大库存
          }))
          this.total = res.data.total
        } else {
          // 降级到 Mock 数据
          this.loadMockData()
        }
      }).catch(err => {
        console.error('API 调用失败:', err)
        this.loading = false;
        this.loadMockData()
      })
    },
    
    // Mock 数据降级方案
    loadMockData() {
      const mockData = this.generateMockData()
      
      if (this.produce) {
        this.tableData = mockData.filter(item => 
          item.produce.includes(this.produce)
        )
      } else {
        this.tableData = mockData
      }
      this.total = this.tableData.length
      this.loading = false
    },
    save() {
      this.request.post("/inventory", this.form).then(res => {
        if (res.code === '200') {
          this.$message.success("操作成功")
          this.dialogFormVisible = false
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },
    
    handleAdd() {
      this.dialogFormVisible = true
      this.form = { maxStock: 100 }
    },
    
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogFormVisible = true
    },
    
    del(id) {
      this.$confirm('确认报废此物资？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.request.delete("/inventory/" + id).then(res => {
          if (res.code === '200') {
            this.$message.success("已报废出库")
            this.load()
          } else {
            this.$message.error("操作失败")
          }
        })
      }).catch(() => {})
    },
    
    exp() {
      window.open(this.apiConfig.inventoryExport)
    },
    
    // 计算库存百分比
    getStockPercentage(item) {
      const max = item.maxStock || 100
      const current = item.number || 0
      return Math.min(100, Math.round((current / max) * 100))
    },
    
    // 获取库存水位等级
    getStockLevelClass(item) {
      const percentage = this.getStockPercentage(item)
      if (percentage >= 50) return 'level-high'
      if (percentage >= 20) return 'level-medium'
      return 'level-low'
    },
    
    // 获取库存周转率（模拟）
    getTurnoverRate(item) {
      const rates = ['高', '中', '低']
      const hash = (item.id || 0) % 3
      return rates[hash]
    },
    
    // 获取物资图标
    getItemImage(name) {
      const imageMap = {
        '复合肥': require('@/assets/moreng.png'),
        '尿素': require('@/assets/yumi.png'),
        '草甘膦': require('@/assets/xiaomai.png'),
        '吡虫啉': require('@/assets/aiye.png'),
        '锄头': require('@/assets/moreng.png'),
        '喷雾器': require('@/assets/yumi.png'),
        '有机肥': require('@/assets/moreng.png'),
        '水溶肥': require('@/assets/xiaomai.png'),
        '地膜': require('@/assets/aiye.png'),
        '滴灌管': require('@/assets/yumi.png'),
        '种子': require('@/assets/moreng.png'),
        '防虫网': require('@/assets/aiye.png')
      }
      
      for (let key in imageMap) {
        if (name && name.includes(key)) {
          return imageMap[key]
        }
      }
      return require('@/assets/moreng.png')
    },
    
    // 温度等级
    getTempClass(temp) {
      if (temp >= 20 && temp <= 26) return 'suitable'
      if (temp > 26 && temp <= 30) return 'warning'
      return 'danger'
    },
    
    getTempLabel(temp) {
      if (temp >= 20 && temp <= 26) return '适宜'
      if (temp > 26 && temp <= 30) return '偏高'
      return '异常'
    },
    
    // 湿度等级
    getHumidityClass(humidity) {
      if (humidity >= 40 && humidity <= 60) return 'suitable'
      if (humidity < 40) return 'warning'
      return 'danger'
    },
    
    getHumidityLabel(humidity) {
      if (humidity >= 40 && humidity <= 60) return '正常'
      if (humidity < 40) return '干燥'
      return '潮湿'
    },
    
    // 启动环境监控
    startEnvironmentMonitor() {
      this.envTimer = setInterval(() => {
        // 模拟环境数据波动
        this.warehouseTemp = 22 + Math.random() * 6
        this.warehouseHumidity = 40 + Math.random() * 25
      }, 10000)
    }
  }
}
</script>

<style scoped>
/* ========== 京东物流蓝白风格 - 库存管理系统 ========== */

.jd-logistics-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
}

/* ===== 顶部 KPI 看板 ===== */
.kpi-dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  background: #ffffff;
  border-radius: 2px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-left: 4px solid #0066CC;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.kpi-card.primary {
  border-left-color: #0066CC;
}

.kpi-card.warning {
  border-left-color: #FA8C16;
}

.kpi-card.success {
  border-left-color: #52C41A;
}

.kpi-card.info {
  border-left-color: #1890FF;
}

.kpi-icon {
  width: 56px;
  height: 56px;
  border-radius: 2px;
  background: #E3F2FD;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.kpi-card.primary .kpi-icon {
  background: #E3F2FD;
}

.kpi-card.warning .kpi-icon {
  background: #FFF7E6;
}

.kpi-card.success .kpi-icon {
  background: #F6FFED;
}

.kpi-card.info .kpi-icon {
  background: #E6F7FF;
}

.kpi-content {
  flex: 1;
}

.kpi-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  line-height: 1;
  margin-bottom: 4px;
  font-family: 'DIN', 'Roboto Mono', monospace;
}

.kpi-trend {
  font-size: 12px;
  color: #999;
}

/* ===== 主内容区 ===== */
.main-content {
  width: 100%;
}

/* ===== 数据表格卡片 ===== */
.data-table-card {
  background: #ffffff;
  border-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* ===== 工具栏 ===== */
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.data-count {
  font-size: 12px;
  color: #999;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 2px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  display: flex;
  gap: 0;
}

.jd-input {
  width: 200px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-right: none;
  border-radius: 2px 0 0 2px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.jd-input:focus {
  border-color: #0066CC;
}

.jd-btn {
  height: 32px;
  padding: 0 16px;
  background: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.search-box .jd-btn {
  border-radius: 0 2px 2px 0;
  border-left: none;
}

.jd-btn:hover {
  color: #0066CC;
  border-color: #0066CC;
}

.jd-btn.primary {
  background: #0066CC;
  border-color: #0066CC;
  color: #ffffff;
}

.jd-btn.primary:hover {
  background: #005AA0;
  border-color: #005AA0;
}

/* ===== 加载状态 ===== */
.table-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #999;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f0f0f0;
  border-top-color: #0066CC;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 数据表格 ===== */
.inventory-table {
  width: 100%;
}

.table-header {
  display: flex;
  background: #fafafa;
  border-bottom: 2px solid #e8e8e8;
}

.th {
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
  text-align: left;
}

.table-body {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.table-row {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.table-row:hover {
  background: #fafafa;
}

.td {
  padding: 14px 16px;
  font-size: 13px;
  color: #333;
  display: flex;
  align-items: center;
}

/* 物资名称单元格 */
.item-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-thumb {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border: 1px solid #e8e8e8;
  border-radius: 2px;
  padding: 4px;
  background: #fafafa;
}

.item-name-text {
  font-weight: 500;
  color: #333;
}

/* 位置单元格 */
.location-cell {
  line-height: 1.4;
}

.location-sub {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* 库存进度条单元格 */
.stock-bar-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.stock-bar-bg {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.stock-bar-fill {
  height: 100%;
  transition: width 0.3s;
  border-radius: 4px;
}

.stock-bar-fill.level-high {
  background: #52C41A;
}

.stock-bar-fill.level-medium {
  background: #FA8C16;
}

.stock-bar-fill.level-low {
  background: #F5222D;
}

.stock-percent {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  min-width: 36px;
  text-align: right;
}

/* 库存数量 */
.stock-number {
  font-weight: 600;
  font-family: 'DIN', 'Roboto Mono', monospace;
}

.stock-number.level-high {
  color: #52C41A;
}

.stock-number.level-medium {
  color: #FA8C16;
}

.stock-number.level-low {
  color: #F5222D;
}

.stock-ratio {
  color: #999;
  font-size: 12px;
  font-family: 'DIN', 'Roboto Mono', monospace;
}

/* 操作按钮 */
.action-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-link {
  background: none;
  border: none;
  color: #0066CC;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.action-link:hover {
  color: #005AA0;
}

.action-link.danger {
  color: #F5222D;
}

.action-link.danger:hover {
  color: #D9001B;
}

.action-sep {
  color: #d9d9d9;
}
</style>

