@Component
export default class HeaderBar {
  @Prop title: string = '';
  @Prop showBack: boolean = false;
  @Prop rightIcon: string = '';
  @Event onBackClick: () => void = () => {};
  @Event onRightClick: () => void = () => {};

  build() {
    Row() {
      if (this.showBack) {
        Text('←')
          .fontSize(24)
          .fontColor(Color.White)
          .padding(8)
          .onClick(() => {
            this.onBackClick();
          })
      } else {
        Text('')
          .width(40)
      }

      Text(this.title)
        .layoutWeight(1)
        .fontSize(18)
        .fontColor(Color.White)
        .fontWeight(FontWeight.Medium)
        .textAlign(TextAlign.Center)

      if (this.rightIcon) {
        Text(this.rightIcon)
          .fontSize(22)
          .fontColor(Color.White)
          .padding(8)
          .onClick(() => {
            this.onRightClick();
          })
      } else {
        Text('')
          .width(40)
      }
    }
    .width('100%')
    .height(56)
    .backgroundColor('#4CAF50')
    .padding({ left: 8, right: 8 })
  }
}
