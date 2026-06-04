<template>
  <div class="ios-container">
    <div class="ios-header-bar">
      <div class="header-left">
        <div class="title-box">
          <i class="el-icon-box icon-blue"></i>
          <h1>供给协同中心</h1>
        </div>
        <p class="subtitle">聚焦库存健康、补给稳定与物资保障台账</p>
      </div>
      <div class="header-right">
        <el-button class="ios-btn-primary" size="small" icon="el-icon-refresh" @click="fetchRealData" :loading="loading">
          刷新数据
        </el-button>
        <el-button class="ios-btn-secondary" size="small" icon="el-icon-download" @click="exportData">导出报表</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="metrics-row" v-loading="loading">
      <el-col :span="6">
        <div class="ios-card metric-card">
          <div class="metric-icon-box bg-cyan-light">
            <i class="el-icon-finished text-cyan"></i>
          </div>
          <div class="metric-info">
            <div class="metric-label">库存健康率</div>
            <div class="metric-value">{{ inventoryHealthRate }}<span class="unit">%</span></div>
            <div class="metric-sub" :class="{ 'text-red': lowStockItems.length > 0 }">
              {{ lowStockItems.length }} 项物资低于安全线
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="ios-card metric-card">
          <div class="metric-icon-box bg-blue-light">
            <i class="el-icon-school text-blue"></i>
          </div>
          <div class="metric-info">
            <div class="metric-label">物资品种数</div>
            <div class="metric-value">{{ inventoryData.length }}<span class="unit">类</span></div>
            <div class="metric-sub">总库存量 {{ inventoryUnitTotal }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="ios-card metric-card">
          <div class="metric-icon-box bg-orange-light">
            <i class="el-icon-wallet text-orange"></i>
          </div>
          <div class="metric-info">
            <div class="metric-label">采购总额</div>
            <div class="metric-value"><span class="unit">¥ </span>{{ formatCurrency(purchaseAmount).replace('¥','') }}</div>
            <div class="metric-sub">平均单笔 ¥ {{ formatCurrency(averagePurchaseTicket).replace('¥','') }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="ios-card metric-card">
          <div class="metric-icon-box bg-purple-light">
            <i class="el-icon-connection text-purple"></i>
          </div>
          <div class="metric-info">
            <div class="metric-label">合作供应商</div>
            <div class="metric-value">{{ supplierCount }}<span class="unit">家</span></div>
            <div class="metric-sub">供应协同数据实时更新</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="main-layout" type="flex">
      
      <el-col :span="16" class="flex-col">
        <div class="ios-card flex-1 flex-col">
          <div class="card-header">
            <div class="ios-segmented-control">
              <button 
                :class="['segment-btn', { active: activeTab === 'inventory' }]" 
                @click="activeTab = 'inventory'">实时库存</button>
              <button 
                :class="['segment-btn', { active: activeTab === 'purchase' }]" 
                @click="activeTab = 'purchase'">采购记录</button>
            </div>
            
            <div class="header-actions" style="display: flex; gap: 16px; align-items: center;">
              <el-input 
                placeholder="搜索物资名称或编号" 
                prefix-icon="el-icon-search" 
                v-model="searchQuery"
                @clear="activeTab === 'inventory' ? loadInventory() : loadPurchase()"
                @keyup.enter="activeTab === 'inventory' ? loadInventory() : loadPurchase()"
                class="ios-input search-input"
                size="small"
                style="width: 240px;"
                clearable>
              </el-input>
              <el-button v-if="activeTab === 'inventory'" class="ios-btn-sm-primary" icon="el-icon-plus" @click="openInventoryDialog()">入库登记</el-button>
              <el-button v-if="activeTab === 'inventory' && selectedInventory.length > 0" class="ios-btn-sm-primary" style="background: #f56c6c !important;" icon="el-icon-delete" @click="batchDeleteInventory">批量删除({{ selectedInventory.length }})</el-button>
              <el-button v-if="activeTab === 'purchase'" class="ios-btn-sm-primary" icon="el-icon-plus" @click="openPurchaseDialog()">新建采购</el-button>
              <el-button v-if="activeTab === 'purchase' && selectedPurchase.length > 0" class="ios-btn-sm-primary" style="background: #f56c6c !important;" icon="el-icon-delete" @click="batchDeletePurchase">批量删除({{ selectedPurchase.length }})</el-button>
            </div>
          </div>

          <div class="card-body table-container" v-loading="loading">
            <div v-if="activeTab === 'inventory'" style="height: 100%; display: flex; flex-direction: column;">
              <el-table
                :data="filteredInventory"
                style="width: 100%"
                height="100%"
                class="ios-table"
                :header-cell-style="tableHeaderStyle"
                @selection-change="onInventorySelect">

                <el-table-column type="selection" width="50" />
                <el-table-column prop="code" label="物资编号" min-width="120"></el-table-column>
                <el-table-column prop="name" label="物资名称" min-width="150">
                  <template #default="scope">
                    <span class="strong-text">{{ scope.row.name || scope.row.materialName || scope.row.produce || '未命名' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="warehouse" label="所在仓区" min-width="120"></el-table-column>
                <el-table-column label="当前库存" min-width="140" align="right">
                  <template #default="scope">
                    <span class="num-text">{{ scope.row.stock || scope.row.number || scope.row.quantity || 0 }}</span> {{ scope.row.unit || '件' }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100" align="center">
                  <template #default="scope">
                    <span :class="['status-pill', getStockStatusClass(scope.row)]">
                      {{ getStockStatusText(scope.row) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right" align="center">
                  <template #default="scope">
                    <el-button link class="ios-btn-link" @click="openInventoryDialog(scope.row)">编辑</el-button>
                    <el-button link class="ios-btn-link action-danger" @click="removeInventory(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div v-if="activeTab === 'purchase'" style="height: 100%; display: flex; flex-direction: column;">
              <el-table
                :data="filteredPurchase"
                style="width: 100%"
                height="100%"
                class="ios-table"
                :header-cell-style="tableHeaderStyle"
                @selection-change="onPurchaseSelect">

                <el-table-column type="selection" width="50" />
                <el-table-column prop="orderNo" label="采购单号" min-width="140"></el-table-column>
                <el-table-column prop="itemName" label="采购项目" min-width="150">
                  <template #default="scope">
                    <span class="strong-text">{{ scope.row.itemName || scope.row.product || scope.row.name || '未命名项目' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip>
                  <template #default="scope">{{ scope.row.supplier || scope.row.provider }}</template>
                </el-table-column>
                <el-table-column label="单价" min-width="120" align="right">
                  <template #default="scope">
                    <span class="num-text">{{ formatCurrency(scope.row.price) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="总价" min-width="140" align="right">
                  <template #default="scope">
                    <span class="num-text text-blue">{{ formatCurrency(getPurchaseTotal(scope.row)) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center" fixed="right">
                  <template #default="scope">
                    <el-button link class="ios-btn-link" @click="openPurchaseDialog(scope.row)">编辑</el-button>
                    <el-button link class="ios-btn-link action-danger" @click="removePurchase(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="8" class="flex-col">
        <div class="ios-card flex-1 flex-col">
          <div class="card-header border-bottom">
            <div class="header-title">
              <i class="el-icon-warning-outline" style="color: #FF3B30;"></i> 智能预警与风险
            </div>
            <span class="badge-red" v-if="lowStockItems.length > 0">{{ lowStockItems.length }} 项待处理</span>
          </div>

          <div class="card-body scroll-body" v-loading="loading">
            
            <div v-if="lowStockItems.length === 0" class="ios-empty-state">
               <div class="empty-icon-circle"><i class="el-icon-circle-check"></i></div>
               <h3>库存健康</h3>
               <p>当前无低于安全线的物资</p>
            </div>

            <div v-else class="risk-list">
              <div class="risk-section-title">低于安全库存报警</div>
              
              <div class="risk-item" v-for="(item, index) in lowStockItems" :key="index">
                <div class="risk-icon">
                  <i class="el-icon-shopping-cart-full"></i>
                </div>
                <div class="risk-content">
                  <div class="risk-name">{{ item.name || item.materialName || item.produce }} <span class="risk-code">#{{ item.code || item.id }}</span></div>
                  <div class="risk-desc">
                    当前: <span class="text-red">{{ item.stock || item.number || item.quantity || 0 }}</span> {{ item.unit || '件' }} 
                    <span class="divider">|</span> 
                    安全线: {{ item.safeStock || 0 }} {{ item.unit || '件' }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

    </el-row>

    <el-dialog
      :title="inventoryForm.id ? '编辑库存档案' : '新建库存档案'"
      v-model="inventoryDialogVisible"
      width="540px"
      :close-on-click-modal="false"
      custom-class="ios-dialog"
    >
      <el-form :model="inventoryForm" :rules="inventoryRules" ref="inventoryFormRef" label-width="100px" size="small" class="ios-form">
        <el-form-item label="物资名称" prop="produce">
          <el-input v-model="inventoryForm.produce" placeholder="输入物资名称" />
        </el-form-item>
        <el-form-item label="所属仓区" prop="warehouse">
          <el-input v-model="inventoryForm.warehouse" placeholder="例如：A区化肥库" />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input-number v-model="inventoryForm.number" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="安全库存线">
          <el-input-number v-model="inventoryForm.safeStock" :min="1" style="width: 100%" />
          <div style="font-size: 12px; color: #8E8E93; margin-top: 4px;">库存低于此数值将触发首页红色报警</div>
        </el-form-item>
        <el-form-item label="最大库存">
          <el-input-number v-model="inventoryForm.maxStock" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="登记/保管员">
          <el-input v-model="inventoryForm.keeper" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="inventoryForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="ios-btn-secondary" @click="inventoryDialogVisible = false">取消</el-button>
          <el-button class="ios-btn-primary" @click="saveInventory">确定保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      :title="purchaseForm.id ? '编辑采购记录' : '新建采购记录'"
      v-model="purchaseDialogVisible"
      width="540px"
      :close-on-click-modal="false"
      custom-class="ios-dialog"
    >
      <el-form :model="purchaseForm" :rules="purchaseRules" ref="purchaseFormRef" label-width="100px" size="small" class="ios-form">
        <el-form-item label="采购物资" prop="product">
          <el-input v-model="purchaseForm.product" placeholder="输入采购物资名称" />
        </el-form-item>
        <el-form-item label="采购数量">
          <el-input-number v-model="purchaseForm.number" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="采购单价">
          <el-input-number v-model="purchaseForm.price" :min="0" :precision="2" :step="0.1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="provider">
          <el-input v-model="purchaseForm.provider" placeholder="输入供应商全称" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="purchaseForm.phone" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="purchaseForm.address" />
        </el-form-item>
        <el-form-item label="采购负责人">
          <el-input v-model="purchaseForm.purchaser" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="ios-btn-secondary" @click="purchaseDialogVisible = false">取消</el-button>
          <el-button class="ios-btn-primary" @click="savePurchase">确定保存</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script>
// 初始表单构造器
function createInventoryForm() {
  return { produce: '', warehouse: '', region: '', number: 1, safeStock: 20, maxStock: 100, dailyConsumption: 0, keeper: '', remark: '' }
}
function createPurchaseForm(user) {
  return { product: '', price: 0, number: 1, provider: '', address: '', phone: '', purchaser: user.nickname || user.username || '', remark: '' }
}

export default {
  name: 'SupplyCenter',
  data() {
    const user = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : {}
    return {
      user,
      activeTab: 'inventory',
      searchQuery: '',
      loading: false,

      inventoryData: [],
      purchaseData: [],

      inventoryDialogVisible: false,
      purchaseDialogVisible: false,
      inventoryForm: createInventoryForm(),
      purchaseForm: createPurchaseForm(user),
      selectedInventory: [],
      selectedPurchase: [],
      inventoryPageNum: 1,
      inventoryPageSize: 20,
      inventoryTotal: 0,
      purchasePageNum: 1,
      purchasePageSize: 20,
      purchaseTotal: 0,
      inventoryRules: {
        produce: [{ required: true, message: '请输入物资名称', trigger: 'blur' }],
        warehouse: [{ required: true, message: '请输入所属仓区', trigger: 'blur' }]
      },
      purchaseRules: {
        product: [{ required: true, message: '请输入采购物资名称', trigger: 'blur' }],
        provider: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }]
      },
    }
  },
  computed: {
    inventoryHealthRate() {
      if (this.inventoryData.length === 0) return 100;
      const healthyCount = this.inventoryData.length - this.lowStockItems.length;
      return Math.round((healthyCount / this.inventoryData.length) * 100);
    },
    inventoryUnitTotal() {
      return this.inventoryData.reduce((sum, item) => sum + (Number(item.stock || item.number || item.quantity) || 0), 0).toLocaleString();
    },
    purchaseAmount() {
      return this.purchaseData.reduce((sum, item) => sum + this.getPurchaseTotal(item), 0);
    },
    averagePurchaseTicket() {
      if (this.purchaseData.length === 0) return 0;
      return Math.round(this.purchaseAmount / this.purchaseData.length);
    },
    supplierCount() {
      const suppliers = new Set(this.purchaseData.map(item => item.supplier || item.provider).filter(Boolean));
      return suppliers.size;
    },
    lowStockItems() {
      return this.inventoryData.filter(item => {
        const stock = Number(item.stock || item.number || item.quantity) || 0;
        const safe = Number(item.safeStock) || 0;
        return safe > 0 && stock < safe;
      });
    },
    filteredInventory() {
      if (!this.searchQuery) return this.inventoryData;
      const lowerQuery = this.searchQuery.toLowerCase();
      return this.inventoryData.filter(item => {
        const name = item.name || item.materialName || item.produce || '';
        const code = item.code || String(item.id) || '';
        return name.toLowerCase().includes(lowerQuery) || code.toLowerCase().includes(lowerQuery);
      });
    },
    filteredPurchase() {
      if (!this.searchQuery) return this.purchaseData;
      const lowerQuery = this.searchQuery.toLowerCase();
      return this.purchaseData.filter(item => {
        const name = item.itemName || item.product || item.name || '';
        const orderNo = item.orderNo || String(item.id) || '';
        const supplier = item.supplier || item.provider || '';
        return name.toLowerCase().includes(lowerQuery) || 
               orderNo.toLowerCase().includes(lowerQuery) ||
               supplier.toLowerCase().includes(lowerQuery);
      });
    }
  },
  created() {
    this.fetchRealData();
  },
  methods: {
    async fetchRealData() {
      this.loading = true;
      try {
        const invRes = await this.request.get('/inventory/page', { params: { pageNum: this.inventoryPageNum, pageSize: this.inventoryPageSize } });
        if (invRes.code === '200' && invRes.data) {
          this.inventoryData = invRes.data.records || invRes.data || [];
          this.inventoryTotal = invRes.data.total || 0;
        }
        const purRes = await this.request.get('/purchase/page', { params: { pageNum: this.purchasePageNum, pageSize: this.purchasePageSize } });
        if (purRes.code === '200' && purRes.data) {
          this.purchaseData = purRes.data.records || purRes.data || [];
          this.purchaseTotal = purRes.data.total || 0;
        }
      } catch (error) {
        this.$message.error('数据加载失败，请稍后重试');
      } finally {
        this.loading = false;
      }
    },

    // ================= 弹窗开启与关闭逻辑 =================
    openInventoryDialog(row) {
      this.inventoryForm = row ? Object.assign(createInventoryForm(), JSON.parse(JSON.stringify(row))) : createInventoryForm()
      // 同步可能的不同字段名
      if (row && !this.inventoryForm.produce) this.inventoryForm.produce = row.name || row.materialName || '';
      if (row && !this.inventoryForm.number) this.inventoryForm.number = row.stock || row.quantity || 0;
      this.inventoryDialogVisible = true
    },
    openPurchaseDialog(row) {
      this.purchaseForm = row ? Object.assign(createPurchaseForm(this.user), JSON.parse(JSON.stringify(row))) : createPurchaseForm(this.user)
      if (row && !this.purchaseForm.product) this.purchaseForm.product = row.itemName || row.name || '';
      if (row && !this.purchaseForm.provider) this.purchaseForm.provider = row.supplier || '';
      this.purchaseDialogVisible = true
    },

    saveInventory() {
      this.$refs.inventoryFormRef.validate((valid) => {
        if (!valid) return
        const payload = Object.assign({}, this.inventoryForm);
        if (!payload.safeStock) payload.safeStock = 20;
        const isEdit = !!payload.id;
        this.request.post('/inventory', payload).then(res => {
          if (res.code === '200') {
            this.$message.success(isEdit ? '库存档案已更新' : '已成功录入新库存');
            this.inventoryDialogVisible = false;
            this.fetchRealData();
          } else {
            this.$message.error(res.msg || '保存失败');
          }
        }).catch(() => { this.$message.error('保存失败，请检查网络') });
      })
    },

    savePurchase() {
      this.$refs.purchaseFormRef.validate((valid) => {
        if (!valid) return
        const payload = Object.assign({}, this.purchaseForm);
        const isEdit = !!payload.id;
        this.request.post('/purchase', payload).then(res => {
          if (res.code === '200') {
            this.$message.success(isEdit ? '采购记录已更新' : '已成功录入新采购单');
            this.purchaseDialogVisible = false;
            this.fetchRealData();
          } else {
            this.$message.error(res.msg || '保存失败');
          }
        }).catch(() => { this.$message.error('保存失败，请检查网络') });
      })
    },

    // ================= 删除逻辑 =================
    removeInventory(row) {
      this.$confirm(`确定删除物资“${row.produce || row.name}”吗？`, '删除确认', { type: 'warning' }).then(() => {
        this.request.delete('/inventory/' + row.id).then(res => {
          if (res.code === '200') { this.$message.success('删除成功'); this.fetchRealData(); }
          else { this.$message.error(res.msg || '删除失败'); }
        }).catch(() => { this.$message.error('删除失败，请检查网络') });
      }).catch(err => { console.error('Delete inventory confirmation canceled:', err) });
    },

    removePurchase(row) {
      this.$confirm(`确定删除采购记录“${row.product || row.itemName}”吗？`, '删除确认', { type: 'warning' }).then(() => {
        this.request.delete('/purchase/' + row.id).then(res => {
          if (res.code === '200') { this.$message.success('删除成功'); this.fetchRealData(); }
          else { this.$message.error(res.msg || '删除失败'); }
        }).catch(() => { this.$message.error('删除失败，请检查网络') });
      }).catch(err => { console.error('Delete purchase record confirmation canceled:', err) });
    },

    // ================= 批量选择与删除 =================
    onInventorySelect(selection) {
      this.selectedInventory = selection
    },
    onPurchaseSelect(selection) {
      this.selectedPurchase = selection
    },
    batchDeleteInventory() {
      const ids = this.selectedInventory.map(item => item.id)
      if (ids.length === 0) return
      this.$confirm(`确定批量删除 ${ids.length} 条库存记录吗？`, '批量删除确认', { type: 'warning' }).then(() => {
        this.request.post('/inventory/del/batch', ids).then(res => {
          if (res.code === '200') { this.$message.success('批量删除成功'); this.selectedInventory = []; this.fetchRealData() }
          else { this.$message.error(res.msg || '批量删除失败') }
        }).catch(() => { this.$message.error('批量删除失败，请检查网络') })
      }).catch(() => {})
    },
    batchDeletePurchase() {
      const ids = this.selectedPurchase.map(item => item.id)
      if (ids.length === 0) return
      this.$confirm(`确定批量删除 ${ids.length} 条采购记录吗？`, '批量删除确认', { type: 'warning' }).then(() => {
        this.request.post('/purchase/del/batch', ids).then(res => {
          if (res.code === '200') { this.$message.success('批量删除成功'); this.selectedPurchase = []; this.fetchRealData() }
          else { this.$message.error(res.msg || '批量删除失败') }
        }).catch(() => { this.$message.error('批量删除失败，请检查网络') })
      }).catch(() => {})
    },
    exportData() {
      const endpoint = this.activeTab === 'inventory' ? '/inventory/export' : '/purchase/export'
      window.open(endpoint)
    },

    // ================= 辅助工具方法 =================
    getPurchaseTotal(row) {
      return (Number(row.price) || 0) * (Number(row.number) || 0)
    },
    formatCurrency(value) {
      return '¥' + (Number(value) || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    getStockStatusClass(row) {
      const stock = Number(row.stock || row.number || row.quantity) || 0;
      const safe = Number(row.safeStock) || 0;
      if (safe > 0 && stock < safe) return 'pill-red';
      if (safe > 0 && stock < safe * 1.2) return 'pill-orange';
      return 'pill-blue';
    },
    getStockStatusText(row) {
      const stock = Number(row.stock || row.number || row.quantity) || 0;
      const safe = Number(row.safeStock) || 0;
      if (safe > 0 && stock < safe) return '需补货';
      if (safe > 0 && stock < safe * 1.2) return '偏低';
      return '健康';
    },
    tableHeaderStyle() {
      return { backgroundColor: '#F8F9FA', color: '#64748B', fontWeight: '600', fontSize: '13px', borderBottom: '1px solid #E5E5EA' };
    }
  }
}
</script>

<style scoped>
/* ========================================================== */
/* B2B 经典企业级后台风格 - 供给协同中心 (Supply Center) */
/* ========================================================== */
.ios-container {
  padding: 28px;
  background:
    radial-gradient(circle at 18% 0%, rgba(47, 141, 247, 0.08), transparent 26%),
    linear-gradient(180deg, #f7f9fc 0%, #f3f6fa 100%);
  min-height: calc(100vh - 60px);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #303133;
}

/* --- 公共工具类 --- */
.flex-col { display: flex; flex-direction: column; }
.flex-1 { flex: 1; min-height: 0; }
.ios-card {
  background: #ffffff; 
  border-radius: 8px;
  box-shadow: 0 14px 34px rgba(31, 45, 61, 0.06);
  border: 0;
  overflow: hidden;
}

/* --- 1. 顶部标题栏 --- */
.ios-header-bar {
  display: flex; justify-content: space-between; align-items: center; 
  background: rgba(255, 255, 255, 0.96); padding: 24px 32px; margin-bottom: 24px;
  border-radius: 8px; box-shadow: 0 14px 34px rgba(31, 45, 61, 0.06); border: 0;
}
.header-left { display: flex; flex-direction: column; }
.title-box { display: flex; align-items: center; gap: 8px; margin-bottom: 4px;}
.title-box h1 { font-size: 20px; font-weight: 600; color: #303133; margin: 0;}
.icon-blue { font-size: 24px; color: #409eff; }
.subtitle { font-size: 13px; color: #909399; margin: 0; }

.header-right { display: flex; gap: 12px; }
.ios-btn-primary {
  background: #2f8df7 !important; border: 0 !important; color: white !important;
  border-radius: 8px !important; font-weight: 600 !important; padding: 10px 20px !important;
  box-shadow: 0 10px 18px rgba(47, 141, 247, 0.18) !important;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}
.ios-btn-primary:hover {
  background: #2479d6 !important;
  box-shadow: 0 12px 22px rgba(36, 121, 214, 0.2) !important;
  transform: none !important;
}
.ios-btn-secondary {
  background: #ffffff !important; border: 1px solid #e1e7ef !important; color: #5d6674 !important;
  border-radius: 8px !important; font-weight: 600 !important; padding: 10px 20px !important;
  box-shadow: none !important;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}
.ios-btn-secondary:hover { background: #f7fbff !important; color: #2f8df7 !important; border-color: #c8defa !important; transform: none !important; }
.ios-btn-sm-primary {
  background: #2f8df7 !important; border: 0 !important; color: white !important;
  border-radius: 8px !important; font-weight: 600 !important; padding: 8px 16px !important;
  box-shadow: none !important;
  transition: background-color 0.2s ease;
}
.ios-btn-sm-primary:hover { background: #2479d6 !important; transform: none !important; }

/* --- 2. 核心指标卡片 --- */
.metrics-row { margin-bottom: 24px; }
.metric-card { padding: 20px 24px; display: flex; align-items: center; gap: 16px; border: 0; border-radius: 8px; }
.metric-icon-box {
  width: 48px; height: 48px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0;
}
.bg-cyan-light { background: #e0f7fa; } .text-cyan { color: #00bcd4; }
.bg-blue-light { background: #ecf5ff; } .text-blue { color: #409eff; }
.bg-orange-light { background: #fdf6ec; } .text-orange { color: #e6a23c; }
.bg-purple-light { background: #f4f4f5; } .text-purple { color: #909399; }
.text-red { color: #f56c6c !important; }

.metric-info { display: flex; flex-direction: column; overflow: hidden; justify-content: center; }
.metric-label { font-size: 14px; color: #8a95a6; margin-bottom: 8px; }
.metric-value { font-size: 28px; font-weight: 700; color: #1f2937; margin-bottom: 4px; white-space: nowrap; line-height: 1; }
.unit { font-size: 14px; font-weight: normal; color: #909399; margin: 0 2px; }
.metric-sub { font-size: 13px; color: #c0c4cc; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 4px; }

/* --- 3. 主体布局 --- */
.main-layout { height: calc(100vh - 250px); min-height: 500px; }
.card-header {
  padding: 0 24px; display: flex; justify-content: space-between; align-items: center; 
  border-bottom: 1px solid #eef2f7; height: 60px;
}
.card-header.border-bottom { border-bottom: 1px solid #eef2f7; }
.header-title { font-size: 16px; font-weight: 600; color: #303133; display: flex; align-items: center; gap: 8px; }
.header-title i { font-size: 18px; }
.card-body { flex: 1; min-height: 0; overflow: hidden;}
.scroll-body { overflow-y: auto; padding: 16px; }

/* 分段控制器 (经典后台标签页风格) */
.ios-segmented-control {
  display: flex; height: 100%;
}
.segment-btn {
  padding: 0 4px; margin-right: 32px; border: none; background: transparent;
  font-size: 15px; font-weight: 500; color: #606266; cursor: pointer; transition: all 0.2s ease;
  display: flex; align-items: center; height: 100%; border-bottom: 2px solid transparent;
  position: relative; top: 1px;
}
.segment-btn:last-child { margin-right: 0; }
.segment-btn:hover { color: #409eff; }
.segment-btn.active {
  color: #409eff; font-weight: 600; border-bottom: 2px solid #409eff;
}

/* 搜索框与工具栏 */
:deep(.search-input .el-input__wrapper ){
  min-height: 36px;
  background: #f6f8fb !important;
  border: 0 !important;
  border-radius: 8px !important;
  box-shadow: inset 0 0 0 1px transparent !important;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}
:deep(.search-input .el-input__wrapper:hover ){
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #dce5ef !important;
}
:deep(.search-input .el-input__wrapper.is-focus ){
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #2f8df7, 0 0 0 3px rgba(47, 141, 247, 0.1) !important;
}
:deep(.search-input .el-input__inner ){
  border: 0 !important; border-radius: 0 !important; height: 32px; line-height: 32px; font-size: 13px; background: transparent !important; box-shadow: none !important;
}
:deep(.search-input .el-input__inner:focus ){ border: 0 !important; box-shadow: none !important; }

/* 表格样式重写（强力去绿覆盖 global.css 和 green-theme.css） */
.table-container { padding: 0; }
:deep(.ios-table.el-table ){
  background: #ffffff !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  border: none !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}
:deep(.ios-table .el-table__body-wrapper::-webkit-scrollbar ){
  width: 6px; height: 6px;
}
:deep(.ios-table .el-table__body-wrapper::-webkit-scrollbar-thumb ){
  background: #cbd5e1 !important; border-radius: 3px;
}
:deep(.ios-table .el-table__body-wrapper::-webkit-scrollbar-thumb:hover ){
  background: #94a3b8 !important;
}

:deep(.ios-table th.el-table__cell ){
  background: #f8fafc !important;
  color: #606266 !important;
  font-weight: 600 !important;
  border-bottom: 1px solid #eef2f7 !important;
  padding: 12px 16px !important;
}
:deep(.ios-table td.el-table__cell ){
  padding: 14px 16px !important;
  border-bottom: 1px solid #f0f3f7 !important;
  background: #ffffff !important;
}
:deep(.ios-table tbody tr:hover > td ){
  background-color: #f8fbff !important;
}
:deep(.ios-table--striped .el-table__body tr.el-table__row--striped td ){
  background: #fafafa !important;
}
:deep(.ios-table::before), :deep(.ios-table::after ){ display: none !important; }
.strong-text { font-weight: 500; color: #303133; }
.num-text { font-family: monospace; color: #606266; font-size: 14px;}

.ios-btn-link { color: #409eff; font-weight: 500; padding: 0 8px; }
.action-danger { color: #f56c6c !important; }

/* 状态标签 */
.status-pill {
  display: inline-block; padding: 4px 9px; border-radius: 8px; font-size: 12px; border: 1px solid transparent;
}
.pill-blue { background: #ecf5ff; color: #409eff; border-color: #d9ecff; }
.pill-orange { background: #fdf6ec; color: #e6a23c; border-color: #faecd8; }
.pill-red { background: #fef0f0; color: #f56c6c; border-color: #fde2e2; }
.badge-red { background: #f56c6c; color: white; padding: 2px 8px; border-radius: 10px; font-size: 12px; }

/* --- 风险看板列表 --- */
.risk-list { padding-top: 0; }
.risk-section-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; }

.risk-item {
  display: flex; align-items: center; padding: 14px 16px; background: #f8fafc;
  border: 0; border-radius: 8px; margin-bottom: 10px;
}
.risk-item:hover { background: #f3f7fc; }

.risk-icon {
  width: 32px; height: 32px; border-radius: 8px; background: #fff1f2; color: #f56c6c;
  display: flex; align-items: center; justify-content: center; font-size: 16px; margin-right: 12px; flex-shrink: 0;
}

.risk-content { flex: 1; min-width: 0; }
.risk-name { font-size: 14px; font-weight: 500; color: #303133; margin-bottom: 4px; }
.risk-code { color: #909399; font-size: 13px;}
.risk-desc { font-size: 13px; color: #606266; margin-top: 4px; display: block;}
.divider { margin: 0 8px; color: #dcdfe6; }

/* 空状态 */
.ios-empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #909399; text-align: center; }
.empty-icon-circle { width: 60px; height: 60px; border-radius: 50%; background: #f4f4f5; display: flex; align-items: center; justify-content: center; font-size: 28px; margin-bottom: 12px; color: #c0c4cc;}
.ios-empty-state h3 { font-size: 15px; font-weight: 500; color: #303133; margin: 0 0 8px 0;}
.ios-empty-state p { font-size: 13px; margin: 0;}

/* ================= 弹窗表单精修 ================= */
:deep(.ios-dialog ){
  border-radius: 8px !important; overflow: hidden !important;
  border: 0 !important; box-shadow: 0 18px 48px rgba(31, 45, 61, 0.16) !important;
}
:deep(.ios-dialog .el-dialog__header ){
  background: #ffffff !important; padding: 20px 24px !important; border-bottom: 1px solid #eef2f7 !important;
}
:deep(.ios-dialog .el-dialog__title ){
  font-weight: 600 !important; font-size: 16px !important; color: #303133 !important;
}
:deep(.ios-dialog .el-dialog__body ){ padding: 24px !important; }
:deep(.ios-dialog .el-dialog__footer ){
  padding: 16px 24px !important; border-top: 1px solid #eef2f7 !important; background: #ffffff !important;
}

:deep(.ios-form .el-form-item__label ){ font-weight: 500 !important; color: #606266 !important; }
:deep(.ios-form .el-input__wrapper),
:deep(.ios-form .el-select__wrapper) {
  background: #f6f8fb !important;
  border: 0 !important;
  border-radius: 8px !important;
  outline: 0 !important;
  box-shadow: inset 0 0 0 1px #edf1f6 !important;
  transition: background-color 0.2s ease, box-shadow 0.2s ease !important;
}
:deep(.ios-form .el-input__wrapper:hover),
:deep(.ios-form .el-select__wrapper:hover) {
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #dce5ef !important;
}
:deep(.ios-form .el-input__wrapper.is-focus),
:deep(.ios-form .el-select__wrapper.is-focused) {
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #2f8df7, 0 0 0 3px rgba(47, 141, 247, 0.1) !important;
}
:deep(.ios-form .el-input__inner),
:deep(.ios-form .el-select__input) {
  border: 0 !important;
  border-radius: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  outline: 0 !important;
}
:deep(.ios-form .el-input__inner:focus),
:deep(.ios-form .el-select__input:focus) {
  border: 0 !important;
  box-shadow: none !important;
}
:deep(.ios-form .el-textarea__inner ){
  background: #f6f8fb !important;
  border: 0 !important;
  border-radius: 8px !important;
  outline: 0 !important;
  box-shadow: inset 0 0 0 1px #edf1f6 !important;
  transition: background-color 0.2s ease, box-shadow 0.2s ease !important;
}
:deep(.ios-form .el-textarea__inner:hover ){
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #dce5ef !important;
}
:deep(.ios-form .el-textarea__inner:focus ){
  background: #ffffff !important;
  border: 0 !important;
  box-shadow: inset 0 0 0 1px #2f8df7, 0 0 0 3px rgba(47, 141, 247, 0.1) !important;
}
:deep(.ios-form .el-input-number__increase),
:deep(.ios-form .el-input-number__decrease ){
  background: transparent !important;
  border-top: 0 !important;
  border-bottom: 0 !important;
  border-color: #dce5ef !important;
}
</style>
