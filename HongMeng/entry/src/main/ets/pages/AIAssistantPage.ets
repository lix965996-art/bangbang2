import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse } from '../common/HttpClient';

interface ChatMessage {
  id: number;
  type: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

@Entry
@Component
struct AIAssistantPage {
  @State messages: ChatMessage[] = [];
  @State inputText: string = '';
  @State isLoading: boolean = false;
  @State isTyping: boolean = false;
  private scroller: Scroller = new Scroller();

  private httpClient: HttpClient = HttpClient.getInstance();

  build() {
    Column() {
      HeaderBar({
        title: 'AI农业助手',
        showBack: true,
        onBackClick: () => {
          router.back();
        }
      })

      // 消息列表
      List({ scroller: this.scroller }) {
        ForEach(this.messages, (message: ChatMessage) => {
          ListItem() {
            this.MessageBubble(message)
          }
        })

        // 正在输入提示
        if (this.isTyping) {
          ListItem() {
            Row() {
              LoadingProgress()
                .width(20)
                .height(20)
                .color('#4CAF50')

              Text('AI正在思考...')
                .fontSize(12)
                .fontColor('#999999')
                .margin({ left: 8 })
            }
            .padding(12)
          }
        }
      }
      .layoutWeight(1)
      .width('100%')
      .padding({ left: 16, right: 16, top: 12, bottom: 12 })
      .scrollBar(BarState.Off)
      .edgeEffect(EdgeEffect.Spring)

      // 快捷问题
      if (this.messages.length === 0) {
        Column() {
          Text('你可以问我:')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ bottom: 12 })

          Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.Start }) {
            this.QuickQuestion('🌡️ 温度过高怎么办?')
            this.QuickQuestion('💧 湿度太低怎么处理?')
            this.QuickQuestion('🐛 如何防治病虫害?')
            this.QuickQuestion('🌱 什么时候适合播种?')
            this.QuickQuestion('📈 如何提高产量?')
            this.QuickQuestion('💊 农药使用建议?')
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 12 })
      }

      // 输入区域
      Row() {
        TextInput({ placeholder: '请输入农业问题...', text: this.inputText })
          .layoutWeight(1)
          .height(44)
          .backgroundColor('#F5F5F5')
          .borderRadius(22)
          .padding({ left: 16, right: 16 })
          .onChange((value: string) => {
            this.inputText = value;
          })

        Button('发送')
          .fontSize(14)
          .backgroundColor('#4CAF50')
          .borderRadius(22)
          .height(44)
          .margin({ left: 12 })
          .enabled(this.inputText.trim().length > 0 && !this.isLoading)
          .onClick(() => {
            this.sendMessage();
          })
      }
      .width('100%')
      .padding(16)
      .backgroundColor(Color.White)
      .border({ width: { top: 1 }, color: '#E0E0E0' })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  @Builder
  MessageBubble(message: ChatMessage) {
    Row() {
      if (message.type === 'user') {
        Blank()
      }

      Column() {
        Text(message.content)
          .fontSize(14)
          .fontColor(message.type === 'user' ? Color.White : '#333333')
      }
      .maxWidth('75%')
      .backgroundColor(message.type === 'user' ? '#4CAF50' : Color.White)
      .borderRadius(12)
      .padding(12)
      .shadow({
        radius: message.type === 'user' ? 0 : 4,
        color: 'rgba(0,0,0,0.05)',
        offsetX: 0,
        offsetY: 2
      })

      if (message.type === 'assistant') {
        Blank()
      }
    }
    .width('100%')
    .margin({ bottom: 12 })
  }

  @Builder
  QuickQuestion(question: string) {
    Text(question)
      .fontSize(12)
      .fontColor('#4CAF50')
      .backgroundColor('#E8F5E9')
      .borderRadius(16)
      .padding({ left: 12, right: 12, top: 6, bottom: 6 })
      .margin({ right: 8, bottom: 8 })
      .onClick(() => {
        this.inputText = question.replace(/^[^\s]+\s/, ''); // 移除表情
        this.sendMessage();
      })
  }

  private async sendMessage() {
    const question = this.inputText.trim();
    if (!question) return;

    // 添加用户消息
    const userMessage: ChatMessage = {
      id: Date.now(),
      type: 'user',
      content: question,
      timestamp: Date.now()
    };
    this.messages = [...this.messages, userMessage];

    this.inputText = '';
    this.isLoading = true;
    this.isTyping = true;

    // 滚动到底部
    setTimeout(() => {
      this.scroller.scrollToIndex(this.messages.length - 1);
    }, 100);

    try {
      const response = await this.httpClient.post<ApiResponse<{ answer: string }>>('/api/chat/ask', {
        question: question
      });

      let answer = '抱歉，我无法回答这个问题。';
      if (response.code === '200' && response.data) {
        answer = response.data.answer || answer;
      }

      // 添加AI回复
      const assistantMessage: ChatMessage = {
        id: Date.now() + 1,
        type: 'assistant',
        content: answer,
        timestamp: Date.now()
      };
      this.messages = [...this.messages, assistantMessage];
    } catch (error) {
      const errorMessage: ChatMessage = {
        id: Date.now() + 1,
        type: 'assistant',
        content: '抱歉，网络连接失败，请稍后再试。',
        timestamp: Date.now()
      };
      this.messages = [...this.messages, errorMessage];
    } finally {
      this.isLoading = false;
      this.isTyping = false;

      // 滚动到底部
      setTimeout(() => {
        this.scroller.scrollToIndex(this.messages.length - 1);
      }, 100);
    }
  }
}