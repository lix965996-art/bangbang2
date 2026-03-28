import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse, Farmland } from '../common/HttpClient';
import { ThemeColors } from '../common/ThemeColors';
import GlassCard from '../components/glass/GlassCard';
import CircularProgress from '../components/charts/CircularProgress';

interface DistributionItem {
  range: string;
  count: number;
  color: string;
}

@Entry
@Component
struct DashboardPage {
  @State isLoading: boolean = false;
  @State farmlands: Farmland[] = [];
  @State totalArea: number = 0;
  @State totalStock: number = 0;
  @State normalCount: number = 0;
  @State warningCount: number = 0;
  @State tempDistribution: DistributionItem[] = [];
  @State humidityDistribution: DistributionItem[] = [];

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

      // 深色背景
      Column() {
        Scroll() {
          Column() {
            // 顶部统计卡片
            Row() {
              this.StatCard('🌾', '农田总数', this.farmlands.length.toString(), '块')
              Blank().width(12)
              this.StatCard('📐', '总面积', this.totalArea.toString(), '亩')
              Blank().width(12)
              this.StatCard('🌱', '存栏数', this.totalStock.toString(), '株')
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 16 })

            Row() {
              this.StatCard('✅', '正常', this.normalCount.toString(), '块')
              Blank().width(12)
              this.StatCard('⚠️', '异常', this.warningCount.toString(), '块')
            }
            .width('100%')
            .padding({ left: 16, right: 16, bottom: 16 })
            .margin({ top: 12 })

            // 温度分布
            Column() {
              Row() {
                Text('🌡️ 温度分布')
                  .fontSize(16)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(ThemeColors.TEXT_LIGHT)

                Blank()

                Text('实时监测')
                  .fontSize(12)
                  .fontColor('rgba(255,255,255,0.6)')
              }
              .width('100%')
              .margin({ bottom: 16 })

              Row() {
                ForEach(this.getTemperatureDistribution(), (item: DistributionItem) => {
                  Column() {
                    Text(item.range)
                      .fontSize(12)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(item.color)

                    Column() {
                      Text(`${item.count}`)
                        .fontSize(16)
                        .fontWeight(FontWeight.Bold)
                        .fontColor(Color.White)
                    }
                    .width('100%')
                    .height(60)
                    .justifyContent(FlexAlign.End)
                    .padding({ bottom: 8 })

                    Row()
                      .width('100%')
                      .height(item.count > 0 ? 40 : 4)
                      .backgroundColor(item.color)
                      .borderRadius({ topLeft: 4, topRight: 4 })
                  }
                  .layoutWeight(1)
                  .margin({ left: 4, right: 4 })
                })
              }
              .width('100%')
            }
            .width('100%')
            .padding(16)
            .backgroundColor('rgba(255,255,255,0.08)')
            .borderRadius(16)
            .margin({ left: 16, right: 16, top: 16 })

            // 湿度分布
            Column() {
              Row() {
                Text('💧 湿度分布')
                  .fontSize(16)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(ThemeColors.TEXT_LIGHT)

                Blank()

                Text('空气湿度')
                  .fontSize(12)
                  .fontColor('rgba(255,255,255,0.6)')
              }
              .width('100%')
              .margin({ bottom: 16 })

              Row() {
                ForEach(this.getHumidityDistribution(), (item: DistributionItem) => {
                  Column() {
                    Text(item.range)
                      .fontSize(12)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(item.color)

                    Column() {
                      Text(`${item.count}`)
                        .fontSize(16)
                        .fontWeight(FontWeight.Bold)
                        .fontColor(Color.White)
                    }
                    .width('100%')
                    .height(60)
                    .justifyContent(FlexAlign.End)
                    .padding({ bottom: 8 })

                    Row()
                      .width('100%')
                      .height(item.count > 0 ? 40 : 4)
                      .backgroundColor(item.color)
                      .borderRadius({ topLeft: 4, topRight: 4 })
                  }
                  .layoutWeight(1)
                  .margin({ left: 4, right: 4 })
                })
              }
              .width('100%')
            }
            .width('100%')
            .padding(16)
            .backgroundColor('rgba(255,255,255,0.08)')
            .borderRadius(16)
            .margin({ left: 16, right: 16, top: 16 })

            // 农田状态概览
            Column() {
              Text('📊 农田状态概览')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor(ThemeColors.TEXT_LIGHT)
                .width('100%')
                .margin({ bottom: 16 })

              if (this.farmlands.length > 0) {
                ForEach(this.farmlands.slice(0, 5), (farmland: Farmland) => {
                  Row() {
                    Column() {
                      Text(farmland.farm || '未命名')
                        .fontSize(14)
                        .fontWeight(FontWeight.Medium)
                        .fontColor(Color.White)

                      Text(farmland.crop || '未知作物')
                        .fontSize(12)
                        .fontColor('rgba(255,255,255,0.6)')
                        .margin({ top: 2 })
                    }
                    .alignItems(HorizontalAlign.Start)
                    .layoutWeight(1)

                    // 温度
                    Column() {
                      Text(`${farmland.temperature || '--'}°C`)
                        .fontSize(14)
                        .fontWeight(FontWeight.Bold)
                        .fontColor(this.getTemperatureColor(farmland.temperature))
                    }
                    .width(60)
                    .alignItems(HorizontalAlign.End)

                    // 湿度
                    Column() {
                      Text(`${farmland.airhumidity || '--'}%`)
                        .fontSize(14)
                        .fontWeight(FontWeight.Bold)
                        .fontColor(this.getHumidityColor(farmland.airhumidity))
                    }
                    .width(60)
                    .alignItems(HorizontalAlign.End)

                    // 状态
                    Column() {
                      Text(farmland.state === '正常' ? '✅' : '⚠️')
                        .fontSize(16)
                    }
                    .width(40)
                    .alignItems(HorizontalAlign.End)
                  }
                  .width('100%')
                  .padding(12)
                  .backgroundColor('rgba(255,255,255,0.05)')
                  .borderRadius(8)
                  .margin({ bottom: 8 })
                })
              } else {
                Column() {
                  Text('暂无数据')
                    .fontSize(14)
                    .fontColor('rgba(255,255,255,0.5)')

                  Text('请先添加农田')
                    .fontSize(12)
                    .fontColor('rgba(255,255,255,0.3)')
                    .margin({ top: 4 })
                }
                .width('100%')
                .padding(40)
              }
            }
            .width('100%')
            .padding(16)
            .backgroundColor('rgba(255,255,255,0.08)')
            .borderRadius(16)
            .margin({ left: 16, right: 16, top: 16, bottom: 20 })
          }
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
      .layoutWeight(1)
      .width('100%')
      .linearGradient({
        angle: 180,
        colors: [['#0F172A', 0.0], ['#1E293B', 1.0]]
      })
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  StatCard(icon: string, title: string, value: string, unit: string) {
    Column() {
      Text(icon)
        .fontSize(24)

      Text(title)
        .fontSize(12)
        .fontColor('rgba(255,255,255,0.7)')
        .margin({ top: 8 })

      Row() {
        Text(value)
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)

        Text(unit)
          .fontSize(12)
          .fontColor('rgba(255,255,255,0.5)')
          .margin({ left: 4, top: 8 })
      }
    }
    .layoutWeight(1)
    .padding(16)
    .backgroundColor('rgba(255,255,255,0.08)')
    .borderRadius(12)
    .alignItems(HorizontalAlign.Center)
  }

  private calculateStats() {
    this.totalArea = this.farmlands.reduce((sum: number, f: Farmland) => {
      return sum + (f.area ? parseInt(f.area) || 0 : 0);
    }, 0);

    this.totalStock = this.farmlands.reduce((sum: number, f: Farmland) => {
      return sum + (f.number || 0);
    }, 0);

    this.normalCount = this.farmlands.filter((f: Farmland) => f.state === '正常').length;
    this.warningCount = this.farmlands.length - this.normalCount;
  }

  private getTemperatureDistribution(): DistributionItem[] {
    let ranges: DistributionItem[] = [
      { range: '<15°C', count: 0, color: '#3B82F6' },
      { range: '15-25°C', count: 0, color: '#22C55E' },
      { range: '25-35°C', count: 0, color: '#F59E0B' },
      { range: '>35°C', count: 0, color: '#EF4444' }
    ];

    this.farmlands.forEach((f: Farmland) => {
      if (f.temperature !== null && f.temperature !== undefined) {
        const temp = f.temperature!;
        if (temp <= 15) ranges[0].count++;
        else if (temp <= 25) ranges[1].count++;
        else if (temp <= 35) ranges[2].count++;
        else ranges[3].count++;
      }
    });

    return ranges;
  }

  private getHumidityDistribution(): DistributionItem[] {
    let ranges: DistributionItem[] = [
      { range: '<40%', count: 0, color: '#EF4444' },
      { range: '40-60%', count: 0, color: '#22C55E' },
      { range: '60-80%', count: 0, color: '#3B82F6' },
      { range: '>80%', count: 0, color: '#9C27B0' }
    ];

    this.farmlands.forEach((f: Farmland) => {
      if (f.airhumidity !== null && f.airhumidity !== undefined) {
        const humidity = f.airhumidity!;
        if (humidity < 40) ranges[0].count++;
        else if (humidity < 60) ranges[1].count++;
        else if (humidity < 80) ranges[2].count++;
        else ranges[3].count++;
      }
    });

    return ranges;
  }

  private getTemperatureColor(temp?: number): string {
    if (!temp) return 'rgba(255,255,255,0.5)';
    if (temp < 15) return '#3B82F6';
    if (temp <= 25) return '#22C55E';
    if (temp <= 35) return '#F59E0B';
    return '#EF4444';
  }

  private getHumidityColor(humidity?: number): string {
    if (!humidity) return 'rgba(255,255,255,0.5)';
    if (humidity < 40) return '#EF4444';
    if (humidity <= 60) return '#22C55E';
    if (humidity <= 80) return '#3B82F6';
    return '#9C27B0';
  }

  private async loadData() {
    this.isLoading = true;
    try {
      const response = await this.httpClient.get<ApiResponse<Farmland[]>>('/statistic');
      if (response.code === '200') {
        this.farmlands = response.data || [];
        this.calculateStats();
      }
    } catch (error) {
      console.error('Load data error:', error);
    } finally {
      this.isLoading = false;
    }
  }
}
