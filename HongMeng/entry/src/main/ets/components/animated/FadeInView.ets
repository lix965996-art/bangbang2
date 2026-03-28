import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct FadeInView {
  @Prop duration: number = 500;
  @Prop delay: number = 0;
  @State private opacity: number = 0;
  @State private translateY: number = 20;
  @Slot content: () => void;

  aboutToAppear() {
    setTimeout(() => {
      animateTo({
        duration: this.duration,
        curve: Curve.EaseOut
      }, () => {
        this.opacity = 1;
        this.translateY = 0;
      });
    }, this.delay);
  }

  build() {
    Column() {
      if (this.content) {
        this.content();
      }
    }
    .opacity(this.opacity)
    .translate({ y: this.translateY })
  }
}
