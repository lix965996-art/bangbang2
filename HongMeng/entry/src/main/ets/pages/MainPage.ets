import router from '@ohos.router';
import TabBar from '../components/TabBar';
import HeaderBar from '../components/HeaderBar';
import DataCard from '../components/DataCard';
import { HttpClient, ApiResponse, DashboardSummary, Alert, Farmland } from '../common/HttpClient';

@Entry
@Component
struct MainPage {
  @State currentTab: number = 0;
  @State isLoading: boolean = false;
  @State dashboardSummary: DashboardSummary = {
    totalArea: 0,
    totalStock: 0,
    farmCount: 0,
    normalCount: 0
  };
  @State alerts: Alert[] = [];
  @State farmlands: Farmland[] = [];

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadData();
  }

  build() {
    Column() {
      // 顶部标题栏
      HeaderBar({
        title: '智慧农场',
        showBack: false,
        rightIcon: '⚙️',
        onRightClick: () => {
          router.pushUrl({ url: 'pages/SettingsPage' });
        }
      })

      // 内容区域
      Stack() {
        if (this.currentTab === 0) {
          this.HomeContent()
        } else if (this.currentTab === 1) {
          this.FarmlandContent()
        } else if (this.currentTab === 2) {
          this.InventoryContent()
        } else if (this.currentTab === 3) {
          this.DetectContent()
        } else if (this.currentTab === 4) {
          this.AssistantContent()
        }
      }
      .layoutWeight(1)
      .width('100%')

      // 底部导航栏
      TabBar({ currentTab: $currentTab })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  // 首页内容
  @Builder
  HomeContent() {
    Scroll() {
      Column() {
        // 欢迎信息
        Row() {
          Column() {
            Text('欢迎回来')
              .fontSize(20)
              .fontWeight(FontWeight.Bold)
              .fontColor('#333333')

            Text('今天也要好好管理农场哦~')
              .fontSize(14)
              .fontColor('#666666')
              .margin({ top: 4 })
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)

          Text('👨‍🌾')
            .fontSize(40)
        }
        .width('100%')
        .padding(16)

        // 数据卡片
        Row() {
          DataCard({
            title: '农田总数',
            value: this.dashboardSummary.farmCount,
            unit: '块',
            icon: '🌾',
            color: '#4CAF50'
          })

          Blank().width(12)

          DataCard({
            title: '总面积',
            value: this.dashboardSummary.totalArea,
            unit: '亩',
            icon: '📐',
            color: '#2196F3'
          })
        }
        .width('100%')
        .padding({ left: 16, right: 16 })

        Row() {
          DataCard({
            title: '存栏数量',
            value: this.dashboardSummary.totalStock,
            unit: '株',
            icon: '🌱',
            color: '#8BC34A'
          })

          Blank().width(12)

          DataCard({
            title: '状态正常',
            value: this.dashboardSummary.normalCount,
            unit: '块',
            icon: '✅',
            color: '#00BCD4'
          })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12 })

        // 快捷操作
        Column() {
          Text('快捷操作')
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .margin({ bottom: 12 })

          Row() {
            this.QuickButton('📊', '大屏展示', () => {
              router.pushUrl({ url: 'pages/DashboardPage' });
            })

            this.QuickButton('🔔', '预警中心', () => {
              router.pushUrl({ url: 'pages/AlertPage' });
            })

            this.QuickButton('📈', '健康指数', () => {
              router.pushUrl({ url: 'pages/HealthPage' });
            })

            this.QuickButton('📷', '果蔬识别', () => {
              this.currentTab = 3;
            })
          }
          .width('100%')
          .justifyContent(FlexAlign.SpaceAround)
        }
        .width('100%')
        .backgroundColor(Color.White)
        .borderRadius(12)
        .padding(16)
        .margin({ top: 20, left: 16, right: 16 })

        // 今日预警
        if (this.alerts.length > 0) {
          Column() {
            Row() {
              Text('今日预警')
                .fontSize(16)
                .fontWeight(FontWeight.Medium)

              Blank()

              Text('查看全部 >')
                .fontSize(12)
                .fontColor('#4CAF50')
                .onClick(() => {
                  router.pushUrl({ url: 'pages/AlertPage' });
                })
            }
            .width('100%')
            .margin({ bottom: 12 })

            ForEach(this.alerts.slice(0, 3), (alert: Alert) => {
              Row() {
                Text('⚠️')
                  .fontSize(20)
                  .margin({ right: 12 })

                Text(alert.message)
                  .fontSize(14)
                  .fontColor('#333333')
                  .layoutWeight(1)
                  .maxLines(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })

                Text(alert.alertLevel === 'high' ? '高' : '中')
                  .fontSize(12)
                  .fontColor(Color.White)
                  .backgroundColor(alert.alertLevel === 'high' ? '#F44336' : '#FF9800')
                  .borderRadius(4)
                  .padding({ left: 6, right: 6, top: 2, bottom: 2 })
              }
              .width('100%')
              .padding(12)
              .backgroundColor('#FFF8E1')
              .borderRadius(8)
              .margin({ bottom: 8 })
            })
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .padding(16)
          .margin({ top: 20, left: 16, right: 16, bottom: 16 })
        }
      }
    }
    .scrollBar(BarState.Off)
  }

  @Builder
  QuickButton(icon: string, label: string, onClick: () => void) {
    Column() {
      Text(icon)
        .fontSize(28)

      Text(label)
        .fontSize(12)
        .fontColor('#666666')
        .margin({ top: 4 })
    }
    .width(70)
    .padding(12)
    .backgroundColor('#F5F5F5')
    .borderRadius(12)
    .onClick(onClick)
  }

  // 农田内容
  @Builder
  FarmlandContent() {
    Column() {
      // 搜索栏
      Row() {
        TextInput({ placeholder: '搜索农田...' })
          .layoutWeight(1)
          .height(40)
          .backgroundColor('#F0F0F0')
          .borderRadius(20)
          .padding({ left: 16 })

        Button('+')
          .width(40)
          .height(40)
          .fontSize(24)
          .backgroundColor('#4CAF50')
          .borderRadius(20)
          .margin({ left: 12 })
          .onClick(() => {
            router.pushUrl({ url: 'pages/FarmlandDetailPage' });
          })
      }
      .width('100%')
      .padding(16)

      // 农田列表
      List() {
        ForEach(this.farmlands, (farmland: Farmland, index: number) => {
          ListItem() {
            this.FarmlandItem(farmland)
          }
          .onClick(() => {
            router.pushUrl({
              url: 'pages/FarmlandDetailPage',
              params: { id: farmland.id }
            });
          })
        })
      }
      .layoutWeight(1)
      .width('100%')
      .padding({ left: 16, right: 16 })
      .divider({ strokeWidth: 1, color: '#F0F0F0' })
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  FarmlandItem(farmland: Farmland) {
    Row() {
      Column() {
        Text(farmland.farm || '未命名农田')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')

        Text(farmland.address || '暂无地址')
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 4 })

        Row() {
          Text(farmland.crop || '未知作物')
            .fontSize(12)
            .fontColor('#4CAF50')
            .backgroundColor('#E8F5E9')
            .borderRadius(4)
            .padding({ left: 6, right: 6, top: 2, bottom: 2 })

          Text(farmland.state || '未知状态')
            .fontSize(12)
            .fontColor(farmland.state === '正常' ? '#4CAF50' : '#FF9800')
            .backgroundColor(farmland.state === '正常' ? '#E8F5E9' : '#FFF3E0')
            .borderRadius(4)
            .padding({ left: 6, right: 6, top: 2, bottom: 2 })
            .margin({ left: 8 })
        }
        .margin({ top: 8 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      // 环境数据
      Column() {
        Row() {
          Text('🌡️')
            .fontSize(14)
          Text(`${farmland.temperature || '--'}°C`)
            .fontSize(12)
            .fontColor('#666666')
        }

        Row() {
          Text('💧')
            .fontSize(14)
          Text(`${farmland.airhumidity || '--'}%`)
            .fontSize(12)
            .fontColor('#666666')
        }
        .margin({ top: 4 })
      }
      .alignItems(HorizontalAlign.End)
    }
    .width('100%')
    .backgroundColor(Color.White)
    .borderRadius(12)
    .padding(16)
    .margin({ bottom: 12 })
    .shadow({
      radius: 4,
      color: 'rgba(0,0,0,0.05)',
      offsetX: 0,
      offsetY: 2
    })
  }

  // 库存内容
  @Builder
  InventoryContent() {
    Column() {
      Row() {
        Text('库存管理')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)

        Blank()

        Button('+ 添加')
          .fontSize(14)
          .backgroundColor('#4CAF50')
          .borderRadius(8)
          .height(36)
      }
      .width('100%')
      .padding(16)

      Text('库存功能开发中...')
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 100 })
    }
    .width('100%')
    .height('100%')
  }

  // 识别内容
  @Builder
  DetectContent() {
    Column() {
      Column() {
        Text('🍎')
          .fontSize(64)

        Text('果蔬成熟度识别')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .margin({ top: 16 })

        Text('使用AI识别果蔬成熟度')
          .fontSize(14)
          .fontColor('#666666')
          .margin({ top: 8 })
      }
      .margin({ top: 60 })

      Button('开始识别')
        .width('80%')
        .height(50)
        .backgroundColor('#4CAF50')
        .borderRadius(25)
        .fontSize(16)
        .margin({ top: 48 })
        .onClick(() => {
          router.pushUrl({ url: 'pages/FruitDetectPage' });
        })

      Button('检测服务状态')
        .width('80%')
        .height(50)
        .backgroundColor('#E0E0E0')
        .borderRadius(25)
        .fontSize(16)
        .fontColor('#666666')
        .margin({ top: 16 })
        .onClick(() => {
          this.checkDetectService();
        })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  // AI助手内容
  @Builder
  AssistantContent() {
    Column() {
      Column() {
        Text('🤖')
          .fontSize(64)

        Text('AI农业助手')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .margin({ top: 16 })

        Text('智能问答，专业建议')
          .fontSize(14)
          .fontColor('#666666')
          .margin({ top: 8 })
      }
      .margin({ top: 60 })

      Button('开始对话')
        .width('80%')
        .height(50)
        .backgroundColor('#4CAF50')
        .borderRadius(25)
        .fontSize(16)
        .margin({ top: 48 })
        .onClick(() => {
          router.pushUrl({ url: 'pages/AIAssistantPage' });
        })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  private async loadData() {
    this.isLoading = true;
    try {
      // 加载Dashboard汇总数据
      const summaryResponse = await this.httpClient.get<ApiResponse<DashboardSummary>>('/statistic/dashboard/summary');
      if (summaryResponse.code === '200') {
        this.dashboardSummary = summaryResponse.data;
      }

      // 加载今日预警
      const alertsResponse = await this.httpClient.get<ApiResponse<Alert[]>>('/alert/today/tasks');
      if (alertsResponse.code === '200') {
        this.alerts = alertsResponse.data || [];
      }

      // 加载农田列表
      const farmlandsResponse = await this.httpClient.get<ApiResponse<Farmland[]>>('/statistic');
      if (farmlandsResponse.code === '200') {
        this.farmlands = farmlandsResponse.data || [];
      }
    } catch (error) {
      console.error('Load data error:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private async checkDetectService() {
    try {
      const response = await this.httpClient.get<ApiResponse<object>>('/fruit-detect/health');
      if (response.code === '200') {
        // 显示服务正常提示
        console.log('检测服务正常');
      } else {
        console.log('检测服务异常');
      }
    } catch (error) {
      console.error('检测服务未启动');
    }
  }
}