import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse, HealthIndex, Farmland } from '../common/HttpClient';

interface HealthData {
  farmlandId: number;
  farmlandName: string;
  healthIndex: number;
  level: string;
}

@Entry
@Component
struct HealthPage {
  @State healthData: Map<string, HealthData> = new Map();
  @State farmlands: Farmland[] = [];
  @State isLoading: boolean = false;

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadHealthData();
  }

  build() {
    Column() {
      HeaderBar({
        title: '健康指数',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(48)
            .height(48)
            .color('#4CAF50')

          Text('正在计算健康指数...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            // 总览卡片
            Row() {
              this.SummaryCard('优秀', this.getCountByLevel('excellent'), '#4CAF50')
              this.SummaryCard('良好', this.getCountByLevel('good'), '#8BC34A')
              this.SummaryCard('一般', this.getCountByLevel('normal'), '#FF9800')
              this.SummaryCard('较差', this.getCountByLevel('poor'), '#F44336')
            }
            .width('100%')
            .justifyContent(FlexAlign.SpaceBetween)
            .padding(16)

            // 详细列表
            Column() {
              Text('农田健康详情')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .margin({ bottom: 16 })

              ForEach(Array.from(this.healthData.values()), (data: HealthData) => {
                this.HealthItem(data)
              })
            }
            .width('100%')
            .backgroundColor(Color.White)
            .borderRadius(12)
            .padding(16)
            .margin({ left: 16, right: 16, bottom: 16 })
          }
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  SummaryCard(label: string, count: number, color: string) {
    Column() {
      Text(count.toString())
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor(color)

      Text(label)
        .fontSize(12)
        .fontColor('#666666')
        .margin({ top: 4 })
    }
    .width('23%')
    .backgroundColor(Color.White)
    .borderRadius(12)
    .padding(12)
    .shadow({
      radius: 4,
      color: 'rgba(0,0,0,0.05)',
      offsetX: 0,
      offsetY: 2
    })
  }

  @Builder
  HealthItem(data: HealthData) {
    Row() {
      Column() {
        Text(data.farmlandName)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor('#333333')

        Text(this.getLevelText(data.level))
          .fontSize(12)
          .fontColor(this.getLevelColor(data.level))
          .margin({ top: 4 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      // 健康指数圆环
      Stack() {
        Progress({ value: data.healthIndex, total: 100, type: ProgressType.Ring })
          .width(60)
          .height(60)
          .color(this.getLevelColor(data.level))
          .style({ strokeWidth: 8 })

        Column() {
          Text(data.healthIndex.toString())
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.getLevelColor(data.level))

          Text('分')
            .fontSize(10)
            .fontColor('#999999')
        }
      }
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#FAFAFA')
    .borderRadius(8)
    .margin({ bottom: 8 })
    .onClick(() => {
      router.pushUrl({
        url: 'pages/FarmlandDetailPage',
        params: { id: data.farmlandId }
      });
    })
  }

  private getLevelText(level: string): string {
    switch (level) {
      case 'excellent': return '优秀 - 农田状态非常好';
      case 'good': return '良好 - 农田状态正常';
      case 'normal': return '一般 - 需要关注';
      case 'poor': return '较差 - 需要立即处理';
      default: return '未知状态';
    }
  }

  private getLevelColor(level: string): string {
    switch (level) {
      case 'excellent': return '#4CAF50';
      case 'good': return '#8BC34A';
      case 'normal': return '#FF9800';
      case 'poor': return '#F44336';
      default: return '#999999';
    }
  }

  private getCountByLevel(level: string): number {
    let count = 0;
    this.healthData.forEach((data: HealthData) => {
      if (data.level === level) {
        count++;
      }
    });
    return count;
  }

  private async loadHealthData() {
    this.isLoading = true;
    try {
      const response = await this.httpClient.get<ApiResponse<Map<string, HealthData>>>('/health-index/calculate/all');
      if (response.code === '200' && response.data) {
        this.healthData = response.data;
      }
    } catch (error) {
      console.error('Load health data error:', error);
    } finally {
      this.isLoading = false;
    }
  }
}
