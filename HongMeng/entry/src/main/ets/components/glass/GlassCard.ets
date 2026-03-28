import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct GlassCard {
  @Prop cardWidth: string | number = '100%';
  @Prop cardHeight: string | number = 'auto';
  @Prop cardPadding: number = 16;
  @Prop cardRadius: number = ThemeColors.RADIUS_LG;
  @Prop cardBgColor: string = ThemeColors.GLASS_WHITE;
  @Prop cardBorder: string = ThemeColors.GLASS_BORDER;
  @Prop shadowValue: number = 16;
  @BuilderParam content: () => void = () => {};

  build() {
    Column() {
      this.content();
    }
    .width(this.cardWidth)
    .height(this.cardHeight)
    .padding(this.cardPadding)
    .backgroundColor(this.cardBgColor)
    .borderRadius(this.cardRadius)
    .border({ width: 1, color: this.cardBorder })
    .backdropBlur(20)
    .shadow({
      radius: this.shadowValue,
      color: ThemeColors.GLASS_SHADOW,
      offsetX: 0,
      offsetY: 4
    })
  }
}
