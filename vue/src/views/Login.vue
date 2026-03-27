<template>
  <div class="login-page">
    <!-- 碎片拼接背景 -->
    <div class="fragments-background">
      <div class="fragments-container">
        <!-- 动态生成碎片 -->
        <div 
          v-for="(fragment, index) in fragments" 
          :key="index"
          class="fragment-tile"
          :style="getFragmentStyle(fragment, index)"
        >
        </div>
      </div>
    </div>
    
    <!-- 渐变融合层 -->
    <div class="gradient-blend"></div>
    
    <!-- 右侧白色背景容器 -->
    <div class="right-panel">
      <!-- 登录卡片 -->
      <div class="login-box">
        <div class="login-card">
          <div class="login-header">
            <div class="login-title-text">登录</div>
            <div class="logo-container">
              <div class="logo-icon-wrapper">
                <img class="logo-icon" :src="require('@/assets/Logo.jpg')" />
              </div>
              <div class="logo-text">BangBang Agro</div>
            </div>
          </div>
          
          <el-form :model="user" :rules="rules" ref="userForm" class="form-container">
            <el-form-item prop="username" class="form-group">
              <div class="input-wrapper">
                <i class="el-icon-user input-icon"></i>
                <el-input 
                  class="form-input" 
                  v-model="user.username"
                  placeholder="请输入用户名"
                  autocomplete="off"
                ></el-input>
              </div>
            </el-form-item>
            
            <el-form-item prop="password" class="form-group">
              <div class="input-wrapper">
                <i class="el-icon-lock input-icon"></i>
                <el-input 
                  class="form-input" 
                  type="password"
                  v-model="user.password"
                  placeholder="请输入密码"
                  show-password
                  autocomplete="off"
                ></el-input>
              </div>
            </el-form-item>
            
            <el-form-item class="form-group">
              <el-button 
                class="login-button" 
                type="primary"
                :loading="logging"
                @click="login"
              >
                {{ logging ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          
            <div class="action-links">
              <el-button type="text" class="link-text" @click="handlePass">找回密码</el-button>
              <div class="register-link">
                <span>还没有账号？</span>
                <span class="link-text" @click="$router.push('/register')">立即注册</span>
              </div>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 找回密码对话框 -->
    <el-dialog title="找回密码" :visible.sync="dialogFormVisible" width="30%">
      <el-form label-width="100px" size="small">
        <el-form-item label="用户名">
          <el-input v-model="pass.username" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="pass.phone" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="passwordBack">重置密码</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {resetRouter, setRoutes} from "@/router";
import Identify from "@/components/Identify";

export default {
  name: "Login",
  data() {
    return {
      user: {},
      pass: {},
      code: '',
      dialogFormVisible: false,
      logging: false,
      // 图片验证码
      identifyCode: '',
      // 验证码规则
      identifyCodes: '3456789ABCDEFGHGKMNPQRSTUVWXY',
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
        ],
      },
      // 背景图片路径
      backgroundImageUrl: require('@/assets/background2.jpg'),
      // 碎片配置：16x16网格 = 256个碎片
      gridSize: 16,
      fragments: []
    }
  },
  components: {Identify},
  mounted() {
    // 重置路由
    resetRouter()
    this.refreshCode()
    this.initFragments()
  },
  methods: {
    initFragments() {
      // 生成碎片数据
      const fragments = [];
      const gridSize = this.gridSize;
      
      for (let row = 0; row < gridSize; row++) {
        for (let col = 0; col < gridSize; col++) {
          const xPercent = gridSize > 1 ? (col / (gridSize - 1)) * 100 : 0;
          const yPercent = gridSize > 1 ? (row / (gridSize - 1)) * 100 : 0;
          
          const randomX = (Math.random() - 0.5) * 2000;
          const randomY = (Math.random() - 0.5) * 2000;
          const randomRotate = (Math.random() - 0.5) * 360;
          const randomScale = 0.2 + Math.random() * 0.3;
          
          fragments.push({
            row,
            col,
            xPercent,
            yPercent,
            randomX,
            randomY,
            randomRotate,
            randomScale,
            delay: (row * gridSize + col) * 0.005
          });
        }
      }
      
      this.fragments = fragments;
    },
    getFragmentStyle(fragment, index) {
      const gridSize = this.gridSize;
      const size = 100 / gridSize;
      const bgSize = gridSize * 100;
      const bgX = fragment.xPercent;
      const bgY = fragment.yPercent;
      const left = fragment.col * size;
      const top = fragment.row * size;
      
      return {
        backgroundImage: `url(${this.backgroundImageUrl})`,
        backgroundSize: `${bgSize}% ${bgSize}%`,
        backgroundPosition: `${bgX}% ${bgY}%`,
        backgroundRepeat: 'no-repeat',
        left: `${left}%`,
        top: `${top}%`,
        width: `${size}%`,
        height: `${size}%`,
        '--fragment-width': `${size}%`,
        '--fragment-height': `${size}%`,
        '--initial-x': `${fragment.randomX}px`,
        '--initial-y': `${fragment.randomY}px`,
        '--initial-rotate': `${fragment.randomRotate}deg`,
        '--initial-scale': fragment.randomScale,
        '--animation-delay': `${fragment.delay}s`
      };
    },
    login() {
      this.$refs['userForm'].validate((valid) => {
        if (valid) {
          this.logging = true;
          this.request.post("/user/login", this.user).then(res => {
            if(res.code === '200') {
              localStorage.setItem("user", JSON.stringify(res.data))
              localStorage.setItem("menus", JSON.stringify(res.data.menus))

              setRoutes()
              this.$router.push("/")
              this.$message.success("登录成功")
            } else {
              this.$message.error(res.msg)
            }
          }).finally(() => {
            this.logging = false;
          })
        }
      });
    },

    refreshCode() {
      this.identifyCode = ''
      this.makeCode(this.identifyCodes, 4)
    },
    // 生成随机验证码
    makeCode(o, l) {
      for (let i = 0; i < l; i++) {
        this.identifyCode += this.identifyCodes[Math.floor(Math.random() * (this.identifyCodes.length))]
      }
    },
    handlePass() {
      this.dialogFormVisible = true
      this.pass = {}
    },
    passwordBack() {
      this.request.put("/user/reset", this.pass).then(res => {
        if (res.code === '200') {
          this.$message.success("重置密码成功，新密码为：123，请尽快修改密码")
          this.dialogFormVisible = false
        } else {
          this.$message.error(res.msg)
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  background: transparent;
}

/* 碎片背景容器 */
.fragments-background {
  position: absolute;
  width: 55%;
  left: 0;
  height: 100%;
  z-index: 0;
  overflow: hidden;
  transform: translateZ(0);
}

.fragments-container {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: hidden;
  transform: translateZ(0);
}

/* 碎片基础样式 */
.fragment-tile {
  position: absolute;
  opacity: 0;
  transform: translate(
    var(--initial-x, 0),
    var(--initial-y, 0)
  ) scale(var(--initial-scale, 0.3)) rotate(var(--initial-rotate, 0deg));
  animation: fragmentAssemble 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  animation-delay: var(--animation-delay, 0s);
  transform-origin: center center;
  backface-visibility: hidden;
  will-change: transform, opacity;
  margin: -1px; /* 负边距消除间隙 */
  padding: 0;
  border: none;
  box-sizing: border-box;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  /* 添加轻微的重叠来消除白线 */
  width: calc(var(--fragment-width) + 2px) !important;
  height: calc(var(--fragment-height) + 2px) !important;
}

/* 碎片飞回组合动画 */
@keyframes fragmentAssemble {
  0% {
    opacity: 0;
    transform: translate(
      var(--initial-x, 0),
      var(--initial-y, 0)
    ) scale(var(--initial-scale, 0.2)) rotate(var(--initial-rotate, 0deg));
  }
  40% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1.02) rotate(0deg);
  }
  70% {
    opacity: 0.95;
    transform: translate(0, 0) scale(1.01) rotate(0deg);
  }
  100% {
    opacity: 1;
    transform: translate(0, 0) scale(1) rotate(0deg);
  }
}

/* 渐变融合层 */
.gradient-blend {
  position: absolute;
  left: 0;
  width: 55%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
  background: 
    radial-gradient(circle at 20% 30%, rgba(34, 197, 94, 0.03), transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(16, 185, 129, 0.02), transparent 50%),
    linear-gradient(to right, transparent 0%, rgba(255, 255, 255, 0.05) 95%, rgba(255, 255, 255, 0.1) 100%);
  animation: fadeInGradient 0.5s ease-in 1.5s forwards;
  opacity: 0;
}

@keyframes fadeInGradient {
  to {
    opacity: 1;
  }
}

/* 右半部分白色背景容器 */
.right-panel {
  position: absolute;
  right: 0;
  top: 0;
  width: 45%;
  height: 100%;
  z-index: 2;
  overflow: hidden;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.login-box {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 380px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 0 auto;
}

.login-card {
  width: 100%;
  background: #ffffff;
  border-radius: 24px;
  padding: 40px 30px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  position: relative;
  overflow: hidden;
}

.login-header {
  text-align: center;
  margin-bottom: 50px;
}

.login-title-text {
  display: block;
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 30px;
  text-align: left;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  margin-bottom: 0;
}

.logo-icon-wrapper {
  background: linear-gradient(135deg, 
    rgba(34, 197, 94, 0.15),
    rgba(16, 185, 129, 0.15)
  );
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(34, 197, 94, 0.2);
  overflow: hidden;
}

.logo-icon {
  width: 60px;
  height: 60px;
  object-fit: cover;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #22c55e;
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 28px;
  width: 100%;
}

.form-group {
  width: 100%;
  margin-bottom: 0 !important;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 0 20px;
  transition: all 0.3s;
}

.input-wrapper:focus-within {
  border-color: #22c55e;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.1);
}

.input-icon {
  font-size: 18px;
  margin-right: 14px;
  color: #9ca3af;
}

.form-input >>> .el-input__inner {
  border: none !important;
  background: transparent !important;
  padding: 20px 0;
  font-size: 14px;
  color: #1f2937;
  box-shadow: none !important;
}

.form-input >>> .el-input__inner::placeholder {
  color: #9ca3af;
}

.form-input >>> .el-input__inner:focus {
  border: none !important;
  box-shadow: none !important;
}

.login-button {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
  height: auto;
}

.login-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(34, 197, 94, 0.4);
}

.login-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.4);
}

.action-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.register-link {
  text-align: right;
  font-size: 13px;
  color: #6b7280;
}

.link-text {
  color: #22c55e !important;
  font-weight: 600;
  cursor: pointer;
  font-size: 13px;
}

.link-text:hover {
  opacity: 0.8;
  text-decoration: underline;
}

/* Element UI 样式覆盖 */
.form-container >>> .el-form-item__error {
  padding-top: 4px;
}

.form-container >>> .el-form-item {
  margin-bottom: 0;
}
</style>

