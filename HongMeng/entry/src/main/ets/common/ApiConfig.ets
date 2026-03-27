// API配置
export class ApiConfig {
  // 后端服务器地址 - 请根据实际部署情况修改
  static readonly BASE_URL: string = 'http://10.0.2.2:9090'; // 模拟器访问本机
  // static readonly BASE_URL: string = 'http://192.168.1.100:9090'; // 真机访问

  // API端点
  static readonly LOGIN: string = '/user/login';
  static readonly REGISTER: string = '/user/register';
  static readonly USER_INFO: string = '/user';

  // 农田相关
  static readonly FARMLAND_LIST: string = '/statistic';
  static readonly FARMLAND_PAGE: string = '/statistic/page';
  static readonly FARMLAND_SAVE: string = '/statistic';
  static readonly FARMLAND_DELETE: string = '/statistic';
  static readonly DASHBOARD: string = '/statistic/dashboard';
  static readonly DASHBOARD_SUMMARY: string = '/statistic/dashboard/summary';

  // 库存相关
  static readonly INVENTORY_LIST: string = '/inventory';
  static readonly INVENTORY_PAGE: string = '/inventory/page';
  static readonly INVENTORY_SAVE: string = '/inventory';
  static readonly INVENTORY_REPLENISHMENT: string = '/inventory/replenishment-alerts';

  // 果蔬识别
  static readonly FRUIT_DETECT: string = '/fruit-detect/analyze';
  static readonly FRUIT_HEALTH: string = '/fruit-detect/health';

  // AI助手
  static readonly AI_ASK: string = '/api/chat/ask';
  static readonly AI_QWEN_PROXY: string = '/api/chat/qwen-proxy';

  // 健康指数
  static readonly HEALTH_INDEX: string = '/health-index/calculate';
  static readonly HEALTH_INDEX_ALL: string = '/health-index/calculate/all';
  static readonly HEALTH_CONFIG: string = '/health-index/config';

  // 预警
  static readonly ALERT_TODAY: string = '/alert/today/tasks';
  static readonly ALERT_PENDING: string = '/alert/pending';
  static readonly ALERT_PROCESS: string = '/alert';
}
