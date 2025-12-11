<template>
  <div class="page-container">
    <div class="kpi-board">
      <div class="kpi-card blue">
        <div class="icon"><i class="el-icon-box"></i></div>
        <div class="text">
          <div class="label">物资总数</div>
          <div class="value">{{ total }} <span class="unit">件</span></div>
        </div>
      </div>
      <div class="kpi-card orange">
        <div class="icon"><i class="el-icon-warning-outline"></i></div>
        <div class="text">
          <div class="label">库存预警</div>
          <div class="value">{{ alertCount }} <span class="unit">项</span></div>
        </div>
      </div>
      <div class="kpi-card green">
        <div class="icon"><i class="el-icon-s-home"></i></div>
        <div class="text">
          <div class="label">仓库使用率</div>
          <div class="value">{{ warehouseUsage }}%</div>
        </div>
      </div>
    </div>

    <div class="main-card">
      <div class="toolbar">
        <div class="search-group">
          <el-input 
            v-model="produce" 
            placeholder="请输入物资名称..." 
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
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">入库</el-button>
          <el-button type="danger" icon="el-icon-delete" plain @click="delBatch" :disabled="!multipleSelection.length">批量出库</el-button>
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
        
        <el-table-column prop="produce" label="物资名称">
          <template slot-scope="scope">
            <span style="font-weight:bold; color:#303133">{{ scope.row.produce }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="warehouse" label="所属仓库">
          <template slot-scope="scope">
            <el-tag size="small" effect="plain">{{ scope.row.warehouse || '默认仓' }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="region" label="存放区域">
           <template slot-scope="scope">
             <i class="el-icon-location-outline"></i> {{ scope.row.region || 'A区' }}
           </template>
        </el-table-column>
        
        <el-table-column prop="number" label="当前库存" align="center">
          <template slot-scope="scope">
            <span :class="{'low-stock': scope.row.number < 10}" style="font-weight:bold">
              {{ scope.row.number }}
            </span>
            <el-tag v-if="scope.row.number < 10" type="danger" size="mini" style="margin-left:5px">低</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="keeper" label="管理员" align="center">
           <template slot-scope="scope">
             <el-avatar size="small" :src="user.avatarUrl" style="vertical-align: middle; margin-right:5px"></el-avatar>
             {{ scope.row.keeper }}
           </template>
        </el-table-column>
        
        <el-table-column prop="remark" label="备注" show-overflow-tooltip></el-table-column>

        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">盘点</el-button>
            <el-button type="text" class="del-btn" icon="el-icon-delete" @click="del(scope.row.id)">报废</el-button>
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

    <el-dialog title="📦 物资入库登记" :visible.sync="dialogFormVisible" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px" size="small" :model="form" class="custom-form">
        <el-form-item label="物资名称">
          <el-input v-model="form.produce" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="所属仓库">
          <el-select v-model="form.warehouse" placeholder="请选择" style="width:100%">
            <el-option label="1号原料仓" value="1号仓"></el-option>
            <el-option label="2号设备仓" value="2号仓"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="存放区域">
          <el-input v-model="form.region" placeholder="例如：A-01货架"></el-input>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="form.number" :min="1" style="width:100%"></el-input-number>
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
export default {
  name: "Inventory",
  data() {
    return {
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      produce: "",
      form: {},
      dialogFormVisible: false,
      multipleSelection: [],
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      loading: false,
      alertCount: 0,
      warehouseUsage: 0
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true;
      this.request.get("/inventory/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          produce: this.produce,
        }
      }).then(res => {
        this.loading = false;
        this.tableData = res.data.records
        this.total = res.data.total
        
        // 计算库存预警数（库存低于安全库存的物资数量）
        this.alertCount = this.tableData.filter(item => 
          item.number !== null && item.safeStock !== null && item.number < item.safeStock
        ).length;
        
        // 计算仓库使用率（当前库存/最大库存）
        if (this.total > 0 && this.tableData.length > 0) {
          const totalCurrent = this.tableData.reduce((sum, item) => sum + (item.number || 0), 0);
          const totalMax = this.tableData.reduce((sum, item) => sum + (item.maxStock || item.number || 100), 0);
          this.warehouseUsage = totalMax > 0 ? Math.round((totalCurrent / totalMax) * 100) : 0;
        } else {
          this.warehouseUsage = 0;
        }
      })
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
      this.form = {}
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogFormVisible = true
    },
    del(id) {
      this.request.delete("/inventory/" + id).then(res => {
        if (res.code === '200') {
          this.$message.success("已报废出库")
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    delBatch() {
      let ids = this.multipleSelection.map(v => v.id)
      this.request.post("/inventory/del/batch", ids).then(res => {
        if (res.code === '200') {
          this.$message.success("批量出库成功")
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },
    reset() {
      this.produce = ""
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
      window.open("http://localhost:9090/inventory/export")
    }
  }
}
</script>

<style scoped>
/* 全局背景 */
.page-container.inventory-page {
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

.inventory-page::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

/* 1. 顶部 KPI 卡片 */
.kpi-board {
  display: flex; gap: 20px; margin-bottom: 20px;
}
.kpi-card {
  flex: 1; background: white; padding: 20px; border-radius: 12px;
  display: flex; align-items: center; gap: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03); transition: none;
}
.kpi-card:hover { transform: none; }
.kpi-card .icon {
  width: 50px; height: 50px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}
.blue .icon { background: #ecf5ff; color: #409eff; }
.orange .icon { background: #fdf6ec; color: #e6a23c; }
.green .icon { background: #f0f9eb; color: #67c23a; }
.kpi-card .text .label { color: #909399; font-size: 13px; margin-bottom: 5px; }
.kpi-card .text .value { color: #303133; font-size: 24px; font-weight: bold; }
.kpi-card .text .unit { font-size: 12px; font-weight: normal; color: #999; }

/* 2. 主内容卡片 */
.main-card {
  background: white; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
}
::v-deep .main-card .el-table { margin: 0 -20px; width: calc(100% + 40px); }
::v-deep .main-card .el-table::before { display: none; }

/* 工具栏 */
.toolbar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
}
.search-group { display: flex; gap: 10px; }
.custom-input { width: 240px; }
.btn-group { display: flex; gap: 10px; }

/* 表格美化 */
.low-stock { color: #f56c6c; }
.del-btn { color: #f56c6c; }
.del-btn:hover { color: #f78989; }

/* 分页 */
.pagination-container {
  margin-top: 20px; display: flex; justify-content: flex-end;
}

/* 禁用所有浮动效果 */
::v-deep .el-button:hover {
  transform: none !important;
  box-shadow: inherit !important;
}

::v-deep .el-card:hover {
  transform: none !important;
  box-shadow: inherit !important;
}

::v-deep .el-table__row:hover > td {
  background: inherit !important;
}

::v-deep .el-input:hover .el-input__inner {
  border-color: inherit !important;
}

::v-deep .el-input__inner:focus {
  border-color: inherit !important;
  box-shadow: none !important;
}

* {
  transition: none !important;
}

/* 修复按钮文字显示 */
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