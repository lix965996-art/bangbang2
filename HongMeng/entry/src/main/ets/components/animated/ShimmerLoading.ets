import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct ShimmerLoading {
  @Prop width: string | number = '100%';
  @Prop height: number = 20;
  @Prop borderRadius: number = ThemeColors.RADIUS_SM;
  @State private shimmerOffset: number = 0;
  private animationTimer: number = -1;

  aboutToAppear() {
    this.startShimmer();
  }

  aboutToDisappear() {
    if (this.animationTimer !== -1) {
      clearInterval(this.animationTimer);
    }
  }

  private startShimmer() {
    this.animationTimer = setInterval(() => {
      this.shimmerOffset = (this.shimmerOffset + 5) % 200;
    }, 30);
  }

  build() {
    Stack() {
      // 基础背景
      Column()
        .width(this.width)
        .height(this.height)
        .backgroundColor('#E2E8F0')
        .borderRadius(this.borderRadius)

      // 闪光效果
      Column()
        .width('30%')
        .height(this.height)
        .linearGradient({
          angle: 90,
          colors: [
            ['rgba(255, 255, 255, 0)', 0.0],
            ['rgba(255, 255, 255, 0.6)', 0.5],
            ['rgba(255, 255, 255, 0)', 1.0]
          ]
        })
        .borderRadius(this.borderRadius)
        .margin({ left: this.shimmerOffset })
    }
    .width(this.width)
    .height(this.height)
    .clip(true)
  }
}