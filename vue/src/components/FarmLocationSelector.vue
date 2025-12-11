<template>
  <el-dialog
    title="定位农场位置"
    :visible.sync="dialogVisible"
    width="70%"
    top="5vh"
    :close-on-click-modal="false"
    append-to-body
    custom-class="map-dialog-new"
    @opened="initMap"
  >
    <div class="location-selector-content">
      <!-- 顶部提示 -->
      <div class="tips-text">
        输入农场地址进行搜索，或在地图上点击选择位置
      </div>
      
      <!-- 搜索区域 -->
      <div class="search-area">
        <label class="search-label">搜索地址：</label>
        <div class="search-input-wrapper">
          <input 
            id="amap-search-input" 
            v-model="searchKeyword" 
            class="search-input" 
            placeholder="请输入地址关键词（至少2个字）" 
            @input="handleInputChange"
            @keyup.enter="handleSearchBtn" 
          />
          <!-- 搜索提示下拉框 -->
          <div v-if="showTips && inputTips.length > 0" class="search-tips-dropdown">
            <div 
              v-for="(tip, index) in inputTips" 
              :key="index"
              class="tip-item"
              @click="selectTip(tip)"
            >
              <i class="el-icon-location"></i>
              <div class="tip-content">
                <div class="tip-name">{{ tip.name }}</div>
                <div class="tip-district">{{ tip.district }}{{ tip.address }}</div>
              </div>
            </div>
          </div>
        </div>
        <button class="search-btn" @click="handleSearchBtn">
          <i class="el-icon-search"></i> 搜索
        </button>
      </div>

      <!-- 地图区域 -->
      <div class="map-area">
        <div id="selector-container" class="map-content"></div>
        <div class="drawing-tip" v-if="isDrawing">
          <i class="el-icon-info"></i> 左键点击地图添加节点，双击鼠标结束绘制
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="bottom-bar">
        <div class="location-info">
          <span v-if="selectedLocation.address">
            <i class="el-icon-location-information"></i> {{ selectedLocation.address }}
          </span>
          <span v-else class="placeholder-text">请搜索地址或在地图上点击选择位置</span>
          <span v-if="currentPath.length > 0" class="coords-info">
            | 已绘制 {{ currentPath.length }} 个坐标点
          </span>
        </div>
        <div class="action-buttons">
          <el-button 
            type="success" 
            @click="startDraw"
            :disabled="isDrawing"
          >
            {{ isDrawing ? '绘制中...' : '绘制区域' }}
          </el-button>
          <el-button 
            type="warning" 
            @click="clearDraw"
            :disabled="!overlay && currentPath.length === 0"
          >
            清除
          </el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSelection">确定</el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import mapConfig from '@/config/map.config.js'

export default {
  name: 'FarmLocationSelector',
  props: {
    visible: { type: Boolean, default: false },
    initialData: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      map: null,
      searchKeyword: '',
      autoComplete: null,
      placeSearch: null,
      geocoder: null,
      mouseTool: null, // 鼠标工具插件
      
      marker: null,     // 单点标记
      overlay: null,    // 绘制的多边形对象
      
      isDrawing: false, // 是否正在绘制
      currentPath: [],  // 当前绘制的路径坐标数组
      isLoadingMap: false, // 是否正在加载地图API
      calculatedArea: 0, // 计算得出的面积（亩）
      calculatedDistrict: '', // 计算得出的区县信息
      
      selectedLocation: { lng: null, lat: null, address: '' },
      amapKey: mapConfig.amap.jsKey, // 从配置文件获取JavaScript API Key
      webApiKey: mapConfig.amap.webKey, // Web服务API Key
      
      // 输入提示相关
      inputTips: [], // 输入提示列表
      showTips: false, // 是否显示提示下拉框
      inputTimer: null // 输入防抖计时器
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible; },
      set(val) { this.$emit('update:visible', val); }
    }
  },
  mounted() {
    // 添加点击事件监听，点击其他地方隐藏下拉框
    document.addEventListener('click', this.handleDocumentClick);
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('click', this.handleDocumentClick);
    // 清除定时器
    if (this.inputTimer) {
      clearTimeout(this.inputTimer);
    }
    // 销毁地图实例，释放资源
    if (this.map) {
      try {
        this.map.destroy();
        this.map = null;
        console.log('✅ 地图实例已销毁');
      } catch (e) {
        console.warn('销毁地图实例时出错:', e);
      }
    }
  },
  methods: {
    // 处理文档点击事件
    handleDocumentClick(e) {
      // 如果点击的不是搜索区域，隐藏下拉框
      const searchArea = this.$el.querySelector('.search-input-wrapper');
      if (searchArea && !searchArea.contains(e.target)) {
        this.showTips = false;
      }
    },
    
    initMap() {
      console.log('🗺️ 开始初始化地图...');
      
      // 检查是否已经在加载中
      if (this.isLoadingMap) {
        console.log('地图正在加载中，请稍候...');
        return;
      }
      
      // 先销毁旧的地图实例（如果存在）
      if (this.map) {
        try {
          this.map.destroy();
          this.map = null;
          console.log('✅ 已销毁旧地图实例');
        } catch (e) {
          console.warn('销毁旧地图实例时出错:', e);
        }
      }
      
      if (!window.AMap) {
        this.isLoadingMap = true;
        console.log('📦 AMap不存在，开始加载...');
        
        // 检查是否已经有script标签在加载
        const existingScript = document.querySelector('script[src*="webapi.amap.com"]');
        if (existingScript) {
          console.log('检测到已有地图脚本，等待加载完成...');
          // 等待现有脚本加载完成
          existingScript.addEventListener('load', () => {
            this.isLoadingMap = false;
            this.createMapInstance();
          });
          return;
        }
        
        const script = document.createElement('script');
        script.src = `https://webapi.amap.com/maps?v=2.0&key=${this.amapKey}&plugin=AMap.AutoComplete,AMap.PlaceSearch,AMap.Geocoder,AMap.ToolBar,AMap.MouseTool`;
        
        script.onload = () => {
          console.log('✅ 高德地图API加载成功');
          this.isLoadingMap = false;
          this.createMapInstance();
        };
        
        script.onerror = () => {
          console.error('❌ 高德地图API加载失败');
          this.isLoadingMap = false;
          this.$message.error('地图加载失败，请检查网络连接或稍后重试');
        };
        
        document.head.appendChild(script);
      } else {
        console.log('✅ AMap已存在，检查插件...');
        
        // 检查必要的插件是否已加载
        if (!window.AMap.MouseTool) {
          console.warn('⚠️ MouseTool插件未加载，尝试重新加载地图API...');
          
          // 移除旧的script标签
          const oldScripts = document.querySelectorAll('script[src*="webapi.amap.com"]');
          oldScripts.forEach(script => {
            try {
              script.remove();
            } catch (e) {
              console.warn('移除旧script失败:', e);
            }
          });
          
          // 重置AMap
          window.AMap = null;
          
          // 重新加载
          this.isLoadingMap = true;
          const script = document.createElement('script');
          script.src = `https://webapi.amap.com/maps?v=2.0&key=${this.amapKey}&plugin=AMap.AutoComplete,AMap.PlaceSearch,AMap.Geocoder,AMap.ToolBar,AMap.MouseTool`;
          
          script.onload = () => {
            console.log('✅ 重新加载地图API成功');
            this.isLoadingMap = false;
            this.createMapInstance();
          };
          
          script.onerror = () => {
            console.error('❌ 重新加载地图API失败');
            this.isLoadingMap = false;
            this.$message.error('地图加载失败');
          };
          
          document.head.appendChild(script);
        } else {
          console.log('✅ 插件已存在，直接创建地图实例');
          this.createMapInstance();
        }
      }
    },

    createMapInstance() {
      this.$nextTick(() => {
        try {
          // 检查AMap是否可用
          if (!window.AMap) {
            console.error('❌ AMap对象不存在');
            this.$message.error('地图API未加载，请刷新页面重试');
            return;
          }
          
          // 检查容器是否存在
          const container = document.getElementById('selector-container');
          if (!container) {
            console.error('❌ 地图容器不存在，延迟重试...');
            setTimeout(() => this.createMapInstance(), 200);
            return;
          }
          
          // 销毁旧地图实例
          if (this.map) {
            try {
              this.map.destroy();
              this.map = null;
            } catch (e) {
              console.warn('销毁旧地图实例时出错:', e);
            }
          }
          
          console.log('🗺️ 开始创建地图实例...');
          
          // 创建新地图实例
          this.map = new window.AMap.Map('selector-container', {
            zoom: mapConfig.amap.defaultZoom,
            center: mapConfig.amap.defaultCenter,  // 使用配置中的默认中心坐标
            viewMode: '2D',
            resizeEnable: true
          });

          console.log('✅ 地图对象创建成功');

          // 添加工具栏
          this.map.addControl(new window.AMap.ToolBar());
          
          // 初始化插件
          this.initPlugins();
          
          // 初始化绘图工具
          this.initMouseTool();

          // 数据回显逻辑
          this.recoverData();

          // 绑定点击事件（仅在非绘制模式下生效）
          this.map.on('click', (e) => {
            if (!this.isDrawing) {
              this.regeoCode([e.lnglat.getLng(), e.lnglat.getLat()]);
            }
          });
          
          console.log('✅ 地图实例创建成功');
        } catch (error) {
          console.error('❌ 创建地图实例失败:', error);
          console.error('错误详情:', error.message);
          console.error('错误堆栈:', error.stack);
          this.$message.error('地图初始化失败: ' + error.message);
        }
      });
    },

    initPlugins() {
      try {
        const AMap = window.AMap;
        
        // 检查必要的插件是否可用
        if (!AMap.AutoComplete || !AMap.PlaceSearch || !AMap.Geocoder) {
          console.error('❌ 地图插件未完全加载');
          this.$message.warning('部分地图功能可能不可用');
          return;
        }
        
        // 初始化自动完成插件，指定城市为张家界
        this.autoComplete = new AMap.AutoComplete({ 
          input: 'amap-search-input',
          city: '张家界',
          citylimit: false  // 不限制只在城市内搜索
        });
        
        this.placeSearch = new AMap.PlaceSearch({ 
          map: this.map, 
          autoFitView: true,
          city: '张家界'
        });
        
        this.geocoder = new AMap.Geocoder({ 
          radius: 1000, 
          extensions: 'all' 
        });

        // 监听选择事件
        this.autoComplete.on('select', (e) => {
          console.log('📍 选择了地点:', e.poi);
          this.searchKeyword = e.poi.name;
          
          // 如果有坐标，直接设置
          if (e.poi.location) {
            const lng = e.poi.location.lng;
            const lat = e.poi.location.lat;
            
            // 设置地图中心并添加标记
            this.map.setCenter([lng, lat]);
            this.map.setZoom(16);
            this.addMarker([lng, lat]);
            
            // 使用逆地理编码获取完整地址和区县信息
            this.geocoder.getAddress([lng, lat], (status, result) => {
              if (status === 'complete' && result.regeocode) {
                const addressComponent = result.regeocode.addressComponent;
                const formattedAddress = result.regeocode.formattedAddress;
                
                console.log('📍 选点逆地理编码:', addressComponent);
                
                this.selectedLocation = {
                  lng: lng,
                  lat: lat,
                  address: formattedAddress
                };
                
                // 优先使用addressComponent中的区县信息
                if (addressComponent.district) {
                  const fullDistrict = (addressComponent.city || '') + addressComponent.district;
                  this.calculatedDistrict = fullDistrict;
                  console.log('✅ 选点获取区县:', fullDistrict);
                } else if (e.poi.district) {
                  // 如果逆地理编码没有district，使用POI的district
                  this.extractAndSetDistrict(e.poi.district + e.poi.name);
                }
              } else {
                // 逆地理编码失败，使用POI信息
                this.selectedLocation = {
                  lng: lng,
                  lat: lat,
                  address: e.poi.name + (e.poi.address ? ', ' + e.poi.address : '')
                };
                
                if (e.poi.district) {
                  this.extractAndSetDistrict(e.poi.district + e.poi.name + (e.poi.address || ''));
                }
              }
            });
          } else {
            // 如果没有坐标，使用搜索
            this.doSearch(e.poi.name);
          }
        });
        
        console.log('✅ 地图插件初始化成功');
      } catch (error) {
        console.error('❌ 初始化插件失败:', error);
        this.$message.warning('地图搜索功能初始化失败');
      }
    },

    // 初始化鼠标绘制工具
    initMouseTool() {
      try {
        console.log('🖌️ 初始化绘图工具...');
        
        // 检查AMap是否存在
        if (!window.AMap) {
          console.error('❌ AMap对象不存在');
          this.$message.error('地图未加载，请关闭对话框重试');
          return;
        }
        
        // 检查MouseTool插件
        if (!window.AMap.MouseTool) {
          console.error('❌ MouseTool插件未加载');
          console.error('当前AMap对象:', window.AMap);
          console.error('可用插件:', Object.keys(window.AMap).filter(k => k.startsWith('A')));
          this.$message.error('绘图功能不可用，请关闭对话框重新打开');
          return;
        }
        
        console.log('✅ MouseTool插件已加载，创建实例...');
        this.mouseTool = new window.AMap.MouseTool(this.map);
      
        // 监听绘制结束事件
        this.mouseTool.on('draw', (e) => {
          this.isDrawing = false;
          // 清除旧的覆盖物（只保留这一个）
          if (this.overlay) {
            this.map.remove(this.overlay);
          }
          this.overlay = e.obj; // 保存当前画的对象
          
          // 获取路径坐标
          const path = e.obj.getPath(); 
          this.currentPath = path.map(point => ({
            lng: point.lng,
            lat: point.lat
          }));
          
          // 计算多边形面积（平方米）
          const areaInSquareMeters = window.AMap.GeometryUtil.ringArea(path);
          // 转换为亩（1亩 = 666.67平方米）
          const areaInMu = (areaInSquareMeters / 666.67).toFixed(1);
          
          // 保存计算的面积
          this.calculatedArea = parseFloat(areaInMu);
          
          // 绘制完成后，使用中心点进行逆地理编码，获取完整地址和区县信息
          const center = e.obj.getBounds().getCenter();
          console.log('📍 绘制区域完成，中心点:', center.lng, center.lat);
          
          // 使用逆地理编码获取详细地址
          this.geocoder.getAddress([center.lng, center.lat], (status, result) => {
            if (status === 'complete' && result.regeocode) {
              const addressComponent = result.regeocode.addressComponent;
              const formattedAddress = result.regeocode.formattedAddress;
              
              console.log('📍 逆地理编码结果:', addressComponent);
              console.log('📍 完整地址:', formattedAddress);
              
              // 更新选中的位置信息
              if (!this.selectedLocation.lng) {
                this.selectedLocation = {
                  lng: center.lng,
                  lat: center.lat,
                  address: formattedAddress
                };
              }
              
              // 优先使用addressComponent中的区县信息
              if (addressComponent.district) {
                // 拼接完整的区县名称：市 + 区县
                const fullDistrict = (addressComponent.city || '') + addressComponent.district;
                this.calculatedDistrict = fullDistrict;
                console.log('✅ 从逆地理编码获取区县:', fullDistrict);
              } else {
                // 如果没有district，尝试从完整地址提取
                this.extractAndSetDistrict(formattedAddress);
              }
            } else {
              console.warn('⚠️ 逆地理编码失败');
              // 如果已有地址，尝试提取区县
              if (this.selectedLocation.address) {
                this.extractAndSetDistrict(this.selectedLocation.address);
              }
            }
          });
          
          this.$message.success(`区域绘制完成！面积约 ${areaInMu} 亩`);
          
          // 触发区县信息更新事件
          this.$emit('district-updated', this.calculatedDistrict);
        });
        
        console.log('✅ 绘图工具初始化成功');
      } catch (error) {
        console.error('❌ 初始化绘图工具失败:', error);
        this.$message.warning('地图绘制功能初始化失败');
      }
    },

    // 回显数据（如果有历史数据）
    recoverData() {
      // 1. 回显点
      if (this.initialData.centerLng) {
        const pos = [this.initialData.centerLng, this.initialData.centerLat];
        this.map.setCenter(pos);
        this.addMarker(pos);
        this.selectedLocation = {
          lng: this.initialData.centerLng,
          lat: this.initialData.centerLat,
          address: this.initialData.address
        };
        
        // 回显计算字段（如果存在）
        if (this.initialData.area) {
          this.calculatedArea = this.initialData.area;
        }
        if (this.initialData.district) {
          this.calculatedDistrict = this.initialData.district;
        }
      }
      
      // 2. 回显区域 (多边形)
      if (this.initialData.coordinates) {
        try {
          const path = JSON.parse(this.initialData.coordinates);
          if (Array.isArray(path) && path.length > 0) {
            const polygonPath = path.map(p => [p.lng, p.lat]);
            
            this.overlay = new window.AMap.Polygon({
              path: polygonPath,
              strokeColor: "#10B981",  // 绿色边框
              strokeWeight: 3,
              strokeOpacity: 0.9,
              fillOpacity: 0.25,
              fillColor: '#10B981',  // 绿色填充
              zIndex: 50,
            });
            this.map.add(this.overlay);
            // 缩放地图以适应多边形
            this.map.setFitView([this.overlay]);
            this.currentPath = path;
          }
        } catch (e) {
          console.error('坐标解析失败', e);
        }
      }
    },

    // --- 按钮操作 ---
    
    startDraw() {
      if (this.overlay) {
        this.map.remove(this.overlay); // 重新绘制前清除旧的
        this.overlay = null;
      }
      this.isDrawing = true;
      // 开启多边形绘制模式
      this.mouseTool.polygon({
        strokeColor: "#10B981",  // 绿色边框
        strokeOpacity: 0.9,
        strokeWeight: 3,
        strokeStyle: 'solid',
        fillColor: "#10B981",  // 绿色填充
        fillOpacity: 0.3
      });
    },
    
    clearDraw() {
      // 确认提示
      this.$confirm('确定要清除已绘制的区域吗？', '清除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 清除多边形覆盖物
        if (this.overlay) {
          this.map.remove(this.overlay);
          this.overlay = null;
        }
        // 关闭绘图工具并清除临时图形
        this.mouseTool.close(true);
        // 清空路径数据
        this.currentPath = [];
        this.isDrawing = false;
        
        this.$message.success('已清除绘制区域');
      }).catch(() => {
        // 用户取消操作
      });
    },

    // 处理输入变化
    handleInputChange() {
      // 清除之前的定时器
      if (this.inputTimer) {
        clearTimeout(this.inputTimer);
      }
      
      // 如果输入为空或小于2个字符，隐藏提示
      if (!this.searchKeyword || this.searchKeyword.trim().length < 2) {
        this.showTips = false;
        this.inputTips = [];
        return;
      }
      
      // 防抖处理，200ms后执行
      this.inputTimer = setTimeout(() => {
        this.getInputTips();
      }, 200);
    },
    
    // 获取输入提示
    async getInputTips() {
      try {
        console.log('🔍 开始获取输入提示，关键词:', this.searchKeyword);
        
        // 调用后端代理接口
        const response = await this.request.get('/amap/inputtips', {
          params: {
            keywords: this.searchKeyword,
            city: '张家界'
          }
        });
        
        console.log('📥 后端返回:', response);
        
        if (response && response.status === '1' && response.tips) {
          // 过滤掉没有坐标的结果
          this.inputTips = response.tips.filter(tip => {
            // 确保有location字段且不为空
            return tip.location && tip.location !== '';
          });
          
          this.showTips = this.inputTips.length > 0;
          console.log('✅ 获取到', this.inputTips.length, '条有效提示');
        } else {
          console.warn('⚠️ 未获取到有效提示');
          this.showTips = false;
          this.inputTips = [];
        }
      } catch (error) {
        console.error('❌ 获取输入提示失败:', error);
        this.showTips = false;
        this.inputTips = [];
      }
    },
    
    // 原来的handleInputTips方法可以删除或保留作为备用
    handleInputTips() {
      // 清除之前的定时器
      if (this.inputTimer) {
        clearTimeout(this.inputTimer);
      }
      
      // 如果输入为空，隐藏提示
      if (!this.searchKeyword || this.searchKeyword.trim().length < 1) {
        this.showTips = false;
        this.inputTips = [];
        return;
      }
      
      // 防抖处理，100ms后执行（更快响应）
      this.inputTimer = setTimeout(() => {
        this.fetchInputTips();
      }, 100);
    },
    
    // 调用高德输入提示API
    async fetchInputTips() {
      console.log('🔍 开始获取输入提示，关键词:', this.searchKeyword);
      
      try {
        const params = {
          keywords: this.searchKeyword,
          city: '张家界', 
          citylimit: 'false',
          datatype: 'all'
        };
        
        console.log('📡 请求参数:', params);
        
        // 使用后端代理请求（需要确保SpringBoot已重启）
        const response = await this.request.get('/amap/inputtips', { params });
        
        console.log('📥 响应数据:', response);
        
        if (response && response.tips && Array.isArray(response.tips)) {
          // 过滤掉没有坐标的结果
          this.inputTips = response.tips.filter(tip => tip.location && tip.location.length > 0);
          this.showTips = this.inputTips.length > 0;
          console.log('✅ 获取输入提示成功:', this.inputTips.length, '条结果');
          
          // 如果没有结果，显示提示
          if (this.inputTips.length === 0) {
            console.log('⚠️ 没有找到有坐标的地点');
          }
        } else {
          console.warn('⚠️ 响应格式不正确:', response);
          this.showTips = false;
        }
      } catch (error) {
        console.error('❌ 获取输入提示失败:', error.message);
        console.error('错误详情:', error);
        
        // 如果是网络错误，提示用户
        if (error.message && error.message.includes('404')) {
          this.$message.warning('提示接口未找到，请重启后端服务');
        } else if (error.message && error.message.includes('Network')) {
          this.$message.warning('网络错误，请检查后端服务是否运行');
        }
        
        // 降级使用原有的搜索功能
        this.fallbackSearch();
      }
    },
    
    // 选择提示项
    selectTip(tip) {
      this.searchKeyword = tip.name;
      this.showTips = false;
      
      // 如果有坐标信息
      if (tip.location) {
        const coords = tip.location.split(',');
        if (coords.length === 2) {
          const lng = parseFloat(coords[0]);
          const lat = parseFloat(coords[1]);
          
          this.selectedLocation = {
            lng: lng,
            lat: lat,
            address: tip.name + (tip.address ? ', ' + tip.address : '')
          };
          
          // 设置地图中心并添加标记
          this.map.setCenter([lng, lat]);
          this.map.setZoom(16);
          this.addMarker([lng, lat]);
          
          // 自动提取区县信息
          const fullAddress = tip.district + tip.name + tip.address;
          this.extractAndSetDistrict(fullAddress);
        }
      }
    },
    
    // 降级搜索（使用原有方法）
    fallbackSearch() {
      if (this.placeSearch && this.searchKeyword) {
        this.doSearch(this.searchKeyword);
      }
    },
    
    handleSearchBtn() {
      this.showTips = false; // 隐藏提示
      if (!this.searchKeyword) return;
      
      // 如果有选中的提示项，使用它
      if (this.inputTips.length > 0) {
        const firstTip = this.inputTips[0];
        this.selectTip(firstTip);
      } else {
        this.doSearch(this.searchKeyword);
      }
    },

    doSearch(keyword) {
      if (this.placeSearch) {
        this.placeSearch.search(keyword, (status, result) => {
          if (status === 'complete' && result.poiList && result.poiList.pois.length > 0) {
            const poi = result.poiList.pois[0];
            this.selectedLocation = {
              lng: poi.location.lng,
              lat: poi.location.lat,
              address: poi.name
            };
            // 如果没有在画图，才自动加个标记
            if (!this.overlay) {
               this.addMarker([poi.location.lng, poi.location.lat]);
            }
          }
        });
      }
    },

    regeoCode(lnglat) {
      this.addMarker(lnglat);
      this.geocoder.getAddress(lnglat, (status, result) => {
        if (status === 'complete' && result.regeocode) {
          const address = result.regeocode.formattedAddress;
          this.selectedLocation = {
            lng: lnglat[0],
            lat: lnglat[1],
            address: address
          };
          
          // 自动提取区县信息
          this.extractAndSetDistrict(address);
          console.log('📍 逆地理编码完成，地址:', address, '区县:', this.calculatedDistrict);
        }
      });
    },

    addMarker(position) {
      if (this.marker) this.map.remove(this.marker);
      
      // 创建绿色主题的自定义标记
      const icon = new window.AMap.Icon({
        size: new window.AMap.Size(40, 50),
        image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNTAiIHZpZXdCb3g9IjAgMCA0MCA1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImdyYWQiIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojMTBCOTgxO3N0b3Atb3BhY2l0eToxIiAvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3R5bGU9InN0b3AtY29sb3I6IzA1OTY2OTtzdG9wLW9wYWNpdHk6MSIgLz48L2xpbmVhckdyYWRpZW50PjwvZGVmcz48cGF0aCBkPSJNMjAgMEMxMSAwIDQgNyA0IDE2YzAgMTAgMTYgMzQgMTYgMzRzMTYtMjQgMTYtMzRjMC05LTctMTYtMTYtMTZ6IiBmaWxsPSJ1cmwoI2dyYWQpIi8+PGNpcmNsZSBjeD0iMjAiIGN5PSIxNiIgcj0iNiIgZmlsbD0id2hpdGUiLz48L3N2Zz4=',
        imageSize: new window.AMap.Size(40, 50)
      });
      
      this.marker = new window.AMap.Marker({ 
        position,
        icon: icon,
        offset: new window.AMap.Pixel(-20, -50),
        animation: 'AMAP_ANIMATION_DROP'  // 添加掉落动画
      });
      this.map.add(this.marker);
    },

    confirmSelection() {
      // 1. 检查是否选择了点
      if (!this.selectedLocation.lng) {
        this.$message.warning('请至少在地图上选择一个中心定位点');
        return;
      }
      
      console.log('📋 准备确认选择');
      console.log('  - 已计算区县:', this.calculatedDistrict);
      console.log('  - 地址:', this.selectedLocation.address);
      
      // 如果没有区县信息，尝试从地址提取
      if (!this.calculatedDistrict && this.selectedLocation.address) {
        console.log('⚠️ 没有区县信息，尝试从地址提取');
        this.extractAndSetDistrict(this.selectedLocation.address);
      }
      
      // 2. 准备返回数据
      const resultData = {
        centerLng: this.selectedLocation.lng,
        centerLat: this.selectedLocation.lat,
        address: this.selectedLocation.address,
        coordinates: '', // 默认为空
        area: this.calculatedArea || 0, // 添加计算的面积
        district: this.calculatedDistrict || '' // 添加计算的区县信息
      };
      
      console.log('✅ 返回数据:', resultData);
      
      // 3. 如果画了区域，把区域坐标转成 JSON 字符串
      if (this.currentPath.length > 0) {
        resultData.coordinates = JSON.stringify(this.currentPath);
      } else {
        // 如果没画区域，自动生成一个默认的小矩形（兼容旧逻辑）
        const offset = 0.002;
        const coords = [
           {lng: this.selectedLocation.lng - offset, lat: this.selectedLocation.lat - offset},
           {lng: this.selectedLocation.lng + offset, lat: this.selectedLocation.lat - offset},
           {lng: this.selectedLocation.lng + offset, lat: this.selectedLocation.lat + offset},
           {lng: this.selectedLocation.lng - offset, lat: this.selectedLocation.lat + offset}
        ];
        resultData.coordinates = JSON.stringify(coords);
        
        // 计算默认矩形的面积
        if (window.AMap && window.AMap.GeometryUtil) {
          const path = coords.map(coord => new window.AMap.LngLat(coord.lng, coord.lat));
          const areaInSquareMeters = window.AMap.GeometryUtil.ringArea(path);
          resultData.area = (areaInSquareMeters / 666.67).toFixed(1);
        }
      }
      
      this.$emit('confirm', resultData);
      this.dialogVisible = false;
    },

    // 从地址中提取区县信息（参考Django版本的逻辑）
    extractAndSetDistrict(address) {
      console.log('🔍 开始提取区县，地址:', address);
      
      if (!address) {
        this.calculatedDistrict = '';
        return;
      }
      
      // 1. 对于学校、景点等POI，优先使用特殊映射
      const poiMapping = {
        '张家界学院': '张家界市永定区',
        '吉首大学张家界': '张家界市永定区',
        '七十二奇楼景区': '张家界市永定区',  // 新增
        '天门山': '张家界市永定区',
        '武陵源风景区': '张家界市武陵源区',
        '张家界国家森林公园': '张家界市武陵源区',
        '湖南中医药大学': '长沙市岳麓区',
        '中南大学': '长沙市岳麓区',
        '湖南大学': '长沙市岳麓区',
        '湖南师范大学': '长沙市岳麓区'
      };
      
      // 特殊道路映射（根据道路名判断区县）
      const roadMapping = {
        '武陵山大道': '张家界市永定区',
        '官黎路': '张家界市永定区',
        '大庸桥': '张家界市永定区',
        '金鞭路': '张家界市武陵源区'
      };
      
      // 检查是否包含学校、景点等关键词
      if (address.includes('学院') || address.includes('大学') || address.includes('学校') || 
          address.includes('景点') || address.includes('公园') || address.includes('广场') ||
          address.includes('景区') || address.includes('山')) {
        for (const [poi, district] of Object.entries(poiMapping)) {
          if (address.includes(poi)) {
            this.calculatedDistrict = district;
            console.log('✅ POI识别区县:', district);
            return;
          }
        }
      }
      
      // 检查是否包含特殊道路
      for (const [road, district] of Object.entries(roadMapping)) {
        if (address.includes(road)) {
          this.calculatedDistrict = district;
          console.log('✅ 道路识别区县:', district);
          return;
        }
      }
      
      // 2. 优先匹配完整的"市+区县"格式
      const fullMatch = address.match(/([\u4e00-\u9fa5]{2,4}市[\u4e00-\u9fa5]{2,3}(?:区|县))/);
      if (fullMatch) {
        this.calculatedDistrict = fullMatch[1];
        console.log('✅ 完整格式识别区县:', fullMatch[1]);
        return;
      }
      
      // 3. 组合匹配：市+区县
      const cityDistrictMatch = address.match(/([\u4e00-\u9fa5]{2,4}市)([\u4e00-\u9fa5]{2,3}(?:区|县))/);
      if (cityDistrictMatch) {
        this.calculatedDistrict = cityDistrictMatch[1] + cityDistrictMatch[2];
        console.log('✅ 组合识别区县:', this.calculatedDistrict);
        return;
      }
      
      // 4. 张家界市下辖区县映射
      const zjjDistrictMapping = {
        '永定区': '张家界市永定区',
        '武陵源区': '张家界市武陵源区',
        '慈利县': '张家界市慈利县',
        '桑植县': '张家界市桑植县'
      };
      
      // 5. 长沙市区县映射
      const csDistrictMapping = {
        '岳麓区': '长沙市岳麓区',
        '芙蓉区': '长沙市芙蓉区',
        '天心区': '长沙市天心区',
        '开福区': '长沙市开福区',
        '雨花区': '长沙市雨花区',
        '望城区': '长沙市望城区'
      };
      
      // 先检查张家界区县
      for (const [district, fullDistrict] of Object.entries(zjjDistrictMapping)) {
        if (address.includes(district)) {
          this.calculatedDistrict = fullDistrict;
          console.log('✅ 张家界区县映射:', fullDistrict);
          return;
        }
      }
      
      // 再检查长沙区县
      for (const [district, fullDistrict] of Object.entries(csDistrictMapping)) {
        if (address.includes(district)) {
          this.calculatedDistrict = fullDistrict;
          console.log('✅ 长沙区县映射:', fullDistrict);
          return;
        }
      }
      
      // 6. 包含"张家界"但没匹配到具体区县的，默认永定区
      if (address.includes('张家界')) {
        this.calculatedDistrict = '张家界市永定区';
        console.log('✅ 张家界默认区县: 张家界市永定区');
        return;
      }
      
      // 7. 包含"长沙"但没匹配到具体区县的，默认岳麓区
      if (address.includes('长沙')) {
        this.calculatedDistrict = '长沙市岳麓区';
        console.log('✅ 长沙默认区县: 长沙市岳麓区');
        return;
      }
      
      this.calculatedDistrict = '未知区县';
      console.log('⚠️ 无法识别区县，设为: 未知区县');
    }
  }
}
</script>

<style scoped>
/* 内容区域 */
.location-selector-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* 顶部提示文字 */
.tips-text {
  color: #67C23A;
  font-size: 14px;
  padding: 5px 0;
}

/* 搜索区域 */
.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-label {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.search-input-wrapper {
  flex: 1;
  position: relative;
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #67C23A;
}

.search-input::placeholder {
  color: #c0c4cc;
}

.search-btn {
  height: 40px;
  padding: 0 20px;
  background: #67C23A;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #85ce61;
}

/* 搜索提示下拉框 */
.search-tips-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 2px;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  max-height: 300px;
  overflow-y: auto;
  z-index: 2000;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  padding: 10px 15px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.tip-item:hover {
  background: #f5f7fa;
}

.tip-item:last-child {
  border-bottom: none;
}

.tip-item i {
  color: #67C23A;
  margin-right: 10px;
  margin-top: 3px;
}

.tip-content {
  flex: 1;
}

.tip-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 2px;
}

.tip-district {
  font-size: 12px;
  color: #909399;
}

/* 地图区域 */
.map-area {
  width: 100%;
  height: 450px;
  position: relative;
  border: 2px solid #67C23A;
  border-radius: 8px;
  overflow: hidden;
}

.map-content {
  width: 100%;
  height: 100%;
}

/* 绘制提示 */
.drawing-tip {
  position: absolute;
  top: 15px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #67C23A, #529b2e);
  color: white;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 13px;
  z-index: 1600;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4);
}

/* 底部操作栏 */
.bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid #ebeef5;
  margin-top: 5px;
}

.location-info {
  flex: 1;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 5px;
}

.location-info i {
  color: #67C23A;
}

.placeholder-text {
  color: #909399;
}

.coords-info {
  color: #409EFF;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

/* 按钮样式调整 */
.action-buttons .el-button--success {
  background: #67C23A;
  border-color: #67C23A;
}

.action-buttons .el-button--success:hover {
  background: #85ce61;
  border-color: #85ce61;
}

.action-buttons .el-button--primary {
  background: #409EFF;
  border-color: #409EFF;
}
</style>

<style>
/* 高德地图自动完成下拉框样式 */
.amap-sug-result { 
  z-index: 9999 !important; 
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1) !important;
  max-height: 300px !important;
  overflow-y: auto !important;
}

.amap-sug-item {
  padding: 10px 15px !important;
  cursor: pointer !important;
  transition: background 0.2s !important;
}

.amap-sug-item:hover {
  background: #f5f7fa !important;
}

.amap-sug-item .poi-title {
  font-size: 14px !important;
  color: #303133 !important;
  font-weight: 500 !important;
}

.amap-sug-item .poi-address {
  font-size: 12px !important;
  color: #909399 !important;
  margin-top: 2px !important;
}

/* 对话框样式 */
.map-dialog-new {
  border-radius: 12px !important;
  overflow: hidden;
}

.map-dialog-new .el-dialog__header {
  padding: 20px 25px 15px;
  border-bottom: 1px solid #ebeef5;
}

.map-dialog-new .el-dialog__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.map-dialog-new .el-dialog__body {
  padding: 20px 25px;
}

.map-dialog-new .el-dialog__footer {
  display: none;
}
</style>