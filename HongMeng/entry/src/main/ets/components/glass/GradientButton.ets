import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct GradientButton {
  @Prop text: string = '';
  @Prop btnWidth: string | number = '100%';
  @Prop btnHeight: number = 48;
  @Prop fontSize: number = 16;
  @Prop fontColor: string = ThemeColors.TEXT_LIGHT;
  @Prop btnRadius: number = ThemeColors.RADIUS_XL;
  @Prop gradientColors: string[] = ThemeColors.PRIMARY_GRADIENT;
  @Prop gradientAngle: number = 135;
  @Prop disabled: boolean = false;
  @State private isPressed: boolean = false;
  @State private currentScale: number = 1.0;
  onClick?: () => void;

  build() {
    Text(this.text)
      .width(this.btnWidth)
      .height(this.btnHeight)
      .fontSize(this.fontSize)
      .fontColor(this.fontColor)
      .fontWeight(FontWeight.Medium)
      .textAlign(TextAlign.Center)
      .borderRadius(this.btnRadius)
      .opacity(this.disabled ? 0.6 : 1.0)
      .scale({ x: this.currentScale, y: this.currentScale })
      .linearGradient({
        angle: this.gradientAngle,
        colors: [
          [this.gradientColors[0], 0.0],
          [this.gradientColors[1], 1.0]
        ]
      })
      .shadow({
        radius: this.isPressed ? 8 : 16,
        color: 'rgba(34, 197, 94, 0.3)',
        offsetX: 0,
        offsetY: this.isPressed ? 2 : 4
      })
      .onClick(() => {
        if (!this.disabled && this.onClick) {
          this.onClick();
        }
      })
      .onTouch((event: TouchEvent) => {
        if (event.type === TouchType.Down && !this.disabled) {
          this.currentScale = 0.96;
          this.isPressed = true;
        } else if (event.type === TouchType.Up || event.type === TouchType.Cancel) {
          this.currentScale = 1.0;
          this.isPressed = false;
        }
      })
  }
}
