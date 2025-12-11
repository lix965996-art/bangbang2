<template>
  <div id="app">
    <router-view />

    <!-- AI聊天机器人浮动球 -->
    <div 
      v-if="showFloatingChat"
      class="ai-chat-container" 
      :style="containerStyle"
      ref="fabContainer"
    >
      <div 
        class="fab-btn" 
        @mousedown="startDrag"
        @click="handleFabClick"
        title="点击咨询 AI 农艺师，拖动可移动位置"
        style="display: flex !important; width: 60px; height: 60px; background: linear-gradient(135deg, #00f2f1, #0066ff); border-radius: 50%; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 4px 20px rgba(0, 102, 255, 0.5); animation: pulse 2s infinite;"
      >
        <img src="@/assets/ai2.png" alt="AI" style="width: 32px; height: 32px; object-fit: contain;" />
      </div>

      <transition name="fade">
        <div v-if="showChat" class="chat-window">
          <div class="chat-header">
            <div class="title">
              <span class="status-dot"></span>
              智能体农情大脑
            </div>
            <span class="close-btn" @click="showChat = false">×</span>
          </div>

          <div class="chat-body" ref="chatBody">
            <div v-for="(msg, index) in messages" :key="index" :class="['message-row', msg.role]">
              <div class="avatar">
                <img v-if="msg.role === 'ai'" src="@/assets/ai2.png" alt="AI" class="avatar-img" />
                <img v-else src="@/assets/nongming.png" alt="农民" class="avatar-img" />
              </div>
              <div class="bubble">
                <div v-html="formatText(msg.content)"></div>
              </div>
            </div>
            
            <div v-if="isLoading" class="message-row ai">
              <div class="avatar"><img src="@/assets/ai2.png" alt="AI" class="avatar-img" /></div>
              <div class="bubble loading">
                <span>.</span><span>.</span><span>.</span>
              </div>
            </div>
          </div>

          <div class="chat-footer">
            <input 
              v-model="inputMsg" 
              @keyup.enter="sendMessage"
              type="text" 
              placeholder="例如：现在的温度适合草莓生长吗？" 
            />
            <button @click="sendMessage" :disabled="isLoading">发送</button>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'App',
  data() {
    return {
      showChat: false,
      inputMsg: '',
      isLoading: false,
      messages: [
        { 
          role: 'ai', 
          content: '你好！我是你的专属 AI 农艺师。我已经连接到大棚传感器，随时为你分析环境数据。' 
        }
      ],
      // 拖动相关
      isDragging: false,
      hasDragged: false,
      dragStartX: 0,
      dragStartY: 0,
      fabX: null,
      fabY: null
    };
  },
  computed: {
    showFloatingChat() {
      // 强制显示聊天机器人，除了登录和注册页面
      const hiddenRoutes = ['/login', '/register'];
      const shouldShow = !hiddenRoutes.includes(this.$route.path);
      console.log('showFloatingChat:', shouldShow, 'current route:', this.$route.path);
      return shouldShow;
    },
    containerStyle() {
      const baseStyle = {
        position: 'fixed !important',
        zIndex: '99999 !important',
        display: 'block !important',
        userSelect: 'none'
      };
      
      if (this.fabX !== null && this.fabY !== null) {
        return {
          ...baseStyle,
          left: this.fabX + 'px',
          top: this.fabY + 'px',
          right: 'auto',
          bottom: 'auto'
        };
      }
      
      // 默认位置：右下角
      return {
        ...baseStyle,
        right: '30px',
        bottom: '30px'
      };
    }
  },
  mounted() {
    this.loadFabPosition();
    document.addEventListener('mousemove', this.onDrag);
    document.addEventListener('mouseup', this.stopDrag);
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onDrag);
    document.removeEventListener('mouseup', this.stopDrag);
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    // 拖动功能
    loadFabPosition() {
      const saved = localStorage.getItem('fabPosition');
      if (saved) {
        try {
          const pos = JSON.parse(saved);
          this.fabX = pos.x;
          this.fabY = pos.y;
          console.log('加载保存的位置:', pos);
        } catch (e) {
          console.error('加载位置失败:', e);
        }
      } else {
        console.log('没有保存的位置，使用默认位置');
      }
    },
    saveFabPosition() {
      const position = {
        x: this.fabX,
        y: this.fabY
      };
      localStorage.setItem('fabPosition', JSON.stringify(position));
      console.log('保存位置:', position);
    },
    startDrag(e) {
      this.isDragging = true;
      this.hasDragged = false;
      const rect = this.$refs.fabContainer.getBoundingClientRect();
      this.dragStartX = e.clientX - rect.left;
      this.dragStartY = e.clientY - rect.top;
      e.preventDefault();
    },
    onDrag(e) {
      if (!this.isDragging) return;
      this.hasDragged = true;
      
      let newX = e.clientX - this.dragStartX;
      let newY = e.clientY - this.dragStartY;
      
      // 限制在窗口范围内，留出按钮尺寸的边距
      const buttonSize = 60;
      const maxX = window.innerWidth - buttonSize;
      const maxY = window.innerHeight - buttonSize;
      
      newX = Math.max(0, Math.min(newX, maxX));
      newY = Math.max(0, Math.min(newY, maxY));
      
      this.fabX = newX;
      this.fabY = newY;
      
      console.log('拖动到位置:', { x: newX, y: newY });
    },
    stopDrag() {
      if (this.isDragging && this.hasDragged) {
        this.saveFabPosition();
      }
      this.isDragging = false;
    },
    handleFabClick() {
      // 只有没有拖动时才触发点击
      if (!this.hasDragged) {
        this.toggleChat();
      }
      this.hasDragged = false;
    },
    toggleChat() {
      this.showChat = !this.showChat;
      if (this.showChat) {
        this.scrollToBottom();
      }
    },
    // 简单的文本换行处理
    formatText(text) {
      return text.replace(/\n/g, '<br>');
    },
    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        if (this.$refs.chatBody) {
          this.$refs.chatBody.scrollTop = this.$refs.chatBody.scrollHeight;
        }
      });
    },
    // 窗口大小变化时调整位置
    handleResize() {
      if (this.fabX !== null && this.fabY !== null) {
        const buttonSize = 60;
        const maxX = window.innerWidth - buttonSize;
        const maxY = window.innerHeight - buttonSize;
        
        // 如果当前位置超出边界，调整到边界内
        let newX = Math.max(0, Math.min(this.fabX, maxX));
        let newY = Math.max(0, Math.min(this.fabY, maxY));
        
        if (newX !== this.fabX || newY !== this.fabY) {
          this.fabX = newX;
          this.fabY = newY;
          this.saveFabPosition();
          console.log('窗口大小变化，调整位置到:', { x: newX, y: newY });
        }
      }
    },
    async sendMessage() {
      if (!this.inputMsg.trim()) return;

      // 1. 显示用户消息
      const question = this.inputMsg;
      this.messages.push({ role: 'user', content: question });
      this.inputMsg = '';
      this.isLoading = true;
      this.scrollToBottom();

      try {
        // 2. 发送给 Spring Boot 后端
        // 注意：这里假设你的后端端口是 8080，如果不是请修改
        const res = await axios.post('http://localhost:9090/api/chat/ask', {
          question: question
        });

        // 3. 显示 AI 回复
        if (res.data && res.data.code === 200) {
          this.messages.push({ role: 'ai', content: res.data.answer });
        } else {
          this.messages.push({ role: 'ai', content: '系统繁忙，请稍后再试。' });
        }

      } catch (error) {
        console.error(error);
        this.messages.push({ role: 'ai', content: '连接服务器失败，请检查后端是否启动。' });
      } finally {
        this.isLoading = false;
        this.scrollToBottom();
      }
    }
  }
};
</script>

<style>
/* 这里保留你原来的全局样式 */
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}

.el-main {
  width: 100%;
  height: 100%;
  padding: 0px !important;
  padding-left: 10px !important;
}

/* --- 👇👇👇 AI 聊天组件样式 👇👇👇 --- */

/* 1. 容器定在右下角 */
.ai-chat-container {
  position: fixed !important;
  bottom: 30px;
  right: 30px;
  z-index: 99999 !important; /* 保证在最上层 */
  font-family: 'Segoe UI', sans-serif;
  pointer-events: auto !important;
  visibility: visible !important;
  opacity: 1 !important;
}

/* 2. 悬浮球按钮 - 可拖动 */
.fab-btn {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #00f2f1, #0066ff);
  border-radius: 50%;
  box-shadow: 0 4px 20px rgba(0, 102, 255, 0.5);
  display: flex !important;
  align-items: center;
  justify-content: center;
  cursor: grab;
  transition: transform 0.2s, box-shadow 0.2s !important;
  user-select: none;
  animation: pulse 2s infinite !important;
}
.fab-btn:hover { 
  transform: scale(1.05); 
  box-shadow: 0 6px 24px rgba(0, 102, 255, 0.6);
}
.fab-btn:active { 
  cursor: grabbing; 
  transform: scale(1.02);
}
.fab-btn .icon { 
  width: 32px; 
  height: 32px; 
  object-fit: contain; 
}

/* 3. 聊天窗口 */
.chat-window {
  position: absolute;
  bottom: 80px;
  right: 0;
  width: 360px;
  height: 520px;
  background-color: #1a1a2e; /* 深色背景 */
  border: 1px solid #16213e;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
}

/* 顶部 */
.chat-header {
  background: #16213e;
  padding: 15px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #0f3460;
}
.chat-header .title { font-weight: bold; display: flex; align-items: center; gap: 8px;}
.status-dot { width: 8px; height: 8px; background: #00f2f1; border-radius: 50%; box-shadow: 0 0 8px #00f2f1; }
.close-btn { cursor: pointer; font-size: 20px; color: #888; }

/* 内容区 */
.chat-body {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  background-color: #1a1a2e;
}
.message-row { display: flex; margin-bottom: 15px; align-items: flex-start; }
.message-row.user { flex-direction: row-reverse; }

.avatar { 
  width: 36px; height: 36px; 
  min-width: 36px; /* 防止被挤压 */
  background: linear-gradient(135deg, #6366f1, #8b5cf6); 
  border-radius: 50%; 
  display: flex; align-items: center; justify-content: center; 
  margin: 0 8px; font-size: 20px;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  padding: 0; /* 移除padding，使用flex居中 */
}
.avatar-img {
  width: 24px;
  height: 24px;
  object-fit: contain;
  border-radius: 0;
  display: block; /* 防止行内元素的额外间距 */
}

.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  color: #e0e0e0;
  background: #252a40; /* AI 气泡颜色 */
}
.message-row.user .bubble {
  background: #0066ff; /* 用户气泡颜色 */
  color: #fff;
}

/* 底部输入框 */
.chat-footer {
  padding: 15px;
  background: #16213e;
  display: flex;
  gap: 10px;
}
.chat-footer input {
  flex: 1;
  padding: 10px;
  border-radius: 20px;
  border: none;
  background: #0f3460;
  color: white;
  outline: none;
}
.chat-footer button {
  padding: 8px 16px;
  background: #00f2f1;
  border: none;
  border-radius: 20px;
  color: #000;
  font-weight: bold;
  cursor: pointer;
}
.chat-footer button:disabled { background: #555; cursor: not-allowed; }

/* 动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter, .fade-leave-to { opacity: 0; }
.loading span { animation: blink 1.4s infinite both; font-size: 20px;}
.loading span:nth-child(2) { animation-delay: 0.2s; }
.loading span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink { 0% { opacity: 0.2; } 20% { opacity: 1; } 100% { opacity: 0.2; } }
@keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.05); } 100% { transform: scale(1); } }
</style>