import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct TypewriterText {
  @Prop fullText: string = '';
  @Prop speed: number = 50; // 每个字符的延迟(毫秒)
  @State private displayedText: string = '';
  @State private isTyping: boolean = false;
  private charIndex: number = 0;
  private animationTimer: number = -1;

  aboutToAppear() {
    this.startTyping();
  }

  aboutToDisappear() {
    if (this.animationTimer !== -1) {
      clearInterval(this.animationTimer);
    }
  }

  private startTyping() {
    this.charIndex = 0;
    this.displayedText = '';
    this.isTyping = true;

    this.animationTimer = setInterval(() => {
      if (this.charIndex < this.fullText.length) {
        this.displayedText += this.fullText[this.charIndex];
        this.charIndex++;
      } else {
        clearInterval(this.animationTimer);
        this.animationTimer = -1;
        this.isTyping = false;
      }
    }, this.speed);
  }

  build() {
    Row() {
      Text(this.displayedText)
        .fontSize(15)
        .fontColor(ThemeColors.TEXT_PRIMARY)
        .lineHeight(24)

      // 闪烁光标
      if (this.isTyping) {
        Text('|')
          .fontSize(15)
          .fontColor(ThemeColors.ACCENT_AI)
          .opacity(1)
          .animation({
            duration: 500,
            curve: Curve.EaseInOut,
            iterations: -1,
            playMode: PlayMode.Alternate
          })
      }
    }
    .alignItems(VerticalAlign.Top)
  }
}
