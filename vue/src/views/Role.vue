<template>
  <div class="role-universe">
    <div class="control-deck">
      <div class="deck-left">
        <h2 class="module-title">
          <span class="highlight">R</span>ole Center
          <small>角色权限控制中枢</small>
        </h2>
      </div>
      <div class="deck-right">
        <div class="search-capsule">
          <i class="el-icon-search"></i>
          <input 
            v-model="name" 
            placeholder="搜索角色标识或名称..." 
            @keyup.enter="load"
            type="text"
          />
        </div>
        <el-button type="primary" icon="el-icon-plus" round class="glow-btn" @click="handleAdd">新建角色</el-button>
        <el-button type="text" icon="el-icon-refresh" class="refresh-btn" @click="load"></el-button>
      </div>
    </div>

    <div class="matrix-container" v-loading="loading">
      <div class="role-grid">
        
        <div class="role-card add-card" @click="handleAdd">
          <div class="dashed-border">
            <i class="el-icon-plus"></i>
            <p>Create New Role</p>
          </div>
        </div>

        <div 
          class="role-card" 
          v-for="item in tableData" 
          :key="item.id"
          :class="getCardTheme(item.flag)"
        >
          <div class="card-header">
            <div class="role-avatar">
              <span>{{ item.name.substring(0,1) }}</span>
            </div>
            <div class="role-badges">
               <el-tooltip content="系统唯一标识" placement="top">
                 <span class="code-badge">{{ item.flag }}</span>
               </el-tooltip>
            </div>
            <el-dropdown trigger="click" class="card-more" @command="handleCommand($event, item)">
              <i class="el-icon-more"></i>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" icon="el-icon-edit">编辑信息</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="el-icon-delete" style="color: #F56C6C">删除角色</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div class="card-body">
            <h3 class="role-name">{{ item.name }}</h3>
            <p class="role-desc">{{ item.description || '暂无描述信息，请完善该角色的职能定义。' }}</p>
          </div>

          <div class="card-visual">
            <div class="stat-row">
              <span class="label">权限覆盖率</span>
              <span class="value">{{ getFakePermissionRate(item.id) }}%</span>
            </div>
            <el-progress 
              :percentage="getFakePermissionRate(item.id)" 
              :show-text="false" 
              :stroke-width="6" 
              :color="getProgressColor(item.flag)">
            </el-progress>
          </div>

          <div class="card-footer">
            <el-button 
              class="action-btn" 
              plain 
              size="small" 
              icon="el-icon-s-operation" 
              @click="selectMenu(item)"
            >
              配置菜单权限
            </el-button>
          </div>
          
          <div class="bg-pattern"></div>
        </div>
      </div>
      
      <div v-if="tableData.length === 0 && !loading" class="empty-placeholder">
        <img src="https://cdn-icons-png.flaticon.com/512/7486/7486754.png" width="120" />
        <p>未找到相关角色数据</p>
      </div>
    </div>

    <div class="floating-pagination">
       <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pageNum"
          :page-sizes="[8, 12, 24, 48]"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total">
      </el-pagination>
    </div>

    <el-dialog :title="form.id ? '编辑角色' : '创建新角色'" v-model="dialogFormVisible" width="400px" custom-class="glass-dialog">
      <el-form label-position="top" size="medium">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="请输入名称"></el-input>
        </el-form-item>
        <el-form-item label="唯一标识">
          <el-input v-model="form.flag" placeholder="例如：ROLE_USER"></el-input>
        </el-form-item>
        <el-form-item label="职能描述">
          <el-input type="textarea" v-model="form.description" rows="3"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存提交</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer
      title="菜单权限配置"
      v-model="menuDialogVis"
      direction="rtl"
      size="400px">
      <div style="padding: 20px; height: calc(100% - 60px); overflow: auto;">
         <el-tree
            :props="props"
            :data="menuData"
            show-checkbox
            node-key="id"
            ref="tree"
            :default-expanded-keys="expends"
            :default-checked-keys="checks">
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span><i :class="data.icon"></i> {{ data.name }}</span>
              </span>
            </template>
         </el-tree>
      </div>
      <div class="drawer-actions">
        <el-button @click="menuDialogVis = false" style="width: 45%">取消</el-button>
        <el-button type="primary" @click="saveRoleMenu" style="width: 45%">确认授权</el-button>
      </div>
    </el-drawer>

  </div>
</template>

<script>
export default {
  name: "RoleMatrix",
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 11, // 卡片布局每页显示11个（+1个新增按钮=12格）
      name: "",
      form: {},
      dialogFormVisible: false,
      menuDialogVis: false,
      menuData: [],
      props: { label: 'name' },
      expends: [],
      checks: [],
      roleId: 0,
      roleFlag: '',
      ids: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true;
      this.request.get("/role/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          name: this.name,
        }
      }).then(res => {
        this.tableData = res.data.records
        this.total = res.data.total
        this.loading = false;
      })
      this.request.get("/menu/ids").then(r => { this.ids = r.data })
    },
    // 模拟一个随机的权限覆盖率，增加界面动态感
    getFakePermissionRate(id) {
       return (id * 13) % 40 + 60; // 生成 60-100 之间的伪随机数
    },
    // 根据角色Flag返回主题色类名
    getCardTheme(flag) {
      if(flag === 'ROLE_ADMIN') return 'theme-admin';
      if(flag && flag.includes('MANAGER')) return 'theme-manager';
      return 'theme-user';
    },
    getProgressColor(flag) {
      if(flag === 'ROLE_ADMIN') return '#f56c6c'; // 红
      if(flag && flag.includes('MANAGER')) return '#e6a23c'; // 橙
      return '#409EFF'; // 蓝
    },
    handleCommand(command, row) {
      if(command === 'edit') this.handleEdit(row);
      if(command === 'delete') this.del(row.id);
    },
    save() {
      this.request.post("/role", this.form).then(res => {
        if (res.code === '200') {
          this.$message.success("保存成功")
          this.dialogFormVisible = false
          this.load()
        } else {
          this.$message.error("保存失败")
        }
      })
    },
    saveRoleMenu() {
      this.request.post("/role/roleMenu/" + this.roleId, this.$refs.tree.getCheckedKeys()).then(res => {
        if (res.code === '200') {
          this.$notify({title:'成功', message:'权限分配已生效', type:'success'});
          this.menuDialogVis = false
          if (this.roleFlag === 'ROLE_ADMIN') {
            this.$store.commit("logout")
          }
        } else {
          this.$message.error(res.msg)
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
      this.$confirm('此操作将永久删除该角色, 是否继续?', '警示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.request.delete("/role/" + id).then(res => {
          if (res.code === '200') {
            this.$message.success("删除成功")
            this.load()
          } else {
            this.$message.error("删除失败")
          }
        })
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
    async selectMenu(role) {
      this.roleId = role.id
      this.roleFlag = role.flag
      this.request.get("/menu").then(res => {
        this.menuData = res.data
        this.expends = this.menuData.map(v => v.id)
      })
      this.menuDialogVis = true
      this.request.get("/role/roleMenu/" + this.roleId).then(res => {
        this.checks = res.data
        this.$refs.tree.setCheckedKeys(res.data)
        this.ids.forEach(id => {
          if (!this.checks.includes(id)) {
            this.$refs.tree.setChecked(id, false)
          }
        })
      })
    },
  }
}
</script>

<style lang="scss" scoped>
/* 核心变量定义 */
$bg-main: #f3f5f8;
$card-bg: #ffffff;
$primary: #4e54c8;
$text-main: #2c3e50;
$text-sub: #7f8c8d;
$shadow-hover: 0 15px 30px rgba(0,0,0,0.1);

.role-universe {
  padding: 20px;
  background-color: $bg-main;
  min-height: calc(100vh - 80px);
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', sans-serif;
  
  /* 顶部导航 */
  .control-deck {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding: 0 10px;
    
    .module-title {
      font-size: 24px;
      font-weight: 700;
      color: $text-main;
      display: flex;
      align-items: center;
      .highlight { color: $primary; font-size: 32px; margin-right: 2px;}
      small { font-size: 14px; color: #999; margin-left: 15px; font-weight: 400; }
    }
    
    .deck-right {
      display: flex;
      align-items: center;
      gap: 15px;
      
      .search-capsule {
        background: #fff;
        padding: 8px 15px;
        border-radius: 20px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        display: flex;
        align-items: center;
        width: 260px;
        transition: all 0.3s;
        
        i { color: #999; margin-right: 8px; }
        input { border: none; outline: none; width: 100%; color: #666; font-size: 14px;}
        &:focus-within { width: 320px; box-shadow: 0 4px 15px rgba(78, 84, 200, 0.15); }
      }
      
      .glow-btn {
        background: linear-gradient(135deg, #4e54c8, #8f94fb);
        border: none;
        box-shadow: 0 4px 15px rgba(78, 84, 200, 0.3);
        transition: transform 0.2s;
        &:hover { transform: translateY(-2px); }
      }
      
      .refresh-btn { font-size: 20px; color: #999; &:hover { color: $primary; transform: rotate(180deg); transition: 0.5s;} }
    }
  }

  /* 卡片矩阵 */
  .matrix-container {
    .role-grid {
      display: grid;
      /* 自适应列宽，最小300px，自动填满 */
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 25px;
      
      .role-card {
        background: $card-bg;
        border-radius: 16px;
        padding: 24px;
        position: relative;
        overflow: hidden;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        border: 1px solid rgba(255,255,255,0.6);
        box-shadow: 0 2px 10px rgba(0,0,0,0.03);
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        min-height: 220px;
        cursor: default;

        &:hover {
          transform: translateY(-8px);
          box-shadow: $shadow-hover;
          .card-footer .action-btn { background: #f0f2f5; color: $primary; }
        }
        
        /* 头部 */
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 15px;
          z-index: 2;
          
          .role-avatar {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: #f0f2f5;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            font-weight: bold;
            color: #666;
            box-shadow: inset 0 0 10px rgba(0,0,0,0.05);
          }
          
          .role-badges {
            flex: 1;
            margin-left: 15px;
            .code-badge {
              font-family: 'Consolas', monospace;
              background: #f4f4f5;
              color: #909399;
              padding: 2px 8px;
              border-radius: 4px;
              font-size: 12px;
            }
          }
          
          .card-more {
            cursor: pointer;
            padding: 5px;
            color: #ccc;
            &:hover { color: #666; }
          }
        }
        
        /* 中部 */
        .card-body {
          flex: 1;
          z-index: 2;
          .role-name { font-size: 18px; margin: 0 0 8px 0; color: #333; }
          .role-desc { font-size: 13px; color: #888; line-height: 1.5; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 40px;}
        }
        
        /* 视觉统计 */
        .card-visual {
          margin: 20px 0;
          z-index: 2;
          .stat-row {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #aaa;
            margin-bottom: 5px;
          }
        }
        
        /* 底部 */
        .card-footer {
          z-index: 2;
          .action-btn {
            width: 100%;
            border-radius: 8px;
            border: 1px solid #ebeef5;
            transition: all 0.3s;
          }
        }
        
        /* 背景纹理装饰 */
        .bg-pattern {
          position: absolute;
          top: -20px;
          right: -20px;
          width: 150px;
          height: 150px;
          border-radius: 50%;
          opacity: 0.05;
          z-index: 1;
        }

        /* ---------------- 主题色定制 ---------------- */
        /* Admin 主题 */
        &.theme-admin {
          border-top: 4px solid #F56C6C;
          .role-avatar { color: #F56C6C; background: #fef0f0; }
          .bg-pattern { background: radial-gradient(circle, #F56C6C, transparent); }
        }
        /* Manager 主题 */
        &.theme-manager {
          border-top: 4px solid #E6A23C;
          .role-avatar { color: #E6A23C; background: #fdf6ec; }
          .bg-pattern { background: radial-gradient(circle, #E6A23C, transparent); }
        }
        /* User 主题 (Default) */
        &.theme-user {
          border-top: 4px solid #409EFF;
          .role-avatar { color: #409EFF; background: #ecf5ff; }
          .bg-pattern { background: radial-gradient(circle, #409EFF, transparent); }
        }
      }
      
      /* 新增按钮卡片 特效 */
      .add-card {
        background: transparent;
        border: none;
        box-shadow: none;
        padding: 0;
        
        .dashed-border {
          width: 100%;
          height: 100%;
          border: 2px dashed #dcdfe6;
          border-radius: 16px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: #909399;
          transition: all 0.3s;
          background: rgba(255,255,255,0.5);
          cursor: pointer;
          
          i { font-size: 32px; margin-bottom: 10px; }
          p { font-weight: 500; }
          
          &:hover {
            border-color: $primary;
            color: $primary;
            background: rgba(78, 84, 200, 0.05);
          }
        }
      }
    }
    
    .empty-placeholder {
      text-align: center;
      padding: 50px;
      color: #909399;
    }
  }

  /* 悬浮分页 */
  .floating-pagination {
    position: fixed;
    bottom: 20px;
    right: 30px;
    background: #fff;
    padding: 10px 20px;
    border-radius: 30px;
    box-shadow: 0 5px 20px rgba(0,0,0,0.08);
    z-index: 100;
  }
  
  /* 抽屉底部按钮区 */
  .drawer-actions {
    position: absolute;
    bottom: 0;
    width: 100%;
    border-top: 1px solid #e8e8e8;
    padding: 15px 20px;
    background: #fff;
    display: flex;
    justify-content: space-between;
  }
}
</style>
