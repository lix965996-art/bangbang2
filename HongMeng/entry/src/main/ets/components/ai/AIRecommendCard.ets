import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct AIRecommendCard {
  @Prop title: string = '';
  @Prop description: string = '';
  @Prop icon: string = '💡';
  @Prop type: 'tip' | 'warning' | 'info' | 'success' = 'tip';
  @State private isExpanded: boolean = false;
  onClick?: () => void;

  private getTypeColors(): { bg: string, border: string, text: string } {
    switch (this.type) {
      case 'warning':
        return { bg: 'rgba(245, 158, 11, 0.1)', border: ThemeColors.ACCENT_WARNING, text: ThemeColors.ACCENT_WARNING };
      case 'info':
        return { bg: 'rgba(6, 182, 212, 0.1)', border: ThemeColors.ACCENT_INFO, text: ThemeColors.ACCENT_INFO };
      case 'success':
        return { bg: 'rgba(16, 185, 129, 0.1)', border: ThemeColors.ACCENT_SUCCESS, text: ThemeColors.ACCENT_SUCCESS };
      default:
        return { bg: 'rgba(139, 92, 246, 0.1)', border: ThemeColors.ACCENT_AI, text: ThemeColors.ACCENT_AI };
    }
  }

  build() {
    Column() {
      Row() {
        Text(this.icon)
          .fontSize(24)
          .margin({ right: 12 })

        Column() {
          Text(this.title)
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor(ThemeColors.TEXT_PRIMARY)

          Text(this.description)
            .fontSize(13)
            .fontColor(ThemeColors.TEXT_SECONDARY)
            .margin({ top: 4 })
            .maxLines(this.isExpanded ? 100 : 2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .layoutWeight(1)
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')
      .alignItems(VerticalAlign.Top)
    }
    .width('100%')
    .padding(16)
    .backgroundColor(this.getTypeColors().bg)
    .borderRadius(ThemeColors.RADIUS_MD)
    .border({ width: 1, color: this.getTypeColors().border })
    .onClick(() => {
      this.isExpanded = !this.isExpanded;
      if (this.onClick) {
        this.onClick();
      }
    })
  }
}
