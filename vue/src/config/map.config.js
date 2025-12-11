/**
 * 高德地图配置文件
 * 统一管理地图相关配置，避免硬编码
 */

const mapConfig = {
  // 高德地图API配置
  amap: {
    // JavaScript API Key（前端地图使用）
    jsKey: 'a0dc4534ab26be714e94cef345e480aa',
    
    // Web服务API Key（后端REST API使用）- 备用
    webKey: 'ad1347855b35e84080018cf5c811d3e7',
    
    // API版本
    version: '2.0',
    
    // 需要加载的插件
    plugins: [
      'AMap.AutoComplete',
      'AMap.PlaceSearch',
      'AMap.Geocoder',
      'AMap.ToolBar',
      'AMap.MouseTool',
      'AMap.GeometryUtil'
    ],
    
    // 默认城市（张家界）
    defaultCity: '430800',
    
    // 默认中心点坐标（张家界市中心）
    defaultCenter: [110.479, 29.117],
    
    // 默认缩放级别
    defaultZoom: 14
  },
  
  // 区县映射配置
  districtMapping: {
    // 张家界市区县
    zjj: {
      '永定区': '张家界市永定区',
      '武陵源区': '张家界市武陵源区',
      '慈利县': '张家界市慈利县',
      '桑植县': '张家界市桑植县'
    },
    // 长沙市区县
    cs: {
      '岳麓区': '长沙市岳麓区',
      '芙蓉区': '长沙市芙蓉区',
      '天心区': '长沙市天心区',
      '开福区': '长沙市开福区',
      '雨花区': '长沙市雨花区',
      '望城区': '长沙市望城区'
    }
  },
  
  // POI特殊映射
  poiMapping: {
    '张家界学院': '张家界市永定区',
    '吉首大学张家界': '张家界市永定区',
    '湖南中医药大学': '长沙市岳麓区',
    '中南大学': '长沙市岳麓区',
    '湖南大学': '长沙市岳麓区',
    '湖南师范大学': '长沙市岳麓区'
  }
}

export default mapConfig
