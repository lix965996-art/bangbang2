import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient } from '../common/HttpClient';
import { StorageUtil } from '../common/StorageUtil';

interface UserInfo {
  username?: string;
  token?: string;
}

@Entry
@Component
struct SettingsPage {
  @State serverUrl: string = 'http://10.0.2.2:9090';
  @State username: string = '';
  @State version: string = '1.0.0';

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    this.loadUserInfo();
  }

  build() {
    Column() {
      HeaderBar({
        title: '设置',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      Scroll() {
        Column() {
          // 用户信息
          Column() {
            Row() {
              Column() {
                Text('👤')
                  .fontSize(48)

                Text(this.username || '未登录')
                  .fontSize(18)
                  .fontWeight(FontWeight.Bold)
                  .fontColor('#333333')
                  .margin({ top: 12 })

                Text('智慧农场用户')
                  .fontSize(12)
                  .fontColor('#999999')
                  .margin({ top: 4 })
              }
              .alignItems(HorizontalAlign.Center)
            }
            .width('100%')
            .padding(24)
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .margin({ top: 16, left: 16, right: 16 })

          // 设置项
          Column() {
            this.SettingItem('🌐', '服务器地址', this.serverUrl, () => {
              // 弹出输入框修改服务器地址
            })

            Divider().color('#E0E0E0').margin({ left: 48 })

            this.SettingItem('📱', '版本号', this.version, () => {})

            Divider().color('#E0E0E0').margin({ left: 48 })

            this.SettingItem('❓', '帮助与反馈', '', () => {})

            Divider().color('#E0E0E0').margin({ left: 48 })

            this.SettingItem('📜', '用户协议', '', () => {})

            Divider().color('#E0E0E0').margin({ left: 48 })

            this.SettingItem('🔒', '隐私政策', '', () => {})
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .margin({ top: 16, left: 16, right: 16 })

          // 关于
          Column() {
            this.SettingItem('ℹ️', '关于系统', '智慧农场管理系统', () => {})
          }
          .width('100%')
          .backgroundColor(Color.White)
          .borderRadius(12)
          .margin({ top: 16, left: 16, right: 16 })

          // 退出登录
          Button('退出登录')
            .width('90%')
            .height(50)
            .backgroundColor('#F44336')
            .borderRadius(25)
            .fontSize(16)
            .fontColor(Color.White)
            .margin({ top: 32, bottom: 32 })
            .onClick(() => {
              this.logout();
            })
        }
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  SettingItem(icon: string, title: string, value: string, onClick: () => void) {
    Row() {
      Text(icon)
        .fontSize(24)
        .width(40)

      Text(title)
        .fontSize(16)
        .fontColor('#333333')
        .layoutWeight(1)

      if (value) {
        Text(value)
          .fontSize(14)
          .fontColor('#999999')
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .constraintSize({ maxWidth: 150 })
      }

      Text('>')
        .fontSize(16)
        .fontColor('#CCCCCC')
        .margin({ left: 8 })
    }
    .width('100%')
    .padding(16)
    .onClick(onClick)
  }

  private async loadUserInfo() {
    try {
      const userInfoStr = await StorageUtil.getString(StorageUtil.KEYS.USER_INFO);
      if (userInfoStr) {
        const userInfo: UserInfo = JSON.parse(userInfoStr) as UserInfo;
        this.username = userInfo.username || '';
      }
    } catch (error) {
      console.error('Load user info error:', error);
    }
  }

  private async logout() {
    try {
      // 清除登录状态
      await StorageUtil.delete(StorageUtil.KEYS.TOKEN);
      await StorageUtil.delete(StorageUtil.KEYS.USER_INFO);
      await StorageUtil.put(StorageUtil.KEYS.IS_LOGGED_IN, false);
      this.httpClient.clearToken();

      // 返回登录页
      router.replaceUrl({
        url: 'pages/LoginPage'
      });
    } catch (error) {
      console.error('Logout error:', error);
    }
  }
}
