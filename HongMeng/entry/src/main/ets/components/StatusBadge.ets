@Component
export default class StatusBadge {
  @Prop status: string = '';
  @Prop type: 'success' | 'warning' | 'error' | 'info' = 'info';

  private getColor(): string {
    switch (this.type) {
      case 'success':
        return '#E8F5E9';
      case 'warning':
        return '#FFF3E0';
      case 'error':
        return '#FFEBEE';
      case 'info':
      default:
        return '#E3F2FD';
    }
  }

  private getTextColor(): string {
    switch (this.type) {
      case 'success':
        return '#4CAF50';
      case 'warning':
        return '#FF9800';
      case 'error':
        return '#F44336';
      case 'info':
      default:
        return '#2196F3';
    }
  }

  build() {
    Text(this.status)
      .fontSize(12)
      .fontColor(this.getTextColor())
      .backgroundColor(this.getColor())
      .borderRadius(4)
      .padding({ left: 8, right: 8, top: 4, bottom: 4 })
  }
}
