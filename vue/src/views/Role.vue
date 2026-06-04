<template>
  <div class="role-page">
    <section class="role-header">
      <div>
        <h2>角色管理</h2>
        <p>维护系统角色，并为不同角色分配可访问的菜单权限。</p>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增角色</el-button>
    </section>

    <section class="role-panel">
      <div class="role-toolbar">
        <el-input
          v-model="name"
          clearable
          prefix-icon="el-icon-search"
          placeholder="搜索角色名称"
          @clear="reloadFromFirstPage"
          @keyup.enter="reloadFromFirstPage"
        />
        <div class="role-toolbar__actions">
          <el-button type="primary" icon="el-icon-search" @click="reloadFromFirstPage">查询</el-button>
          <el-button plain icon="el-icon-refresh" @click="reset">重置</el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="角色名称" min-width="160" />
        <el-table-column prop="flag" label="权限标识" min-width="160">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.flag || '未设置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link icon="el-icon-menu" @click="openMenuDialog(row)">菜单权限</el-button>
            <el-button link icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link class="role-delete" icon="el-icon-delete" @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="role-pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        />
      </div>
    </section>

    <el-dialog
      :title="form.id ? '编辑角色' : '新增角色'"
      v-model="dialogFormVisible"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form ref="roleFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：管理员" />
        </el-form-item>
        <el-form-item label="权限标识" prop="flag">
          <el-input v-model="form.flag" placeholder="例如：ROLE_ADMIN" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="角色职责说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      :title="currentRole ? `${currentRole.name} - 菜单权限` : '菜单权限'"
      v-model="menuDialogVisible"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-tree
        ref="menuTree"
        :data="menuTree"
        :props="treeProps"
        node-key="id"
        show-checkbox
        default-expand-all
        check-strictly
        empty-text="暂无菜单数据"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMenu" @click="saveRoleMenu">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
const success = (res) => res && (res.code === '200' || res.code === 200)

export default {
  name: 'Role',
  data() {
    return {
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      name: '',
      form: {},
      loading: false,
      saving: false,
      dialogFormVisible: false,
      menuDialogVisible: false,
      savingMenu: false,
      currentRole: null,
      menuTree: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      rules: {
        name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        flag: [{ required: true, message: '请输入权限标识', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.load()
    this.loadMenus()
  },
  methods: {
    load() {
      this.loading = true
      this.request.get('/role/page', {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          name: this.name || ''
        }
      }).then(res => {
        if (success(res) && res.data) {
          this.tableData = res.data.records || []
          this.total = res.data.total || 0
        } else {
          this.tableData = []
          this.total = 0
          this.$message.error((res && res.msg) || '角色列表加载失败')
        }
      }).catch(() => {
        this.tableData = []
        this.total = 0
        this.$message.error('角色列表加载失败，请检查网络')
      }).finally(() => {
        this.loading = false
      })
    },
    loadMenus() {
      this.request.get('/menu', { params: { name: '' } }).then(res => {
        this.menuTree = success(res) && Array.isArray(res.data) ? res.data : []
      }).catch(() => {
        this.menuTree = []
      })
    },
    reloadFromFirstPage() {
      this.pageNum = 1
      this.load()
    },
    reset() {
      this.name = ''
      this.reloadFromFirstPage()
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize
      this.pageNum = 1
      this.load()
    },
    handleCurrentChange(pageNum) {
      this.pageNum = pageNum
      this.load()
    },
    handleAdd() {
      this.form = { name: '', flag: '', description: '' }
      this.dialogFormVisible = true
      this.$nextTick(() => {
        if (this.$refs.roleFormRef) this.$refs.roleFormRef.clearValidate()
      })
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      this.dialogFormVisible = true
      this.$nextTick(() => {
        if (this.$refs.roleFormRef) this.$refs.roleFormRef.clearValidate()
      })
    },
    save() {
      this.$refs.roleFormRef.validate(valid => {
        if (!valid) return
        this.saving = true
        this.request.post('/role', this.form).then(res => {
          if (success(res)) {
            this.$message.success('角色保存成功')
            this.dialogFormVisible = false
            this.load()
          } else {
            this.$message.error((res && res.msg) || '角色保存失败')
          }
        }).catch(() => {
          this.$message.error('角色保存失败，请检查网络')
        }).finally(() => {
          this.saving = false
        })
      })
    },
    del(id) {
      this.$confirm('确认删除该角色？删除后对应权限配置也将不可用。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.request.delete(`/role/${id}`).then(res => {
          if (success(res)) {
            this.$message.success('角色已删除')
            this.load()
          } else {
            this.$message.error((res && res.msg) || '删除失败')
          }
        }).catch(() => {
          this.$message.error('删除失败，请检查网络')
        })
      }).catch(() => {})
    },
    openMenuDialog(row) {
      this.currentRole = row
      this.menuDialogVisible = true
      if (!this.menuTree.length) {
        this.loadMenus()
      }
      this.request.get(`/role/roleMenu/${row.id}`).then(res => {
        const checkedIds = success(res) && Array.isArray(res.data) ? res.data : []
        this.$nextTick(() => {
          if (this.$refs.menuTree) {
            this.$refs.menuTree.setCheckedKeys(checkedIds)
          }
        })
      }).catch(() => {
        this.$message.error('菜单权限加载失败')
      })
    },
    saveRoleMenu() {
      if (!this.currentRole) return
      const tree = this.$refs.menuTree
      const checked = tree ? tree.getCheckedKeys() : []
      const halfChecked = tree && tree.getHalfCheckedKeys ? tree.getHalfCheckedKeys() : []
      const menuIds = Array.from(new Set([...checked, ...halfChecked]))

      this.savingMenu = true
      this.request.post(`/role/roleMenu/${this.currentRole.id}`, menuIds).then(res => {
        if (success(res)) {
          this.$message.success('菜单权限已保存')
          this.menuDialogVisible = false
        } else {
          this.$message.error((res && res.msg) || '菜单权限保存失败')
        }
      }).catch(() => {
        this.$message.error('菜单权限保存失败，请检查网络')
      }).finally(() => {
        this.savingMenu = false
      })
    }
  }
}
</script>

<style scoped>
.role-page {
  min-height: calc(100vh - 90px);
  padding: 22px;
  background: #f6f8fb;
}

.role-header,
.role-panel {
  border: 1px solid #dbe4ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

.role-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
  margin-bottom: 16px;
}

.role-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
}

.role-header p {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 13px;
}

.role-panel {
  padding: 18px;
}

.role-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.role-toolbar :deep(.el-input) {
  max-width: 320px;
}

.role-toolbar__actions {
  display: flex;
  gap: 10px;
}

.role-delete {
  color: #dc2626;
}

.role-pagination {
  padding: 18px 0 4px;
  text-align: center;
}

@media (max-width: 720px) {
  .role-page {
    padding: 14px;
  }

  .role-header,
  .role-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .role-toolbar :deep(.el-input) {
    max-width: none;
  }

  .role-toolbar__actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}
</style>
