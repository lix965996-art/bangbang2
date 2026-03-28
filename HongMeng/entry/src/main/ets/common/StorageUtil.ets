// 本地存储工具类
import preferences from '@ohos.data.preferences';

interface StorageKeys {
  TOKEN: string;
  USER_INFO: string;
  USERNAME: string;
  IS_LOGGED_IN: string;
  SERVER_URL: string;
}

export class StorageUtil {
  private static readonly STORE_NAME = 'smart_farm_store';
  private static store: preferences.Preferences | null = null;

  // 存储键常量
  static readonly KEYS: StorageKeys = {
    TOKEN: 'user_token',
    USER_INFO: 'user_info',
    USERNAME: 'username',
    IS_LOGGED_IN: 'is_logged_in',
    SERVER_URL: 'server_url'
  };

  // 初始化存储
  static async init(context: Context): Promise<void> {
    try {
      StorageUtil.store = await preferences.getPreferences(context, StorageUtil.STORE_NAME);
    } catch (error) {
      console.error('StorageUtil init failed:', error);
    }
  }

  // 保存数据
  static async put(key: string, value: string | number | boolean): Promise<boolean> {
    try {
      if (StorageUtil.store) {
        await StorageUtil.store.put(key, value);
        await StorageUtil.store.flush();
        return true;
      }
    } catch (error) {
      console.error('StorageUtil put failed:', error);
    }
    return false;
  }

  // 获取字符串
  static async getString(key: string, defaultValue: string = ''): Promise<string> {
    try {
      if (StorageUtil.store) {
        return await StorageUtil.store.get(key, defaultValue) as string;
      }
    } catch (error) {
      console.error('StorageUtil getString failed:', error);
    }
    return defaultValue;
  }

  // 获取数字
  static async getNumber(key: string, defaultValue: number = 0): Promise<number> {
    try {
      if (StorageUtil.store) {
        return await StorageUtil.store.get(key, defaultValue) as number;
      }
    } catch (error) {
      console.error('StorageUtil getNumber failed:', error);
    }
    return defaultValue;
  }

  // 获取布尔值
  static async getBoolean(key: string, defaultValue: boolean = false): Promise<boolean> {
    try {
      if (StorageUtil.store) {
        return await StorageUtil.store.get(key, defaultValue) as boolean;
      }
    } catch (error) {
      console.error('StorageUtil getBoolean failed:', error);
    }
    return defaultValue;
  }

  // 删除数据
  static async delete(key: string): Promise<boolean> {
    try {
      if (StorageUtil.store) {
        await StorageUtil.store.delete(key);
        await StorageUtil.store.flush();
        return true;
      }
    } catch (error) {
      console.error('StorageUtil delete failed:', error);
    }
    return false;
  }

  // 清空所有数据
  static async clear(): Promise<boolean> {
    try {
      if (StorageUtil.store) {
        await StorageUtil.store.clear();
        await StorageUtil.store.flush();
        return true;
      }
    } catch (error) {
      console.error('StorageUtil clear failed:', error);
    }
    return false;
  }
}
