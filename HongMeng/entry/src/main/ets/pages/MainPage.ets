import router from '@ohos.router';
import TabBar from '../components/TabBar';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse, DashboardSummary, Alert, Farmland } from '../common/HttpClient';
import { ThemeColors } from '../common/ThemeColors';
import GradientButton from '../components/glass/GradientButton';

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
  @State greetingOpacity: number = 0;
  @State cardTranslateY: number = 20;

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadData();
    animateTo({ duration: 600, curve: Curve.EaseOut }, () => {
      this.greetingOpacity = 1;
      this.cardTranslateY = 0;
    });
  }

  build() {
    Column() {
      HeaderBar({
        title: '智慧农场',
        showBack: false,
        rightIcon: '⚙️',
        onRightClick: () => {
          router.pushUrl({ url: 'pages/SettingsPage' });
        }
      })

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

      TabBar({ currentTab: $currentTab })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(ThemeColors.BG_LIGHT)
  }

  @Builder
  HomeContent() {
    Scroll() {
      Column() {
        // 欢迎信息
        Column() {
          Row() {
            Column() {
              Text(this.getGreeting())
                .fontSize(24)
                .fontWeight(FontWeight.Bold)
                .fontColor(ThemeColors.TEXT_LIGHT)

              Text('智慧农场管理系统')
                .fontSize(14)
                .fontColor('rgba(255,255,255,0.8)')
                .margin({ top: 4 })
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)

            Column() {
              Text('🤖')
                .fontSize(32)
            }
            .width(56)
            .height(56)
            .borderRadius(28)
            .justifyContent(FlexAlign.Center)
            .backgroundColor('rgba(255,255,255,0.2)')
            .backdropBlur(10)
          }
          .width('100%')
          .padding(20)
        }
        .width('100%')
        .linearGradient({
          angle: 135,
          colors: [[ThemeColors.PRIMARY_GREEN, 0.0], [ThemeColors.PRIMARY_BLUE, 1.0]]
        })
        .borderRadius({ bottomLeft: 24, bottomRight: 24 })
        .opacity(this.greetingOpacity)

        // 数据卡片区域
        Column() {
          Row() {
            this.DataCardItem('🌾', '农田总数', this.dashboardSummary.farmCount.toString(), '块', ThemeColors.PRIMARY_GREEN)
            Blank().width(12)
            this.DataCardItem('📐', '总面积', this.dashboardSummary.totalArea.toString(), '亩', ThemeColors.PRIMARY_BLUE)
          }
          .width('100%')
          .translate({ y: this.cardTranslateY })

          Row() {
            this.DataCardItem('🌱', '存栏数量', this.dashboardSummary.totalStock.toString(), '株', ThemeColors.ACCENT_SUCCESS)
            Blank().width(12)
            this.DataCardItem('✅', '状态正常', this.dashboardSummary.normalCount.toString(), '块', ThemeColors.ACCENT_INFO)
          }
          .width('100%')
          .margin({ top: 12 })
        }
        .width('100%')
        .padding(16)
        .margin({ top: -20 })

        // 快捷操作
        Column() {
          Row() {
            Text('快捷操作')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor(ThemeColors.TEXT_PRIMARY)

            Blank()

            Text('更多 >')
              .fontSize(12)
              .fontColor(ThemeColors.TEXT_SECONDARY)
          }
          .width('100%')
          .margin({ bottom: 16 })

          Row() {
            this.QuickButtonModern('📊', '数据大屏', ThemeColors.PRIMARY_BLUE, () => {
              router.pushUrl({ url: 'pages/DashboardPage' });
            })

            this.QuickButtonModern('🔔', '预警中心', ThemeColors.ACCENT_WARNING, () => {
              router.pushUrl({ url: 'pages/AlertPage' });
            })

            this.QuickButtonModern('📈', '健康指数', ThemeColors.ACCENT_SUCCESS, () => {
              router.pushUrl({ url: 'pages/HealthPage' });
            })

            this.QuickButtonModern('📷', '果蔬识别', ThemeColors.ACCENT_AI, () => {
              this.currentTab = 3;
            })
          }
          .width('100%')
          .justifyContent(FlexAlign.SpaceBetween)
        }
        .width('100%')
        .backgroundColor('rgba(255,255,255,0.8)')
        .backdropBlur(20)
        .borderRadius(16)
        .padding(20)
        .margin({ top: 16, left: 16, right: 16 })
        .border({ width: 1, color: 'rgba(255,255,255,0.5)' })

        // 今日预警
        if (this.alerts.length > 0) {
          Column() {
            Row() {
              Text('⚠️ 今日预警')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .fontColor(ThemeColors.TEXT_PRIMARY)

              Blank()

              Text(`${this.alerts.length} 条`)
                .fontSize(14)
                .fontColor(ThemeColors.ACCENT_DANGER)
                .backgroundColor('rgba(239, 68, 68, 0.1)')
                .borderRadius(12)
                .padding({ left: 12, right: 12, top: 4, bottom: 4 })
            }
            .width('100%')
            .margin({ bottom: 16 })

            ForEach(this.alerts.slice(0, 3), (alert: Alert) => {
              this.AlertItem(alert)
            })

            GradientButton({
              text: '查看全部预警',
              btnHeight: 44,
              fontSize: 14,
              gradientColors: [ThemeColors.ACCENT_WARNING, ThemeColors.ACCENT_DANGER],
              onClick: () => {
                router.pushUrl({ url: 'pages/AlertPage' });
              }
            })
              .margin({ top: 12 })
          }
          .width('100%')
          .backgroundColor('#FFFFFF')
          .borderRadius(16)
          .padding(20)
          .margin({ top: 16, left: 16, right: 16, bottom: 20 })
        }
      }
    }
    .scrollBar(BarState.Off)
  }

  @Builder
  DataCardItem(icon: string, title: string, value: string, unit: string, color: string) {
    Column() {
      Row() {
        Text(icon)
          .fontSize(20)

        Text(title)
          .fontSize(12)
          .fontColor(ThemeColors.TEXT_SECONDARY)
          .margin({ left: 6 })
      }
      .width('100%')
      .margin({ bottom: 12 })

      Row() {
        Text(value)
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor(color)

        Text(unit)
          .fontSize(12)
          .fontColor(ThemeColors.TEXT_TERTIARY)
          .margin({ left: 4, top: 12 })
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .layoutWeight(1)
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(16)
  }

  @Builder
  QuickButtonModern(icon: string, label: string, color: string, onClick: () => void) {
    Column() {
      Column() {
        Text(icon)
          .fontSize(24)
      }
      .width(48)
      .height(48)
      .borderRadius(12)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(color + '15')

      Text(label)
        .fontSize(12)
        .fontColor(ThemeColors.TEXT_PRIMARY)
        .margin({ top: 8 })
    }
    .onClick(onClick)
  }

  @Builder
  AlertItem(alert: Alert) {
    Row() {
      Column() {
        Text(alert.alertLevel === 'high' ? '🔴' : '🟠')
          .fontSize(12)
      }
      .width(24)
      .height(24)
      .borderRadius(12)
      .justifyContent(FlexAlign.Center)

      Column() {
        Text(alert.message)
          .fontSize(14)
          .fontColor(ThemeColors.TEXT_PRIMARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })

        Text(this.formatTime(alert.createTime || ''))
          .fontSize(11)
          .fontColor(ThemeColors.TEXT_TERTIARY)
          .margin({ top: 2 })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      .margin({ left: 12 })

      Text(alert.alertLevel === 'high' ? '高' : '中')
        .fontSize(11)
        .fontColor('#FFFFFF')
        .backgroundColor(alert.alertLevel === 'high' ? ThemeColors.ACCENT_DANGER : ThemeColors.ACCENT_WARNING)
        .borderRadius(4)
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#FEF2F2')
    .borderRadius(12)
    .margin({ bottom: 8 })
  }

  @Builder
  FarmlandContent() {
    Column() {
      Row() {
        TextInput({ placeholder: '搜索农田...' })
          .layoutWeight(1)
          .height(44)
          .backgroundColor('#F1F5F9')
          .borderRadius(12)
          .padding({ left: 16 })

        Column() {
          Text('+')
            .fontSize(24)
            .fontColor('#FFFFFF')
        }
        .width(44)
        .height(44)
        .justifyContent(FlexAlign.Center)
        .linearGradient({
          angle: 135,
          colors: [[ThemeColors.PRIMARY_GREEN, 0.0], [ThemeColors.PRIMARY_BLUE, 1.0]]
        })
        .borderRadius(12)
        .margin({ left: 12 })
        .onClick(() => {
          router.pushUrl({ url: 'pages/FarmlandDetailPage' });
        })
      }
      .width('100%')
      .padding(16)

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
          .fontColor(ThemeColors.TEXT_PRIMARY)

        Text(farmland.address || '暂无地址')
          .fontSize(12)
          .fontColor(ThemeColors.TEXT_SECONDARY)
          .margin({ top: 4 })

        Row() {
          Text(farmland.crop || '未知作物')
            .fontSize(12)
            .fontColor(ThemeColors.PRIMARY_GREEN)
            .backgroundColor('rgba(34, 197, 94, 0.1)')
            .borderRadius(4)
            .padding({ left: 8, right: 8, top: 4, bottom: 4 })

          Text(farmland.state || '未知状态')
            .fontSize(12)
            .fontColor(farmland.state === '正常' ? ThemeColors.ACCENT_SUCCESS : ThemeColors.ACCENT_WARNING)
            .backgroundColor(farmland.state === '正常' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(245, 158, 11, 0.1)')
            .borderRadius(4)
            .padding({ left: 8, right: 8, top: 4, bottom: 4 })
            .margin({ left: 8 })
        }
        .margin({ top: 8 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Column() {
        Text(`${farmland.temperature || '--'}°C`)
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.getTemperatureColor(farmland.temperature))

        Text(`${farmland.airhumidity || '--'}%`)
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.getHumidityColor(farmland.airhumidity))
          .margin({ top: 4 })
      }
      .alignItems(HorizontalAlign.End)
    }
    .width('100%')
    .padding(16)
    .backgroundColor('#FFFFFF')
    .borderRadius(16)
    .margin({ bottom: 12 })
  }

  @Builder
  InventoryContent() {
    Column() {
      Row() {
        Text('库存管理')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor(ThemeColors.TEXT_PRIMARY)

        Blank()

        GradientButton({
          text: '+ 添加',
          btnWidth: 80,
          btnHeight: 36,
          fontSize: 14,
          btnRadius: 8
        })
      }
      .width('100%')
      .padding(16)

      Text('库存功能开发中...')
        .fontSize(14)
        .fontColor(ThemeColors.TEXT_TERTIARY)
        .margin({ top: 100 })
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  DetectContent() {
    Column() {
      Column() {
        Column() {
          Text('🍎')
            .fontSize(64)
        }
        .width(120)
        .height(120)
        .borderRadius(60)
        .justifyContent(FlexAlign.Center)
        .linearGradient({
          angle: 135,
          colors: [['rgba(239, 68, 68, 0.1)', 0.0], ['rgba(245, 158, 11, 0.1)', 1.0]]
        })

        Text('果蔬成熟度识别')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(ThemeColors.TEXT_PRIMARY)
          .margin({ top: 24 })

        Text('使用AI技术智能识别果蔬成熟度')
          .fontSize(14)
          .fontColor(ThemeColors.TEXT_SECONDARY)
          .margin({ top: 8 })
      }
      .margin({ top: 80 })

      GradientButton({
        text: '开始识别',
        btnWidth: '80%',
        btnHeight: 50,
        fontSize: 16,
        onClick: () => {
          router.pushUrl({ url: 'pages/FruitDetectPage' });
        }
      })
        .margin({ top: 48 })

      Button('检测服务状态')
        .width('80%')
        .height(50)
        .backgroundColor('#F1F5F9')
        .borderRadius(25)
        .fontSize(16)
        .fontColor(ThemeColors.TEXT_SECONDARY)
        .margin({ top: 16 })
        .onClick(() => {
          this.checkDetectService();
        })
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  AssistantContent() {
    Column() {
      Column() {
        Column() {
          Text('🤖')
            .fontSize(48)
        }
        .width(100)
        .height(100)
        .borderRadius(50)
        .justifyContent(FlexAlign.Center)
        .linearGradient({
          angle: 135,
          colors: [[ThemeColors.ACCENT_AI, 0.0], ['#EC4899', 1.0]]
        })
        .shadow({
          radius: 20,
          color: 'rgba(139, 92, 246, 0.4)',
          offsetX: 0,
          offsetY: 8
        })

        Text('AI农业助手')
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(ThemeColors.TEXT_PRIMARY)
          .margin({ top: 24 })

        Text('智能问答，专业建议')
          .fontSize(14)
          .fontColor(ThemeColors.TEXT_SECONDARY)
          .margin({ top: 8 })
      }
      .margin({ top: 80 })

      GradientButton({
        text: '开始对话',
        btnWidth: '80%',
        btnHeight: 50,
        fontSize: 16,
        gradientColors: [ThemeColors.ACCENT_AI, '#EC4899'],
        onClick: () => {
          router.pushUrl({ url: 'pages/AIAssistantPage' });
        }
      })
        .margin({ top: 48 })
    }
    .width('100%')
    .height('100%')
  }

  private getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 6) return '夜深了';
    if (hour < 12) return '早上好';
    if (hour < 14) return '中午好';
    if (hour < 18) return '下午好';
    return '晚上好';
  }

  private formatTime(time: string): string {
    if (!time) return '';
    try {
      const date = new Date(time);
      const now = new Date();
      const diff = now.getTime() - date.getTime();
      const minutes = Math.floor(diff / 60000);
      if (minutes < 60) return `${minutes}分钟前`;
      const hours = Math.floor(minutes / 60);
      if (hours < 24) return `${hours}小时前`;
      return `${Math.floor(hours / 24)}天前`;
    } catch {
      return time;
    }
  }

  private getTemperatureColor(temp?: number): string {
    if (!temp) return ThemeColors.TEXT_TERTIARY;
    if (temp < 15) return ThemeColors.PRIMARY_BLUE;
    if (temp <= 25) return ThemeColors.ACCENT_SUCCESS;
    if (temp <= 35) return ThemeColors.ACCENT_WARNING;
    return ThemeColors.ACCENT_DANGER;
  }

  private getHumidityColor(humidity?: number): string {
    if (!humidity) return ThemeColors.TEXT_TERTIARY;
    if (humidity < 40) return ThemeColors.ACCENT_DANGER;
    if (humidity <= 70) return ThemeColors.ACCENT_SUCCESS;
    return ThemeColors.PRIMARY_BLUE;
  }

  private async loadData() {
    this.isLoading = true;
    try {
      const summaryResponse = await this.httpClient.get<ApiResponse<DashboardSummary>>('/statistic/dashboard/summary');
      if (summaryResponse.code === '200' && summaryResponse.data) {
        this.dashboardSummary = summaryResponse.data;
      }

      const alertsResponse = await this.httpClient.get<ApiResponse<Alert[]>>('/alert/today/tasks');
      if (alertsResponse.code === '200') {
        this.alerts = alertsResponse.data || [];
      }

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
      console.log(response.code === '200' ? '检测服务正常' : '检测服务异常');
    } catch (error) {
      console.error('检测服务未启动');
    }
  }
}
