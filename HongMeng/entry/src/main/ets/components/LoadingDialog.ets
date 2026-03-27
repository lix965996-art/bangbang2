@Component
export default class LoadingDialog {
  @Prop message: string = '加载中...';
  @Prop visible: boolean = false;

  build() {
    if (this.visible) {
      Column() {
        Column() {
          LoadingProgress()
            .width(48)
            .height(48)
            .color('#4CAF50')

          Text(this.message)
            .fontSize(14)
            .fontColor('#666666')
            .margin({ top: 12 })
        }
        .backgroundColor(Color.White)
        .borderRadius(8)
        .padding(24)
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      }
      .width('100%')
      .height('100%')
      .backgroundColor('rgba(0,0,0,0.4)')
      .justifyContent(FlexAlign.Center)
      .alignItems(HorizontalAlign.Center)
    }
  }
}
