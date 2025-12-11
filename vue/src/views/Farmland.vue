<template>
  <div class="farmland-manage">
    <div class="toolbar-card">
      <div class="title-area">
        <div class="main-title"> 农田资产管理</div>
        <div class="sub-title">共管理 {{ total }} 块高标准农田 · 资产数字化率 100%</div>
      </div>
      <div class="action-area">
        <el-input 
          v-model="farm" 
          placeholder=" 搜索农田或作物..." 
          style="width: 240px" 
          prefix-icon="el-icon-search"
          clearable 
          @clear="load"
          @keyup.enter.native="load">
        </el-input>
        <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        <el-button type="success" icon="el-icon-plus" @click="handleAdd">新建地块</el-button>
        <el-button type="warning" icon="el-icon-download" @click="exp">导出报表</el-button>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <el-table 
        :data="tableData" 
        stripe 
        v-loading="loading"
        element-loading-text="正在加载农田数据..."
        style="width: 100%"
        :header-cell-style="{background:'#f8fafa', color:'#606266', fontWeight:'bold'}"
        :empty-text="farm ? '未找到匹配的农田信息' : '暂无农田数据，请点击【新建地块】开始添加'"
        :default-sort="{prop: 'id', order: 'ascending'}"
      >
        <el-table-column prop="id" label="ID" width="60" align="center" sortable></el-table-column>
        
        <el-table-column label="农田信息" width="220">
          <template slot-scope="scope">
            <div class="farm-info-cell">
              <div class="farm-icon">
                <img :src="getCropIcon(scope.row.crop)" class="farm-icon-img" />
              </div>
              <div>
                <div class="farm-name">{{ scope.row.farm }}</div>
                <div class="farm-addr"><i class="el-icon-location-outline"></i> {{ scope.row.address || '位置未录入' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="crop" label="作物" width="100">
           <template slot-scope="scope">
             <el-tag size="small" effect="plain">{{ scope.row.crop }}</el-tag>
           </template>
        </el-table-column>
        
        <el-table-column prop="area" label="面积 (亩)" width="100" align="center">
          <template slot-scope="scope">
            <span style="font-weight: bold; color: #409EFF">{{ scope.row.area }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="keeper" label="负责人" width="120">
           <template slot-scope="scope">
             <div class="keeper-tag">
               <i class="el-icon-user"></i> {{ scope.row.keeper }}
             </div>
           </template>
        </el-table-column>

        <el-table-column label=" AI品质评级" width="180">
          <template slot-scope="scope">
             <el-rate
              :value="getFakeScore(scope.row.id)"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value} 分">
            </el-rate>
          </template>
        </el-table-column>

        <el-table-column label="资产操作" align="center">
          <template slot-scope="scope">
            <el-button 
              type="text" 
              class="trace-btn"
              icon="el-icon-s-ticket" 
              @click="openTrace(scope.row)"
            >生成溯源档案</el-button>
            <el-divider direction="vertical"></el-divider>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" class="del-btn" icon="el-icon-delete" @click="del(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="padding: 20px 0; display: flex; justify-content: flex-end;">
        <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="pageNum"
            :page-sizes="[5, 10, 20]"
            :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total">
        </el-pagination>
      </div>
    </el-card>
    
    <!-- 适量的底部空间 -->
    <div style="height: 50px; opacity: 0; pointer-events: none;"></div>

    <el-dialog title="农田信息录入" :visible.sync="dialogFormVisible" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px" size="medium" :model="form" :rules="rules" ref="farmForm" class="custom-form">
        <el-form-item label="农田名称" prop="farm">
          <el-input v-model="form.farm" placeholder="例如：A1号有机示范田"></el-input>
        </el-form-item>
        <el-form-item label="种植作物" prop="crop">
          <div style="display: flex; flex-wrap: wrap; gap: 10px; align-items: center;">
            <el-tag
              :key="tag"
              v-for="tag in dynamicTags"
              closable
              :disable-transitions="false"
              @close="handleClose(tag)">
              {{tag}}
            </el-tag>
            <el-input
              class="input-new-tag"
              v-if="inputVisible"
              v-model="inputValue"
              ref="saveTagInput"
              size="small"
              style="width: 100px"
              @keyup.enter.native="handleInputConfirm"
              @blur="handleInputConfirm"
            >
            </el-input>
            <el-button v-else class="button-new-tag" size="small" @click="showInput">+ 添加作物</el-button>
          </div>
        </el-form-item>
        <el-form-item label="面积 (亩)" prop="area">
          <el-input-number v-model="form.area" :min="0.1" :max="10000" :precision="1" placeholder="通过绘制区域自动计算" readonly></el-input-number>
        </el-form-item>
        <el-form-item label="具体地址">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="地址可通过地图自动获取" @blur="extractDistrictFromAddress"></el-input>
        </el-form-item>
        <el-form-item label="所属区县">
          <el-input v-model="form.district" placeholder="将从地址自动识别" readonly></el-input>
        </el-form-item>
        <el-form-item label="负责人" prop="keeper">
          <el-input v-model="form.keeper" placeholder="例如：张三"></el-input>
        </el-form-item>
        
        <el-form-item label="地理位置">
          <el-button 
            type="success" 
            plain
            icon="el-icon-map-location" 
            @click="openLocationSelector"
            size="small"
          >
            {{ form.centerLng ? '已定位 (点击修改)' : '在地图上标记' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="save">确 定</el-button>
      </div>
    </el-dialog>

    <farm-location-selector
      :visible.sync="locationSelectorVisible"
      :initial-data="form"
      @confirm="handleLocationConfirm"
    />

    <el-dialog 
      :visible.sync="traceVisible" 
      width="420px" 
      custom-class="mobile-preview-dialog"
      :show-close="false"
      top="2vh"
      append-to-body
    >
      <div class="mobile-shell" v-if="currentTraceRow">
        <div class="mobile-notch"></div>
        <div class="status-bar">
          <span class="time">12:30</span>
          <div class="icons">
            <i class="el-icon-connection"></i>
            <i class="el-icon-full-screen"></i>
            <i class="el-icon-s-battery"></i>
          </div>
        </div>
        
        <div class="h5-content">
          
          <div class="h5-hero">
            <div class="hero-bg"></div>
            <div class="crop-avatar-box">
              <img :src="getCropIcon(currentTraceRow.crop)" class="crop-emoji-img" />
            </div>
            <div class="hero-info">
              <div class="crop-name">{{ currentTraceRow.crop }} <span class="tag-organic">有机认证</span></div>
              <div class="farm-name"><i class="el-icon-location-outline"></i> {{ currentTraceRow.farm }}</div>
            </div>
          </div>

          <div class="h5-card ai-score-card">
            <div class="score-header">
              <span class="brand"> AI 智脑认证</span>
              <span class="date">{{ new Date().toLocaleDateString() }}</span>
            </div>
            <div class="score-body">
              <div class="score-left">
                <div class="big-score">{{ traceData.score }}</div>
                <div class="score-label">综合品质分</div>
              </div>
              <div class="score-right">
                <div class="level-badge">{{ traceData.level }}</div>
                <div class="hash-code">Hash: 0x7f...3a9</div>
              </div>
            </div>
            <div class="ai-comment">
              “{{ traceData.desc }}”
            </div>
          </div>

          <div class="h5-card env-data-card">
            <div class="card-title">生长环境报告</div>
            <div class="env-grid">
              <div class="env-item">
                <div class="icon sun"><i class="el-icon-sunny"></i></div>
                <div class="val">{{ traceData.sunlight || 1200 }}h</div>
                <div class="lbl">累计光照</div>
              </div>
              <div class="env-item">
                <div class="icon water"><i class="el-icon-heavy-rain"></i></div>
                <div class="val">{{ traceData.waterCount || 45 }}次</div>
                <div class="lbl">AI 智控灌溉</div>
              </div>
              <div class="env-item">
                <div class="icon temp"><i class="el-icon-stopwatch"></i></div>
                <div class="val">24.5°C</div>
                <div class="lbl">平均积温</div>
              </div>
            </div>
          </div>

          <div class="h5-card timeline-card">
            <div class="card-title">全生命周期履历</div>
            <div class="custom-timeline">
              <div class="timeline-item" v-for="(item, index) in traceData.timeline" :key="index">
                <div class="tl-left">
                  <div class="tl-date">{{ item.date.split('-')[0] }}/{{ item.date.split('-')[1] }}</div>
                </div>
                <div class="tl-dot" :style="{borderColor: item.color}"></div>
                <div class="tl-right">
                  <div class="tl-title">{{ item.title }}</div>
                  <div class="tl-desc">{{ item.desc }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="security-footer">
            <img :src="`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Trace_${currentTraceRow.id}`" class="qr-code" />
            <p>扫码查验区块链存证</p>
            <div class="blockchain-id">Block Height: #12,450,392</div>
          </div>

        </div>

        <div class="mobile-tabbar">
          <div class="tab-btn"><i class="el-icon-share"></i> 分享</div>
          <div class="tab-btn primary" @click="traceVisible = false">关闭预览</div>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import FarmLocationSelector from '@/components/FarmLocationSelector.vue'

export default {
  name: "Farmland",
  components: {
    FarmLocationSelector
  },
  data() {
    return {
      tableData: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      farm: "",
      form: {},
      dialogFormVisible: false,
      locationSelectorVisible: false,
      loading: false, // 加载状态
      user: localStorage.getItem("user") ? JSON.parse(localStorage.getItem("user")) : {},
      
      // 表单验证规则
      rules: {
        farm: [
          { required: true, message: '请输入农田名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        crop: [
          { required: true, message: '请输入种植作物', trigger: 'blur' }
        ],
        area: [
          { required: true, message: '请输入面积', trigger: 'change' },
          { type: 'number', min: 0.1, max: 10000, message: '面积需在 0.1-10000 亩之间', trigger: 'change' }
        ],
        keeper: [
          { required: true, message: '请输入负责人', trigger: 'blur' }
        ]
      },
      
      // 溯源弹窗相关数据
      traceVisible: false, 
      currentTraceRow: null,
      traceData: {
        score: 98,
        level: 'S级',
        sunlight: 1200,
        waterCount: 45,
        desc: '',
        timeline: []
      },
      
      // Tags输入控制
      dynamicTags: [],
      inputVisible: false,
      inputValue: ''
    }
  },
  created() {
    this.load();
  },
  methods: {
    // Tags 处理方法
    handleClose(tag) {
      this.dynamicTags.splice(this.dynamicTags.indexOf(tag), 1);
    },

    showInput() {
      this.inputVisible = true;
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },

    handleInputConfirm() {
      let inputValue = this.inputValue;
      if (inputValue) {
        this.dynamicTags.push(inputValue);
      }
      this.inputVisible = false;
      this.inputValue = '';
    },

    load() {
      this.loading = true
      this.request.get("/statistic/page", {
        params: {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          farm: this.farm,
        }
      }).then(res => {
        if(res.data) {
          this.tableData = res.data.records
          this.total = res.data.total
        }
      }).catch(err => {
        this.$message.error('加载数据失败：' + (err.message || '未知错误'))
      }).finally(() => {
        this.loading = false
      })
    },

    // --- 核心逻辑：动态生成溯源数据 ---
    openTrace(row) {
      this.currentTraceRow = row;
      
      // 1. 动态计算评分 (模拟)
      const baseScore = 95;
      const fluctuating = (row.id % 5); 
      this.traceData.score = baseScore - fluctuating;
      this.traceData.level = this.traceData.score > 90 ? 'S级特优' : 'A级优选';
      
      // 2. 模拟IoT累计数据
      this.traceData.sunlight = 1000 + (row.id * 50); 
      this.traceData.waterCount = 20 + (row.id % 10); 

      // 3. 动态生成 AI 评语
      this.traceData.desc = `该批次 ${row.crop} 种植于 ${row.address || '标准示范区'}。全周期生长环境数据完整，AI模型判定其成熟度完美。`;

      // 4. 生成时间轴
      this.traceData.timeline = this.generateTimeline(row.crop);
      
      this.traceVisible = true;
      this.$message.success('🔗 区块链溯源数据校验通过');
    },

    generateTimeline(crop) {
      const today = new Date();
      const dateStr = (offset) => {
        const d = new Date();
        d.setDate(today.getDate() - offset);
        return `${d.getMonth()+1}-${d.getDate()}`;
      };

      if (crop && (crop.includes('果') || crop.includes('莓'))) {
        return [
          { color: '#67C23A', date: dateStr(90), title: '智能移栽', desc: '完成定植，成活率 99%' },
          { color: '#409EFF', date: dateStr(60), title: '花期管控', desc: 'AI 控温 25°C，蜜蜂授粉' },
          { color: '#E6A23C', date: dateStr(15), title: '糖度检测', desc: '抽样检测糖度达 12%' },
          { color: '#67C23A', date: '刚刚', title: '数字建档', desc: '生成溯源码，准备上市' }
        ];
      } else {
        return [
          { color: '#67C23A', date: dateStr(45), title: '种苗播种', desc: '基质检测合格' },
          { color: '#409EFF', date: dateStr(20), title: '水肥一体化', desc: '智能灌溉系统介入' },
          { color: '#67C23A', date: '刚刚', title: '采摘上市', desc: '完成农残快速检测' }
        ];
      }
    },

    getFakeScore(id) {
      return 3 + (id % 20) / 10;
    },
    getCropIcon(crop) {
      if (!crop) return require('@/assets/moreng.png');
      if (crop.includes('麦')) return require('@/assets/xiaomai.png');
      if (crop.includes('艾')) return require('@/assets/aiye.png');
      if (crop.includes('竹')) return require('@/assets/zhuzi.png');
      if (crop.includes('玉') || crop.includes('米')) return require('@/assets/yumi.png');
      if (crop.includes('棕')) return require('@/assets/zongshu.png');
      if (crop.includes('柿') || crop.includes('红')) return require('@/assets/xihongshi.png');
      return require('@/assets/moreng.png');
    },

    save() {
      // 将 Tags 转换为逗号分隔字符串
      this.form.crop = this.dynamicTags.join(',');

      // 表单验证
      this.$refs.farmForm.validate((valid) => {
        if (!valid) {
          this.$message.warning('请完善必填信息')
          return false
        }
        
        // 验证通过，提交数据
        this.request.post("/statistic", this.form).then(res => {
          if (res.code === '200') {
            this.$message.success("保存成功")
            this.dialogFormVisible = false
            this.load()
          } else {
            this.$message.error("保存失败：" + (res.msg || '未知错误'))
          }
        }).catch(err => {
          this.$message.error('提交失败：' + (err.message || '网络异常'))
        })
      })
    },
    handleAdd() {
      this.dialogFormVisible = true
      this.form = {}
      this.dynamicTags = [] // 重置 Tags
      // 重置表单验证状态
      this.$nextTick(() => {
        if (this.$refs.farmForm) {
          this.$refs.farmForm.clearValidate()
        }
      })
    },
    handleEdit(row) {
      this.form = JSON.parse(JSON.stringify(row))
      // 将 crop 字符串转换为 Tags 数组
      this.dynamicTags = this.form.crop ? this.form.crop.split(',') : []
      this.dialogFormVisible = true
      // 重置表单验证状态
      this.$nextTick(() => {
        if (this.$refs.farmForm) {
          this.$refs.farmForm.clearValidate()
        }
      })
    },
    del(id) {
      this.$confirm('此操作将永久删除该农田数据，是否继续？', '⚠️ 删除确认', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.request.delete("/statistic/" + id).then(res => {
          if (res.code === '200') {
            this.$message.success("✅ 删除成功")
            this.load()
          } else {
            this.$message.error("删除失败：" + (res.msg || '未知错误'))
          }
        }).catch(err => {
          this.$message.error('删除失败：' + (err.message || '网络异常'))
        })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    exp() {
      this.$message.info('📊 正在生成Excel报表，请稍候...')
      const exportUrl = this.request.defaults.baseURL + '/statistic/export'
      setTimeout(() => {
        window.open(exportUrl)
      }, 300)
    },
    handleSizeChange(pageSize) {
      this.pageSize = pageSize
      this.load()
    },
    handleCurrentChange(pageNum) {
      this.pageNum = pageNum
      this.load()
    },
    openLocationSelector() {
      this.locationSelectorVisible = true
    },
    handleLocationConfirm(locationData) {
      this.form.centerLng = locationData.centerLng
      this.form.centerLat = locationData.centerLat
      this.form.coordinates = locationData.coordinates
      this.form.address = locationData.address || ''  // 自动填充地址
      
      // 自动填充面积
      if (locationData.area && locationData.area > 0) {
        this.form.area = parseFloat(locationData.area)
      }
      
      // 自动填充区县信息（优先使用地图组件计算的结果）
      if (locationData.district) {
        this.$set(this.form, 'district', locationData.district)
        console.log('✅ 使用地图组件计算的区县:', locationData.district)
      } else if (locationData.address) {
        // 如果地图组件没有计算出区县，则手动提取
        this.extractDistrictFromAddress()
      }
      
      // 显示成功消息
      if (locationData.address) {
        const areaText = locationData.area ? `，面积约 ${locationData.area} 亩` : ''
        const districtText = this.form.district ? `，区县：${this.form.district}` : ''
        this.$message.success(`位置已设置：${locationData.address}${areaText}${districtText}`)
      } else {
        this.$message.success('地理位置设置成功')
      }
    },
    
    // 从地址中提取区县信息（参考Django版本的逻辑）
    extractDistrictFromAddress() {
      const address = this.form.address || ''
      
      if (!address) {
        this.$set(this.form, 'district', '')
        return
      }
      
      // 1. 对于学校、景点等POI，优先使用特殊映射（参考Django POI搜索逻辑）
      const poiMapping = {
        '张家界学院': '张家界市永定区',
        '吉首大学张家界': '张家界市永定区',
        '湖南中医药大学': '长沙市岳麓区',
        '中南大学': '长沙市岳麓区',
        '湖南大学': '长沙市岳麓区',
        '湖南师范大学': '长沙市岳麓区'
      }
      
      // 检查是否包含学校、景点等关键词
      if (address.includes('学院') || address.includes('大学') || address.includes('学校') || 
          address.includes('景点') || address.includes('公园') || address.includes('广场')) {
        for (const [poi, district] of Object.entries(poiMapping)) {
          if (address.includes(poi)) {
            this.$set(this.form, 'district', district)
            console.log('✅ POI识别区县:', district)
            return
          }
        }
      }
      
      // 2. 优先匹配完整的"市+区县"格式
      const fullMatch = address.match(/([\u4e00-\u9fa5]{2,4}市[\u4e00-\u9fa5]{2,3}(?:区|县))/)
      
      if (fullMatch) {
        this.$set(this.form, 'district', fullMatch[1])
        console.log('✅ 完整格式识别区县:', fullMatch[1])
        return
      }
      
      // 3. 组合匹配：市+区县
      const cityDistrictMatch = address.match(/([\u4e00-\u9fa5]{2,4}市)([\u4e00-\u9fa5]{2,3}(?:区|县))/)
      
      if (cityDistrictMatch) {
        this.$set(this.form, 'district', cityDistrictMatch[1] + cityDistrictMatch[2])
        console.log('✅ 组合识别区县:', this.form.district)
        return
      }
      
      // ... (后面还有很多，这里只替换前半部分，或者全部替换)

      
      // 4. 张家界市下辖区县映射（参考Django的get_districts接口）
      const zjjDistrictMapping = {
        '永定区': '张家界市永定区',
        '武陵源区': '张家界市武陵源区',
        '慈利县': '张家界市慈利县',
        '桑植县': '张家界市桑植县'
      }
      
      // 5. 长沙市区县映射
      const csDistrictMapping = {
        '岳麓区': '长沙市岳麓区',
        '芙蓉区': '长沙市芙蓉区',
        '天心区': '长沙市天心区',
        '开福区': '长沙市开福区',
        '雨花区': '长沙市雨花区',
        '望城区': '长沙市望城区'
      }
      
      // 先检查张家界区县
      for (const [district, fullDistrict] of Object.entries(zjjDistrictMapping)) {
        if (address.includes(district)) {
          this.$set(this.form, 'district', fullDistrict)
          console.log('✅ 张家界区县映射:', fullDistrict)
          return
        }
      }
      
      // 再检查长沙区县
      for (const [district, fullDistrict] of Object.entries(csDistrictMapping)) {
        if (address.includes(district)) {
          this.$set(this.form, 'district', fullDistrict)
          console.log('✅ 长沙区县映射:', fullDistrict)
          return
        }
      }
      
      // 6. 包含"张家界"但没匹配到具体区县的，默认永定区（参考Django逻辑）
      if (address.includes('张家界')) {
        this.$set(this.form, 'district', '张家界市永定区')
        console.log('✅ 张家界默认区县: 张家界市永定区')
        return
      }
      
      // 7. 包含"长沙"但没匹配到具体区县的，默认岳麓区
      if (address.includes('长沙')) {
        this.$set(this.form, 'district', '长沙市岳麓区')
        console.log('✅ 长沙默认区县: 长沙市岳麓区')
        return
      }
      
      this.$set(this.form, 'district', '未知区县')  // 参考Django版本
      console.log('⚠️ 无法识别区县，设为: 未知区县')
    }
  }
}
</script>

<style scoped>
/* 农田管理页面滚动设置 */
.farmland-manage {
  padding: 20px;
  padding-bottom: 30px;
  background: #f0f2f5;
  min-height: 100vh;
  height: auto;
  max-height: none;
  overflow-y: scroll !important;
  overflow-x: hidden !important;
  position: relative;
  /* 隐藏滚动条 */
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE/Edge */
}

.farmland-manage::-webkit-scrollbar {
  display: none !important; /* Chrome/Safari */
  width: 0 !important;
  height: 0 !important;
}

.farmland-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
  max-height: 100vh;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  /* 隐藏滚动条 */
  scrollbar-width: none !important; /* Firefox */
  -ms-overflow-style: none !important; /* IE/Edge */
}

.farmland-page::-webkit-scrollbar {
  display: none !important; /* Chrome/Safari */
  width: 0 !important;
  height: 0 !important;
}

/* 重复样式已删除，使用上面的主要样式 */

/* 顶部工具栏美化 */
.toolbar-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.02);
}
.main-title { font-size: 20px; font-weight: 800; color: #303133; margin-bottom: 5px; }
.sub-title { font-size: 13px; color: #909399; }
.action-area { display: flex; gap: 10px; }

/* 表格卡片 */
.table-card { border-radius: 12px; border: none; }
::v-deep .table-card .el-card__body { padding: 0; }
.farm-info-cell { display: flex; align-items: center; gap: 10px; }
.farm-icon {
  width: 40px; height: 40px; background: #ecfdf5; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}
.farm-icon-img { width: 32px; height: 32px; object-fit: contain; }
.crop-emoji-img { width: 60px; height: 60px; object-fit: contain; }

.farm-name { font-weight: bold; font-size: 14px; color: #303133; }
.farm-addr { font-size: 12px; color: #909399; margin-top: 2px; }
.keeper-tag { color: #606266; font-size: 13px; }

.trace-btn { color: #d97706; font-weight: bold; }
.trace-btn:hover { color: #d97706; }
.del-btn { color: #f56c6c; }
.del-btn:hover { color: #f56c6c; }

/* 禁用所有按钮浮动和动态效果 */
::v-deep .el-button {
  transition: none !important;
  transform: none !important;
}

::v-deep .el-button:hover {
  transform: none !important;
  box-shadow: inherit !important;
}

::v-deep .el-button:active {
  transform: none !important;
}

::v-deep .el-button:focus {
  transform: none !important;
  box-shadow: none !important;
  outline: none !important;
}

::v-deep .el-button--text:hover {
  background: transparent !important;
  transform: none !important;
}

::v-deep .el-button--primary:hover {
  transform: none !important;
}

::v-deep .el-button--success:hover {
  transform: none !important;
}

::v-deep .el-button--success.is-plain:hover {
  transform: none !important;
}

::v-deep .el-button--warning:hover {
  transform: none !important;
}

/* 确保按钮文字正常显示 */
::v-deep .el-button span {
  color: inherit !important;
  visibility: visible !important;
  opacity: 1 !important;
}

::v-deep .el-button {
  color: inherit !important;
}

::v-deep .el-button--success.is-plain {
  color: #ffffff !important;
  background-color: #67c23a !important;
  border-color: #67c23a !important;
}

::v-deep .el-button--success.is-plain span {
  color: #ffffff !important;
}

/* 顶部工具栏按钮文字颜色设置为白色 */
::v-deep .el-button--primary {
  color: #ffffff !important;
}

::v-deep .el-button--primary span {
  color: #ffffff !important;
}

::v-deep .el-button--success {
  color: #ffffff !important;
}

::v-deep .el-button--success span {
  color: #ffffff !important;
}

::v-deep .el-button--warning {
  color: #ffffff !important;
}

::v-deep .el-button--warning span {
  color: #ffffff !important;
}

::v-deep .el-table__row:hover > td {
  background: inherit !important;
}

::v-deep .el-input:hover .el-input__inner {
  border-color: inherit !important;
}

::v-deep .el-input__inner:focus {
  border-color: inherit !important;
  box-shadow: none !important;
}

/* --- 手机仿真外壳 --- */
::v-deep .mobile-preview-dialog {
  border-radius: 40px; /* 更圆润的边角，像 iPhone */
  background: transparent; 
  box-shadow: none;
  overflow: visible;
}
::v-deep .mobile-preview-dialog .el-dialog__header { display: none; }
::v-deep .mobile-preview-dialog .el-dialog__body { padding: 0; }

.mobile-shell {
  background: #f2f4f8;
  height: 750px; /* 手机高度 */
  border-radius: 40px;
  border: 12px solid #1a1a1a; /* 黑色边框模拟机身 */
  box-shadow: 0 20px 50px rgba(0,0,0,0.3);
  position: relative;
  overflow: hidden;
  display: flex; flex-direction: column;
}

/* 刘海屏 */
.mobile-notch {
  position: absolute; top: 0; left: 50%; transform: translateX(-50%);
  width: 120px; height: 25px; background: #1a1a1a;
  border-radius: 0 0 15px 15px; z-index: 10;
}

.status-bar {
  height: 40px; padding: 10px 20px 0 20px; display: flex; justify-content: space-between; align-items: center;
  color: #333; font-size: 12px; font-weight: bold; z-index: 5;
}

.h5-content {
  flex: 1; overflow-y: auto; padding-bottom: 20px;
  /* 隐藏滚动条 */
  scrollbar-width: none; 
}
.h5-content::-webkit-scrollbar { display: none; }

/* --- 头部 Hero --- */
.h5-hero {
  position: relative; padding: 30px 20px; text-align: center;
  background: linear-gradient(180deg, #e0f7fa 0%, #f2f4f8 100%);
}
.crop-avatar-box {
  width: 80px; height: 80px; background: white; border-radius: 50%; margin: 0 auto 15px;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
  font-size: 40px;
}
.crop-name { font-size: 20px; font-weight: 800; color: #1f2937; display: flex; align-items: center; justify-content: center; gap: 8px; }
.tag-organic { background: #10b981; color: white; font-size: 10px; padding: 2px 6px; border-radius: 4px; }
.farm-name { color: #6b7280; font-size: 12px; margin-top: 5px; }

/* --- 卡片通用 --- */
.h5-card {
  background: white; border-radius: 16px; margin: 0 15px 15px 15px; padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.card-title { font-weight: bold; font-size: 15px; color: #374151; margin-bottom: 15px; border-left: 3px solid #10b981; padding-left: 8px; }

/* AI 评分卡片 */
.ai-score-card {
  background: linear-gradient(135deg, #1f2937, #111827); color: white;
  margin-top: -20px; position: relative; /* 上浮效果 */
}
.score-header { display: flex; justify-content: space-between; font-size: 10px; opacity: 0.7; margin-bottom: 15px; }
.score-body { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.big-score { font-size: 42px; font-weight: 900; color: #fbbf24; line-height: 1; }
.score-label { font-size: 10px; opacity: 0.8; }
.level-badge { background: rgba(255,255,255,0.2); padding: 4px 10px; border-radius: 20px; font-weight: bold; text-align: center; }
.hash-code { font-size: 10px; font-family: monospace; margin-top: 5px; opacity: 0.5; }
.ai-comment { background: rgba(255,255,255,0.1); padding: 10px; border-radius: 8px; font-size: 12px; line-height: 1.4; color: #d1d5db; font-style: italic; }

/* 环境数据网格 */
.env-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.env-item { text-align: center; background: #f9fafb; padding: 10px 0; border-radius: 8px; }
.env-item .icon { font-size: 20px; margin-bottom: 5px; }
.sun { color: #f59e0b; } .water { color: #3b82f6; } .temp { color: #ef4444; }
.env-item .val { font-weight: bold; font-size: 14px; color: #333; }
.env-item .lbl { font-size: 10px; color: #9ca3af; }

/* 自定义时间轴 */
.custom-timeline { padding-left: 10px; }
.timeline-item { display: flex; margin-bottom: 20px; position: relative; }
.timeline-item:last-child { margin-bottom: 0; }
.timeline-item::before { /* 竖线 */
  content: ''; position: absolute; left: 58px; top: 5px; bottom: -25px; width: 2px; background: #e5e7eb; z-index: 0;
}
.timeline-item:last-child::before { display: none; }
.tl-left { width: 50px; text-align: right; font-size: 12px; color: #9ca3af; padding-right: 10px; padding-top: 2px; }
.tl-dot { width: 10px; height: 10px; border-radius: 50%; border: 2px solid #ddd; background: white; z-index: 1; margin-top: 4px; }
.tl-right { flex: 1; padding-left: 15px; }
.tl-title { font-weight: bold; font-size: 14px; color: #374151; }
.tl-desc { font-size: 12px; color: #6b7280; margin-top: 2px; }

/* 底部 */
.security-footer { text-align: center; padding: 20px 0; opacity: 0.8; }
.qr-code { width: 100px; height: 100px; mix-blend-mode: multiply; }
.security-footer p { font-size: 10px; color: #666; margin: 5px 0; }
.blockchain-id { font-family: monospace; font-size: 10px; color: #999; background: #eee; display: inline-block; padding: 2px 6px; border-radius: 4px; }

.mobile-tabbar {
  height: 60px; background: white; border-top: 1px solid #eee;
  display: flex; align-items: center; padding: 0 20px; gap: 15px;
}
.tab-btn { flex: 1; height: 40px; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; cursor: pointer; }
.tab-btn:first-child { background: #f3f4f6; color: #333; }
.tab-btn.primary { background: #10b981; color: white; box-shadow: 0 4px 10px rgba(16, 185, 129, 0.3); }
</style>