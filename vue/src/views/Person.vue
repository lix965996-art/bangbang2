<template>
  <div class="person-page">
    <section class="person-panel profile-panel">
      <div class="panel-heading">
        <div>
          <p class="panel-eyebrow">Profile</p>
          <h2>个人资料</h2>
        </div>
      </div>

      <div class="profile-summary">
        <el-upload
          class="avatar-uploader"
          :action="apiBaseUrl + '/file/upload'"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
        >
          <div class="avatar-frame">
            <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
            <span class="avatar-action">更换头像</span>
          </div>
        </el-upload>

        <div class="uid-card">
          <span class="uid-label">帮帮农 ID</span>
          <div class="uid-row">
            <span class="uid-value">{{ form.uid || '暂无' }}</span>
            <el-button
              v-if="form.uid"
              text
              size="small"
              icon="el-icon-document-copy"
              @click="copyUid"
            >复制</el-button>
            <el-tooltip content="把这个 ID 发给好友，对方可以通过添加好友搜索到你" placement="right">
              <i class="el-icon-question uid-tip"></i>
            </el-tooltip>
          </div>
        </div>
      </div>

      <el-form class="ios-form" label-position="top" size="large">
        <div class="form-grid">
          <el-form-item label="用户名">
            <el-input v-model="form.username" disabled autocomplete="off"></el-input>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" autocomplete="off"></el-input>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" autocomplete="off"></el-input>
          </el-form-item>
          <el-form-item label="电话">
            <el-input v-model="form.phone" autocomplete="off"></el-input>
          </el-form-item>
          <el-form-item class="span-full" label="地址">
            <el-input type="textarea" v-model="form.address" autocomplete="off" :rows="3"></el-input>
          </el-form-item>
        </div>
        <div class="form-actions">
          <el-button type="primary" @click="save">保存资料</el-button>
        </div>
      </el-form>
    </section>

    <section class="person-panel ai-panel">
      <div class="panel-heading ai-heading">
        <div class="heading-title">
          <i class="el-icon-cpu"></i>
          <div>
            <p class="panel-eyebrow">AI Settings</p>
            <h2>AI 模型配置</h2>
          </div>
        </div>
        <el-tag v-if="aiCfg.provider" size="large" type="success" effect="plain">
          {{ providerLabel(aiCfg.provider) }}
        </el-tag>
      </div>

      <el-form class="ios-form ai-form" :model="aiCfg" label-position="top" size="large">
        <div class="form-grid">
          <el-form-item class="span-full" label="提供商">
            <el-select v-model="aiCfg.provider" placeholder="选择提供商" @change="onProviderChange">
              <el-option label="通义千问 (Qwen)" value="qwen"></el-option>
              <el-option label="DeepSeek" value="deepseek"></el-option>
              <el-option label="智谱 GLM" value="glm"></el-option>
              <el-option label="MiniMax" value="minimax"></el-option>
              <el-option label="OpenAI" value="openai"></el-option>
              <el-option label="自定义" value="custom"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="Base URL">
            <el-input
              v-model="aiCfg.baseUrl"
              placeholder="https://api.xxx.com/v1"
              :disabled="aiCfg.provider !== 'custom'"
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item label="API Key">
            <el-input
              v-model="aiCfg.apiKey"
              placeholder="粘贴你的 API Key"
              show-password
              clearable
              autocomplete="new-password"
            ></el-input>
          </el-form-item>

          <el-form-item class="span-full" label="模型">
            <el-input v-model="aiCfg.modelName" placeholder="如 qwen-max / deepseek-chat"></el-input>
          </el-form-item>
        </div>

        <div class="section-label">
          <span>对话模型</span>
          <em>可选，留空则复用主模型</em>
        </div>

        <div class="form-grid">
          <el-form-item label="对话模型">
            <el-input
              v-model="aiCfg.chatModelName"
              placeholder="如 qwen-turbo / deepseek-chat"
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item label="对话 URL">
            <el-input v-model="aiCfg.chatBaseUrl" placeholder="留空则复用主模型 Base URL" clearable></el-input>
          </el-form-item>

          <el-form-item class="span-full" label="对话 Key">
            <el-input
              v-model="aiCfg.chatApiKey"
              placeholder="留空则复用主模型 API Key"
              show-password
              clearable
              autocomplete="new-password"
            ></el-input>
          </el-form-item>
        </div>

        <div class="form-actions">
          <el-button type="primary" :loading="aiSaving" @click="saveAiConfig">保存配置</el-button>
          <el-button :loading="aiTesting" @click="testAiConnection">
            <i class="el-icon-connection"></i> 测试主模型
          </el-button>
          <el-button :loading="aiTestingChat" @click="testChatConnection">
            <i class="el-icon-chat-line-round"></i> 测试对话模型
          </el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script>
export default {
  name: "Person",
  data() {
    let user = {};
    try {
      const userStr = localStorage.getItem("user");
      user = userStr ? JSON.parse(userStr) : {};
    } catch (e) {
      console.error('解析用户信息失败:', e);
      user = {};
    }
    
    return {
      form: {},
      user: user,
      // ── AI 配置 ──
      aiCfg: {
        provider: 'qwen',
        baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        apiKey: '',
        modelName: 'qwen-max',
        chatModelName: '',
        chatBaseUrl: '',
        chatApiKey: '',
      },
      aiPresets: {},
      aiSaving: false,
      aiTesting: false,
      aiTestingChat: false,
    }
  },
  created() {
    this.getUser().then(res => { this.form = res })
    this.loadAiConfig()
    this.loadAiPresets()
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
    async getUser() {
      if (!this.user.username) {
        this.$message.error("用户信息异常，请重新登录")
        return {}
      }
      try {
        return (await this.request.get("/user/username/" + this.user.username)).data
      } catch (e) {
        this.$message.error("获取用户信息失败")
        return {}
      }
    },
    save() {
      this.request.post("/user", this.form).then(res => {
        if (res.code === '200') {
          this.$message.success("保存成功")

          // 触发父级更新User的方法
          this.$emit("refreshUser")

          // 更新浏览器存储的用户信息
          this.getUser().then(res => {
            const stored = localStorage.getItem("user");
            const currentUser = stored ? JSON.parse(stored) : {};
            res.token = currentUser.token || '';
            localStorage.setItem("user", JSON.stringify(res))
          })

        } else {
          this.$message.error("保存失败")
        }
      }).catch(() => { this.$message.error('保存失败，请检查网络') })
    },
    copyUid() {
      const uid = this.form.uid
      if (!uid) return
      if (navigator.clipboard) {
        navigator.clipboard.writeText(uid).then(() => {
          this.$message.success('帮帮农ID已复制：' + uid)
        })
      } else {
        // 兜底方案
        const el = document.createElement('textarea')
        el.value = uid
        document.body.appendChild(el)
        el.select()
        document.execCommand('copy')
        document.body.removeChild(el)
        this.$message.success('帮帮农ID已复制：' + uid)
      }
    },
    handleAvatarSuccess(res) {
      const url = typeof res === 'string' ? res : (res && res.data)
      if (url && typeof url === 'string') {
        this.form.avatarUrl = url
        this.$message.success("上传成功")
        return
      }
      this.$message.error((res && res.msg) || "上传失败")
    },

    // ── AI 配置方法 ──
    async loadAiPresets() {
      try {
        const res = await this.request.get('/ai-config/presets')
        if (res.code === '200') this.aiPresets = res.data
      } catch (e) { /* 忽略 */ }
    },
    async loadAiConfig() {
      try {
        const res = await this.request.get('/ai-config')
        if (res.code === '200' && res.data) {
          this.aiCfg = Object.assign(this.aiCfg, res.data)
        }
      } catch (e) { /* 忽略 */ }
    },
    onProviderChange(provider) {
      if (provider === 'custom') return
      const preset = this.aiPresets[provider]
      if (preset) {
        this.aiCfg.baseUrl   = preset.baseUrl
        this.aiCfg.modelName = preset.model
      }
    },
    async saveAiConfig() {
      // 只在主模型 Key 完全为空时拦截；含 **** 的脱敏 Key 后端会自动还原，无需重填
      if (!this.aiCfg.apiKey) {
        this.$message.warning('请输入 API Key')
        return
      }
      this.aiSaving = true
      try {
        const res = await this.request.post('/ai-config', this.aiCfg)
        if (res.code === '200') {
          this.$message.success('AI 配置已保存 ✅')
          this.aiCfg = Object.assign(this.aiCfg, res.data)
        } else {
          this.$message.error(res.msg || '保存失败')
        }
      } catch (e) {
        this.$message.error('保存失败：' + e.message)
      } finally {
        this.aiSaving = false
      }
    },
    async testAiConnection() {
      if (!this.aiCfg.apiKey) {
        this.$message.warning('请先填写 API Key 并保存，再测试')
        return
      }
      this.aiTesting = true
      try {
        const res = await this.request.post('/ai-config/test', this.aiCfg)
        if (res.code === '200') {
          this.$message.success(res.data || '连接成功 ✅')
        } else {
          this.$message.error('连接失败：' + (res.msg || res.data))
        }
      } catch (e) {
        this.$message.error('测试异常：' + e.message)
      } finally {
        this.aiTesting = false
      }
    },
    async testChatConnection() {
      // 对话模型 Key 可能为空（此时复用主模型），用主 Key 兜底判断
      const effectiveKey = this.aiCfg.chatApiKey || this.aiCfg.apiKey
      if (!effectiveKey) {
        this.$message.warning('请先填写 API Key 并保存，再测试')
        return
      }
      // 构造测试用配置：用对话模型字段，留空时复用主模型字段
      const testCfg = {
        provider:  this.aiCfg.provider,
        baseUrl:   this.aiCfg.chatBaseUrl  || this.aiCfg.baseUrl,
        apiKey:    this.aiCfg.chatApiKey   || this.aiCfg.apiKey,
        modelName: this.aiCfg.chatModelName || this.aiCfg.modelName,
      }
      this.aiTestingChat = true
      try {
        const res = await this.request.post('/ai-config/test', testCfg)
        if (res.code === '200') {
          this.$message.success((res.data || '连接成功 ✅') + '（对话模型）')
        } else {
          this.$message.error('对话模型连接失败：' + (res.msg || res.data))
        }
      } catch (e) {
        this.$message.error('测试异常：' + e.message)
      } finally {
        this.aiTestingChat = false
      }
    },
    providerLabel(p) {
      const map = { qwen:'通义千问', deepseek:'DeepSeek', glm:'智谱GLM', minimax:'MiniMax', openai:'OpenAI', custom:'自定义' }
      return map[p] || p
    },
  }
}
</script>

<style scoped>
.person-page {
  width: 100%;
  min-height: calc(100vh - 70px);
  padding: 28px 36px 48px;
  display: grid;
  grid-template-columns: minmax(420px, 0.88fr) minmax(600px, 1.12fr);
  gap: 28px;
  align-items: start;
  background:
    radial-gradient(circle at 18% 0%, rgba(46, 125, 50, 0.08), transparent 26%),
    radial-gradient(circle at 78% 8%, rgba(20, 184, 166, 0.07), transparent 28%),
    linear-gradient(180deg, #f7faf8 0%, #f3f7f5 100%);
}

.person-panel {
  width: 100%;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(214, 226, 218, 0.72);
  border-radius: 20px;
  box-shadow: 0 16px 42px rgba(33, 57, 44, 0.08);
  overflow: hidden;
}

.panel-heading {
  min-height: 78px;
  padding: 24px 32px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-eyebrow {
  margin: 0 0 6px;
  color: #7f8d82;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.panel-heading h2 {
  margin: 0;
  color: #17271d;
  font-size: 22px;
  font-weight: 750;
  line-height: 1.25;
}

.heading-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.heading-title > i {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #168a55;
  background: #ecf8f0;
  border-radius: 10px;
  font-size: 18px;
}

.profile-summary {
  padding: 28px 32px 18px;
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
}

.avatar-uploader {
  width: 140px;
}

.avatar-uploader :deep(.el-upload) {
  width: 140px;
  height: 140px;
  border: 0;
  border-radius: 18px;
  cursor: pointer;
  overflow: hidden;
}

.avatar-frame {
  position: relative;
  width: 140px;
  height: 140px;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(180deg, #f6f8f6, #eef4ef);
  box-shadow: 0 14px 28px rgba(33, 57, 44, 0.1);
}

.avatar-frame:hover .avatar-action {
  opacity: 1;
  transform: translateY(0);
}

.avatar-uploader-icon {
  width: 140px;
  height: 140px;
  color: #9aa5b5;
  font-size: 28px;
}

.avatar-uploader-icon :deep(svg) {
  width: 28px;
  height: 28px;
}

.avatar-action {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(17, 24, 39, 0.72);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  opacity: 0;
  transform: translateY(6px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.avatar {
  width: 140px;
  height: 140px;
  object-fit: cover;
  display: block;
}

.uid-card {
  min-height: 84px;
  padding: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: transparent;
  border: 0;
  border-radius: 0;
}

.uid-label {
  color: #657066;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 10px;
  letter-spacing: 0.02em;
}

.uid-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.uid-value {
  color: #13a164;
  font-size: 28px;
  font-weight: 750;
  letter-spacing: 1px;
  line-height: 1;
}

.uid-tip {
  color: #9aa79c;
  cursor: pointer;
  font-size: 15px;
}

.ios-form {
  padding: 8px 32px 32px;
}

.ai-form {
  padding-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 20px;
  row-gap: 18px;
}

.span-full {
  grid-column: 1 / -1;
}

.section-label {
  margin: 10px 0 16px;
  padding-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-label span {
  color: #17271d;
  font-size: 15px;
  font-weight: 700;
}

.section-label em {
  color: #8b978e;
  font-size: 13px;
  font-style: normal;
}

.form-actions {
  padding-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.person-page :deep(.el-form-item) {
  margin-bottom: 0;
}

.person-page :deep(.el-form-item__label) {
  margin-bottom: 8px;
  padding: 0;
  color: #37483d !important;
  font-size: 13px;
  font-weight: 700 !important;
  line-height: 1.2;
}

.person-page :deep(.el-input),
.person-page :deep(.el-select),
.person-page :deep(.el-textarea) {
  width: 100%;
}

.person-page :deep(.el-input__wrapper),
.person-page :deep(.el-select .el-input__wrapper) {
  min-height: 46px;
  padding: 0 15px;
  background: #f6f8f6 !important;
  border: 0 !important;
  border-radius: 12px !important;
  box-shadow: inset 0 0 0 1px transparent !important;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.person-page :deep(.el-input__wrapper:hover),
.person-page :deep(.el-select .el-input__wrapper:hover) {
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #dce7df !important;
}

.person-page :deep(.el-input__wrapper.is-focus),
.person-page :deep(.el-select .el-input__wrapper.is-focus) {
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #22b86f, 0 0 0 4px rgba(34, 184, 111, 0.1) !important;
}

.person-page :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #f0f4f1 !important;
  box-shadow: none !important;
}

.person-page :deep(.el-input__inner) {
  height: 100%;
  color: #1f2d24;
  font-size: 15px;
  font-weight: 500;
  background: transparent !important;
  border: 0 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
}

.person-page :deep(.el-input__inner:focus) {
  border: 0 !important;
  box-shadow: none !important;
}

.person-page :deep(.el-input__inner::placeholder),
.person-page :deep(.el-textarea__inner::placeholder) {
  color: #9ca89e;
}

.person-page :deep(.el-textarea__inner) {
  min-height: 92px !important;
  padding: 12px 14px;
  color: #1f2d24;
  background: #f6f8f6 !important;
  border: 0 !important;
  border-radius: 12px !important;
  box-shadow: none !important;
  resize: vertical;
  font-size: 15px;
  line-height: 1.55;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.person-page :deep(.el-textarea__inner:hover) {
  background: #ffffff !important;
  box-shadow: inset 0 0 0 1px #dce7df !important;
}

.person-page :deep(.el-textarea__inner:focus) {
  background: #ffffff !important;
  border: 0 !important;
  box-shadow: inset 0 0 0 1px #22b86f, 0 0 0 4px rgba(34, 184, 111, 0.1) !important;
}

.person-page :deep(.el-button) {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 10px !important;
  font-weight: 650;
  transform: none !important;
  transition: background-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease !important;
}

.person-page :deep(.el-button--primary) {
  background: #22a765 !important;
  border: 0 !important;
  box-shadow: 0 10px 18px rgba(34, 167, 101, 0.18) !important;
}

.person-page :deep(.el-button--primary:hover) {
  background: #168a55 !important;
  box-shadow: 0 12px 22px rgba(22, 138, 85, 0.2) !important;
}

.person-page :deep(.el-button:not(.el-button--primary)) {
  background: #ffffff !important;
  border: 1px solid #e1e8e3 !important;
  color: #46564b !important;
  box-shadow: none !important;
}

.person-page :deep(.el-button:not(.el-button--primary):hover) {
  color: #168a55 !important;
  border-color: #b9dac7 !important;
  background: #f7fbf8 !important;
}

.person-page :deep(.el-tag) {
  height: 34px;
  padding: 0 14px;
  border-radius: 10px !important;
  font-weight: 700;
  background: #eef8f1 !important;
  border-color: #9dd9b4 !important;
  color: #21643d !important;
}

@media (max-width: 1400px) {
  .person-page {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .person-page {
    padding: 16px;
  }

  .person-panel {
    border-radius: 16px;
  }

  .panel-heading,
  .profile-summary,
  .ios-form {
    padding-left: 18px;
    padding-right: 18px;
  }

  .profile-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .avatar-uploader,
  .avatar-uploader :deep(.el-upload),
  .avatar-frame,
  .avatar,
  .avatar-uploader-icon {
    width: 128px;
    height: 128px;
  }

  .uid-value {
    font-size: 22px;
  }
}

@media (max-width: 980px) {
  .profile-summary {
    grid-template-columns: 1fr;
  }

  .avatar-uploader,
  .avatar-frame,
  .avatar,
  .avatar-uploader :deep(.el-upload) {
    width: 132px;
    height: 132px;
  }
}
</style>
