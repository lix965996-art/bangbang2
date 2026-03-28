import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct CircularProgress {
  @Prop progress: number = 0; // 0-100
  @Prop circleSize: number = 120;
  @Prop strokeWidth: number = 12;
  @Prop bgColor: string = '#E2E8F0';
  @Prop gradientColors: string[] = ThemeColors.PRIMARY_GRADIENT;
  @Prop showPercentage: boolean = true;
  @Prop label: string = '';
  @Prop valueText: string = '';
  @Prop progressColor: string = gradientColors[0];
  @State private animatedProgress: number = 0;

  aboutToAppear() {
    this.animateProgress();
  }

  private animateProgress() {
    animateTo({
      duration: 1000,
      curve: Curve.EaseOut
    }, () => {
      this.animatedProgress = this.progress;
    });
  }

  build() {
    Stack() {
      // 背景圆环
      Column()
        .width(this.circleSize)
        .height(this.circleSize)
        .borderRadius(this.circleSize / 2)
        .border({
          width: this.strokeWidth,
          color: this.bgColor
        })

      // 进度圆环 (简化版，使用边框模拟)
      Column()
        .width(this.circleSize - this.strokeWidth)
        .height(this.circleSize - this.strokeWidth)
        .borderRadius((this.circleSize - this.strokeWidth) / 2)
        .border({
          width: this.strokeWidth,
          color: this.progressColor
        })
        .opacity(this.animatedProgress / 100)

      // 中心内容
      Column() {
        if (this.showPercentage) {
          Text(`${Math.round(this.animatedProgress)}%`)
            .fontSize(this.circleSize * 0.2)
            .fontWeight(FontWeight.Bold)
            .fontColor(ThemeColors.TEXT_PRIMARY)
        } else if (this.valueText) {
          Text(this.valueText)
            .fontSize(this.circleSize * 0.2)
            .fontWeight(FontWeight.Bold)
            .fontColor(ThemeColors.TEXT_PRIMARY)
        }

        if (this.label) {
          Text(this.label)
            .fontSize(this.circleSize * 0.1)
            .fontColor(ThemeColors.TEXT_SECONDARY)
            .margin({ top: 4 })
        }
      }
      .justifyContent(FlexAlign.Center)
    }
    .width(this.circleSize)
    .height(this.circleSize)
  }
}
