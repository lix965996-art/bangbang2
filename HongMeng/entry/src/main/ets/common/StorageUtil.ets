// 本地存储工具类
import preferences from '@ohos.data.preferences';

export class StorageUtil {
  private static readonly STORE_NAME = 'smart_farm_store';
  private static store: preferences.Preferences | null = null;

  // 初始化存储
  static async init(context: Context): Promise<void> {
    try {
      this.store = await preferences.getPreferences(context, this.STORE_NAME);
    } catch (error) {
      console.error('StorageUtil init failed:', error);
    }
  }

  // 保存数据
  static async put(key: string, value: string | number | boolean): Promise<boolean> {
    try {
      if (this.store) {
        await this.store.put(key, value);
        await this.store.flush();
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
      if (this.store) {
        return await this.store.get(key, defaultValue) as string;
      }
    } catch (error) {
      console.error('StorageUtil getString failed:', error);
    }
    return defaultValue;
  }

  // 获取数字
  static async getNumber(key: string, defaultValue: number = 0): Promise<number> {
    try {
      if (this.store) {
        return await this.store.get(key, defaultValue) as number;
      }
    } catch (error) {
      console.error('StorageUtil getNumber failed:', error);
    }
    return defaultValue;
  }

  // 获取布尔值
  static async getBoolean(key: string, defaultValue: boolean = false): Promise<boolean> {
    try {
      if (this.store) {
        return await this.store.get(key, defaultValue) as boolean;
      }
    } catch (error) {
      console.error('StorageUtil getBoolean failed:', error);
    }
    return defaultValue;
  }

  // 删除数据
  static async delete(key: string): Promise<boolean> {
    try {
      if (this.store) {
        await this.store.delete(key);
        await this.store.flush();
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
      if (this.store) {
        await this.store.clear();
        await this.store.flush();
        return true;
      }
    } catch (error) {
      console.error('StorageUtil clear failed:', error);
    }
    return false;
  }

  // 存储键常量
  static readonly KEYS = {
    TOKEN: 'user_token',
    USER_INFO: 'user_info',
    USERNAME: 'username',
    IS_LOGGED_IN: 'is_logged_in',
    SERVER_URL: 'server_url'
  };
}
