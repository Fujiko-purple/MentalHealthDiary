# 心理健康日记 (Mental Health Diary)

一款关注用户心理健康的Android应用,集成了情绪记录、AI心理咨询、正念呼吸等功能。

## 主要功能

### 1. 情绪记录与追踪
- 每日情绪打分(1-5分)
- 心情日记撰写
- 情绪统计图表分析
- 查看历史记录

### 2. AI心理助手
- 基于大语言模型的智能对话
- 专业的心理咨询辅助
- 支持自定义API配置
- 温暖共情的对话风格

### 3. 正念呼吸练习
- 多种呼吸模式:
    - 标准呼吸(4-4节奏)
    - 专注呼吸(4-6节奏)
    - 提神呼吸(6-2节奏)
    - 安眠呼吸(4-8节奏)
- 练习数据统计
- 成就系统

### 4. 贴心提醒
- 每日心理健康提示
- 可自定义提醒时间
- 智能推送通知

## 技术特点

- 采用Material Design设计规范
- Room数据库本地存储
- WorkManager实现定时任务
- SharedPreferences管理配置
- 支持自定义AI模型接入
- 图表可视化(MPAndroidChart)

## 配置说明

### AI接口配置
在设置中可以自定义以下参数:
1. API密钥(custom_api_key)
2. API地址(custom_api_base)
3. 模型名称(custom_model_name)

### 默认配置
java
BASE_URL = "https://api.siliconflow.cn/"
MODEL_NAME = "deepseek-ai/DeepSeek-R1"

## 系统要求
- 最低支持Android SDK 24 (Android 7.0)
- 目标SDK 33
- 需要网络权限
- 需要通知权限(Android 13及以上需要动态申请)

## 依赖库
- androidx.room:room-runtime:2.5.0 (数据库)
- androidx.work:work-runtime:2.8.1 (后台任务)
- androidx.preference:preference:1.2.0 (设置界面)
- com.github.PhilJay:MPAndroidChart:v3.1.0 (图表)
- androidx.viewpager2:viewpager2:1.0.0 (引导页)

## 安全说明
- 支持HTTPS安全连接
- 用户数据本地存储
- API密钥密文显示

## 开发者配置
1. 在`app/build.gradle`中配置API密钥:
   gradle
   buildTypes {
   debug {
   buildConfigField "String", "API_KEY", "\"your_api_key_here\""
   }
   }

2. 在`AndroidManifest.xml`中添加必要权限

## 项目结构
app/src/main/java/com/example/mentalhealthdiary/
├── activity/
├── adapter/
├── api/
├── config/
├── model/
├── service/
└── utils/

## 未来计划

- [ ] 多种ai性格切换功能（例如：猫娘，甘雨，卡芙卡，夏目贵志）
- [ ] 心情记录ai分析功能-根据已有的心情统计 ai分析你目前的心理状况 然后给出合理的建议
- [ ] ai对话后台运行功能
- [ ] ai对话历史管理ui优化
- [ ] 手机上ai对话中 滑倒屏幕上面后计时器停止计时 思考过程停止问题
- [ ] 角色性格 提示词优化
- [ ] ai思考中禁止切换其他ai性格
- [ ] 多个ai对话窗口的优化问题-确保每个对话窗口都可以进行对话 互不影响
- [ ] ai思考超时返回报错
- [ ] ai对话编辑功能（可删除 改写 再发送）
- [ ] ai语音功能 将当前的文字念出
- [ ] ai助手的聊天ui优化
- [ ] 正念呼吸界面的音频加入✔
- [ ] 呼吸选择框和正在播放框架的优化问题✔
- [ ] 不同手机上 正在播放框架和练习时长框架重合问题---自适应屏幕布局✔
- [ ] 用户自主接入音乐✔
- [ ] 正念呼吸中 上方的引导呼吸节奏文字不随着呼吸模式的切换而切换 都是默认的“吸气4秒 呼气4秒” 但是其他呼吸模式的呼吸节奏不是这样的✔
- [ ] 进行某一个呼吸模式时 禁止切换另外的呼吸模式 只有结束当下的呼吸训练 才能切换别的呼吸模式✔
- [ ] 
- [ ] 自由呼吸模式下 在我的歌单里 可以选择 循环播放-列表播放-随机播放 
- [ ] 导入歌曲界面的优化-导入新歌曲按键始终在上端 防止后续歌曲导入过多而翻阅太麻烦
- [ ] 可以一键选择全部歌曲删除
- [ ] 自由呼吸训练开始时 可以点击导入歌曲按键 打开我的歌单 自由切换别的歌曲进行训练

## 贡献指南


## 许可证