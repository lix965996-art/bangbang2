import router from '@ohos.router';
import { HttpClient, ApiResponse, UserInfo } from '../common/HttpClient';
import { StorageUtil } from '../common/StorageUtil';
import { ThemeColors } from '../common/ThemeColors';
import GradientButton from '../components/glass/GradientButton';

interface LoginRequest {
  username: string;
  password: string;
  nickname?: string;
}

@Entry
@Component
struct LoginPage {
  @State username: string = '';
  @State password: string = '';
  @State isLoading: boolean = false;
  @State errorMessage: string = '';
  @State isRegisterMode: boolean = false;
  @State usernameFocused: boolean = false;
  @State passwordFocused: boolean = false;
  @State logoScale: number = 0.8;
  @State cardOpacity: number = 0;
  @State cardTranslateY: number = 30;

  private httpClient: HttpClient = HttpClient.getInstance();

  aboutToAppear() {
    setTimeout(() => {
      animateTo({ duration: 600, curve: Curve.Spring }, () => {
        this.logoScale = 1.0;
      });
    }, 100);

    setTimeout(() => {
      animateTo({ duration: 500, curve: Curve.EaseOut }, () => {
        this.cardOpacity = 1;
        this.cardTranslateY = 0;
      });
    }, 300);
  }

  build() {
    Stack() {
      // 动态渐变背景
      Column()
        .width('100%')
        .height('100%')
        .linearGradient({
          angle: 180,
          colors: [
            ['#E0F2FE', 0.0],
            ['#DBEAFE', 0.3],
            ['#E0E7FF', 0.6],
            ['#F0FDF4', 1.0]
          ]
        })

      // 浮动装饰圆
      Column()
        .width(300)
        .height(300)
        .backgroundColor('rgba(34, 197, 94, 0.15)')
        .borderRadius(150)
        .position({ x: -100, y: 100 })
        .blur(80)

      Column()
        .width(250)
        .height(250)
        .backgroundColor('rgba(59, 130, 246, 0.12)')
        .borderRadius(125)
        .position({ x: 280, y: 400 })
        .blur(60)

      Column()
        .width(180)
        .height(180)
        .backgroundColor('rgba(139, 92, 246, 0.1)')
        .borderRadius(90)
        .position({ x: 100, y: 550 })
        .blur(50)

      // 主内容
      Column() {
        // Logo区域
        Column() {
          Stack() {
            // Logo光晕
            Column()
              .width(100)
              .height(100)
              .borderRadius(50)
              .linearGradient({
                angle: 135,
                colors: [['rgba(34, 197, 94, 0.3)', 0.0], ['rgba(59, 130, 246, 0.3)', 1.0]]
              })
              .blur(20)

            Column() {
              Text('🌾')
                .fontSize(48)
            }
            .width(80)
            .height(80)
            .borderRadius(40)
            .justifyContent(FlexAlign.Center)
            .linearGradient({
              angle: 135,
              colors: [['#22C55E', 0.0], ['#3B82F6', 1.0]]
            })
            .shadow({
              radius: 20,
              color: 'rgba(34, 197, 94, 0.4)',
              offsetX: 0,
              offsetY: 8
            })
          }
          .width(100)
          .height(100)
          .scale({ x: this.logoScale, y: this.logoScale })

          Text('智慧农场')
            .fontSize(32)
            .fontWeight(FontWeight.Bold)
            .linearGradient({
              angle: 135,
              colors: [['#22C55E', 0.0], ['#3B82F6', 1.0]]
            })
            .margin({ top: 20 })

          Text('Smart Farm System')
            .fontSize(14)
            .fontColor(ThemeColors.TEXT_SECONDARY)
            .margin({ top: 6 })
            .letterSpacing(2)
        }
        .margin({ top: 60, bottom: 32 })

        // 毛玻璃登录卡片
        Column() {
          // 标题
          Text(this.isRegisterMode ? '创建账号' : '欢迎回来')
            .fontSize(22)
            .fontWeight(FontWeight.Medium)
            .fontColor(ThemeColors.TEXT_PRIMARY)
            .margin({ bottom: 24 })

          // 用户名输入
          Column() {
            Text('用户名')
              .fontSize(12)
              .fontColor(ThemeColors.TEXT_SECONDARY)
              .margin({ bottom: 8 })

            Stack() {
              // 聚焦发光效果
              if (this.usernameFocused) {
                Column()
                  .width('100%')
                  .height(50)
                  .borderRadius(12)
                  .backgroundColor('rgba(34, 197, 94, 0.1)')
                  .shadow({
                    radius: 12,
                    color: 'rgba(34, 197, 94, 0.3)',
                    offsetX: 0,
                    offsetY: 0
                  })
              }

              TextInput({ placeholder: '请输入用户名', text: this.username })
                .width('100%')
                .height(50)
                .backgroundColor(this.usernameFocused ? '#FFFFFF' : '#F8FAFC')
                .borderRadius(12)
                .padding({ left: 16, right: 16 })
                .border({
                  width: this.usernameFocused ? 2 : 1,
                  color: this.usernameFocused ? ThemeColors.PRIMARY_GREEN : '#E2E8F0'
                })
                .onChange((value: string) => {
                  this.username = value;
                })
                .onFocus(() => {
                  this.usernameFocused = true;
                })
                .onBlur(() => {
                  this.usernameFocused = false;
                })
            }
          }
          .width('100%')
          .alignItems(HorizontalAlign.Start)
          .margin({ bottom: 20 })

          // 密码输入
          Column() {
            Text('密码')
              .fontSize(12)
              .fontColor(ThemeColors.TEXT_SECONDARY)
              .margin({ bottom: 8 })

            Stack() {
              if (this.passwordFocused) {
                Column()
                  .width('100%')
                  .height(50)
                  .borderRadius(12)
                  .backgroundColor('rgba(34, 197, 94, 0.1)')
                  .shadow({
                    radius: 12,
                    color: 'rgba(34, 197, 94, 0.3)',
                    offsetX: 0,
                    offsetY: 0
                  })
              }

              TextInput({ placeholder: '请输入密码', text: this.password })
                .width('100%')
                .height(50)
                .type(InputType.Password)
                .backgroundColor(this.passwordFocused ? '#FFFFFF' : '#F8FAFC')
                .borderRadius(12)
                .padding({ left: 16, right: 16 })
                .border({
                  width: this.passwordFocused ? 2 : 1,
                  color: this.passwordFocused ? ThemeColors.PRIMARY_GREEN : '#E2E8F0'
                })
                .onChange((value: string) => {
                  this.password = value;
                })
                .onFocus(() => {
                  this.passwordFocused = true;
                })
                .onBlur(() => {
                  this.passwordFocused = false;
                })
            }
          }
          .width('100%')
          .alignItems(HorizontalAlign.Start)
          .margin({ bottom: 16 })

          // 错误信息
          if (this.errorMessage) {
            Row() {
              Text('⚠️')
                .fontSize(14)
                .margin({ right: 6 })

              Text(this.errorMessage)
                .fontSize(13)
                .fontColor(ThemeColors.ACCENT_DANGER)
            }
            .width('100%')
            .padding(12)
            .backgroundColor('rgba(239, 68, 68, 0.1)')
            .borderRadius(8)
            .margin({ bottom: 16 })
          }

          // 登录/注册按钮
          GradientButton({
            text: this.isLoading ? '处理中...' : (this.isRegisterMode ? '立即注册' : '登录'),
            height: 50,
            borderRadius: 12,
            disabled: this.isLoading,
            onClick: () => {
              this.handleLogin();
            }
          })

          // 切换注册/登录
          Row() {
            Text(this.isRegisterMode ? '已有账号？' : '没有账号？')
              .fontSize(14)
              .fontColor(ThemeColors.TEXT_SECONDARY)

            Text(this.isRegisterMode ? '立即登录' : '立即注册')
              .fontSize(14)
              .fontWeight(FontWeight.Medium)
              .linearGradient({
                angle: 90,
                colors: [['#22C55E', 0.0], ['#3B82F6', 1.0]]
              })
              .margin({ left: 6 })
              .onClick(() => {
                animateTo({ duration: 200, curve: Curve.EaseOut }, () => {
                  this.isRegisterMode = !this.isRegisterMode;
                  this.errorMessage = '';
                });
              })
          }
          .margin({ top: 24 })
        }
        .width('88%')
        .padding(28)
        .backgroundColor('rgba(255, 255, 255, 0.75)')
        .borderRadius(24)
        .backdropBlur(20)
        .border({ width: 1, color: 'rgba(255, 255, 255, 0.5)' })
        .shadow({
          radius: 40,
          color: 'rgba(0, 0, 0, 0.08)',
          offsetX: 0,
          offsetY: 16
        })
        .opacity(this.cardOpacity)
        .translate({ y: this.cardTranslateY })

        // 底部信息
        Column() {
          Row() {
            Text('🚀')
              .fontSize(12)
            Text(' 鸿蒙原生 · ')
              .fontSize(12)
              .fontColor(ThemeColors.TEXT_TERTIARY)
            Text('AI驱动')
              .fontSize(12)
              .fontWeight(FontWeight.Medium)
              .fontColor(ThemeColors.ACCENT_AI)
          }
          .margin({ top: 40 })

          Text('全国大学生服务创新设计大赛')
            .fontSize(11)
            .fontColor(ThemeColors.TEXT_TERTIARY)
            .margin({ top: 6 })
        }
      }
      .width('100%')
      .height('100%')
      .justifyContent(FlexAlign.Start)
    }
    .width('100%')
    .height('100%')
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
        const registerData: LoginRequest = {
          username: this.username,
          password: this.password,
          nickname: this.username
        };
        const response = await this.httpClient.post<ApiResponse<UserInfo>>('/user/register', registerData);

        if (response.code === '200') {
          this.isRegisterMode = false;
          this.errorMessage = '注册成功，请登录';
        } else {
          this.errorMessage = response.msg || '注册失败';
        }
      } else {
        const loginData: LoginRequest = {
          username: this.username,
          password: this.password
        };
        const response = await this.httpClient.post<ApiResponse<UserInfo>>('/user/login', loginData);

        if (response.code === '200' && response.data) {
          const userInfo = response.data;
          if (userInfo.token) {
            this.httpClient.setToken(userInfo.token);
            await StorageUtil.put(StorageUtil.KEYS.TOKEN, userInfo.token);
          }
          await StorageUtil.put(StorageUtil.KEYS.USER_INFO, JSON.stringify(userInfo));
          await StorageUtil.put(StorageUtil.KEYS.IS_LOGGED_IN, true);

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
