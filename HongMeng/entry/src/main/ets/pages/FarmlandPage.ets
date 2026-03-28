import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse, Farmland, QueryParam, PaginatedRecords } from '../common/HttpClient';

@Entry
@Component
struct FarmlandPage {
  @State farmlands: Farmland[] = [];
  @State searchKeyword: string = '';
  @State isLoading: boolean = false;
  @State pageNum: number = 1;
  @State pageSize: number = 20;
  @State hasMore: boolean = true;

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadFarmlands();
  }

  build() {
    Column() {
      HeaderBar({
        title: '农田管理',
        showBack: true,
        rightIcon: '+',
        onBackClick: () => {
          router.back();
        },
        onRightClick: () => {
          router.pushUrl({ url: 'pages/FarmlandDetailPage' });
        }
      })

      // 搜索栏
      Row() {
        TextInput({ placeholder: '搜索农田名称...', text: this.searchKeyword })
          .layoutWeight(1)
          .height(44)
          .backgroundColor('#F5F5F5')
          .borderRadius(22)
          .padding({ left: 16, right: 16 })
          .onChange((value: string) => {
            this.searchKeyword = value;
            this.pageNum = 1;
            this.loadFarmlands();
          })

        Button('搜索')
          .fontSize(14)
          .backgroundColor('#4CAF50')
          .borderRadius(22)
          .height(44)
          .margin({ left: 12 })
          .onClick(() => {
            this.pageNum = 1;
            this.loadFarmlands();
          })
      }
      .width('100%')
      .padding(16)

      // 农田列表
      if (this.isLoading && this.farmlands.length === 0) {
        Column() {
          LoadingProgress()
            .width(48)
            .height(48)
            .color('#4CAF50')

          Text('加载中...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else if (this.farmlands.length === 0) {
        Column() {
          Text('🌾')
            .fontSize(64)

          Text('暂无农田数据')
            .fontSize(16)
            .fontColor('#999999')
            .margin({ top: 16 })

          Button('添加农田')
            .fontSize(14)
            .backgroundColor('#4CAF50')
            .borderRadius(8)
            .height(40)
            .margin({ top: 24 })
            .onClick(() => {
              router.pushUrl({ url: 'pages/FarmlandDetailPage' });
            })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        List() {
          ForEach(this.farmlands, (farmland: Farmland, index: number) => {
            ListItem() {
              this.FarmlandCard(farmland)
            }
            .onClick(() => {
              router.pushUrl({
                url: 'pages/FarmlandDetailPage',
                params: { id: farmland.id }
              });
            })
          })

          // 加载更多
          if (this.hasMore) {
            ListItem() {
              Row() {
                LoadingProgress()
                  .width(24)
                  .height(24)
                  .color('#4CAF50')

                Text('加载更多...')
                  .fontSize(12)
                  .fontColor('#999999')
                  .margin({ left: 8 })
              }
              .width('100%')
              .height(50)
              .justifyContent(FlexAlign.Center)
            }
          }
        }
        .layoutWeight(1)
        .width('100%')
        .padding({ left: 16, right: 16 })
        .scrollBar(BarState.Off)
        .edgeEffect(EdgeEffect.Spring)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  FarmlandCard(farmland: Farmland) {
    Column() {
      Row() {
        Column() {
          Row() {
            Text(farmland.farm || '未命名农田')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor('#333333')

            Text(farmland.state === '正常' ? '正常' : '异常')
              .fontSize(12)
              .fontColor(Color.White)
              .backgroundColor(farmland.state === '正常' ? '#4CAF50' : '#FF9800')
              .borderRadius(4)
              .padding({ left: 8, right: 8, top: 4, bottom: 4 })
              .margin({ left: 12 })
          }

          Text(farmland.address || '暂无地址信息')
            .fontSize(12)
            .fontColor('#999999')
            .margin({ top: 4 })

          if (farmland.district) {
            Text(farmland.district)
              .fontSize(12)
              .fontColor('#666666')
              .margin({ top: 2 })
          }
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Text('>')
          .fontSize(20)
          .fontColor('#CCCCCC')
      }
      .width('100%')

      // 分割线
      Divider()
        .color('#E0E0E0')
        .margin({ top: 12, bottom: 12 })

      // 环境数据
      Row() {
        this.EnvItem('🌡️', '温度', `${farmland.temperature || '--'}°C`, this.getTempColor(farmland.temperature))
        this.EnvItem('💧', '空气湿度', `${farmland.airhumidity || '--'}%`, '#2196F3')
        this.EnvItem('🌱', '土壤湿度', `${farmland.soilhumidity || '--'}%`, '#8BC34A')
        this.EnvItem('☀️', '光照', `${farmland.light || '--'}lux`, '#FF9800')
      }
      .width('100%')
      .justifyContent(FlexAlign.SpaceBetween)

      // 作物信息
      Row() {
        Text(`作物: ${farmland.crop || '未知'}`)
          .fontSize(12)
          .fontColor('#666666')

        Text(`面积: ${farmland.area || '--'} 亩`)
          .fontSize(12)
          .fontColor('#666666')
          .margin({ left: 16 })

        if (farmland.number) {
          Text(`数量: ${farmland.number}`)
            .fontSize(12)
            .fontColor('#666666')
            .margin({ left: 16 })
        }
      }
      .margin({ top: 12 })

      // 设备状态
      Row() {
        if (farmland.filllight) {
          Text(`补光灯: ${farmland.filllight}`)
            .fontSize(10)
            .fontColor('#999999')
        }
        if (farmland.pump) {
          Text(`水泵: ${farmland.pump}`)
            .fontSize(10)
            .fontColor('#999999')
            .margin({ left: 12 })
        }
        if (farmland.monitor) {
          Text(`监控: ${farmland.monitor}`)
            .fontSize(10)
            .fontColor('#999999')
            .margin({ left: 12 })
        }
      }
      .margin({ top: 8 })
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

  @Builder
  EnvItem(icon: string, label: string, value: string, color: string) {
    Column() {
      Text(icon)
        .fontSize(16)

      Text(value)
        .fontSize(14)
        .fontWeight(FontWeight.Bold)
        .fontColor(color)
        .margin({ top: 4 })

      Text(label)
        .fontSize(10)
        .fontColor('#999999')
    }
    .alignItems(HorizontalAlign.Center)
  }

  private getTempColor(temp?: number): string {
    if (!temp) return '#999999';
    if (temp < 15) return '#2196F3';
    if (temp <= 28) return '#4CAF50';
    return '#F44336';
  }

  private async loadFarmlands() {
    this.isLoading = true;
    try {
      const params: QueryParam[] = [
        { key: 'pageNum', value: String(this.pageNum) },
        { key: 'pageSize', value: String(this.pageSize) },
        { key: 'farm', value: this.searchKeyword }
      ];
      const response = await this.httpClient.get<ApiResponse<PaginatedRecords<Farmland>>>('/statistic/page', params);

      if (response.code === '200' && response.data) {
        if (this.pageNum === 1) {
          this.farmlands = response.data.records || [];
        } else {
          this.farmlands = [...this.farmlands, ...(response.data.records || [])];
        }
        this.hasMore = (response.data.records?.length || 0) >= this.pageSize;
      }
    } catch (error) {
      console.error('Load farmlands error:', error);
    } finally {
      this.isLoading = false;
    }
  }
}
