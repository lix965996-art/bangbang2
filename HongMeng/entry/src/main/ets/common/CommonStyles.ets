// 通用样式定义
export class CommonStyles {
  // 间距
  static readonly SPACING_XS: number = 4;
  static readonly SPACING_SM: number = 8;
  static readonly SPACING_MD: number = 16;
  static readonly SPACING_LG: number = 24;
  static readonly SPACING_XL: number = 32;

  // 圆角
  static readonly RADIUS_SM: number = 4;
  static readonly RADIUS_MD: number = 8;
  static readonly RADIUS_LG: number = 12;
  static readonly RADIUS_XL: number = 16;
  static readonly RADIUS_FULL: number = 999;

  // 字体大小
  static readonly FONT_XS: number = 10;
  static readonly FONT_SM: number = 12;
  static readonly FONT_MD: number = 14;
  static readonly FONT_LG: number = 16;
  static readonly FONT_XL: number = 18;
  static readonly FONT_XXL: number = 24;
  static readonly FONT_TITLE: number = 28;

  // 颜色
  static readonly COLOR_PRIMARY: string = '#4CAF50';
  static readonly COLOR_PRIMARY_DARK: string = '#388E3C';
  static readonly COLOR_ACCENT: string = '#8BC34A';
  static readonly COLOR_BACKGROUND: string = '#F5F5F5';
  static readonly COLOR_CARD: string = '#FFFFFF';
  static readonly COLOR_TEXT_PRIMARY: string = '#333333';
  static readonly COLOR_TEXT_SECONDARY: string = '#666666';
  static readonly COLOR_TEXT_HINT: string = '#999999';
  static readonly COLOR_DIVIDER: string = '#E0E0E0';
  static readonly COLOR_ERROR: string = '#F44336';
  static readonly COLOR_WARNING: string = '#FF9800';
  static readonly COLOR_SUCCESS: string = '#4CAF50';
  static readonly COLOR_INFO: string = '#2196F3';

  // 状态颜色
  static readonly STATE_NORMAL: string = '#4CAF50';
  static readonly STATE_WARNING: string = '#FF9800';
  static readonly STATE_DANGER: string = '#F44336';
}

// 通用样式函数
export function getCardStyle(): Record<string, string | number> {
  return {
    backgroundColor: CommonStyles.COLOR_CARD,
    borderRadius: CommonStyles.RADIUS_LG,
    padding: CommonStyles.SPACING_MD
  };
}

export function getRowStyle(): Record<string, string | number> {
  return {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%'
  };
}

export function getShadowStyle(): Record<string, string | number> {
  return {
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  };
}
