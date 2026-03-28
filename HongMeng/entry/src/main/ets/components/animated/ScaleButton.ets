import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct ScaleButton {
  @Prop btnWidth: string | number = '100%';
  @Prop btnHeight: string | number = 'auto';
  @Prop btnPadding: number = 16;
  @Prop btnBgColor: string = ThemeColors.BG_CARD;
  @Prop btnRadius: number = ThemeColors.RADIUS_MD;
  @Prop scaleTarget: number = 0.96;
  @State private currentScale: number = 1.0;
  @BuilderParam content: () => void = () => {};
  onClick?: () => void;

  build() {
    Column() {
      this.content();
    }
    .width(this.btnWidth)
    .height(this.btnHeight)
    .padding(this.btnPadding)
    .backgroundColor(this.btnBgColor)
    .borderRadius(this.btnRadius)
    .scale({ x: this.currentScale, y: this.currentScale })
    .onClick(() => {
      if (this.onClick) {
        this.onClick();
      }
    })
    .gesture(
      TapGesture()
        .onAction(() => {
          if (this.onClick) {
            this.onClick();
          }
        })
    )
    .onTouch((event: TouchEvent) => {
      if (event.type === TouchType.Down) {
        animateTo({
          duration: 100,
          curve: Curve.EaseOut
        }, () => {
          this.currentScale = this.scaleTarget;
        });
      } else if (event.type === TouchType.Up || event.type === TouchType.Cancel) {
        animateTo({
          duration: 150,
          curve: Curve.EaseOut
        }, () => {
          this.currentScale = 1.0;
        });
      }
    })
  }
}
