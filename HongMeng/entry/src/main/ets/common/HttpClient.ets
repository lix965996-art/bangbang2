import http from '@ohos.net.http';
import { ApiConfig } from './ApiConfig';

// HTTP请求工具类
export class HttpClient {
  private static instance: HttpClient;
  private token: string = '';

  private constructor() {}

  static getInstance(): HttpClient {
    if (!HttpClient.instance) {
      HttpClient.instance = new HttpClient();
    }
    return HttpClient.instance;
  }

  // 设置Token
  setToken(token: string) {
    this.token = token;
  }

  // 获取Token
  getToken(): string {
    return this.token;
  }

  // 清除Token
  clearToken() {
    this.token = '';
  }

  // 构建完整URL
  private buildUrl(endpoint: string): string {
    return ApiConfig.BASE_URL + endpoint;
  }

  // GET请求
  async get<T>(endpoint: string, params?: Record<string, string | number>): Promise<T> {
    let url = this.buildUrl(endpoint);
    if (params) {
      const queryString = Object.entries(params)
        .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`)
        .join('&');
      url += `?${queryString}`;
    }

    const httpRequest = http.createHttp();
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      if (this.token) {
        headers['token'] = this.token;
      }

      const response = await httpRequest.request(url, {
        method: http.RequestMethod.GET,
        header: headers,
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string);
        return result as T;
      } else {
        throw new Error(`HTTP Error: ${response.responseCode}`);
      }
    } finally {
      httpRequest.destroy();
    }
  }

  // POST请求
  async post<T>(endpoint: string, data?: object): Promise<T> {
    const url = this.buildUrl(endpoint);
    const httpRequest = http.createHttp();

    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      if (this.token) {
        headers['token'] = this.token;
      }

      const response = await httpRequest.request(url, {
        method: http.RequestMethod.POST,
        header: headers,
        extraData: data ? JSON.stringify(data) : undefined,
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string);
        return result as T;
      } else {
        throw new Error(`HTTP Error: ${response.responseCode}`);
      }
    } finally {
      httpRequest.destroy();
    }
  }

  // DELETE请求
  async delete<T>(endpoint: string, id: number): Promise<T> {
    const url = this.buildUrl(`${endpoint}/${id}`);
    const httpRequest = http.createHttp();

    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      if (this.token) {
        headers['token'] = this.token;
      }

      const response = await httpRequest.request(url, {
        method: http.RequestMethod.DELETE,
        header: headers,
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string);
        return result as T;
      } else {
        throw new Error(`HTTP Error: ${response.responseCode}`);
      }
    } finally {
      httpRequest.destroy();
    }
  }

  // 上传文件
  async uploadFile(endpoint: string, filePath: string): Promise<ApiResponse<object>> {
    const url = this.buildUrl(endpoint);
    const httpRequest = http.createHttp();

    try {
      const headers: Record<string, string> = {
        'Content-Type': 'multipart/form-data'
      };
      if (this.token) {
        headers['token'] = this.token;
      }

      const response = await httpRequest.request(url, {
        method: http.RequestMethod.POST,
        header: headers,
        extraData: {
          file: filePath
        },
        connectTimeout: 60000,
        readTimeout: 60000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string);
        return result as ApiResponse<object>;
      } else {
        return {
          code: '500',
          msg: `HTTP Error: ${response.responseCode}`,
          data: null
        };
      }
    } catch (error) {
      return {
        code: '500',
        msg: error.message || '网络请求失败',
        data: null
      };
    } finally {
      httpRequest.destroy();
    }
  }
}

// 通用响应类型
export interface ApiResponse<T> {
  code: string;
  msg?: string;
  data: T;
}

// 用户信息
export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  email?: string;
  phone?: string;
  address?: string;
  avatarUrl?: string;
  role?: string;
  token?: string;
}

// 农田数据
export interface Farmland {
  id?: number;
  farm: string;
  area: string;
  address: string;
  district?: string;
  crop?: string;
  number?: number;
  state?: string;
  temperature?: number;
  airhumidity?: number;
  soilhumidity?: number;
  carbon?: number;
  ph?: number;
  light?: number;
  filllight?: string;
  monitor?: string;
  pump?: string;
  keeper?: string;
  centerLng?: number;
  centerLat?: number;
  coordinates?: string;
}

// 库存数据
export interface Inventory {
  id?: number;
  produce: string;
  warehouse?: string;
  number?: number;
  unit?: string;
  safeStock?: number;
  keeper?: string;
}

// 预警数据
export interface Alert {
  id?: number;
  farmlandId?: number;
  farmlandName?: string;
  alertType: string;
  alertLevel: string;
  message: string;
  status: string;
  createTime?: string;
  processTime?: string;
  processor?: string;
}

// 健康指数
export interface HealthIndex {
  farmlandId: number;
  farmlandName: string;
  healthIndex: number;
  level: string;
}

// Dashboard汇总
export interface DashboardSummary {
  totalArea: number;
  totalStock: number;
  farmCount: number;
  normalCount: number;
}

// 果蔬检测结果
export interface FruitDetectResult {
  className: string;
  confidence: number;
  ripeness: string;
  bbox?: number[];
}
