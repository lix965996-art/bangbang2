import router from '@ohos.router';
import { picker } from '@kit.CoreFileKit';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse } from '../common/HttpClient';
import { ThemeColors } from '../common/ThemeColors';
import GradientButton from '../components/glass/GradientButton';
import CircularProgress from '../components/charts/CircularProgress';

interface DetectResult {
  className: string;
  confidence: number;
  ripeness: string;
  bbox?: number[];
}

interface DetectResponse {
  detections?: DetectResult[];
}

@Entry
@Component
struct FruitDetectPage {
  @State selectedImage: string = '';
  @State isDetecting: boolean = false;
  @State detectResults: DetectResult[] = [];
  @State serviceAvailable: boolean = true;
  @State scanAnimation: number = 0;
  @State scanLineY: number = 0;

  private httpClient: HttpClient = HttpClient.getInstance();
  private scanTimer: number = -1;

  aboutToAppear() {
    this.startScanAnimation();
  }

  aboutToDisappear() {
    if (this.scanTimer !== -1) {
      clearInterval(this.scanTimer);
    }
  }

  build() {
    Column() {
      HeaderBar({
        title: '果蔬识别',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      Scroll() {
        Column() {
          // 图片预览区域
          if (this.selectedImage) {
            Stack() {
              Column() {
                // 扫描动画叠加层
                if (this.isDetecting) {
                  Column() {
                    Row() {
                      Row()
                        .width(2)
                        .height('100%')
                        .backgroundColor('#00FF00')
                        .opacity(0.8)
                      Blank()
                      Row()
                        .width(2)
                        .height('100%')
                        .backgroundColor('#00FF00')
                        .opacity(0.8)
                    }
                    .width('100%')
                    .height(220)

                    Row() {
                      Row()
                        .width(2)
                        .height(2)
                        .backgroundColor('#00FF00')
                        .shadow({ radius: 10, color: '#00FF00', offsetX: 0, offsetY: 0 })
                      Row()
                        .layoutWeight(1)
                        .height(2)
                        .backgroundColor('#00FF00')
                        .shadow({ radius: 10, color: '#00FF00', offsetX: 0, offsetY: 0 })
                      Row()
                        .width(2)
                        .height(2)
                        .backgroundColor('#00FF00')
                        .shadow({ radius: 10, color: '#00FF00', offsetX: 0, offsetY: 0 })
                    }
                    .width('100%')
                    .position({ y: this.scanLineY, x: 0 })
                  }
                  .width('100%')
                  .height(220)
                  .borderRadius(16)
                  .backgroundColor('rgba(0,0,0,0.5)')
                }
              }
              .width('100%')
              .height(220)
              .borderRadius(16)
            }
            .width('100%')
            .margin({ top: 16, left: 16, right: 16 })
          } else {
            // AR风格选择区域
            Column() {
              Stack() {
                Row() {
                  Column() {
                    Row()
                      .width(30)
                      .height(3)
                      .backgroundColor('#22C55E')
                    Column()
                      .width(3)
                      .height(30)
                      .backgroundColor('#22C55E')
                  }
                  .alignItems(HorizontalAlign.Start)

                  Blank()

                  Column() {
                    Row()
                      .width(30)
                      .height(3)
                      .backgroundColor('#22C55E')
                    Column()
                      .width(3)
                      .height(30)
                      .backgroundColor('#22C55E')
                  }
                  .alignItems(HorizontalAlign.End)
                }
                .width('100%')
                .justifyContent(FlexAlign.SpaceBetween)

                Column() {
                  Text('🍎')
                    .fontSize(48)

                  Text('点击选择果蔬图片')
                    .fontSize(16)
                    .fontColor('rgba(255,255,255,0.8)')
                    .margin({ top: 12 })

                  Text('支持 AI 智能识别成熟度')
                    .fontSize(12)
                    .fontColor('rgba(255,255,255,0.5)')
                    .margin({ top: 4 })
                }
                .layoutWeight(1)
                .justifyContent(FlexAlign.Center)

                Row() {
                  Column() {
                    Row()
                      .width(30)
                      .height(3)
                      .backgroundColor('#22C55E')
                    Column()
                      .width(3)
                      .height(30)
                      .backgroundColor('#22C55E')
                  }
                  .alignItems(HorizontalAlign.Start)

                  Blank()

                  Column() {
                    Row()
                      .width(30)
                      .height(3)
                      .backgroundColor('#22C55E')
                    Column()
                      .width(3)
                      .height(30)
                      .backgroundColor('#22C55E')
                  }
                  .alignItems(HorizontalAlign.End)
                }
                .width('100%')
                .justifyContent(FlexAlign.SpaceBetween)
              }
              .width('100%')
              .height(220)
            }
            .width('100%')
            .margin({ top: 16, left: 16, right: 16 })
            .backgroundColor('rgba(34, 197, 94, 0.1)')
            .borderRadius(16)
            .border({ width: 2, color: 'rgba(34, 197, 94, 0.5)', style: BorderStyle.Dashed })
            .onClick(() => {
              this.selectImage();
            })
          }

          // 操作按钮
          Row() {
            GradientButton({
              text: '📷 选择图片',
              btnWidth: '45%',
              btnHeight: 50,
              fontSize: 15,
              gradientColors: ['#3B82F6', '#1D4ED8'],
              onClick: () => {
                this.selectImage();
              }
            })

            GradientButton({
              text: this.isDetecting ? '识别中...' : '🔍 开始识别',
              btnWidth: '45%',
              btnHeight: 50,
              fontSize: 15,
              gradientColors: ['#22C55E', '#16A34A'],
              disabled: this.selectedImage === '' || this.isDetecting,
              onClick: () => {
                this.startDetect();
              }
            })
          }
          .width('100%')
          .justifyContent(FlexAlign.SpaceEvenly)
          .padding({ top: 16, bottom: 16 })

          // 识别进度
          if (this.isDetecting) {
            Column() {
              Row() {
                LoadingProgress()
                  .width(32)
                  .height(32)
                  .color('#22C55E')

                Text('AI正在分析图片...')
                  .fontSize(14)
                  .fontColor(ThemeColors.TEXT_PRIMARY)
                  .margin({ left: 12 })
              }

              Row() {
                ForEach([0, 1, 2], (i: number) => {
                  Column()
                    .width(8)
                    .height(8)
                    .backgroundColor(i <= this.scanAnimation % 3 ? '#22C55E' : 'rgba(34,197,94,0.3)')
                    .borderRadius(4)
                    .margin({ right: 4 })
                })
              }
              .margin({ top: 12 })
            }
            .width('100%')
            .padding(20)
            .backgroundColor('rgba(34, 197, 94, 0.1)')
            .borderRadius(12)
            .margin({ left: 16, right: 16 })
          }

          // 识别结果
          if (this.detectResults.length > 0) {
            Column() {
              Row() {
                Text('🎯 识别结果')
                  .fontSize(18)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(ThemeColors.TEXT_PRIMARY)

                Blank()

                Text(`${this.detectResults.length} 个结果`)
                  .fontSize(14)
                  .fontColor(ThemeColors.TEXT_SECONDARY)
              }
              .width('100%')
              .margin({ bottom: 16 })

              ForEach(this.detectResults, (result: DetectResult, index: number) => {
                this.ResultCard(result, index)
              })
            }
            .width('100%')
            .backgroundColor('#FFFFFF')
            .borderRadius(16)
            .padding(16)
            .margin({ top: 16, left: 16, right: 16 })
            .shadow({
              radius: 8,
              color: 'rgba(0,0,0,0.1)',
              offsetX: 0,
              offsetY: 2
            })
          }

          // 使用说明
          Column() {
            Row() {
              Text('💡 使用技巧')
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor(ThemeColors.TEXT_PRIMARY)

              Blank()

              Text('提高识别准确率')
                .fontSize(12)
                .fontColor(ThemeColors.TEXT_TERTIARY)
            }
            .width('100%')
            .margin({ bottom: 12 })

            this.TipItem('📸', '选择清晰的果蔬照片')
            this.TipItem('☀️', '保证光线充足均匀')
            this.TipItem('🎯', '确保果蔬主体完整可见')
            this.TipItem('🔧', '确保Python检测服务已启动')
          }
          .width('100%')
          .backgroundColor('#FFFFFF')
          .borderRadius(16)
          .padding(16)
          .margin({ top: 16, left: 16, right: 16, bottom: 20 })
          .shadow({
            radius: 4,
            color: 'rgba(0,0,0,0.05)',
            offsetX: 0,
            offsetY: 2
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
  ResultCard(result: DetectResult, index: number) {
    Column() {
      Row() {
        Column() {
          Text(`${index + 1}. ${result.className}`)
            .fontSize(18)
            .fontWeight(FontWeight.Bold)
            .fontColor(ThemeColors.TEXT_PRIMARY)

          Text(this.getRipenessLabel(result.ripeness))
            .fontSize(14)
            .fontColor(this.getRipenessColor(result.ripeness))
            .margin({ top: 4 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        // 置信度环形图
        Column() {
          CircularProgress({
            progress: result.confidence * 100,
            circleSize: 60,
            strokeWidth: 4,
            progressColor: this.getConfidenceColor(result.confidence)
          })
        }
      }
      .width('100%')
      .margin({ bottom: 16 })

      // 置信度进度条
      Column() {
        Row() {
          Text('置信度')
            .fontSize(12)
            .fontColor(ThemeColors.TEXT_SECONDARY)

          Text(`${(result.confidence * 100).toFixed(1)}%`)
            .fontSize(12)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.getConfidenceColor(result.confidence))
        }
        .width('100%')
        .justifyContent(FlexAlign.SpaceBetween)
        .margin({ bottom: 8 })

        Row() {
          Column() {
            Row()
              .width('100%')
              .height(8)
              .backgroundColor(this.getConfidenceColor(result.confidence))
              .borderRadius(4)
          }
          .layoutWeight(1)
          .backgroundColor('#F0F0F0')
          .borderRadius(4)
          .height(8)
        }
        .width('100%')
      }
      .width('100%')
      .padding(12)
      .backgroundColor('#FAFAFA')
      .borderRadius(12)
    }
    .width('100%')
    .backgroundColor('#F8FAFC')
    .borderRadius(16)
    .padding(16)
    .margin({ bottom: 12 })
    .border({ width: 1, color: '#E5E7EB' })
  }

  @Builder
  TipItem(icon: string, text: string) {
    Row() {
      Column() {
        Text(icon)
          .fontSize(20)
      }
      .width(40)
      .height(40)
      .backgroundColor('#F0FDF4')
      .borderRadius(10)
      .justifyContent(FlexAlign.Center)

      Text(text)
        .fontSize(14)
        .fontColor(ThemeColors.TEXT_SECONDARY)
        .layoutWeight(1)
        .margin({ left: 12 })
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#FAFAFA')
    .borderRadius(12)
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
      case 'ripe': return '#22C55E';
      case 'unripe': return '#F59E0B';
      case 'overripe': return '#EF4444';
      default: return '#6B7280';
    }
  }

  private getConfidenceColor(confidence: number): string {
    if (confidence >= 0.8) return '#22C55E';
    if (confidence >= 0.5) return '#F59E0B';
    return '#EF4444';
  }

  private startScanAnimation() {
    this.scanTimer = setInterval(() => {
      this.scanAnimation = (this.scanAnimation + 1) % 3;
      this.scanLineY = (this.scanLineY + 3) % 200;
      if (this.scanLineY > 200) {
        this.scanLineY = 0;
      }
    }, 100);
  }

  private async selectImage() {
    try {
      const photoSelectOptions = new picker.PhotoSelectOptions();
      photoSelectOptions.MIMEType = picker.PhotoViewMIMETypes.IMAGE_TYPE;
      photoSelectOptions.maxSelectNumber = 1;

      const photoPicker = new picker.PhotoViewPicker();
      const result = await photoPicker.select(photoSelectOptions);

      if (result.photoUris && result.photoUris.length > 0) {
        this.selectedImage = result.photoUris[0];
        this.detectResults = [];
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
        const data = response.data as DetectResponse;
        if (data.detections && Array.isArray(data.detections)) {
          this.detectResults = data.detections;
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
