import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import AlertCard from '../components/AlertCard';
import { HttpClient, ApiResponse, Alert } from '../common/HttpClient';

@Entry
@Component
struct AlertPage {
  @State alerts: Alert[] = [];
  @State isLoading: boolean = false;
  @State activeTab: number = 0; // 0: 今日任务, 1: 全部预警

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadAlerts();
  }

  build() {
    Column() {
      HeaderBar({
        title: '预警中心',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      // Tab切换
      Row() {
        Text('今日任务')
          .fontSize(16)
          .fontColor(this.activeTab === 0 ? '#4CAF50' : '#666666')
          .fontWeight(this.activeTab === 0 ? FontWeight.Bold : FontWeight.Normal)
          .padding(12)
          .onClick(() => {
            this.activeTab = 0;
            this.loadAlerts();
          })

        Text('全部预警')
          .fontSize(16)
          .fontColor(this.activeTab === 1 ? '#4CAF50' : '#666666')
          .fontWeight(this.activeTab === 1 ? FontWeight.Bold : FontWeight.Normal)
          .padding(12)
          .onClick(() => {
            this.activeTab = 1;
            this.loadAlerts();
          })

        Blank()

        Text(`共 ${this.alerts.length} 条`)
          .fontSize(14)
          .fontColor('#999999')
          .padding(12)
      }
      .width('100%')
      .backgroundColor(Color.White)
      .border({ width: { bottom: 1 }, color: '#E0E0E0' })

      // 预警列表
      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(48)
            .height(48)
            .color('#4CAF50')

          Text('加载中...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else if (this.alerts.length === 0) {
        Column() {
          Text('🎉')
            .fontSize(64)

          Text('暂无预警信息')
            .fontSize(16)
            .fontColor('#999999')
            .margin({ top: 16 })

          Text('所有农田运行正常')
            .fontSize(14)
            .fontColor('#CCCCCC')
            .margin({ top: 8 })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            ForEach(this.alerts, (alert: Alert) => {
              AlertCard({
                alertType: alert.alertType,
                alertLevel: alert.alertLevel,
                message: alert.message,
                createTime: alert.createTime || '',
                status: alert.status,
                onProcess: () => {
                  this.processAlert(alert);
                }
              })
            })
          }
          .width('100%')
          .padding(16)
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  private async loadAlerts() {
    this.isLoading = true;
    try {
      const endpoint = this.activeTab === 0 ? '/alert/today/tasks' : '/alert/pending';
      const response = await this.httpClient.get<ApiResponse<Alert[]>>(endpoint);

      if (response.code === '200') {
        this.alerts = response.data || [];
      }
    } catch (error) {
      console.error('Load alerts error:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private async processAlert(alert: Alert) {
    try {
      const response = await this.httpClient.post<ApiResponse<null>>(`/alert/${alert.id}/process`, {
        processor: '鸿蒙端用户'
      });

      if (response.code === '200') {
        this.loadAlerts();
      }
    } catch (error) {
      console.error('Process alert error:', error);
    }
  }
}