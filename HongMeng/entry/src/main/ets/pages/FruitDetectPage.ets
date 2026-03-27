import router from '@ohos.router';
import imagepicker from '@ohos.multimedia.imagePicker';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse } from '../common/HttpClient';

interface DetectResult {
  className: string;
  confidence: number;
  ripeness: string;
  bbox?: number[];
}

@Entry
@Component
struct FruitDetectPage {
  @State selectedImage: string = '';
  @State isDetecting: boolean = false;
  @State detectResults: DetectResult[] = [];
  @State serviceAvailable: boolean = true;
  @State previewImage: PixelMap | undefined = undefined;

  private httpClient: HttpClient = HttpClient.getInstance();

  build() {
    Column() {
      HeaderBar({
        title: '果蔬成熟度识别',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      Scroll() {
        Column() {
          // 图片预览区域
          Column() {
            if (this.selectedImage) {
              Text(this.selectedImage)
                .fontSize(14)
                .fontColor('#4CAF50')
                .margin({ top: 12 })

              Text('✅ 图片已选择')
                .fontSize(12)
                .fontColor('#999999')
                .margin({ top: 8 })
            } else {
              Column() {
                Text('🍎')
                  .fontSize(64)

                Text('请选择一张果蔬图片')
                  .fontSize(14)
                  .fontColor('#999999')
                  .margin({ top: 16 })
              }
              .width('100%')
              .height(200)
              .justifyContent(FlexAlign.Center)
              .backgroundColor('#F5F5F5')
              .borderRadius(12)
            }
          }
          .width('100%')
          .height(220)
          .backgroundColor('#FAFAFA')
          .borderRadius(12)
          .border({ width: 2, color: '#E0E0E0', style: BorderStyle.Dashed })
          .justifyContent(FlexAlign.Center)
          .onClick(() => {
            this.selectImage();
          })

          // 操作按钮
          Row() {
            Button('📷 选择图片')
              .layoutWeight(1)
              .height(50)
              .backgroundColor('#2196F3')
              .borderRadius(8)
              .fontSize(16)
              .margin({ right: 12 })
              .onClick(() => {
                this.selectImage();
              })

            Button('🔍 开始识别')
              .layoutWeight(1)
              .height(50)
              .backgroundColor('#4CAF50')
              .borderRadius(8)
              .fontSize(16)
              .enabled(this.selectedImage !== '' && !this.isDetecting)
              .onClick(() => {
                this.startDetect();
              })
          }
          .width('100%')
          .padding(16)

          // 识别进度
          if (this.isDetecting) {
            Column() {
              LoadingProgress()
                .width(48)
                .height(48)
                .color('#4CAF50')

              Text('正在分析图片...')
                .fontSize(14)
                .fontColor('#666666')
                .margin({ top: 12 })
            }
            .width('100%')
            .padding(24)
          }

          // 识别结果
          if (this.detectResults.length > 0) {
            Column() {
              Text('识别结果')
                .fontSize(18)
                .fontWeight(FontWeight.Bold)
                .margin({ bottom: 16 })

              ForEach(this.detectResults, (result: DetectResult, index: number) => {
                this.ResultCard(result, index)
              })
            }
            .width('100%')
            .backgroundColor(Color.White)
            .borderRadius(12)
            .padding(16)
            .margin({ top: 16 })
          }

          // 使用说明
          Column() {
            Text('使用说明')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .margin({ bottom: 12 })

            this.TipItem('📸', '选择清晰的果蔬照片')
            this.TipItem('🌈', '保证光线充足')
            this.TipItem('🔍', '确保果蔬主体完整可见')
            this.TipItem('⚙️', '确保Python检测服务已启动')
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
  ResultCard(result: DetectResult, index: number) {
    Column() {
      Row() {
        Text(`${index + 1}. ${result.className}`)
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')

        Blank()

        Text(`${(result.confidence * 100).toFixed(1)}%`)
          .fontSize(14)
          .fontColor('#4CAF50')
          .backgroundColor('#E8F5E9')
          .borderRadius(4)
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
      }
      .width('100%')
      .margin({ bottom: 12 })

      Row() {
        Text('成熟度:')
          .fontSize(14)
          .fontColor('#666666')

        Text(this.getRipenessLabel(result.ripeness))
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.getRipenessColor(result.ripeness))
          .margin({ left: 8 })

        Blank()

        Text(this.getRipenessIcon(result.ripeness))
          .fontSize(24)
      }
      .width('100%')

      // 置信度条
      Column() {
        Row() {
          ForEach([1, 2, 3, 4, 5], (i: number) => {
            Row()
              .width('18%')
              .height(8)
              .backgroundColor(i <= Math.round(result.confidence * 5) ? '#4CAF50' : '#E0E0E0')
              .borderRadius(4)
              .margin({ right: i < 5 ? '2%' : 0 })
          })
        }
        .width('100%')
      }
      .margin({ top: 12 })
    }
    .width('100%')
    .backgroundColor('#FAFAFA')
    .borderRadius(8)
    .padding(12)
    .margin({ bottom: 12 })
  }

  @Builder
  TipItem(icon: string, text: string) {
    Row() {
      Text(icon)
        .fontSize(18)
        .width(30)

      Text(text)
        .fontSize(14)
        .fontColor('#666666')
    }
    .width('100%')
    .margin({ bottom: 8 })
  }

  private getRipenessLabel(ripeness: string): string {
    switch (ripeness) {
      case 'ripe': return '已成熟';
      case 'unripe': return '未成熟';
      case 'overripe': return '过熟';
      default: return ripeness;
    }
  }

  private getRipenessColor(ripeness: string): string {
    switch (ripeness) {
      case 'ripe': return '#4CAF50';
      case 'unripe': return '#FF9800';
      case 'overripe': return '#F44336';
      default: return '#666666';
    }
  }

  private getRipenessIcon(ripeness: string): string {
    switch (ripeness) {
      case 'ripe': return '✅';
      case 'unripe': return '🔶';
      case 'overripe': return '⚠️';
      default: return '❓';
    }
  }

  private async selectImage() {
    try {
      const selectOptions = new imagepicker.PhotoSelectOptions();
      selectOptions.MIMEType = imagepicker.PhotoViewMIMETypes.IMAGE_TYPE;
      selectOptions.maxSelectNumber = 1;

      const picker = new imagepicker.PhotoViewPicker();
      const result = await picker.select(selectOptions);

      if (result.photoUris && result.photoUris.length > 0) {
        this.selectedImage = result.photoUris[0];
        this.detectResults = []; // 清空上次结果
      }
    } catch (error) {
      console.error('Select image error:', error);
    }
  }

  private async startDetect() {
    if (!this.selectedImage) {
      return;
    }

    this.isDetecting = true;
    this.detectResults = [];

    try {
      const response = await this.httpClient.uploadFile('/fruit-detect/analyze', this.selectedImage);
      if (response.code === '200' && response.data) {
        const data = response.data as any;
        if (Array.isArray(data)) {
          this.detectResults = data as DetectResult[];
        } else if (data.detections && Array.isArray(data.detections)) {
          this.detectResults = data.detections as DetectResult[];
        }
      } else {
        console.error('Detect failed:', response.msg);
      }
    } catch (error) {
      console.error('Detect error:', error);
    } finally {
      this.isDetecting = false;
    }
  }
}