@Component
export default class DataCard {
  @Prop title: string = '';
  @Prop value: string | number = '';
  @Prop unit: string = '';
  @Prop icon: string = '';
  @Prop color: string = '#4CAF50';
  @Prop onClick?: () => void;

  build() {
    Column() {
      Row() {
        Text(this.icon)
          .fontSize(24)
          .margin({ right: 8 })

        Text(this.title)
          .fontSize(14)
          .fontColor('#666666')
      }
      .width('100%')
      .margin({ bottom: 8 })

      Row() {
        Text(String(this.value))
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.color)

        if (this.unit) {
          Text(this.unit)
            .fontSize(14)
            .fontColor('#999999')
            .margin({ left: 4, top: 10 })
        }
      }
      .alignItems(VerticalAlign.Bottom)
    }
    .width('100%')
    .backgroundColor(Color.White)
    .borderRadius(12)
    .padding(16)
    .shadow({
      radius: 8,
      color: 'rgba(0,0,0,0.08)',
      offsetX: 0,
      offsetY: 2
    })
    .onClick(() => {
      if (this.onClick) {
        this.onClick();
      }
    })
  }
}
