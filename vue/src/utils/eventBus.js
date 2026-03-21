import Vue from 'vue';

// 创建事件总线用于跨组件通信
export const eventBus = new Vue();

// 事件类型常量
export const EVENTS = {
  IRRIGATION_ON: 'irrigation:on',
  IRRIGATION_OFF: 'irrigation:off',
  LED_ON: 'led:on',
  LED_OFF: 'led:off'
};
