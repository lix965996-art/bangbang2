import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct AnimatedBackground {
  @Prop gradientColors: [string, number][] = [['#E0F2FE', 0.0], ['#DBEAFE', 0.5], ['#E0E7FF', 1.0]];
  @Prop gradientAngle: number = 180;
  @State private offset1: number = 0.0;
  @State private offset2: number = 0.5;
  @State private offset3: number = 1.0;
  private animationTimer: number = -1;

  aboutToAppear() {
    this.startAnimation();
  }

  aboutToDisappear() {
    if (this.animationTimer !== -1) {
      clearInterval(this.animationTimer);
    }
  }

  private startAnimation() {
    this.animationTimer = setInterval(() => {
      // 微妙的颜色位置偏移动画
      this.offset1 = (this.offset1 + 0.002) % 0.1;
      this.offset2 = 0.5 + Math.sin(Date.now() / 3000) * 0.05;
      this.offset3 = 1.0 - this.offset1;
    }, 50);
  }

  build() {
    Column() {
      // 浮动装饰圆
      Row() {
        // 左上角装饰
        Column()
          .width(200)
          .height(200)
          .backgroundColor('rgba(34, 197, 94, 0.1)')
          .borderRadius(100)
          .position({ x: -50, y: 100 })
          .blur(60)

        // 右下角装饰
        Column()
          .width(300)
          .height(300)
          .backgroundColor('rgba(59, 130, 246, 0.1)')
          .borderRadius(150)
          .position({ x: 250, y: 500 })
          .blur(80)

        // 中心装饰
        Column()
          .width(150)
          .height(150)
          .backgroundColor('rgba(139, 92, 246, 0.08)')
          .borderRadius(75)
          .position({ x: 150, y: 300 })
          .blur(50)
      }
      .width('100%')
      .height('100%')
    }
    .width('100%')
    .height('100%')
    .linearGradient({
      angle: this.gradientAngle,
      colors: [
        [this.gradientColors[0][0], this.offset1],
        [this.gradientColors[1][0], this.offset2],
        [this.gradientColors[2][0], this.offset3]
      ]
    })
  }
}
