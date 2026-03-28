@Component
export default struct AlertCard {
  @Prop alertType: string = '';
  @Prop alertLevel: string = '';
  @Prop message: string = '';
  @Prop createTime: string = '';
  @Prop status: string = 'pending';
  onProcess: () => void = () => {};

  private getLevelColor(): string {
    switch (this.alertLevel) {
      case 'high':
      case 'critical':
        return '#F44336';
      case 'medium':
        return '#FF9800';
      case 'low':
      default:
        return '#4CAF50';
    }
  }

  private getTypeIcon(): string {
    switch (this.alertType) {
      case 'temperature':
        return '🌡️';
      case 'humidity':
        return '💧';
      case 'soil':
        return '🌍';
      case 'light':
        return '☀️';
      case 'pest':
        return '🐛';
      default:
        return '⚠️';
    }
  }

  build() {
    Column() {
      Row() {
        Text(this.getTypeIcon())
          .fontSize(28)
          .margin({ right: 12 })

        Column() {
          Text(this.message)
            .fontSize(14)
            .fontColor('#333333')
            .maxLines(2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })

          Text(this.createTime)
            .fontSize(12)
            .fontColor('#999999')
            .margin({ top: 4 })
        }
        .layoutWeight(1)
        .alignItems(HorizontalAlign.Start)

        if (this.status === 'pending') {
          Button('处理')
            .fontSize(12)
            .backgroundColor('#4CAF50')
            .borderRadius(4)
            .height(28)
            .onClick(() => {
              this.onProcess();
            })
        } else {
          Text('已处理')
            .fontSize(12)
            .fontColor('#999999')
        }
      }
      .width('100%')

      Row() {
        Text(this.alertLevel === 'high' ? '高' : this.alertLevel === 'medium' ? '中' : '低')
          .fontSize(10)
          .fontColor(Color.White)
          .backgroundColor(this.getLevelColor())
          .borderRadius(4)
          .padding({ left: 6, right: 6, top: 2, bottom: 2 })
          .margin({ right: 8 })

        Text(this.alertType)
          .fontSize(10)
          .fontColor('#666666')
      }
      .margin({ top: 8 })
    }
    .width('100%')
    .backgroundColor(Color.White)
    .borderRadius(12)
    .padding(16)
    .margin({ bottom: 12 })
    .shadow({
      radius: 4,
      color: 'rgba(0,0,0,0.05)',
      offsetX: 0,
      offsetY: 2
    })
  }
}
