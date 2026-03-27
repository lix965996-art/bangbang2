import router from '@ohos.router';
import { HttpClient, ApiResponse, UserInfo } from '../common/HttpClient';
import { StorageUtil } from '../common/StorageUtil';

@Entry
@Component
struct LoginPage {
  @State username: string = '';
  @State password: string = '';
  @State isLoading: boolean = false;
  @State errorMessage: string = '';
  @State isRegisterMode: boolean = false;

  private httpClient: HttpClient = HttpClient.getInstance();

  build() {
    Column() {
      // Logo区域
      Column() {
        Text('🌾')
          .fontSize(64)
          .margin({ bottom: 16 })

        Text('智慧农场管理系统')
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor('#4CAF50')

        Text('Smart Farm System')
          .fontSize(14)
          .fontColor('#999999')
          .margin({ top: 8 })
      }
      .margin({ top: 80, bottom: 48 })

      // 表单区域
      Column() {
        // 用户名输入
        Column() {
          Text('用户名')
            .fontSize(12)
            .fontColor('#666666')
            .margin({ bottom: 8 })

          TextInput({ placeholder: '请输入用户名', text: this.username })
            .width('100%')
            .height(48)
            .backgroundColor('#F5F5F5')
            .borderRadius(8)
            .padding({ left: 16, right: 16 })
            .onChange((value: string) => {
              this.username = value;
            })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .margin({ bottom: 20 })

        // 密码输入
        Column() {
          Text('密码')
            .fontSize(12)
            .fontColor('#666666')
            .margin({ bottom: 8 })

          TextInput({ placeholder: '请输入密码', text: this.password })
            .width('100%')
            .height(48)
            .type(InputType.Password)
            .backgroundColor('#F5F5F5')
            .borderRadius(8)
            .padding({ left: 16, right: 16 })
            .onChange((value: string) => {
              this.password = value;
            })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .margin({ bottom: 12 })

        // 错误信息
        if (this.errorMessage) {
          Text(this.errorMessage)
            .fontSize(12)
            .fontColor('#F44336')
            .margin({ bottom: 12 })
        }

        // 登录按钮
        Button(this.isLoading ? '登录中...' : (this.isRegisterMode ? '注册' : '登录'))
          .width('100%')
          .height(48)
          .backgroundColor('#4CAF50')
          .borderRadius(8)
          .fontSize(16)
          .fontColor(Color.White)
          .enabled(!this.isLoading)
          .onClick(() => {
            this.handleLogin();
          })
          .margin({ top: 8 })

        // 切换注册/登录
        Row() {
          Text(this.isRegisterMode ? '已有账号？' : '没有账号？')
            .fontSize(14)
            .fontColor('#666666')

          Text(this.isRegisterMode ? '立即登录' : '立即注册')
            .fontSize(14)
            .fontColor('#4CAF50')
            .margin({ left: 4 })
            .onClick(() => {
              this.isRegisterMode = !this.isRegisterMode;
              this.errorMessage = '';
            })
        }
        .margin({ top: 20 })
      }
      .width('85%')
      .padding(24)
      .backgroundColor(Color.White)
      .borderRadius(16)
      .shadow({
        radius: 16,
        color: 'rgba(0,0,0,0.1)',
        offsetX: 0,
        offsetY: 4
      })

      // 底部信息
      Column() {
        Text('鸿蒙原生版')
          .fontSize(12)
          .fontColor('#CCCCCC')
          .margin({ top: 48 })

        Text('服务创新设计大赛参赛作品')
          .fontSize(10)
          .fontColor('#CCCCCC')
          .margin({ top: 4 })
      }
    }
    .width('100%')
    .height('100%')
    .linearGradient({
      angle: 180,
      colors: [['#E8F5E9', 0.0], ['#C8E6C9', 1.0]]
    })
  }

  private async handleLogin() {
    if (!this.username || !this.password) {
      this.errorMessage = '请输入用户名和密码';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    try {
      if (this.isRegisterMode) {
        // 注册
        const response = await this.httpClient.post<ApiResponse<UserInfo>>('/user/register', {
          username: this.username,
          password: this.password,
          nickname: this.username
        });

        if (response.code === '200') {
          this.isRegisterMode = false;
          this.errorMessage = '注册成功，请登录';
        } else {
          this.errorMessage = response.msg || '注册失败';
        }
      } else {
        // 登录
        const response = await this.httpClient.post<ApiResponse<UserInfo>>('/user/login', {
          username: this.username,
          password: this.password
        });

        if (response.code === '200' && response.data) {
          // 保存登录信息
          const userInfo = response.data;
          if (userInfo.token) {
            this.httpClient.setToken(userInfo.token);
            await StorageUtil.put(StorageUtil.KEYS.TOKEN, userInfo.token);
          }
          await StorageUtil.put(StorageUtil.KEYS.USER_INFO, JSON.stringify(userInfo));
          await StorageUtil.put(StorageUtil.KEYS.IS_LOGGED_IN, true);

          // 跳转到主页
          router.replaceUrl({
            url: 'pages/MainPage'
          });
        } else {
          this.errorMessage = response.msg || '登录失败';
        }
      }
    } catch (error) {
      this.errorMessage = '网络错误，请检查连接';
      console.error('Login error:', error);
    } finally {
      this.isLoading = false;
    }
  }
}
