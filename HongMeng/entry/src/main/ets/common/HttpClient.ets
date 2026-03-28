import http from '@ohos.net.http';
import { ApiConfig } from './ApiConfig';

// 通用响应类型
export interface ApiResponse<T> {
  code: string;
  msg?: string;
  data?: T;
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

// 分页记录响应
export interface PaginatedRecords<T> {
  records: T[];
}

// 查询参数项
export interface QueryParam {
  key: string;
  value: string;
}

// 请求头对象
interface HeaderObject {
  'Content-Type': string;
  'token'?: string;
}

// HTTP请求头类
export class HttpHeaders {
  private contentType: string = 'application/json';
  private token: string = '';

  setContentType(type: string): void {
    this.contentType = type;
  }

  getContentType(): string {
    return this.contentType;
  }

  setToken(token: string): void {
    this.token = token;
  }

  getToken(): string {
    return this.token;
  }

  hasToken(): boolean {
    return this.token.length > 0;
  }

  // 转换为请求头对象
  toHeaderObject(): HeaderObject {
    if (this.hasToken()) {
      const headerWithToken: HeaderObject = {
        'Content-Type': this.contentType,
        'token': this.token
      };
      return headerWithToken;
    }
    const headerWithoutToken: HeaderObject = {
      'Content-Type': this.contentType
    };
    return headerWithoutToken;
  }
}

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

  setToken(token: string): void {
    this.token = token;
  }

  getToken(): string {
    return this.token;
  }

  clearToken(): void {
    this.token = '';
  }

  private buildUrl(endpoint: string): string {
    return ApiConfig.BASE_URL + endpoint;
  }

  private buildQueryParams(params: QueryParam[]): string {
    if (params.length === 0) {
      return '';
    }
    const parts: string[] = [];
    for (let i = 0; i < params.length; i++) {
      const p = params[i];
      parts.push(p.key + '=' + encodeURIComponent(p.value));
    }
    return '?' + parts.join('&');
  }

  private createHeaders(contentType?: string): HttpHeaders {
    const headers = new HttpHeaders();
    if (contentType) {
      headers.setContentType(contentType);
    }
    if (this.token) {
      headers.setToken(this.token);
    }
    return headers;
  }

  async get<T>(endpoint: string, params?: QueryParam[]): Promise<T> {
    let url = this.buildUrl(endpoint);
    if (params && params.length > 0) {
      url += this.buildQueryParams(params);
    }

    const httpRequest = http.createHttp();
    try {
      const headers = this.createHeaders();
      const response = await httpRequest.request(url, {
        method: http.RequestMethod.GET,
        header: headers.toHeaderObject(),
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string) as T;
        return result;
      } else {
        throw new Error('HTTP Error: ' + response.responseCode);
      }
    } finally {
      httpRequest.destroy();
    }
  }

  async post<T>(endpoint: string, data?: object): Promise<T> {
    const url = this.buildUrl(endpoint);
    const httpRequest = http.createHttp();

    try {
      const headers = this.createHeaders();
      const response = await httpRequest.request(url, {
        method: http.RequestMethod.POST,
        header: headers.toHeaderObject(),
        extraData: data ? JSON.stringify(data) : undefined,
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string) as T;
        return result;
      } else {
        throw new Error('HTTP Error: ' + response.responseCode);
      }
    } finally {
      httpRequest.destroy();
    }
  }

  async delete<T>(endpoint: string, id: number): Promise<T> {
    const url = this.buildUrl(endpoint + '/' + id);
    const httpRequest = http.createHttp();

    try {
      const headers = this.createHeaders();
      const response = await httpRequest.request(url, {
        method: http.RequestMethod.DELETE,
        header: headers.toHeaderObject(),
        connectTimeout: 30000,
        readTimeout: 30000
      });

      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string) as T;
        return result;
      } else {
        throw new Error('HTTP Error: ' + response.responseCode);
      }
    } finally {
      httpRequest.destroy();
    }
  }

  async uploadFile(endpoint: string, filePath: string): Promise<ApiResponse<object>> {
    const url = this.buildUrl(endpoint);
    const httpRequest = http.createHttp();

    try {
      const headers = this.createHeaders('multipart/form-data');
      const response = await httpRequest.request(url, {
        method: http.RequestMethod.POST,
        header: headers.toHeaderObject(),
        extraData: {
          file: filePath
        },
        connectTimeout: 60000,
        readTimeout: 60000
      });
      if (response.responseCode === 200) {
        const result = JSON.parse(response.result as string) as ApiResponse<object>;
        return result;
      } else {
        const errorResponse: ApiResponse<object> = {
          code: '500',
          msg: 'HTTP Error: ' + response.responseCode,
          data: undefined
        };
        return errorResponse;
      }
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : '网络请求失败';
      const errorResponse: ApiResponse<object> = {
        code: '500',
        msg: errorMsg,
        data: undefined
      };
      return errorResponse;
    } finally {
      httpRequest.destroy();
    }
  }
}
