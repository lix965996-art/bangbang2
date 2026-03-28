import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse, Inventory, QueryParam, PaginatedRecords } from '../common/HttpClient';

@Entry
@Component
struct InventoryPage {
  @State inventories: Inventory[] = [];
  @State searchKeyword: string = '';
  @State isLoading: boolean = false;
  @State pageNum: number = 1;
  @State pageSize: number = 20;
  @State hasMore: boolean = true;

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadInventories();
  }

  build() {
    Column() {
      HeaderBar({
        title: '库存管理',
        showBack: true,
        rightIcon: '+',
        onBackClick: () => {
          router.back();
        }
      })

      // 搜索栏
      Row() {
        TextInput({ placeholder: '搜索物资...', text: this.searchKeyword })
          .layoutWeight(1)
          .height(44)
          .backgroundColor('#F5F5F5')
          .borderRadius(22)
          .padding({ left: 16, right: 16 })
          .onChange((value: string) => {
            this.searchKeyword = value;
            this.pageNum = 1;
            this.loadInventories();
          })
      }
      .width('100%')
      .padding(16)

      // 库存列表
      if (this.isLoading && this.inventories.length === 0) {
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
      } else if (this.inventories.length === 0) {
        Column() {
          Text('📦')
            .fontSize(64)

          Text('暂无库存数据')
            .fontSize(16)
            .fontColor('#999999')
            .margin({ top: 16 })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        List() {
          ForEach(this.inventories, (inventory: Inventory, index: number) => {
            ListItem() {
              this.InventoryCard(inventory)
            }
          })
        }
        .layoutWeight(1)
        .width('100%')
        .padding({ left: 16, right: 16 })
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  InventoryCard(inventory: Inventory) {
    Column() {
      Row() {
        Column() {
          Text(inventory.produce || '未命名物资')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor('#333333')

          if (inventory.warehouse) {
            Text(`仓库: ${inventory.warehouse}`)
              .fontSize(12)
              .fontColor('#999999')
              .margin({ top: 4 })
          }

          if (inventory.keeper) {
            Text(`管理员: ${inventory.keeper}`)
              .fontSize(12)
              .fontColor('#999999')
              .margin({ top: 2 })
          }
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Column() {
          Text(`${inventory.number || 0}`)
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.getStockColor(inventory))

          Text(inventory.unit || '件')
            .fontSize(12)
            .fontColor('#999999')
        }
        .alignItems(HorizontalAlign.End)
      }
      .width('100%')

      // 库存状态
      if (inventory.safeStock && inventory.number !== undefined) {
        Row() {
          if (inventory.number < inventory.safeStock) {
            Text('⚠️ 库存不足')
              .fontSize(12)
              .fontColor('#F44336')
              .backgroundColor('#FFEBEE')
              .borderRadius(4)
              .padding({ left: 8, right: 8, top: 4, bottom: 4 })
          } else {
            Text('✅ 库存充足')
              .fontSize(12)
              .fontColor('#4CAF50')
              .backgroundColor('#E8F5E9')
              .borderRadius(4)
              .padding({ left: 8, right: 8, top: 4, bottom: 4 })
          }

          Text(`安全库存: ${inventory.safeStock}`)
            .fontSize(12)
            .fontColor('#999999')
            .margin({ left: 12 })
        }
        .margin({ top: 12 })
      }
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

  private getStockColor(inventory: Inventory): string {
    if (!inventory.number) return '#999999';
    if (inventory.safeStock && inventory.number < inventory.safeStock) return '#F44336';
    return '#4CAF50';
  }

  private async loadInventories() {
    this.isLoading = true;
    try {
      const params: QueryParam[] = [
        { key: 'pageNum', value: String(this.pageNum) },
        { key: 'pageSize', value: String(this.pageSize) },
        { key: 'produce', value: this.searchKeyword }
      ];
      const response = await this.httpClient.get<ApiResponse<PaginatedRecords<Inventory>>>('/inventory/page', params);

      if (response.code === '200' && response.data) {
        if (this.pageNum === 1) {
          this.inventories = response.data.records || [];
        } else {
          this.inventories = [...this.inventories, ...(response.data.records || [])];
        }
        this.hasMore = (response.data.records?.length || 0) >= this.pageSize;
      }
    } catch (error) {
      console.error('Load inventories error:', error);
    } finally {
      this.isLoading = false;
    }
  }
}