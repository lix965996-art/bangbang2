import Vue from 'vue';

/**
 * 全局加载状态管理
 */

let loadingInstance = null;
let loadingCount = 0;

const LoadingConstructor = Vue.extend({
  template: `
    <div v-if="visible" class="global-loading-overlay">
      <div class="loading-container">
        <div class="loading-spinner">
          <div class="spinner-circle"></div>
          <div class="spinner-circle"></div>
          <div class="spinner-circle"></div>
        </div>
        <div class="loading-text">{{ text || '加载中...' }}</div>
        <div class="loading-progress" v-if="showProgress">
          <el-progress
            :percentage="progress"
            :status="progressStatus"
            :stroke-width="4"
            :show-text="false"
          />
        </div>
      </div>
    </div>
  `,
  props: {
    visible: Boolean,
    text: String,
    showProgress: Boolean,
    progress: Number,
    progressStatus: String
  }
});

/**
 * 显示加载动画
 * @param {string} text - 加载提示文字
 * @param {Object} options - 配置选项
 */
export const showLoading = (text = '加载中...', options = {}) => {
  loadingCount++;

  // 避免重复创建
  if (!loadingInstance) {
    loadingInstance = new LoadingConstructor({
      el: document.createElement('div'),
      propsData: {
        visible: true,
        text,
        ...options
      }
    });
    document.body.appendChild(loadingInstance.$el);
  } else {
    loadingInstance.visible = true;
    if (text) {
      loadingInstance.text = text;
    }
    if (options.showProgress !== undefined) {
      loadingInstance.showProgress = options.showProgress;
    }
    if (options.progress !== undefined) {
      loadingInstance.progress = options.progress;
    }
    if (options.progressStatus !== undefined) {
      loadingInstance.progressStatus = options.progressStatus;
    }
  }

  return loadingInstance;
};

/**
 * 隐藏加载动画
 */
export const hideLoading = () => {
  loadingCount--;
  if (loadingCount <= 0 && loadingInstance) {
    loadingInstance.visible = false;
    setTimeout(() => {
      if (loadingInstance && !loadingInstance.visible) {
        document.body.removeChild(loadingInstance.$el);
        loadingInstance = null;
      }
    }, 300); // 等待动画完成
  }
};

/**
 * 显示带进度的加载动画
 * @param {string} text - 提示文字
 * @param {number} progress - 进度 0-100
 * @param {string} status - 状态
 */
export const showProgress = (text = '处理中...', progress = 0, status = '') => {
  showLoading(text, {
    showProgress: true,
    progress,
    progressStatus: status
  });

  return {
    update: (newProgress, newStatus) => {
      if (loadingInstance) {
        loadingInstance.progress = newProgress;
        if (newStatus) {
          loadingInstance.progressStatus = newStatus;
        }
      }
    },
    hide: hideLoading
  };
};

/**
 * 为异步函数添加加载状态
 * @param {Function} asyncFn - 异步函数
 * @param {string} loadingText - 加载提示文字
 * @param {Object} options - 配置选项
 */
export const withLoading = (asyncFn, loadingText = '加载中...', options = {}) => {
  return async (...args) => {
    showLoading(loadingText, options);
    try {
      const result = await asyncFn(...args);
      return result;
    } catch (error) {
      throw error;
    } finally {
      hideLoading();
    }
  };
};

// Vue 指令：v-loading
export const loadingDirective = {
  inserted(el, binding) {
    const { value, modifiers } = binding;

    if (value) {
      el._loadingInstance = showLoading(modifiers.text ? '加载中...' : undefined);
    }
  },
  update(el, binding) {
    const { value, modifiers } = binding;

    if (el._loadingInstance) {
      el._loadingInstance.visible = value;

      // 如果文本变化，更新文本
      if (modifiers.text && value) {
        el._loadingInstance.text = '加载中...';
      }
    }
  },
  unbind(el) {
    if (el._loadingInstance) {
      el._loadingInstance.$destroy();
      el._loadingInstance = null;
    }
  }
};

// 注册为 Vue 全局指令
Vue.directive('loading', loadingDirective);