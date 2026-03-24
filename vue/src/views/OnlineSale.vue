<template>
  <div class="page-container">
    <div class="kpi-board">
      <div class="kpi-card blue">
        <div class="icon"><i class="el-icon-shopping-cart-2"></i></div>
        <div class="text">
          <div class="label">在售商品</div>
          <div class="value">{{ onSaleCount }} <span class="unit">件</span></div>
        </div>
      </div>
      <div class="kpi-card orange">
        <div class="icon"><i class="el-icon-money"></i></div>
        <div class="text">
          <div class="label">预计收入</div>
          <div class="value">¥{{ totalIncome.toLocaleString() }}</div>
        </div>
      </div>
      <div class="kpi-card green">
        <div class="icon"><i class="el-icon-sold-out"></i></div>
        <div class="text">
          <div class="label">已售罄</div>
          <div class="value">{{ soldOutCount }} <span class="unit">件</span></div>
        </div>
      </div>
    </div>

    <div class="main-card">
      <div class="toolbar">
        <div class="search-group">
          <el-input 
            v-model="searchProduce" 
            placeholder="请输入商品名称..." 
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
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">上架商品</el-button>
          <el-button type="danger" icon="el-icon-delete" plain @click="delBatch" :disabled="!multipleSelection.length">批量下架</el-button>
          <el-button type="success" icon="el-icon-download" plain @click="exp">导出报表</el-button>
        </div>
      </div>

      <el-table 
        :data="tableData" 
        style="width: 100%" 
        :header-cell-style="{background:'#f8fafa', color:'#333', fontWeight:'bold'}"
        @selection-change="handleSelectionChange"
        v-loading="loading"
        :default-sort="{prop: 'id', order: 'descending'}"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="70" align="center" sortable></el-table-column>
        
        <el-table-column prop="produce" label="商品名称" min-width="120">
          <template slot-scope="scope">
            <span style="font-weight:bold; color:#303133">{{ scope.row.produce }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="warehouse" label="所属仓库" width="100">
          <template slot-scope="scope">
            <el-tag size="small" effect="plain">{{ scope.row.warehouse || '默认仓' }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="quantity" label="出售数量" width="100" align="center">
          <template slot-scope="scope">
            <span style="font-weight:bold; color:#409eff">{{ scope.row.quantity }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="price" label="单价(元)" width="100" align="center">
          <template slot-scope="scope">
            <span style="color:#e6a23c; font-weight:bold">¥{{ scope.row.price }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="totalPrice" label="总价(元)" width="110" align="center">
          <template slot-scope="scope">
            <span style="color:#67c23a; font-weight:bold">¥{{ scope.row.totalPrice }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="seller" label="销售员" width="100" align="center"></el-table-column>
        
        <el-table-column prop="remark" label="备注" show-overflow-tooltip></el-table-column>

        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" class="status-btn" icon="el-icon-sold-out" @click="toggleStatus(scope.row)">
              {{ scope.row.status === '上架中' ? '下架' : '上架' }}
            </el-button>
            <el-button type="text" class="del-btn" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
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

    <!-- 上架商品弹窗 -->
    <el-dialog :title="form.id ? '📝 编辑商品' : '🛒 上架商品'" :visible.sync="dialogFormVisible" width="550px" :close-on-click-modal="false">
      <el-form label-width="100px" size="small" :model="form" :rules="rules" ref="formRef" class="custom-form">
        <el-form-item label="选择商品" prop="inventoryId">
          <el-select v-model="form.inventoryId" placeholder="请选择库存商品" style="width:100%" @change="onInventoryChange" :disabled="!!form.id">
            <el-option 
              v-for="item in inventoryList" 
              :key="item.id" 
              :label="`${item.produce} (库存: ${item.number})`" 
              :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="form.produce" disabled></el-input>
        </el-form-item>
        <el-form-item label="所属仓库">
          <el-input v-model="form.warehouse" disabled></el-input>
        </el-form-item>
        <el-form-item label="出售数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :max="maxQuantity" style="width:100%"></el-input-number>
          <span class="form-tip" v-if="maxQuantity > 0">最大可售: {{ maxQuantity }}</span>
        </el-form-item>
        <el-form-item label="单价(元)" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" style="width:100%"></el-input-number>
        </el-form-item>
        <!-- 销售员字段已移除，系统自动记录 -->
        <el-form-item label="备注">
          <el-input type="textarea" v-model="form.remark" placeholder="可填写商品描述、优惠信息等"></el-input>
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
  name: "OnlineSale",
  data() {
    return {
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      searchProduce: "",
      form: {},
      dialogFormVisible: false,
      multipleSelection: [],
      loading: false,
      inventoryList: [],  // 库存列表
      maxQuantity: 0,     // 最大可售数量
      rules: {
        inventoryId: [{ required: true, message: '请选择商品', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入出售数量', trigger: 'blur' }],
        price: [{ required: true, message: '请输入单价', trigger: 'blur' }]
      }
    }
  },
  computed: {
    // 在售商品数
    onSaleCount() {
      return this.tableData.filter(item => item.status === '上架中').length;
    },
    // 已售罄数
    soldOutCount() {
      return this.tableData.filter(item => item.status === '已售罄').length;
    },
    // 预计总收入
    totalIncome() {
      return this.tableData
        .filter(item => item.status === '上架中')
        .reduce((sum, item) => sum + parseFloat(item.totalPrice || 0), 0);
    },
    apiBaseUrl() {
      return this.request.defaults.baseURL || '';
    }
  },
  created() {
    this.load();
    this.loadInventory();
  },
  methods: {
    load() {
      this.loading = true;
      this.request.get("/onlineSale/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          produce: this.searchProduce,
        }
      }).then(res => {
        this.loading = false;
        if (res && res.data) {
          this.tableData = res.data.records || [];
          this.total = res.data.total || 0;
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    // 加载库存列表
    loadInventory() {
      this.request.get("/inventory/page", {
        params: { pageNum: 1, pageSize: 1000, produce: "" }
      }).then(res => {
        if (res && res.data && res.data.records) {
          this.inventoryList = res.data.records.filter(item => item.number > 0);
        }
      }).catch(() => {});
    },
    // 选择库存商品时
    onInventoryChange(inventoryId) {
      const item = this.inventoryList.find(i => i.id === inventoryId);
      if (item) {
        this.form.produce = item.produce;
        this.form.warehouse = item.warehouse;
        this.maxQuantity = item.number;
        this.form.quantity = 1;
      }
    },
    save() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.request.post("/onlineSale", this.form).then(res => {
          if (res.code === '200') {
            this.$message.success("操作成功");
            this.dialogFormVisible = false;
            this.load();
            this.loadInventory();
          } else {
            this.$message.error(res.msg || "操作失败");
          }
        });
      });
    },
    handleAdd() {
      this.loadInventory();
      this.dialogFormVisible = true;
      this.form = { status: '上架中' };
      this.maxQuantity = 0;
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row));
      const item = this.inventoryList.find(i => i.id === row.inventoryId);
      this.maxQuantity = item ? item.number + row.quantity : row.quantity;
      this.dialogFormVisible = true;
    },
    toggleStatus(row) {
      const newStatus = row.status === '上架中' ? '已下架' : '上架中';
      this.request.put(`/onlineSale/status/${row.id}?status=${newStatus}`).then(res => {
        if (res.code === '200') {
          this.$message.success(`已${newStatus === '上架中' ? '上架' : '下架'}`);
          this.load();
        }
      });
    },
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    delBatch() {
      this.$confirm('确定批量下架选中的商品吗?', '提示', { type: 'warning' }).then(() => {
        let ids = this.multipleSelection.map(v => v.id);
        this.request.post("/onlineSale/del/batch", ids).then(res => {
          if (res.code === '200') {
            this.$message.success("批量下架成功");
            this.load();
          }
        });
      });
    },
    // 删除单个商品
    handleDelete(row) {
      this.$confirm(`确定要删除商品「${row.produce}」吗？删除后将无法恢复！`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.request.delete(`/onlineSale/${row.id}`).then(res => {
          if (res.code === '200') {
            this.$message.success('删除成功');
            this.load();
            this.loadInventory();
          } else {
            this.$message.error(res.msg || '删除失败');
          }
        });
      }).catch(() => {});
    },
    reset() {
      this.searchProduce = "";
      this.load();
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize;
      this.load();
    },
    handleCurrentChange(pageNum) {
      this.pageNum = pageNum;
      this.load();
    },
    getStatusType(status) {
      if (status === '上架中') return 'success';
      if (status === '已售罄') return 'danger';
      return 'info';
    },
    exp() {
      this.request.download("/onlineSale/export", "online-sale.xlsx");
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* KPI卡片 */
.kpi-board { display: flex; gap: 20px; margin-bottom: 20px; }
.kpi-card {
  flex: 1; background: white; padding: 20px; border-radius: 12px;
  display: flex; align-items: center; gap: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
}
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

/* 主内容卡片 */
.main-card {
  background: white; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
}

/* 工具栏 */
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-group { display: flex; gap: 10px; }
.custom-input { width: 240px; }
.btn-group { display: flex; gap: 10px; }

/* 表格 */
.status-btn { color: #e6a23c; }
.status-btn:hover { color: #f0a020; }
.del-btn { color: #f56c6c; }
.del-btn:hover { color: #f78989; }

/* 分页 */
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }

/* 表单提示 */
.form-tip { color: #909399; font-size: 12px; margin-left: 10px; }

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
