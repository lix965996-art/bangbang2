import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse, Farmland } from '../common/HttpClient';

@Entry
@Component
struct FarmlandDetailPage {
  @State farmland: Farmland = {} as Farmland;
  @State isLoading: boolean = false;
  @State isEditing: boolean = false;
  @State farmlandId: number = 0;

  // 表单字段
  @State farmName: string = '';
  @State area: string = '';
  @State address: string = '';
  @State crop: string = '';
  @State number: string = '';
  @State temperature: string = '';
  @State airHumidity: string = '';
  @State soilHumidity: string = '';
  @State ph: string = '';
  @State carbon: string = '';
  @State light: string = '';
  @State state: string = '正常';

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    const params = router.getParams() as { id?: number };
    if (params && params.id) {
      this.farmlandId = params.id;
      this.isEditing = true;
      this.loadFarmland();
    }
  }

  build() {
    Column() {
      HeaderBar({
        title: this.isEditing ? '编辑农田' : '新增农田',
        showBack: true,
        rightIcon: this.isEditing ? '🗑️' : '',
        onBackClick: () => {
          router.back();
        },
        onRightClick: () => {
          this.deleteFarmland();
        }
      })

      Scroll() {
        Column() {
          // 基本信息
          Column() {
            Text('基本信息')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .margin({ bottom: 16 })

            this.FormInput('农田名称', this.farmName, '请输入农田名称', (value) => {
              this.farmName = value;
            })

            this.FormInput('面积(亩)', this.area, '请输入面积', (value) => {
              this.area = value;
            })

            this.FormInput('地址', this.address, '请输入地址', (value) => {
              this.address = value;
            })

            this.FormInput('作物', this.crop, '请输入作物名称', (value) => {
              this.crop = value;
            })

            this.FormInput('数量', this.number, '请输入数量', (value) => {
              this.number = value;
            })
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .padding(16)
          .margin({ top: 16, left: 16, right: 16 })

          // 环境参数
          Column() {
            Text('环境参数')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .margin({ bottom: 16 })

            this.FormInput('温度(℃)', this.temperature, '请输入温度', (value) => {
              this.temperature = value;
            })

            this.FormInput('空气湿度(%)', this.airHumidity, '请输入空气湿度', (value) => {
              this.airHumidity = value;
            })

            this.FormInput('土壤湿度(%)', this.soilHumidity, '请输入土壤湿度', (value) => {
              this.soilHumidity = value;
            })

            this.FormInput('pH值', this.ph, '请输入pH值', (value) => {
              this.ph = value;
            })

            this.FormInput('CO2(ppm)', this.carbon, '请输入CO2含量', (value) => {
              this.carbon = value;
            })

            this.FormInput('光照(lux)', this.light, '请输入光照强度', (value) => {
              this.light = value;
            })
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .padding(16)
          .margin({ top: 16, left: 16, right: 16 })

          // 生长状态
          Column() {
            Text('生长状态')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .margin({ bottom: 16 })

            Row() {
              this.StateButton('正常', this.state === '正常', () => {
                this.state = '正常';
              })
              this.StateButton('良好', this.state === '良好', () => {
                this.state = '良好';
              })
              this.StateButton('一般', this.state === '一般', () => {
                this.state = '一般';
              })
              this.StateButton('异常', this.state === '异常', () => {
                this.state = '异常';
              })
            }
            .width('100%')
            .justifyContent(FlexAlign.SpaceBetween)
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .padding(16)
          .margin({ top: 16, left: 16, right: 16 })

          // 保存按钮
          Button(this.isEditing ? '保存修改' : '添加农田')
            .width('90%')
            .height(50)
            .backgroundColor('#4CAF50')
            .borderRadius(25)
            .fontSize(16)
            .fontColor(Color.White)
            .margin({ top: 24, bottom: 24 })
            .onClick(() => {
              this.saveFarmland();
            })
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
  FormInput(label: string, value: string, placeholder: string, onChange: (value: string) => void) {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor('#666666')
        .width(100)

      TextInput({ placeholder: placeholder, text: value })
        .layoutWeight(1)
        .height(44)
        .backgroundColor('#F5F5F5')
        .borderRadius(8)
        .padding({ left: 12, right: 12 })
        .onChange(onChange)
    }
    .width('100%')
    .margin({ bottom: 12 })
  }

  @Builder
  StateButton(label: string, isActive: boolean, onClick: () => void) {
    Text(label)
      .fontSize(14)
      .fontColor(isActive ? Color.White : '#666666')
      .backgroundColor(isActive ? '#4CAF50' : '#F5F5F5')
      .borderRadius(8)
      .padding({ left: 16, right: 16, top: 8, bottom: 8 })
      .onClick(onClick)
  }

  private async loadFarmland() {
    this.isLoading = true;
    try {
      const response = await this.httpClient.get<ApiResponse<Farmland>>(`/statistic/${this.farmlandId}`);
      if (response.code === '200' && response.data) {
        this.farmland = response.data;
        this.farmName = this.farmland.farm || '';
        this.area = this.farmland.area || '';
        this.address = this.farmland.address || '';
        this.crop = this.farmland.crop || '';
        this.number = String(this.farmland.number || '');
        this.temperature = String(this.farmland.temperature || '');
        this.airHumidity = String(this.farmland.airhumidity || '');
        this.soilHumidity = String(this.farmland.soilhumidity || '');
        this.ph = String(this.farmland.ph || '');
        this.carbon = String(this.farmland.carbon || '');
        this.light = String(this.farmland.light || '');
        this.state = this.farmland.state || '正常';
      }
    } catch (error) {
      console.error('Load farmland error:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private async saveFarmland() {
    if (!this.farmName) {
      return;
    }

    const data: Farmland = {
      id: this.isEditing ? this.farmlandId : undefined,
      farm: this.farmName,
      area: this.area,
      address: this.address,
      crop: this.crop,
      number: this.number ? parseInt(this.number) : undefined,
      temperature: this.temperature ? parseFloat(this.temperature) : undefined,
      airhumidity: this.airHumidity ? parseInt(this.airHumidity) : undefined,
      soilhumidity: this.soilHumidity ? parseInt(this.soilHumidity) : undefined,
      ph: this.ph ? parseFloat(this.ph) : undefined,
      carbon: this.carbon ? parseInt(this.carbon) : undefined,
      light: this.light ? parseInt(this.light) : undefined,
      state: this.state
    };

    try {
      const response = await this.httpClient.post<ApiResponse<null>>('/statistic', data);
      if (response.code === '200') {
        router.back();
      }
    } catch (error) {
      console.error('Save farmland error:', error);
    }
  }

  private async deleteFarmland() {
    try {
      const response = await this.httpClient.delete<ApiResponse<null>>('/statistic', this.farmlandId);
      if (response.code === '200') {
        router.back();
      }
    } catch (error) {
      console.error('Delete farmland error:', error);
    }
  }
}