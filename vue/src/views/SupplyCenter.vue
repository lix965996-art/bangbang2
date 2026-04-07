<template>
  <div class="business-page supply-center-page" v-loading="inventoryLoading || purchaseLoading">
    <section class="supply-hero">
      <div class="supply-hero__intro">
        <span class="supply-hero__eyebrow">业务协同</span>
        <h1>供给协同中心</h1>
        <p>
          供给协同中心聚焦库存健康、补给稳定与供应协作，统一呈现仓储状态、补货风险、
          采购关系和日常台账，支撑农业生产过程中的物资保障。
        </p>
      </div>
      <div class="supply-hero__metrics">
        <article class="metric-card">
          <span>库存健康率</span>
          <strong>{{ inventoryHealthRate }}%</strong>
          <small>{{ lowStockItems.length }} 项低于安全线</small>
        </article>
        <article class="metric-card">
          <span>覆盖仓区</span>
          <strong>{{ warehouseCount }}</strong>
          <small>总库存 {{ inventoryUnitTotal }}</small>
        </article>
        <article class="metric-card">
          <span>本期采购额</span>
          <strong>{{ formatCurrency(purchaseAmount) }}</strong>
          <small>平均单笔 {{ formatCurrency(averagePurchaseTicket) }}</small>
        </article>
        <article class="metric-card">
          <span>合作供应商</span>
          <strong>{{ supplierCount }}</strong>
          <small>{{ supplyNarrative }}</small>
        </article>
      </div>
    </section>

    <section class="supply-insight-grid">
      <article class="insight-panel insight-panel--risk">
        <div class="insight-panel__header">
          <div>
            <h2>补给风险清单</h2>
            <p>优先识别低于安全库存线的物资，明确当前补货顺序。</p>
          </div>
          <span class="insight-panel__pill">{{ lowStockItems.length }} 项待补</span>
        </div>

        <div v-if="lowStockItems.length" class="risk-list">
          <div v-for="item in lowStockItems" :key="item.id" class="risk-item">
            <div class="risk-item__main">
              <div class="risk-item__title">{{ item.produce }}</div>
              <div class="risk-item__meta">
                {{ item.warehouse || '默认仓区' }} · 当前 {{ item.number || 0 }} / 安全线 {{ getSafeStock(item) }}
              </div>
            </div>
            <div class="risk-item__side">
              <strong>补 {{ getRestockGap(item) }}</strong>
              <span>{{ getCoverageDays(item) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">当前没有低于安全线的物资，供给链处于平稳状态。</div>
      </article>

      <article class="insight-panel insight-panel--provider">
        <div class="insight-panel__header">
          <div>
            <h2>供应协作梯队</h2>
            <p>聚焦供应商稳定度、集中度与当前补给结构。</p>
          </div>
          <span class="insight-panel__pill">{{ providerRanking.length }} 家活跃供应商</span>
        </div>

        <div v-if="providerRanking.length" class="provider-list">
          <div v-for="item in providerRanking" :key="item.name" class="provider-item">
            <div class="provider-item__top">
              <div>
                <div class="provider-item__name">{{ item.name }}</div>
                <div class="provider-item__meta">{{ item.count }} 笔采购</div>
              </div>
              <strong>{{ formatCurrency(item.total) }}</strong>
            </div>
            <el-progress
              :percentage="getProviderShare(item)"
              :show-text="false"
              :stroke-width="8"
              color="#d97706"
            />
          </div>
        </div>
        <div v-else class="empty-state">当前暂无采购记录，尚未形成稳定供应关系。</div>

        <div class="provider-note">
          <span>当前研判</span>
          <p>{{ supplyNarrative }}</p>
        </div>
      </article>
    </section>

    <section class="ledger-panel">
      <div class="ledger-panel__header">
        <div>
          <h2>供给台账</h2>
          <p>统一查看库存与采购记录，支撑日常登记、查询和导出。</p>
        </div>
      </div>

      <el-tabs v-model="activeLedger" class="supply-tabs">
        <el-tab-pane label="库存台账" name="inventory">
          <div class="ledger-toolbar">
            <el-input
              v-model="inventoryKeyword"
              clearable
              placeholder="搜索库存物资"
              prefix-icon="el-icon-search"
              @clear="loadInventory"
              @keyup.enter.native="loadInventory"
            />
            <div class="ledger-toolbar__actions">
              <el-button type="primary" icon="el-icon-plus" @click="openInventoryDialog()">入库登记</el-button>
              <el-button plain icon="el-icon-refresh" @click="loadInventory">刷新</el-button>
              <el-button plain icon="el-icon-download" @click="exportInventory">导出</el-button>
            </div>
          </div>

          <el-table :data="inventoryList" stripe>
            <el-table-column prop="produce" label="物资名称" min-width="180" />
            <el-table-column prop="warehouse" label="仓区" width="120" />
            <el-table-column prop="region" label="库位" width="120" />
            <el-table-column label="当前 / 安全线" width="140" align="center">
              <template slot-scope="{ row }">
                {{ row.number || 0 }} / {{ getSafeStock(row) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template slot-scope="{ row }">
                <span :class="['table-status', getInventoryStateClass(row)]">
                  {{ getInventoryState(row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="覆盖天数" width="120" align="center">
              <template slot-scope="{ row }">
                {{ getCoverageDays(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="keeper" label="保管员" width="110" align="center" />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" @click="openInventoryDialog(row)">编辑</el-button>
                <el-button type="text" class="action-danger" @click="removeInventory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="采购台账" name="purchase">
          <div class="ledger-toolbar">
            <el-input
              v-model="purchaseKeyword"
              clearable
              placeholder="搜索采购物资"
              prefix-icon="el-icon-search"
              @clear="loadPurchase"
              @keyup.enter.native="loadPurchase"
            />
            <div class="ledger-toolbar__actions">
              <el-button type="primary" icon="el-icon-plus" @click="openPurchaseDialog()">新建采购</el-button>
              <el-button plain icon="el-icon-refresh" @click="loadPurchase">刷新</el-button>
              <el-button plain icon="el-icon-download" @click="exportPurchase">导出</el-button>
            </div>
          </div>

          <el-table :data="purchaseList" stripe>
            <el-table-column prop="product" label="采购物资" min-width="180" />
            <el-table-column prop="provider" label="供应商" min-width="160" />
            <el-table-column prop="number" label="数量" width="90" align="center" />
            <el-table-column label="单价" width="120" align="center">
              <template slot-scope="{ row }">
                {{ formatCurrency(row.price) }}
              </template>
            </el-table-column>
            <el-table-column label="采购额" width="120" align="center">
              <template slot-scope="{ row }">
                {{ formatCurrency(getPurchaseTotal(row)) }}
              </template>
            </el-table-column>
            <el-table-column prop="purchaser" label="采购人" width="110" align="center" />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" @click="openPurchaseDialog(row)">编辑</el-button>
                <el-button type="text" class="action-danger" @click="removePurchase(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      :title="inventoryForm.id ? '编辑库存档案' : '新建库存档案'"
      :visible.sync="inventoryDialogVisible"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form :model="inventoryForm" label-width="100px" size="small">
        <el-form-item label="物资名称">
          <el-input v-model="inventoryForm.produce" />
        </el-form-item>
        <el-form-item label="所属仓区">
          <el-input v-model="inventoryForm.warehouse" />
        </el-form-item>
        <el-form-item label="库位编码">
          <el-input v-model="inventoryForm.region" />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input-number v-model="inventoryForm.number" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="安全库存">
          <el-input-number v-model="inventoryForm.safeStock" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大库存">
          <el-input-number v-model="inventoryForm.maxStock" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日均消耗">
          <el-input-number
            v-model="inventoryForm.dailyConsumption"
            :min="0"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="保管员">
          <el-input v-model="inventoryForm.keeper" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inventoryForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="inventoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveInventory">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :title="purchaseForm.id ? '编辑采购记录' : '新建采购记录'"
      :visible.sync="purchaseDialogVisible"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form :model="purchaseForm" label-width="100px" size="small">
        <el-form-item label="物资名称">
          <el-input v-model="purchaseForm.product" />
        </el-form-item>
        <el-form-item label="采购数量">
          <el-input-number v-model="purchaseForm.number" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number
            v-model="purchaseForm.price"
            :min="0"
            :precision="2"
            :step="0.1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="purchaseForm.provider" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="purchaseForm.phone" />
        </el-form-item>
        <el-form-item label="联系地址">
          <el-input v-model="purchaseForm.address" />
        </el-form-item>
        <el-form-item label="采购人">
          <el-input v-model="purchaseForm.purchaser" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="purchaseForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePurchase">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import apiConfig from '@/config/api.config'

function createInventoryForm() {
  return {
    produce: '',
    warehouse: '',
    region: '',
    number: 1,
    safeStock: 20,
    maxStock: 100,
    dailyConsumption: 0,
    keeper: '',
    remark: ''
  }
}

function createPurchaseForm(user) {
  return {
    product: '',
    price: 0,
    number: 1,
    provider: '',
    address: '',
    phone: '',
    purchaser: user.nickname || user.username || '',
    remark: ''
  }
}

export default {
  name: 'SupplyCenter',
  data() {
    const user = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : {}
    return {
      apiConfig,
      user,
      activeLedger: 'inventory',
      inventoryKeyword: '',
      purchaseKeyword: '',
      inventoryList: [],
      purchaseList: [],
      inventoryLoading: false,
      purchaseLoading: false,
      inventoryDialogVisible: false,
      purchaseDialogVisible: false,
      inventoryForm: createInventoryForm(),
      purchaseForm: createPurchaseForm(user)
    }
  },
  computed: {
    inventoryHealthRate() {
      if (!this.inventoryList.length) {
        return 100
      }
      const safeCount = this.inventoryList.filter(item => (Number(item.number) || 0) >= this.getSafeStock(item)).length
      return Math.round((safeCount / this.inventoryList.length) * 100)
    },
    inventoryUnitTotal() {
      return this.inventoryList.reduce((sum, item) => sum + (Number(item.number) || 0), 0)
    },
    warehouseCount() {
      return new Set(this.inventoryList.map(item => item.warehouse || '默认仓区')).size
    },
    lowStockItems() {
      return this.inventoryList
        .map(item => Object.assign({}, item, { gap: this.getRestockGap(item) }))
        .filter(item => item.gap > 0)
        .sort((a, b) => b.gap - a.gap)
        .slice(0, 6)
    },
    purchaseAmount() {
      return this.purchaseList.reduce((sum, item) => sum + this.getPurchaseTotal(item), 0)
    },
    averagePurchaseTicket() {
      if (!this.purchaseList.length) {
        return 0
      }
      return this.purchaseAmount / this.purchaseList.length
    },
    supplierCount() {
      return new Set(
        this.purchaseList
          .map(item => (item.provider || '').trim())
          .filter(Boolean)
      ).size
    },
    providerRanking() {
      const bucket = {}
      this.purchaseList.forEach(item => {
        const name = item.provider && item.provider.trim() ? item.provider.trim() : '待补充供应商'
        if (!bucket[name]) {
          bucket[name] = { name, count: 0, total: 0 }
        }
        bucket[name].count += 1
        bucket[name].total += this.getPurchaseTotal(item)
      })
      return Object.values(bucket)
        .sort((a, b) => b.total - a.total)
        .slice(0, 5)
    },
    supplyNarrative() {
      if (!this.purchaseList.length && !this.inventoryList.length) {
        return '当前尚未沉淀可用于研判的供给样本。'
      }
      if (this.lowStockItems.length >= 4) {
        return '当前补给压力偏高，建议先锁定低于安全线的物资，再安排补货。'
      }
      if (this.providerRanking.length <= 1 && this.purchaseList.length > 0) {
        return '采购来源过于集中，建议至少补足两家以上稳定供应商。'
      }
      return '库存与采购处于可控区间，可转入周转与成本优化观察。'
    }
  },
  created() {
    this.refreshAll()
  },
  methods: {
    refreshAll() {
      this.loadInventory()
      this.loadPurchase()
    },
    loadInventory() {
      this.inventoryLoading = true
      this.request.get('/inventory/page', {
        params: {
          pageNum: 1,
          pageSize: 200,
          produce: this.inventoryKeyword
        }
      }).then(res => {
        this.inventoryList = res.code === '200' ? (res.data.records || []) : []
      }).finally(() => {
        this.inventoryLoading = false
      })
    },
    loadPurchase() {
      this.purchaseLoading = true
      this.request.get('/purchase/page', {
        params: {
          pageNum: 1,
          pageSize: 200,
          product: this.purchaseKeyword
        }
      }).then(res => {
        this.purchaseList = res.code === '200' ? (res.data.records || []) : []
      }).finally(() => {
        this.purchaseLoading = false
      })
    },
    openInventoryDialog(row) {
      this.inventoryForm = row ? JSON.parse(JSON.stringify(row)) : createInventoryForm()
      this.inventoryDialogVisible = true
    },
    saveInventory() {
      const payload = Object.assign({}, this.inventoryForm)
      if (!payload.safeStock) {
        payload.safeStock = Math.max(10, Math.round((Number(payload.maxStock) || 100) * 0.2))
      }
      this.request.post('/inventory', payload).then(res => {
        if (res.code === '200') {
          this.$message.success('库存档案已更新')
          this.inventoryDialogVisible = false
          this.loadInventory()
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      })
    },
    removeInventory(row) {
      this.$confirm(`确定删除物资“${row.produce}”吗？`, '删除确认', {
        type: 'warning'
      }).then(() => {
        this.request.delete('/inventory/' + row.id).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.loadInventory()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {})
    },
    openPurchaseDialog(row) {
      this.purchaseForm = row ? JSON.parse(JSON.stringify(row)) : createPurchaseForm(this.user)
      this.purchaseDialogVisible = true
    },
    savePurchase() {
      this.request.post('/purchase', this.purchaseForm).then(res => {
        if (res.code === '200') {
          this.$message.success('采购记录已更新')
          this.purchaseDialogVisible = false
          this.loadPurchase()
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      })
    },
    removePurchase(row) {
      this.$confirm(`确定删除采购记录“${row.product}”吗？`, '删除确认', {
        type: 'warning'
      }).then(() => {
        this.request.delete('/purchase/' + row.id).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.loadPurchase()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {})
    },
    exportInventory() {
      window.open(this.apiConfig.inventoryExport)
    },
    exportPurchase() {
      window.open(this.apiConfig.purchaseExport)
    },
    getSafeStock(item) {
      const maxStock = Number(item.maxStock) || 100
      return Number(item.safeStock) || Math.max(10, Math.round(maxStock * 0.2))
    },
    getRestockGap(item) {
      return Math.max(0, this.getSafeStock(item) - (Number(item.number) || 0))
    },
    getCoverageDays(item) {
      const dailyConsumption = Number(item.dailyConsumption) || 0
      if (!dailyConsumption) {
        return '待计算'
      }
      return Math.floor((Number(item.number) || 0) / dailyConsumption) + ' 天'
    },
    getInventoryState(item) {
      const current = Number(item.number) || 0
      const safeStock = this.getSafeStock(item)
      const maxStock = Number(item.maxStock) || 100
      if (current < safeStock) {
        return '需要补货'
      }
      if (current / maxStock > 0.8) {
        return '储备充足'
      }
      return '平稳周转'
    },
    getInventoryStateClass(item) {
      const text = this.getInventoryState(item)
      if (text === '需要补货') {
        return 'table-status--risk'
      }
      if (text === '储备充足') {
        return 'table-status--safe'
      }
      return 'table-status--neutral'
    },
    getPurchaseTotal(row) {
      return (Number(row.price) || 0) * (Number(row.number) || 0)
    },
    getProviderShare(item) {
      if (!this.purchaseAmount) {
        return 0
      }
      return Math.min(100, Math.round((item.total / this.purchaseAmount) * 100))
    },
    formatCurrency(value) {
      return '¥' + (Number(value) || 0).toFixed(2)
    }
  }
}
</script>

<style scoped>
.business-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.12), transparent 30%),
    linear-gradient(180deg, #f7fbf7 0%, #eef6f2 100%);
}

.supply-hero,
.supply-insight-grid,
.ledger-panel {
  max-width: 1480px;
  margin: 0 auto 22px;
}

.supply-hero {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(15, 118, 110, 0.1);
  box-shadow: 0 20px 45px rgba(148, 163, 184, 0.12);
}

.supply-hero__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(217, 119, 6, 0.12);
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.supply-hero h1 {
  margin: 16px 0 12px;
  font-size: 34px;
  color: #0f172a;
}

.supply-hero p {
  margin: 0;
  line-height: 1.8;
  color: #475569;
}

.supply-hero__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff 0%, #f6fbf8 100%);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.metric-card span,
.metric-card small {
  display: block;
}

.metric-card span {
  color: #64748b;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 28px;
  color: #0f172a;
}

.metric-card small {
  color: #0f766e;
}

.supply-insight-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 18px;
}

.insight-panel,
.ledger-panel {
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 42px rgba(148, 163, 184, 0.1);
}

.insight-panel__header,
.ledger-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.insight-panel__header h2,
.ledger-panel__header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #0f172a;
}

.insight-panel__header p,
.ledger-panel__header p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.insight-panel__pill {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.risk-list,
.provider-list {
  display: grid;
  gap: 12px;
}

.risk-item,
.provider-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.risk-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.risk-item__title,
.provider-item__name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.risk-item__meta,
.provider-item__meta {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.risk-item__side {
  text-align: right;
  white-space: nowrap;
}

.risk-item__side strong {
  display: block;
  font-size: 18px;
  color: #dc2626;
}

.risk-item__side span {
  color: #64748b;
  font-size: 12px;
}

.provider-item__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.provider-note {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(217, 119, 6, 0.12), rgba(15, 118, 110, 0.08));
}

.provider-note span {
  display: inline-block;
  margin-bottom: 8px;
  color: #92400e;
  font-size: 12px;
  font-weight: 700;
}

.provider-note p {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.ledger-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.ledger-toolbar .el-input {
  max-width: 320px;
}

.ledger-toolbar__actions {
  display: flex;
  gap: 10px;
}

.table-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 78px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.table-status--safe {
  color: #047857;
  background: rgba(16, 185, 129, 0.12);
}

.table-status--risk {
  color: #dc2626;
  background: rgba(248, 113, 113, 0.14);
}

.table-status--neutral {
  color: #334155;
  background: rgba(148, 163, 184, 0.14);
}

.action-danger {
  color: #dc2626;
}

.empty-state {
  padding: 36px 18px;
  text-align: center;
  color: #94a3b8;
  border-radius: 18px;
  background: #f8fafc;
}

::v-deep .supply-tabs .el-tabs__header {
  margin-bottom: 20px;
}

::v-deep .supply-tabs .el-tabs__nav-wrap::after {
  background-color: #e2e8f0;
}

::v-deep .supply-tabs .el-tabs__item {
  height: 42px;
  line-height: 42px;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .supply-hero,
  .supply-insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .business-page {
    padding: 16px;
  }

  .supply-hero,
  .insight-panel,
  .ledger-panel {
    padding: 18px;
    border-radius: 22px;
  }

  .supply-hero__metrics {
    grid-template-columns: 1fr;
  }

  .ledger-toolbar {
    flex-direction: column;
  }

  .ledger-toolbar .el-input {
    max-width: none;
  }

  .ledger-toolbar__actions {
    flex-wrap: wrap;
  }
}
</style>
