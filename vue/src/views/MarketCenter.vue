<template>
  <div class="business-page market-center-page" v-loading="salesLoading || onlineLoading">
    <section class="market-hero">
      <div class="market-hero__intro">
        <span class="market-hero__eyebrow">业务协同</span>
        <h1>产销协同中心</h1>
        <p>
          产销协同中心统一展示订单去向、在架状态和待转化库存，支撑销售履约、
          联销管理与库存转化，形成完整的产销协作视图。
        </p>
      </div>
      <div class="market-hero__metrics">
        <article class="metric-card">
          <span>销售总额</span>
          <strong>{{ formatCurrency(totalSalesAmount) }}</strong>
          <small>{{ salesList.length }} 笔订单</small>
        </article>
        <article class="metric-card">
          <span>客户覆盖</span>
          <strong>{{ buyerCount }}</strong>
          <small>主销作物 {{ hotCropName || '待形成样本' }}</small>
        </article>
        <article class="metric-card">
          <span>在架品项</span>
          <strong>{{ onShelfCount }}</strong>
          <small>预计货值 {{ formatCurrency(onShelfValue) }}</small>
        </article>
        <article class="metric-card">
          <span>待转上架</span>
          <strong>{{ pendingListings.length }}</strong>
          <small>联销覆盖率 {{ listingCoverageRate }}%</small>
        </article>
      </div>
    </section>

    <section class="market-grid">
      <article class="market-panel market-panel--channel">
        <div class="market-panel__header">
          <div>
            <h2>客户去向分布</h2>
            <p>展示客户覆盖、订单分布和当前主要销售去向。</p>
          </div>
        </div>

        <div v-if="buyerRanking.length" class="channel-list">
          <div v-for="item in buyerRanking" :key="item.name" class="channel-item">
            <div class="channel-item__top">
              <div>
                <div class="channel-item__name">{{ item.name }}</div>
                <div class="channel-item__meta">{{ item.count }} 笔订单</div>
              </div>
              <strong>{{ formatCurrency(item.total) }}</strong>
            </div>
            <div class="channel-item__bar">
              <div class="channel-item__fill" :style="{ width: getBuyerShare(item) + '%' }"></div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">当前没有销售样本，暂时无法形成渠道分布。</div>
      </article>

      <article class="market-panel market-panel--listing">
        <div class="market-panel__header">
          <div>
            <h2>上架状态板</h2>
            <p>集中呈现在架、下架与售罄状态，便于跟踪联销进度。</p>
          </div>
        </div>

        <div class="listing-columns">
          <div v-for="column in lifecycleColumns" :key="column.key" class="listing-column">
            <div class="listing-column__header">
              <span>{{ column.title }}</span>
              <strong>{{ column.items.length }}</strong>
            </div>
            <div v-if="column.items.length" class="listing-column__body">
              <div v-for="item in column.items" :key="item.id" class="listing-card">
                <div class="listing-card__name">{{ item.produce }}</div>
                <div class="listing-card__meta">{{ item.warehouse || '默认仓区' }} · {{ item.quantity }} 件</div>
                <div class="listing-card__price">{{ formatCurrency(item.totalPrice || item.price) }}</div>
              </div>
            </div>
            <div v-else class="listing-column__empty">暂无记录</div>
          </div>
        </div>
      </article>
    </section>

    <section class="market-grid market-grid--secondary">
      <article class="market-panel">
        <div class="market-panel__header">
          <div>
            <h2>待转上架清单</h2>
            <p>把库存中尚未进入联销链路的作物挑出来，便于做后续展示。</p>
          </div>
          <span class="market-panel__pill">{{ pendingListings.length }} 项</span>
        </div>

        <div v-if="pendingListings.length" class="pending-list">
          <div v-for="item in pendingListings" :key="item.id" class="pending-item">
            <div>
              <div class="pending-item__name">{{ item.produce }}</div>
              <div class="pending-item__meta">{{ item.warehouse || '默认仓区' }} · 库存 {{ item.number || 0 }}</div>
            </div>
            <div class="pending-item__flag">待转上架</div>
          </div>
        </div>
        <div v-else class="empty-state">当前库存已基本覆盖到联销环节。</div>
      </article>

      <article class="market-panel">
        <div class="market-panel__header">
          <div>
            <h2>作物成交热度</h2>
            <p>和客户去向分开讲，避免销售页只剩一个重复表格。</p>
          </div>
        </div>

        <div v-if="cropRanking.length" class="crop-grid">
          <div v-for="item in cropRanking" :key="item.name" class="crop-card">
            <div class="crop-card__name">{{ item.name }}</div>
            <div class="crop-card__amount">{{ formatCurrency(item.total) }}</div>
            <div class="crop-card__meta">销量 {{ item.number }} · 订单 {{ item.count }}</div>
          </div>
        </div>
        <div v-else class="empty-state">当前没有形成稳定的销售热度样本。</div>
      </article>
    </section>

    <section class="ledger-panel">
      <div class="ledger-panel__header">
        <div>
          <h2>产销台账</h2>
          <p>统一查看销售订单与上架记录，支撑录入、维护和状态跟踪。</p>
        </div>
      </div>

      <el-tabs v-model="activeLedger" class="market-tabs">
        <el-tab-pane label="销售台账" name="sales">
          <div class="ledger-toolbar">
            <el-input
              v-model="salesKeyword"
              clearable
              placeholder="搜索销售作物"
              prefix-icon="el-icon-search"
              @clear="loadSales"
              @keyup.enter.native="loadSales"
            />
            <div class="ledger-toolbar__actions">
              <el-button type="primary" icon="el-icon-plus" @click="openSalesDialog()">录入订单</el-button>
              <el-button plain icon="el-icon-refresh" @click="loadSales">刷新</el-button>
              <el-button plain icon="el-icon-download" @click="exportSales">导出</el-button>
            </div>
          </div>

          <el-table :data="salesList" stripe>
            <el-table-column prop="product" label="作物名称" min-width="180" />
            <el-table-column prop="buyer" label="客户名称" min-width="160" />
            <el-table-column prop="number" label="数量" width="90" align="center" />
            <el-table-column label="单价" width="120" align="center">
              <template slot-scope="{ row }">
                {{ formatCurrency(row.price) }}
              </template>
            </el-table-column>
            <el-table-column label="订单金额" width="120" align="center">
              <template slot-scope="{ row }">
                {{ formatCurrency(getSalesTotal(row)) }}
              </template>
            </el-table-column>
            <el-table-column prop="shipper" label="出货人" width="110" align="center" />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" @click="openSalesDialog(row)">编辑</el-button>
                <el-button type="text" class="action-danger" @click="removeSales(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="上架管理" name="online">
          <div class="ledger-toolbar">
            <el-input
              v-model="onlineKeyword"
              clearable
              placeholder="搜索上架作物"
              prefix-icon="el-icon-search"
              @clear="loadOnline"
              @keyup.enter.native="loadOnline"
            />
            <div class="ledger-toolbar__actions">
              <el-button type="primary" icon="el-icon-plus" @click="openOnlineDialog()">新建上架</el-button>
              <el-button plain icon="el-icon-refresh" @click="loadOnline">刷新</el-button>
              <el-button plain icon="el-icon-download" @click="exportOnline">导出</el-button>
            </div>
          </div>

          <el-table :data="onlineList" stripe>
            <el-table-column prop="produce" label="上架作物" min-width="180" />
            <el-table-column prop="warehouse" label="仓区" width="120" />
            <el-table-column prop="quantity" label="数量" width="90" align="center" />
            <el-table-column label="总价" width="120" align="center">
              <template slot-scope="{ row }">
                {{ formatCurrency(row.totalPrice || row.price) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template slot-scope="{ row }">
                <span :class="['table-status', getStatusClass(row.status)]">
                  {{ row.status || '待处理' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="seller" label="登记人" width="110" align="center" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" @click="openOnlineDialog(row)">编辑</el-button>
                <el-button type="text" @click="toggleStatus(row)">
                  {{ row.status === '上架中' ? '下架' : '上架' }}
                </el-button>
                <el-button type="text" class="action-danger" @click="removeOnline(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      :title="salesForm.id ? '编辑销售订单' : '录入销售订单'"
      :visible.sync="salesDialogVisible"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form :model="salesForm" label-width="100px" size="small">
        <el-form-item label="作物名称">
          <el-input v-model="salesForm.product" />
        </el-form-item>
        <el-form-item label="销售数量">
          <el-input-number v-model="salesForm.number" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="销售单价">
          <el-input-number
            v-model="salesForm.price"
            :min="0"
            :precision="2"
            :step="0.1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="客户名称">
          <el-input v-model="salesForm.buyer" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="salesForm.phone" />
        </el-form-item>
        <el-form-item label="交付地址">
          <el-input v-model="salesForm.address" />
        </el-form-item>
        <el-form-item label="出货人">
          <el-input v-model="salesForm.shipper" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="salesForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="salesDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSales">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :title="onlineForm.id ? '编辑上架档案' : '新建上架档案'"
      :visible.sync="onlineDialogVisible"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="onlineFormRef" :model="onlineForm" label-width="100px" size="small">
        <el-form-item label="库存物资">
          <el-select
            v-model="onlineForm.inventoryId"
            placeholder="请选择库存物资"
            style="width: 100%"
            :disabled="!!onlineForm.id"
            @change="onInventoryChange"
          >
            <el-option
              v-for="item in inventoryList"
              :key="item.id"
              :label="item.produce + '（库存：' + (item.number || 0) + '）'"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="作物名称">
          <el-input v-model="onlineForm.produce" disabled />
        </el-form-item>
        <el-form-item label="所属仓区">
          <el-input v-model="onlineForm.warehouse" disabled />
        </el-form-item>
        <el-form-item label="上架数量">
          <el-input-number
            v-model="onlineForm.quantity"
            :min="1"
            :max="maxOnlineQuantity || 9999"
            style="width: 100%"
          />
          <div v-if="maxOnlineQuantity" class="field-tip">当前最多可上架 {{ maxOnlineQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number
            v-model="onlineForm.price"
            :min="0.01"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="登记人">
          <el-input v-model="onlineForm.seller" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="onlineForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="onlineDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOnline">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import apiConfig from '@/config/api.config'

function createSalesForm(user) {
  return {
    product: '',
    price: 0,
    number: 1,
    buyer: '',
    address: '',
    phone: '',
    shipper: user.nickname || user.username || '',
    remark: ''
  }
}

function createOnlineForm(user) {
  return {
    inventoryId: '',
    produce: '',
    warehouse: '',
    quantity: 1,
    price: 0,
    totalPrice: 0,
    status: '上架中',
    seller: user.nickname || user.username || '',
    remark: ''
  }
}

export default {
  name: 'MarketCenter',
  data() {
    const user = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : {}
    return {
      apiConfig,
      user,
      activeLedger: 'sales',
      salesKeyword: '',
      onlineKeyword: '',
      salesList: [],
      onlineList: [],
      inventoryList: [],
      salesLoading: false,
      onlineLoading: false,
      salesDialogVisible: false,
      onlineDialogVisible: false,
      salesForm: createSalesForm(user),
      onlineForm: createOnlineForm(user),
      maxOnlineQuantity: 0
    }
  },
  computed: {
    totalSalesAmount() {
      return this.salesList.reduce((sum, item) => sum + this.getSalesTotal(item), 0)
    },
    buyerCount() {
      return new Set(
        this.salesList
          .map(item => (item.buyer || '').trim())
          .filter(Boolean)
      ).size
    },
    buyerRanking() {
      const bucket = {}
      this.salesList.forEach(item => {
        const name = item.buyer && item.buyer.trim() ? item.buyer.trim() : '待补充客户'
        if (!bucket[name]) {
          bucket[name] = { name, count: 0, total: 0 }
        }
        bucket[name].count += 1
        bucket[name].total += this.getSalesTotal(item)
      })
      return Object.values(bucket)
        .sort((a, b) => b.total - a.total)
        .slice(0, 5)
    },
    cropRanking() {
      const bucket = {}
      this.salesList.forEach(item => {
        const name = item.product && item.product.trim() ? item.product.trim() : '未命名作物'
        if (!bucket[name]) {
          bucket[name] = { name, count: 0, number: 0, total: 0 }
        }
        bucket[name].count += 1
        bucket[name].number += Number(item.number) || 0
        bucket[name].total += this.getSalesTotal(item)
      })
      return Object.values(bucket)
        .sort((a, b) => b.total - a.total)
        .slice(0, 6)
    },
    hotCropName() {
      return this.cropRanking.length ? this.cropRanking[0].name : ''
    },
    onShelfCount() {
      return this.onlineList.filter(item => item.status === '上架中').length
    },
    offShelfCount() {
      return this.onlineList.filter(item => item.status === '已下架').length
    },
    soldOutCount() {
      return this.onlineList.filter(item => item.status === '已售罄').length
    },
    onShelfValue() {
      return this.onlineList
        .filter(item => item.status === '上架中')
        .reduce((sum, item) => sum + (Number(item.totalPrice) || this.getOnlineTotal(item)), 0)
    },
    listingCoverageRate() {
      if (!this.inventoryList.length) {
        return 0
      }
      return Math.round((this.onShelfCount / this.inventoryList.length) * 100)
    },
    pendingListings() {
      const activeIds = new Set(
        this.onlineList
          .filter(item => item.status === '上架中')
          .map(item => item.inventoryId)
      )
      return this.inventoryList
        .filter(item => !activeIds.has(item.id))
        .sort((a, b) => (Number(b.number) || 0) - (Number(a.number) || 0))
        .slice(0, 6)
    },
    lifecycleColumns() {
      return [
        { key: '上架中', title: '在架', items: this.onlineList.filter(item => item.status === '上架中') },
        { key: '已下架', title: '已下架', items: this.onlineList.filter(item => item.status === '已下架') },
        { key: '已售罄', title: '已售罄', items: this.onlineList.filter(item => item.status === '已售罄') }
      ]
    }
  },
  created() {
    this.refreshAll()
  },
  methods: {
    refreshAll() {
      this.loadSales()
      this.loadOnline()
      this.loadInventory()
    },
    loadSales() {
      this.salesLoading = true
      this.request.get('/sales/page', {
        params: {
          pageNum: 1,
          pageSize: 200,
          product: this.salesKeyword
        }
      }).then(res => {
        this.salesList = res.code === '200' ? (res.data.records || []) : []
      }).finally(() => {
        this.salesLoading = false
      })
    },
    loadOnline() {
      this.onlineLoading = true
      this.request.get('/onlineSale/page', {
        params: {
          pageNum: 1,
          pageSize: 200,
          produce: this.onlineKeyword
        }
      }).then(res => {
        this.onlineList = res.code === '200' ? (res.data.records || []) : []
      }).finally(() => {
        this.onlineLoading = false
      })
    },
    loadInventory() {
      this.request.get('/inventory/page', {
        params: {
          pageNum: 1,
          pageSize: 1000,
          produce: ''
        }
      }).then(res => {
        this.inventoryList = res.code === '200' ? (res.data.records || []) : []
      })
    },
    openSalesDialog(row) {
      this.salesForm = row ? JSON.parse(JSON.stringify(row)) : createSalesForm(this.user)
      this.salesDialogVisible = true
    },
    saveSales() {
      this.request.post('/sales', this.salesForm).then(res => {
        if (res.code === '200') {
          this.$message.success('销售订单已更新')
          this.salesDialogVisible = false
          this.loadSales()
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      })
    },
    removeSales(row) {
      this.$confirm(`确定删除订单“${row.product}”吗？`, '删除确认', {
        type: 'warning'
      }).then(() => {
        this.request.delete('/sales/' + row.id).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.loadSales()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {})
    },
    openOnlineDialog(row) {
      if (row) {
        this.onlineForm = JSON.parse(JSON.stringify(row))
        this.maxOnlineQuantity = 0
      } else {
        this.onlineForm = createOnlineForm(this.user)
        this.maxOnlineQuantity = 0
      }
      this.onlineDialogVisible = true
    },
    onInventoryChange(inventoryId) {
      const current = this.inventoryList.find(item => item.id === inventoryId)
      if (!current) {
        return
      }
      this.onlineForm.produce = current.produce
      this.onlineForm.warehouse = current.warehouse
      this.onlineForm.quantity = 1
      this.maxOnlineQuantity = Number(current.number) || 0
    },
    saveOnline() {
      const payload = Object.assign({}, this.onlineForm)
      if (!payload.inventoryId) {
        this.$message.warning('请先选择库存物资')
        return
      }
      payload.totalPrice = this.getOnlineTotal(payload)
      if (!payload.status) {
        payload.status = '上架中'
      }
      this.request.post('/onlineSale', payload).then(res => {
        if (res.code === '200') {
          this.$message.success('上架档案已更新')
          this.onlineDialogVisible = false
          this.loadOnline()
          this.loadInventory()
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      })
    },
    toggleStatus(row) {
      const newStatus = row.status === '上架中' ? '已下架' : '上架中'
      this.request.put('/onlineSale/status/' + row.id + '?status=' + newStatus).then(res => {
        if (res.code === '200') {
          this.$message.success(newStatus === '上架中' ? '已上架' : '已下架')
          this.loadOnline()
        } else {
          this.$message.error(res.msg || '状态更新失败')
        }
      })
    },
    removeOnline(row) {
      this.$confirm(`确定删除上架档案“${row.produce}”吗？`, '删除确认', {
        type: 'warning'
      }).then(() => {
        this.request.delete('/onlineSale/' + row.id).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功')
            this.loadOnline()
            this.loadInventory()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
      }).catch(() => {})
    },
    exportSales() {
      window.open(this.apiConfig.salesExport)
    },
    exportOnline() {
      window.open(this.apiConfig.onlineSaleExport)
    },
    getSalesTotal(row) {
      return (Number(row.price) || 0) * (Number(row.number) || 0)
    },
    getOnlineTotal(row) {
      return (Number(row.price) || 0) * (Number(row.quantity) || 0)
    },
    getBuyerShare(item) {
      if (!this.totalSalesAmount) {
        return 0
      }
      return Math.min(100, Math.round((item.total / this.totalSalesAmount) * 100))
    },
    getStatusClass(status) {
      if (status === '上架中') {
        return 'table-status--safe'
      }
      if (status === '已售罄') {
        return 'table-status--risk'
      }
      return 'table-status--neutral'
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
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
}

.market-hero,
.market-grid,
.ledger-panel {
  max-width: 1480px;
  margin: 0 auto 22px;
}

.market-hero {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.1);
  box-shadow: 0 20px 45px rgba(148, 163, 184, 0.12);
}

.market-hero__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.market-hero h1 {
  margin: 16px 0 12px;
  font-size: 34px;
  color: #0f172a;
}

.market-hero p {
  margin: 0;
  line-height: 1.8;
  color: #475569;
}

.market-hero__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff 0%, #f7faff 100%);
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
  color: #2563eb;
}

.market-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.market-grid--secondary {
  grid-template-columns: 0.95fr 1.05fr;
}

.market-panel,
.ledger-panel {
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 42px rgba(148, 163, 184, 0.1);
}

.market-panel__header,
.ledger-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.market-panel__header h2,
.ledger-panel__header h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #0f172a;
}

.market-panel__header p,
.ledger-panel__header p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.market-panel__pill {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.channel-list,
.pending-list,
.crop-grid {
  display: grid;
  gap: 12px;
}

.channel-item,
.pending-item,
.crop-card,
.listing-card {
  padding: 16px 18px;
  border-radius: 20px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.channel-item__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.channel-item__name,
.pending-item__name,
.crop-card__name,
.listing-card__name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.channel-item__meta,
.pending-item__meta,
.crop-card__meta,
.listing-card__meta {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.channel-item__bar {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.channel-item__fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #38bdf8);
}

.listing-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.listing-column {
  padding: 16px;
  border-radius: 22px;
  background: linear-gradient(180deg, #f8fbff 0%, #f1f5f9 100%);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.listing-column__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 700;
  color: #0f172a;
}

.listing-column__body {
  display: grid;
  gap: 10px;
}

.listing-card__price,
.crop-card__amount {
  margin-top: 10px;
  font-weight: 700;
  color: #2563eb;
}

.listing-column__empty,
.empty-state {
  padding: 32px 16px;
  text-align: center;
  color: #94a3b8;
  border-radius: 18px;
  background: #f8fafc;
}

.pending-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.pending-item__flag {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.12);
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.crop-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.field-tip {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

::v-deep .market-tabs .el-tabs__header {
  margin-bottom: 20px;
}

::v-deep .market-tabs .el-tabs__nav-wrap::after {
  background-color: #e2e8f0;
}

::v-deep .market-tabs .el-tabs__item {
  height: 42px;
  line-height: 42px;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .market-hero,
  .market-grid,
  .market-grid--secondary {
    grid-template-columns: 1fr;
  }

  .listing-columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .business-page {
    padding: 16px;
  }

  .market-hero,
  .market-panel,
  .ledger-panel {
    padding: 18px;
    border-radius: 22px;
  }

  .market-hero__metrics,
  .crop-grid {
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
