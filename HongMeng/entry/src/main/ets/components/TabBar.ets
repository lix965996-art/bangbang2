@Component
export default class TabBar {
  @Link currentTab: number;
  private tabs: Array<{ icon: string; label: string }> = [
    { icon: '🏠', label: '首页' },
    { icon: '🌾', label: '农田' },
    { icon: '📦', label: '库存' },
    { icon: '🍎', label: '识别' },
    { icon: '🤖', label: '助手' }
  ];

  build() {
    Row() {
      ForEach(this.tabs, (tab: { icon: string; label: string }, index: number) => {
        Column() {
          Text(tab.icon)
            .fontSize(22)
            .opacity(this.currentTab === index ? 1.0 : 0.5)

          Text(tab.label)
            .fontSize(10)
            .fontColor(this.currentTab === index ? '#4CAF50' : '#999999')
            .margin({ top: 4 })
        }
        .layoutWeight(1)
        .padding({ top: 8, bottom: 8 })
        .onClick(() => {
          this.currentTab = index;
        })
      })
    }
    .width('100%')
    .height(56)
    .backgroundColor(Color.White)
    .border({ width: { top: 1 }, color: '#E0E0E0' })
  }
}
