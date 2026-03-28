import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct AIAvatar {
  @Prop avatarSize: number = 48;
  @Prop showPulse: boolean = true;
  @State private pulseScale: number = 1.0;
  @State private pulseOpacity: number = 0.6;
  private animationTimer: number = -1;

  aboutToAppear() {
    if (this.showPulse) {
      this.startPulse();
    }
  }

  aboutToDisappear() {
    if (this.animationTimer !== -1) {
      clearInterval(this.animationTimer);
    }
  }

  private startPulse() {
    this.animationTimer = setInterval(() => {
      animateTo({
        duration: 1500,
        curve: Curve.EaseInOut,
        iterations: 1,
        playMode: PlayMode.Normal
      }, () => {
        this.pulseScale = 1.3;
        this.pulseOpacity = 0;
      });

      setTimeout(() => {
        this.pulseScale = 1.0;
        this.pulseOpacity = 0.6;
      }, 1500);
    }, 2000);
  }

  build() {
    Stack() {
      // 脉冲效果
      if (this.showPulse) {
        Column()
          .width(this.avatarSize * this.pulseScale)
          .height(this.avatarSize * this.pulseScale)
          .borderRadius((this.avatarSize * this.pulseScale) / 2)
          .border({ width: 2, color: `rgba(139, 92, 246, ${this.pulseOpacity})` })
          .opacity(this.pulseOpacity)
      }

      // AI头像主体
      Column() {
        Text('🤖')
          .fontSize(this.avatarSize * 0.5)
      }
      .width(this.avatarSize)
      .height(this.avatarSize)
      .borderRadius(this.avatarSize / 2)
      .justifyContent(FlexAlign.Center)
      .linearGradient({
        angle: 135,
        colors: [
          [ThemeColors.ACCENT_AI, 0.0],
          ['#EC4899', 1.0]
        ]
      })
      .shadow({
        radius: 12,
        color: 'rgba(139, 92, 246, 0.4)',
        offsetX: 0,
        offsetY: 4
      })
    }
    .width(this.avatarSize * 1.5)
    .height(this.avatarSize * 1.5)
    .alignContent(Alignment.Center)
  }
}
