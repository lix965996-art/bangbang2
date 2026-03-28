import router from '@ohos.router';
import HeaderBar from '../components/HeaderBar';
import { HttpClient, ApiResponse } from '../common/HttpClient';
import { ThemeColors } from '../common/ThemeColors';
import GlassCard from '../components/glass/GlassCard';
import AIAvatar from '../components/ai/AIAvatar';
import TypewriterText from '../components/animated/TypewriterText';

interface ChatMessage {
  id: number;
  type: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

interface ChatRequest {
  question: string;
}

interface ChatResponse {
  answer: string;
}

@Entry
@Component
struct AIAssistantPage {
  @State messages: ChatMessage[] = [];
  @State inputText: string = '';
  @State isLoading: boolean = false;
  @State isTyping: boolean = false;
  @State displayText: string = '';
  @State typingIndex: number = 0;

  private scroller: Scroller = new Scroller();
  private httpClient: HttpClient = HttpClient.getInstance();

  build() {
    Column() {
      // 深色背景头部
      Column() {
        // AI头像区域
        Column() {
          Stack() {
            // 光晕效果
            Column()
              .width(80)
              .height(80)
              .borderRadius(40)
              .linearGradient({
                angle: 135,
                colors: [['rgba(139, 92, 246, 0.3)', 0.0], ['rgba(59, 130, 246, 0.3)', 1.0]]
              })
              .blur(20)

            Column() {
              Text('🤖')
                .fontSize(36)
            }
            .width(64)
            .height(64)
            .borderRadius(32)
            .justifyContent(FlexAlign.Center)
            .linearGradient({
              angle: 135,
              colors: [['#8B5CF6', 0.0], ['#3B82F6', 1.0]]
            })
          }
          .width(80)
          .height(80)

          Text('AI农业助手')
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor(Color.White)
            .margin({ top: 16 })

          Text('智能问答 · 专业建议')
            .fontSize(12)
            .fontColor('rgba(255,255,255,0.6)')
            .margin({ top: 4 })
        }
        .width('100%')
        .padding(24)
      }
      .width('100%')
      .linearGradient({
        angle: 180,
        colors: [['#0F172A', 0.0], ['#1E293B', 1.0]]
      })
      .borderRadius({ bottomLeft: 24, bottomRight: 24 })

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
            Row() {
              if (message.type === 'user') {
                Blank()
              }

              Column() {
                if (message.type === 'assistant') {
                  AIAvatar({ size: 32 })
                    .margin({ bottom: 8 })
                }

                if (this.isTyping && message.type === 'assistant') {
                  TypewriterText({
                    fullText: message.content,
                    speed: 30
                  })
                } else {
                  Text(message.content)
                    .fontSize(15)
                    .fontColor(message.type === 'user' ? Color.White : ThemeColors.TEXT_PRIMARY)
                    .lineHeight(22)
                }
              }
              .constraintSize({ maxWidth: '75%' })
              .backgroundColor(message.type === 'user' ? '#3B82F6' : 'rgba(255,255,255,0.95)')
              .borderRadius(message.type === 'user' ?
                { topLeft: 16, topRight: 16, bottomLeft: 4, bottomRight: 16 } :
                { topLeft: 4, topRight: 16, bottomLeft: 16, bottomRight: 16 })
              .padding(16)
              .shadow({
                radius: 8,
                color: 'rgba(0,0,0,0.15)',
                offsetX: 0,
                offsetY: 2
              })

              if (message.type === 'assistant') {
                Blank()
              }
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 8, bottom: 8 })
          }
        })
      }
      .layoutWeight(1)
      .width('100%')
      .padding({ left: 16, right: 16, bottom: 80 })

      // 输入区域
      Column() {
        Row() {
          TextInput({ placeholder: '输入农业问题...', text: this.inputText })
            .layoutWeight(1)
            .height(48)
            .backgroundColor('rgba(30,41,59,0.8)')
            .borderRadius(24)
            .padding({ left: 16, right: 16 })
            .border({ width: 1, color: 'rgba(255,255,255,0.2)' })
            .fontColor(Color.White)
            .placeholderColor('rgba(255,255,255,0.5)')
            .onChange((value: string) => {
              this.inputText = value;
            })

          Button() {
            Text('发送')
              .fontSize(14)
              .fontWeight(FontWeight.Medium)
              .fontColor(Color.White)
          }
          .width(60)
          .height(48)
          .borderRadius(24)
          .linearGradient({
            angle: 135,
            colors: [['#3B82F6', 0.0], ['#8B5CF6', 1.0]]
          })
          .enabled(this.inputText.trim().length > 0 && !this.isLoading)
          .margin({ left: 12 })
          .onClick(() => {
            this.sendMessage();
          })
        }
        .width('100%')
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 12, bottom: 12 })
      .backgroundColor('#1E293B')
    }
    .width('100%')
    .height('100%')
    .linearGradient({
      angle: 180,
      colors: [['#0F172A', 0.0], ['#1E293B', 1.0]]
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
    this.typingIndex = 0;
    this.displayText = '';

    // 滚动到底部
    setTimeout(() => {
      this.scroller.scrollToIndex(this.messages.length - 1);
    }, 100);

    try {
      const requestData: ChatRequest = { question: question };
      const response = await this.httpClient.post<ApiResponse<ChatResponse>>('/api/chat/ask', requestData);

      let answer = '抱歉，我无法回答这个问题。';
      if (response.code === '200' && response.data) {
        answer = response.data.answer || answer;
      }

      // 添加AI回复
      const assistantMessage: ChatMessage = {
        id: Date.now(),
        type: 'assistant',
        content: answer,
        timestamp: Date.now()
      };
      this.messages = [...this.messages, assistantMessage];
      this.typingIndex = 0;
      this.displayText = '';

      // 开始打字机效果
      this.startTypewriter();
    } catch (error) {
      const errorMessage: ChatMessage = {
        id: Date.now(),
        type: 'assistant',
        content: '抱歉，网络连接失败，请稍后再试',
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

  private startTypewriter() {
    this.isTyping = true;
    const text = this.displayText;
    const length = text.length;
    const interval = setInterval(() => {
      if (this.typingIndex < length) {
        this.typingIndex++;
        this.displayText = text.substring(0, this.typingIndex);
      } else {
        clearInterval(interval);
        this.isTyping = false;
      }
    }, 30);
  }
}