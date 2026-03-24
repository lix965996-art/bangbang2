<template>
  <div class="notice-container">
    
    <div class="ai-dashboard">
      <div class="dashboard-left">
        <div class="big-title">农情预警指挥中心</div>
        <div class="sub-text">DeepSeek 语义分析引擎 · 实时监控中</div>
        <div class="pulse-line"></div>
      </div>
      <div class="dashboard-right">
        <div class="metric-box">
          <div class="metric-label">高频词云</div>
          <div class="tags-cloud">
            <el-tag size="mini" type="danger" effect="dark">炭疽病 98%</el-tag>
            <el-tag size="mini" type="warning" effect="dark">低温预警</el-tag>
            <el-tag size="mini" type="success" effect="dark">有机肥</el-tag>
            <el-tag size="mini" color="#626aef" effect="dark" style="color: white">无人机</el-tag>
          </div>
        </div>
        <div class="metric-box">
          <div class="metric-label">知识库容量</div>
          <div class="digital-number">1,204 <span style="font-size: 12px; color: #aaa">条</span></div>
        </div>
      </div>
    </div>

    <el-row :gutter="20" style="margin-top: 20px;">
      
      <el-col :span="16">
        <el-card class="glass-card" shadow="never">
          <div slot="header" class="panel-header">
            <span><i class="el-icon-timer"></i> 农事作业与预警日志</span>
            <el-button style="float: right; padding: 3px 0; color: #409EFF" type="text" icon="el-icon-plus" @click="handleAdd">发布新日志</el-button>
          </div>

          <div class="timeline-wrapper">
            <el-timeline>
              <el-timeline-item
                v-for="(item, index) in tableData"
                :key="item.id"
                :timestamp="item.time"
                placement="top"
                :color="index === 0 ? '#F56C6C' : '#409EFF'" 
                :size="index === 0 ? 'large' : 'normal'"
                :icon="index === 0 ? 'el-icon-message-solid' : 'el-icon-collection-tag'">
                
                <el-card shadow="hover" :class="['item-card', index === 0 ? 'latest-card' : '']">
                  <div class="new-badge" v-if="index === 0">LATEST</div>

                  <div class="card-inner">
                    <div class="item-header">
                      <span class="item-title">{{ item.name }}</span>
                      <div class="tags-row">
                        <el-tag size="mini" effect="dark" :type="index % 2 === 0 ? 'danger' : 'primary'">{{ index % 2 === 0 ? '紧急' : '常规' }}</el-tag>
                        <el-tag size="mini" type="info" effect="plain" style="margin-left: 5px">DeepSeek 已归档</el-tag>
                      </div>
                    </div>

                    <div class="item-body">
                      <div class="text-content">
                         <span :class="{'typing-effect': index === 0}">{{ item.content }}</span>
                      </div>
                      
                      <div class="img-wrapper" v-if="item.img">
                        <el-image 
                          class="log-image"
                          :src="item.img" 
                          :preview-src-list="[item.img]"
                          fit="cover">
                        </el-image>
                      </div>
                    </div>

                    <div class="item-footer">
                      <span class="author-info"><i class="el-icon-user"></i> {{ item.user || 'AI 助手' }}</span>
                      <div class="actions">
                        <el-button type="text" size="mini" icon="el-icon-magic-stick" @click="mockAnalyze">AI 深度分析</el-button>
                        <el-button type="text" size="mini" icon="el-icon-edit" @click="handleEdit(item)">编辑</el-button>
                        <el-popconfirm title="删除这条记录？" @confirm="del(item.id)">
                          <el-button slot="reference" type="text" size="mini" icon="el-icon-delete" style="color: #F56C6C; margin-left: 10px"></el-button>
                        </el-popconfirm>
                      </div>
                    </div>
                  </div>
                </el-card>

              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="glass-card mb-20">
          <div slot="header" class="panel-header">🔍 智库检索</div>
          <el-input placeholder="输入关键词..." v-model="name" class="search-input" clearable @clear="load">
            <el-button slot="append" icon="el-icon-search" @click="load"></el-button>
          </el-input>
        </el-card>

        <el-card class="glass-card ai-suggest-card">
          <div slot="header" class="panel-header" style="color: white">🤖 DeepSeek 今日建议</div>
          <div class="ai-content">
            <p>“根据近 3 天的日志分析，A5 地块湿度持续偏低。建议启用智能灌溉系统，并将阈值调整为 65%。”</p>
            <el-button size="small" round style="background: rgba(255,255,255,0.2); color: white; border: none; margin-top: 10px">一键执行建议</el-button>
          </div>
        </el-card>

        <el-card class="glass-card mt-20" :body-style="{padding: '0px'}">
          <div class="quick-action" @click="handleAdd">
            <i class="el-icon-camera-solid" style="font-size: 32px; color: #409EFF; margin-bottom: 10px;"></i>
            <div>拍照识别上传</div>
            <div style="font-size: 12px; color: #999; margin-top: 5px">自动识别病虫害</div>
          </div>
        </el-card>
      </el-col>

    </el-row>

    <el-dialog :title="form.id ? '编辑日志' : '📝 发布新农情'" :visible.sync="dialogFormVisible" width="50%" center custom-class="custom-dialog">
      <el-form label-width="80px" size="small" :model="form">
        <el-form-item label="标题">
          <el-input v-model="form.name" placeholder="请输入标题"></el-input>
        </el-form-item>
        <el-form-item label="内容">
          <el-input type="textarea" :rows="5" v-model="form.content" placeholder="输入内容后，可点击下方按钮进行 AI 润色"></el-input>
          <el-button type="text" icon="el-icon-cpu" @click="mockAIPolish" style="color: #67C23A">DeepSeek 智能润色</el-button>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="时间">
              <el-date-picker v-model="form.time" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" style="width: 100%"></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布人">
              <el-input v-model="form.user"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片">
          <el-upload :action="apiBaseUrl + '/file/upload'" :headers="uploadHeaders" :on-success="handleImgUploadSuccess" :show-file-list="false">
            <el-button size="small" type="primary" plain>上传现场图</el-button>
            <span v-if="form.img" style="margin-left: 10px; color: #67C23A"><i class="el-icon-check"></i> 已上传</span>
          </el-upload>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">立即发布</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
export default {
  name: "Notice",
  data() {
    return {
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 5,
      name: "",
      form: {},
      dialogFormVisible: false,
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {}
    }
  },
  created() {
    this.load()
  },
  computed: {
    apiBaseUrl() {
      return this.request.defaults.baseURL || ''
    },
    uploadHeaders() {
      try {
        const userStr = localStorage.getItem("user")
        const user = userStr ? JSON.parse(userStr) : null
        return user && user.token ? { token: user.token } : {}
      } catch (e) {
        return {}
      }
    }
  },
  methods: {
    load() {
      this.request.get("/notice/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          name: this.name,
        }
      }).then(res => {
        this.tableData = res.data.records
        this.total = res.data.total
      })
    },
    // 模拟 AI 润色
    mockAIPolish() {
      if (!this.form.content) return this.$message.warning("请先填写一点内容");
      const loading = this.$loading({ text: 'DeepSeek 正在重组语言逻辑...', background: 'rgba(0,0,0,0.7)' });
      setTimeout(() => {
        loading.close();
        this.form.content = "【AI 优化】" + this.form.content + "。 (系统分析：该描述符合真菌感染特征，建议重点关注。)";
        this.$message.success("润色完成");
      }, 800);
    },
    // 模拟 AI 分析
    mockAnalyze() {
      this.$notify({
        title: 'DeepSeek 分析报告',
        message: '该日志关键词提取：[病害] [湿度]。已自动关联历史相似案例 3 起。',
        type: 'success',
        duration: 4000
      });
    },
    save() {
      this.request.post("/notice", this.form).then(res => {
        if (res.code === '200') {
          this.$message.success("操作成功");
          this.dialogFormVisible = false;
          this.load();
        } else {
          this.$message.error("失败");
        }
      })
    },
    handleAdd() {
      this.dialogFormVisible = true;
      this.form = { time: new Date().toISOString().replace('T', ' ').substring(0, 19), user: this.user.username };
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row));
      this.dialogFormVisible = true;
    },
    del(id) {
      this.request.delete("/notice/" + id).then(res => {
        if (res.code === '200') { this.$message.success("已删除"); this.load(); }
      })
    },
    handleImgUploadSuccess(res) {
      this.form.img = res;
      this.$message.success("图片上传成功");
    }
  }
}
</script>

<style scoped>
/* 全局容器：增加一点浅灰底色，突显卡片 */
.notice-container {
  padding: 20px;
  background-color: #f6f8f9;
  min-height: calc(100vh - 60px);
}

/* --- 1. 顶部 AI 仪表盘 (核心视觉点) --- */
.ai-dashboard {
  background: linear-gradient(135deg, #1f2d3d 0%, #324057 100%);
  color: white;
  padding: 25px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
  margin-bottom: 25px;
  position: relative;
  overflow: hidden;
}
/* 装饰背景圆 */
.ai-dashboard::after {
  content: '';
  position: absolute;
  right: -50px;
  top: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255,255,255,0.05);
  border-radius: 50%;
}

.dashboard-left .big-title {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 1px;
}
.dashboard-left .sub-text {
  font-size: 13px;
  opacity: 0.7;
  margin-top: 5px;
}
/* 呼吸灯线条 */
.pulse-line {
  width: 50px;
  height: 3px;
  background: #00e676;
  margin-top: 15px;
  border-radius: 2px;
  animation: pulse-width 2s infinite;
}
@keyframes pulse-width {
  0% { width: 50px; opacity: 1; }
  50% { width: 100px; opacity: 0.5; }
  100% { width: 50px; opacity: 1; }
}

.dashboard-right {
  display: flex;
  gap: 40px;
  z-index: 1; /* 保证在装饰圆上面 */
}
.metric-box {
  text-align: right;
}
.metric-label {
  font-size: 12px;
  opacity: 0.6;
  margin-bottom: 5px;
}
.digital-number {
  font-family: 'Courier New', Courier, monospace;
  font-size: 28px;
  font-weight: bold;
  color: #00e676;
}
.tags-cloud {
  display: flex;
  gap: 5px;
}

/* --- 2. 玻璃拟态卡片 (通用) --- */
.glass-card {
  border: none;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.panel-header {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* --- 3. 时间轴卡片 --- */
.item-card {
  border-radius: 8px;
  position: relative;
  transition: all 0.3s;
  border-left: 4px solid transparent; /* 默认无边框 */
}
.item-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}
/* 最新一条高亮 */
.latest-card {
  border-left: 4px solid #F56C6C; /* 左侧红线 */
  background: #fffcfc; /* 微微泛红的背景 */
}

/* NEW 标签 */
.new-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #F56C6C;
  color: white;
  font-size: 10px;
  padding: 2px 8px;
  border-bottom-left-radius: 8px;
  border-top-right-radius: 8px;
}

.card-inner {
  padding: 5px 0;
}
.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.item-title {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
}

.item-body {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}
.text-content {
  flex: 1;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  padding-right: 15px;
  /* 多行省略 */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}
.img-wrapper .log-image {
  width: 100px;
  height: 75px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.item-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  border-top: 1px dashed #eee;
  padding-top: 10px;
}

/* --- 4. 右侧功能区 --- */
.ai-suggest-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
}
.ai-content p {
  font-size: 14px;
  line-height: 1.5;
  opacity: 0.9;
}

.quick-action {
  padding: 30px;
  text-align: center;
  cursor: pointer;
  background: #fff;
  transition: all 0.3s;
}
.quick-action:hover {
  background: #f0f9eb;
}

/* 间距工具类 */
.mb-20 { margin-bottom: 20px; }
.mt-20 { margin-top: 20px; }
</style>
