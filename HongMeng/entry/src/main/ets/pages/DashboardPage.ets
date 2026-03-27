import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import DataCard from '../components/DataCard';
import { HttpClient, ApiResponse, Farmland } from '../common/HttpClient';

@Entry
@Component
struct DashboardPage {
  @State isLoading: boolean = false;
  @State farmlands: Farmland[] = [];
  @State totalArea: number = 0;
  @State totalStock: number = 0;
  @State normalCount: number = 0;
  @State warningCount: number = 0;

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadData();
  }

  build() {
    Column() {
      HeaderBar({
        title: '数据大屏',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      Scroll() {
        Column() {
          // 顶部统计卡片
          Row() {
            this.StatCard('🌾', '农田总数', this.farmlands.length.toString(), '#4CAF50')
            this.StatCard('📐', '总面积', `${this.totalArea}亩`, '#2196F3')
            this.StatCard('🌱', '存栏量', this.totalStock.toString(), '#8BC34A')
            this.StatCard('✅', '状态正常', this.normalCount.toString(), '#00BCD4')
          }
          .width('100%')
          .justifyContent(FlexAlign.SpaceBetween)
          .padding(16)

          // 环境监测区域
          Column() {
            Text('实时环境监测')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .margin({ bottom: 16 })

            // 温度分布图占位
            Column() {
              Text('🌡️ 温度分布')
                .fontSize(14)
                .fontColor('#666666')

              Row() {
                ForEach(this.getTemperatureDistribution(), (item: { range: string; count: number; color: string }) => {
                  Column() {
                    Text(item.count.toString())
                      .fontSize(20)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(item.color)

                    Text(item.range)
                      .fontSize(10)
                      .fontColor('#999999')
                      .margin({ top: 4 })
                  }
                  .layoutWeight(1)
                })
              }
              .width('100%')
              .margin({ top: 16 })
            }
            .width('100%')
            .backgroundColor('#F5F5F5')
            .borderRadius(8)
            .padding(16)
            .margin({ top: 12 })

            // 湿度分布
            Column() {
              Text('💧 湿度分布')
                .fontSize(14)
                .fontColor('#666666')

              Row() {
                ForEach(this.getHumidityDistribution(), (item: { range: string; count: number; color: string }) => {
                  Column() {
                    Text(item.count.toString())
                      .fontSize(20)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(item.color)

                    Text(item.range)
                      .fontSize(10)
                      .fontColor('#999999')
                      .margin({ top: 4 })
                  }
                  .layoutWeight(1)
                })
              }
              .width('100%')
              .margin({ top: 16 })
            }
            .width('100%')
            .backgroundColor('#F5F5F5')
            .borderRadius(8)
            .padding(16)
            .margin({ top: 12 })
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .padding(16)
          .margin({ left: 16, right: 16 })

          // 农田状态列表
          Column() {
            Text('农田状态一览')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .margin({ bottom: 16 })

            ForEach(this.farmlands.slice(0, 6), (farmland: Farmland) => {
              this.FarmlandStatusItem(farmland)
            })
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .padding(16)
          .margin({ top: 16, left: 16, right: 16, bottom: 16 })
        }
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  StatCard(icon: string, title: string, value: string, color: string) {
    Column() {
      Text(icon)
        .fontSize(24)

      Text(value)
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .fontColor(color)
        .margin({ top: 8 })

      Text(title)
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
  FarmlandStatusItem(farmland: Farmland) {
    Row() {
      Column() {
        Text(farmland.farm || '未命名')
          .fontSize(14)
          .fontWeight(FontWeight.Medium)

        Text(farmland.crop || '未知作物')
          .fontSize(12)
          .fontColor('#999999')
          .margin({ top: 2 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Row() {
        // 温度
        Column() {
          Text(`${farmland.temperature || '--'}°C`)
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.getTemperatureColor(farmland.temperature))

          Text('温度')
            .fontSize(10)
            .fontColor('#999999')
        }
        .margin({ right: 16 })

        // 湿度
        Column() {
          Text(`${farmland.airhumidity || '--'}%`)
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.getHumidityColor(farmland.airhumidity))

          Text('湿度')
            .fontSize(10)
            .fontColor('#999999')
        }
        .margin({ right: 16 })

        // 状态
        Text(farmland.state || '未知')
          .fontSize(12)
          .fontColor(Color.White)
          .backgroundColor(farmland.state === '正常' ? '#4CAF50' : '#FF9800')
          .borderRadius(4)
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
      }
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#FAFAFA')
    .borderRadius(8)
    .margin({ bottom: 8 })
  }

  private getTemperatureDistribution(): Array<{ range: string; count: number; color: string }> {
    const ranges = [
      { range: '<15°C', min: -Infinity, max: 15, count: 0, color: '#2196F3' },
      { range: '15-25°C', min: 15, max: 25, count: 0, color: '#4CAF50' },
      { range: '25-35°C', min: 25, max: 35, count: 0, color: '#FF9800' },
      { range: '>35°C', min: 35, max: Infinity, count: 0, color: '#F44336' }
    ];

    this.farmlands.forEach(f => {
      if (f.temperature !== null && f.temperature !== undefined) {
        ranges.forEach(r => {
          if (f.temperature! > r.min && f.temperature! <= r.max) {
            r.count++;
          }
        });
      }
    });

    return ranges.map(r => ({ range: r.range, count: r.count, color: r.color }));
  }

  private getHumidityDistribution(): Array<{ range: string; count: number; color: string }> {
    const ranges = [
      { range: '<40%', min: -Infinity, max: 40, count: 0, color: '#F44336' },
      { range: '40-60%', min: 40, max: 60, count: 0, color: '#4CAF50' },
      { range: '60-80%', min: 60, max: 80, count: 0, color: '#2196F3' },
      { range: '>80%', min: 80, max: Infinity, count: 0, color: '#9C27B0' }
    ];

    this.farmlands.forEach(f => {
      if (f.airhumidity !== null && f.airhumidity !== undefined) {
        ranges.forEach(r => {
          if (f.airhumidity! > r.min && f.airhumidity! <= r.max) {
            r.count++;
          }
        });
      }
    });

    return ranges.map(r => ({ range: r.range, count: r.count, color: r.color }));
  }

  private getTemperatureColor(temp?: number): string {
    if (!temp) return '#999999';
    if (temp < 15) return '#2196F3';
    if (temp <= 25) return '#4CAF50';
    if (temp <= 35) return '#FF9800';
    return '#F44336';
  }

  private getHumidityColor(humidity?: number): string {
    if (!humidity) return '#999999';
    if (humidity < 40) return '#F44336';
    if (humidity <= 70) return '#4CAF50';
    return '#2196F3';
  }

  private async loadData() {
    this.isLoading = true;
    try {
      const response = await this.httpClient.get<ApiResponse<Farmland[]>>('/statistic/dashboard');
      if (response.code === '200' && response.data) {
        this.farmlands = response.data;
        this.calculateStats();
      }
    } catch (error) {
      console.error('Load dashboard error:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private calculateStats() {
    this.totalArea = this.farmlands.reduce((sum, f) => {
      return sum + (f.area ? parseInt(f.area) || 0 : 0);
    }, 0);

    this.totalStock = this.farmlands.reduce((sum, f) => {
      return sum + (f.number || 0);
    }, 0);

    this.normalCount = this.farmlands.filter(f => f.state === '正常').length;
    this.warningCount = this.farmlands.length - this.normalCount;
  }
}
