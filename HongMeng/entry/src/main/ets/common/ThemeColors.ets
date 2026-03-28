// 主题色彩系统 - 国赛版本
export class ThemeColors {
  // 主色调渐变 (活力绿 → 科技蓝)
  static readonly PRIMARY_GREEN: string = '#22C55E';
  static readonly PRIMARY_BLUE: string = '#3B82F6';
  static readonly PRIMARY_GRADIENT: string[] = ['#22C55E', '#3B82F6'];

  // 背景色
  static readonly BG_LIGHT: string = '#F8FAFC';      // 云白
  static readonly BG_DARK: string = '#0F172A';       // 深空
  static readonly BG_CARD: string = '#FFFFFF';

  // 毛玻璃效果
  static readonly GLASS_WHITE: string = 'rgba(255, 255, 255, 0.7)';
  static readonly GLASS_DARK: string = 'rgba(15, 23, 42, 0.8)';
  static readonly GLASS_BORDER: string = 'rgba(255, 255, 255, 0.3)';
  static readonly GLASS_SHADOW: string = 'rgba(0, 0, 0, 0.1)';

  // 强调色
  static readonly ACCENT_AI: string = '#8B5CF6';     // 紫色 - AI相关
  static readonly ACCENT_WARNING: string = '#F59E0B'; // 橙色 - 预警
  static readonly ACCENT_DANGER: string = '#EF4444'; // 红色 - 危险
  static readonly ACCENT_SUCCESS: string = '#10B981'; // 绿色 - 成功
  static readonly ACCENT_INFO: string = '#06B6D4';   // 青色 - 信息

  // 文字颜色
  static readonly TEXT_PRIMARY: string = '#1E293B';
  static readonly TEXT_SECONDARY: string = '#64748B';
  static readonly TEXT_TERTIARY: string = '#94A3B8';
  static readonly TEXT_LIGHT: string = '#FFFFFF';
  static readonly TEXT_DARK_BG: string = '#E2E8F0';

  // 渐变配置
  static readonly GRADIENT_GREEN_BLUE: LinearGradient = {
    angle: 135,
    colors: [['#22C55E', 0.0], ['#3B82F6', 1.0]]
  };

  static readonly GRADIENT_AI: LinearGradient = {
    angle: 135,
    colors: [['#8B5CF6', 0.0], ['#EC4899', 1.0]]
  };

  static readonly GRADIENT_SUCCESS: LinearGradient = {
    angle: 135,
    colors: [['#10B981', 0.0], ['#22C55E', 1.0]]
  };

  static readonly GRADIENT_WARNING: LinearGradient = {
    angle: 135,
    colors: [['#F59E0B', 0.0], ['#EF4444', 1.0]]
  };

  static readonly GRADIENT_DARK: LinearGradient = {
    angle: 180,
    colors: [['#0F172A', 0.0], ['#1E293B', 1.0]]
  };

  static readonly GRADIENT_LOGIN_BG: LinearGradient = {
    angle: 180,
    colors: [['#E0F2FE', 0.0], ['#DBEAFE', 0.5], ['#E0E7FF', 1.0]]
  };

  // 阴影配置
  static readonly SHADOW_SM: ShadowConfig = {
    radius: 4,
    color: 'rgba(0, 0, 0, 0.05)',
    offsetX: 0,
    offsetY: 2
  };

  static readonly SHADOW_MD: ShadowConfig = {
    radius: 12,
    color: 'rgba(0, 0, 0, 0.08)',
    offsetX: 0,
    offsetY: 4
  };

  static readonly SHADOW_LG: ShadowConfig = {
    radius: 24,
    color: 'rgba(0, 0, 0, 0.12)',
    offsetX: 0,
    offsetY: 8
  };

  static readonly SHADOW_GLOW_GREEN: ShadowConfig = {
    radius: 20,
    color: 'rgba(34, 197, 94, 0.3)',
    offsetX: 0,
    offsetY: 0
  };

  static readonly SHADOW_GLOW_BLUE: ShadowConfig = {
    radius: 20,
    color: 'rgba(59, 130, 246, 0.3)',
    offsetX: 0,
    offsetY: 0
  };

  static readonly SHADOW_GLOW_AI: ShadowConfig = {
    radius: 20,
    color: 'rgba(139, 92, 246, 0.3)',
    offsetX: 0,
    offsetY: 0
  };

  // 圆角配置
  static readonly RADIUS_SM: number = 8;
  static readonly RADIUS_MD: number = 12;
  static readonly RADIUS_LG: number = 16;
  static readonly RADIUS_XL: number = 24;
  static readonly RADIUS_FULL: number = 9999;

  // 间距配置
  static readonly SPACE_XS: number = 4;
  static readonly SPACE_SM: number = 8;
  static readonly SPACE_MD: number = 16;
  static readonly SPACE_LG: number = 24;
  static readonly SPACE_XL: number = 32;

  // 动画时长
  static readonly DURATION_FAST: number = 150;
  static readonly DURATION_NORMAL: number = 300;
  static readonly DURATION_SLOW: number = 500;
}

// 渐变配置接口
interface LinearGradient {
  angle: number;
  colors: [string, number][];
}

// 阴影配置接口
interface ShadowConfig {
  radius: number;
  color: string;
  offsetX: number;
  offsetY: number;
}
