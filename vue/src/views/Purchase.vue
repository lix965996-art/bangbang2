<template>
  <div class="page-container">
    <div class="kpi-board">
      <div class="kpi-card orange">
        <div class="icon"><i class="el-icon-money"></i></div>
        <div class="text">
          <div class="label">采购总额</div>
          <div class="value">¥ {{ monthlyTotal.toLocaleString() }}</div>
        </div>
      </div>
      <div class="kpi-card blue">
        <div class="icon"><i class="el-icon-tickets"></i></div>
        <div class="text">
          <div class="label">采购单数</div>
          <div class="value">{{ total }} <span class="unit">笔</span></div>
        </div>
      </div>
      <div class="kpi-card purple">
        <div class="icon"><i class="el-icon-s-custom"></i></div>
        <div class="text">
          <div class="label">合作供应商</div>
          <div class="value">{{ supplierCount }} <span class="unit">家</span></div>
        </div>
      </div>
    </div>

    <div class="main-card">
      <div class="toolbar">
        <div class="search-group">
          <el-input 
            v-model="product" 
            placeholder="搜索采购物资..." 
            prefix-icon="el-icon-search"
            class="custom-input"
            clearable
            @clear="load"
            @keyup.enter.native="load">
          </el-input>
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
          <el-button plain icon="el-icon-refresh" @click="reset">重置</el-button>
        </div>
        
        <div class="btn-group">
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建采购单</el-button>
          <el-button type="danger" icon="el-icon-delete" plain @click="delBatch" :disabled="!multipleSelection.length">批量删除</el-button>
          <el-button type="success" icon="el-icon-download" plain @click="exp">导出报表</el-button>
        </div>
      </div>

      <el-table 
        :data="tableData" 
        style="width: 100%" 
        :header-cell-style="{background:'#f8fafa', color:'#333', fontWeight:'bold'}"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        :default-sort="{prop: 'id', order: 'ascending'}"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="80" align="center" sortable></el-table-column>
        
        <el-table-column prop="product" label="采购物资">
          <template slot-scope="scope">
            <span style="font-weight:bold; color:#303133">{{ scope.row.product }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="price" label="单价 (元)" align="right">
          <template slot-scope="scope">
            <span style="color: #909399">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="number" label="数量" align="center">
           <template slot-scope="scope">
             <el-tag size="small" effect="plain">{{ scope.row.number }}</el-tag>
           </template>
        </el-table-column>

        <el-table-column label="总金额" align="right">
          <template slot-scope="scope">
            <span style="color: #F56C6C; font-weight:bold">
              ¥{{ (scope.row.price * scope.row.number).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="provider" label="供应商" show-overflow-tooltip>
           <template slot-scope="scope">
             <i class="el-icon-office-building"></i> {{ scope.row.provider || '未知供应商' }}
           </template>
        </el-table-column>
        
        <el-table-column prop="purchaser" label="采购员" align="center"></el-table-column>
        <el-table-column prop="time" label="采购时间" width="160" sortable></el-table-column>

        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" class="del-btn" icon="el-icon-delete" @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </div>

    <el-dialog title="📝 采购单填报" :visible.sync="dialogFormVisible" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px" size="small" :model="form" class="custom-form">
        <el-form-item label="物资名称">
          <el-input v-model="form.product" autocomplete="off" placeholder="请输入物资名称"></el-input>
        </el-form-item>
        <el-form-item label="单价 (元)">
          <el-input-number v-model="form.price" :precision="2" :step="0.1" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="采购数量">
          <el-input-number v-model="form.number" :min="1" style="width: 100%" placeholder="请输入数量"></el-input-number>
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.provider" placeholder="请输入供应商名称"></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <!-- 采购员字段已移除，系统自动记录 -->
        <el-form-item label="备注">
          <el-input type="textarea" v-model="form.remark" placeholder="选填"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">提交入库</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "Purchase",
  data() {
    return {
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      product: "",
      form: {},
      dialogFormVisible: false,
      multipleSelection: [],
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      loading: false
    }
  },
  computed: {
    // 计算采购总额（统计表格中所有数据）
    monthlyTotal() {
      return this.tableData
        .reduce((sum, item) => {
          const price = parseFloat(item.price) || 0;
          const num = parseInt(item.number) || 0;
          return sum + (price * num);
        }, 0)
        .toFixed(2);
    },
    // 计算合作供应商数量（去重）
    supplierCount() {
      const suppliers = new Set();
      this.tableData.forEach(item => {
        if (item.provider && item.provider.trim()) {
          suppliers.add(item.provider.trim());
        }
      });
      return suppliers.size;
    },
    apiBaseUrl() {
      return this.request.defaults.baseURL || '';
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true;
      this.request.get("/purchase/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          product: this.product,
        }
      }).then(res => {
        this.loading = false;
        if (res && res.data) {
          this.tableData = res.data.records || [];
          this.total = res.data.total || 0;
        }
      }).catch(() => {
        this.loading = false;
      })
    },
    save() {
      this.request.post("/purchase", this.form).then(res => {
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
      this.form = {}
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogFormVisible = true
    },
    del(id) {
      this.request.delete("/purchase/" + id).then(res => {
        if (res.code === '200') {
          this.$message.success("删除成功")
          this.load()
        } else {
          this.$message.error("删除失败")
        }
      })
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    delBatch() {
      let ids = this.multipleSelection.map(v => v.id)
      this.request.post("/purchase/del/batch", ids).then(res => {
        if (res.code === '200') {
          this.$message.success("批量删除成功")
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },
    reset() {
      this.product = ""
      this.load()
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize
      this.load()
    },
    handleCurrentChange(pageNum) {
      this.pageNum = pageNum
      this.load()
    },
    exp() {
      window.open(this.apiBaseUrl + "/purchase/export")
    }
  }
}
</script>

<style scoped>
/* 复用 Inventory.vue 的样式，保持统一 */
.purchase-page {
  padding: 20px;
  padding-bottom: 80px;
  background: #f5f7fa;
  min-height: 100vh;
  max-height: calc(100vh + 200px);
  overflow-y: auto !important;
  overflow-x: hidden !important;
  /* 隐藏滚动条 */
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE/Edge */
}

.purchase-page::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.page-container { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }
.kpi-board { display: flex; gap: 20px; margin-bottom: 20px; }
.kpi-card { flex: 1; background: white; padding: 20px; border-radius: 12px; display: flex; align-items: center; gap: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.03); transition: 0.3s; }
.kpi-card:hover { transform: none; }
.kpi-card .icon { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.blue .icon { background: #ecf5ff; color: #409eff; }
.orange .icon { background: #fdf6ec; color: #e6a23c; }
.purple .icon { background: #f3e8ff; color: #7c3aed; }
.kpi-card .text .label { color: #909399; font-size: 13px; margin-bottom: 5px; }
.kpi-card .text .value { color: #303133; font-size: 24px; font-weight: bold; }
.kpi-card .text .unit { font-size: 12px; font-weight: normal; color: #999; }
.main-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.03); }
::v-deep .main-card .el-table { margin: 0 -20px; width: calc(100% + 40px); }
::v-deep .main-card .el-table::before { display: none; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-group { display: flex; gap: 10px; }
.custom-input { width: 240px; }
.btn-group { display: flex; gap: 10px; }
.del-btn { color: #f56c6c; }
.del-btn:hover { color: #f78989; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }

/* 修复按钮样式 */
::v-deep .el-button--primary {
  background: #409eff !important;
  border-color: #409eff !important;
  color: #ffffff !important;
}

::v-deep .el-button--danger.is-plain {
  background: #f56c6c !important;
  border-color: #f56c6c !important;
  color: #ffffff !important;
}

::v-deep .el-button--success.is-plain {
  background: #67c23a !important;
  border-color: #67c23a !important;
  color: #ffffff !important;
}

::v-deep .el-button--danger.is-plain:disabled {
  background: #fab6b6 !important;
  border-color: #fab6b6 !important;
  color: #ffffff !important;
}
</style>
