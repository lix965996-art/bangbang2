<template>
  <div class="page-container">
    <div class="kpi-board">
      <div class="kpi-card green">
        <div class="icon"><i class="el-icon-wallet"></i></div>
        <div class="text">
          <div class="label">销售总额</div>
          <div class="value">¥ {{ monthlyTotal.toLocaleString() }}</div>
        </div>
      </div>
      <div class="kpi-card orange">
        <div class="icon"><i class="el-icon-trophy"></i></div>
        <div class="text">
          <div class="label">最热销作物</div>
          <div class="value">{{ hotSalesCrop || '暂无数据' }}</div>
        </div>
      </div>
      <div class="kpi-card blue">
        <div class="icon"><i class="el-icon-s-order"></i></div>
        <div class="text">
          <div class="label">订单总数</div>
          <div class="value">{{ total }} <span class="unit">单</span></div>
        </div>
      </div>
    </div>

    <div class="main-card">
      <div class="toolbar">
        <div class="search-group">
          <el-input 
            v-model="product" 
            placeholder="搜索销售作物..." 
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
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">录入销售单</el-button>
          <el-button type="danger" icon="el-icon-delete" plain @click="delBatch" :disabled="!multipleSelection.length">批量删除</el-button>
          <el-button type="success" icon="el-icon-download" plain @click="exp">导出月报</el-button>
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
        
        <el-table-column prop="product" label="销售作物">
          <template slot-scope="scope">
            <span style="font-weight:bold; color:#303133">{{ scope.row.product }}</span>
            <el-tag size="mini" type="success" style="margin-left: 5px">现货</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="price" label="销售单价 (元)" align="right">
          <template slot-scope="scope">
            <span style="color: #67C23A; font-weight:bold">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="number" label="销售数量" align="center">
           <template slot-scope="scope">
             <span style="font-family: monospace; font-size: 14px">{{ scope.row.number }}</span>
           </template>
        </el-table-column>

        <el-table-column label="销售总额" align="right">
          <template slot-scope="scope">
            <span style="color: #67C23A; font-weight:bold; font-size: 15px">
              + ¥{{ (scope.row.price * scope.row.number).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="buyer" label="采购商/客户" show-overflow-tooltip>
           <template slot-scope="scope">
             <i class="el-icon-s-custom"></i> {{ scope.row.buyer }}
           </template>
        </el-table-column>
        
        <el-table-column prop="time" label="销售时间" width="160" sortable></el-table-column>

        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" class="del-btn" icon="el-icon-delete" @click="del(scope.row.id)">撤单</el-button>
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

    <el-dialog title="💰 销售订单录入" :visible.sync="dialogFormVisible" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px" size="small" :model="form" class="custom-form">
        <el-form-item label="销售作物">
          <el-input v-model="form.product" autocomplete="off" placeholder="请输入销售作物名称"></el-input>
        </el-form-item>
        <el-form-item label="销售单价">
          <el-input-number v-model="form.price" :precision="2" :step="0.1" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="销售数量">
          <el-input-number v-model="form.number" :min="1" style="width: 100%" placeholder="请输入数量"></el-input-number>
        </el-form-item>
        <el-form-item label="采购方">
          <el-input v-model="form.buyer" placeholder="请输入客户名称"></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone"></el-input>
        </el-form-item>
        <el-form-item label="备注信息">
          <el-input type="textarea" v-model="form.remark" placeholder="选填"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确认销售</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "Sales",
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
    // 计算销售总额（统计表格中所有数据）
    monthlyTotal() {
      return this.tableData
        .reduce((sum, item) => {
          const price = parseFloat(item.price) || 0;
          const num = parseInt(item.number) || 0;
          return sum + (price * num);
        }, 0)
        .toFixed(2);
    },
    // 计算最热销作物（按销售总额排名）
    hotSalesCrop() {
      const cropSales = {};
      
      this.tableData.forEach(item => {
        if (!item.product) return;
        const cropName = item.product.trim();
        const price = parseFloat(item.price) || 0;
        const num = parseInt(item.number) || 0;
        const total = price * num;
        
        if (cropSales[cropName]) {
          cropSales[cropName] += total;
        } else {
          cropSales[cropName] = total;
        }
      });
      
      // 找出销售额最高的作物
      let maxSales = 0;
      let hotCrop = '';
      
      for (const [crop, sales] of Object.entries(cropSales)) {
        if (sales > maxSales) {
          maxSales = sales;
          hotCrop = crop;
        }
      }
      
      return hotCrop;
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
      this.request.get("/sales/page", {
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
      this.request.post("/sales", this.form).then(res => {
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
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogFormVisible = true
    },
    del(id) {
      this.request.delete("/sales/" + id).then(res => {
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
      this.request.post("/sales/del/batch", ids).then(res => {
        if (res.code === '200') {
          this.$message.success("批量删除成功")
          this.load()
        } else {
          this.$message.error("批量删除失败")
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
      window.open(this.apiBaseUrl + "/sales/export")
    }
  }
}
</script>

<style scoped>
/* 复用样式，保持统一 */
.sales-page {
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

.sales-page::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.page-container { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }
.kpi-board { display: flex; gap: 20px; margin-bottom: 20px; }
.kpi-card { flex: 1; background: white; padding: 20px; border-radius: 12px; display: flex; align-items: center; gap: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.03); transition: 0.3s; }
.kpi-card:hover { transform: none; }
.kpi-card .icon { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; }
.green .icon { background: #ecfdf5; color: #10b981; }
.orange .icon { background: #fff7ed; color: #f97316; }
.blue .icon { background: #eff6ff; color: #3b82f6; }
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
