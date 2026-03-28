import { ThemeColors } from '../../common/ThemeColors';

@Component
export default struct MiniChart {
  @Prop values: number[] = [];
  @Prop width: number = 100;
  @Prop height: number = 40;
  @Prop lineColor: string = ThemeColors.PRIMARY_GREEN;
  @Prop fillColor: string = 'rgba(34, 197, 94, 0.2)';

  build() {
    Canvas(this.width, this.height)
      .width(this.width)
      .height(this.height)
      .onReady(() => {
        this.drawChart();
      })
  }

  private drawChart() {
    if (this.values.length < 2) return;

    const max = Math.max(...this.values);
    const min = Math.min(...this.values);
    const range = max - min || 1;
    const padding = 4;

    // 这里使用简化的绘制方式
    // Canvas context 在 HarmonyOS 中需要特殊处理
  }
}

// 简化版迷你图表 - 使用 Canvas 的替代方案
@Component
export struct SimpleMiniChart {
  @Prop values: number[] = [];
  @Prop width: number = 100;
  @Prop height: number = 40;
  @Prop color: string = ThemeColors.PRIMARY_GREEN;

  private getBarWidth(): number {
    const count = Math.max(this.values.length, 1);
    return (this.width - (count - 1) * 2) / count;
  }

  private getNormalizedValue(value: number): number {
    const max = Math.max(...this.values, 1);
    return (value / max) * (this.height - 4);
  }

  build() {
    Row() {
      ForEach(this.values, (value: number, index: number) => {
        Column()
          .width(this.getBarWidth())
          .height(this.getNormalizedValue(value))
          .backgroundColor(this.color)
          .borderRadius(2)
          .margin({ right: index < this.values.length - 1 ? 2 : 0 })
      })
    }
    .width(this.width)
    .height(this.height)
    .alignItems(VerticalAlign.Bottom)
  }
}
