# 桌面宠物 - AI对话功能配置指南

## 阿里云百炼 API Key 配置

### 方法一：设置环境变量（推荐）

#### Windows PowerShell:
```powershell
$env:ALIYUN_BAILIAN_API_KEY="your-api-key-here"
```

#### Windows CMD:
```cmd
set ALIYUN_BAILIAN_API_KEY=your-api-key-here
```

#### 永久设置（Windows）:
1. 右键"此电脑" -> "属性" -> "高级系统设置"
2. 点击"环境变量"
3. 在"用户变量"或"系统变量"中新建：
   - 变量名：`ALIYUN_BAILIAN_API_KEY`
   - 变量值：你的API Key

### 方法二：直接在代码中配置

编辑 `MainController.java` 文件，找到以下行：
```java
apiKey = "your-api-key-here";
```

替换为你的真实API Key：
```java
apiKey = "sk-your-actual-api-key";
```

## 获取阿里云百炼 API Key

1. 访问阿里云百炼控制台：https://dashscope.console.aliyun.com/
2. 登录阿里云账号
3. 创建或选择已有应用
4. 在"API-KEY管理"中创建或查看API Key

## 使用说明

### 基本操作：
- **单击左键**：与宠物对话，显示对话泡
- **双击左键**：显示/隐藏宠物
- **右键点击**：打开托盘菜单
- **拖动**：移动宠物位置

### 对话功能：
1. 单击宠物会随机显示问候语
2. 自动调用阿里云百炼大模型获取回复
3. 回复会显示在对话泡中
4. 对话泡3秒后自动消失，或点击手动关闭

## 注意事项

⚠️ **安全提示**：
- 不要将API Key提交到公开代码仓库
- 建议使用环境变量方式配置
- 定期更换API Key

💡 **提示**：
- 确保网络连接正常
- API调用需要消耗额度，请注意使用情况
- 可以修改 `AliyunBailianService.java` 中的系统提示来调整AI的性格

## 自定义配置

### 修改AI性格：
编辑 `AliyunBailianService.java` 中的系统提示：
```java
systemMsg.addProperty("content", "你是一个可爱的桌面宠物助手，回复要简短、活泼、有趣，每次回复不超过50个字。");
```

### 修改对话泡样式：
编辑 `ChatBubble.java` 中的颜色和字体设置：
```java
private Color bubbleColor = new Color(255, 255, 255, 240);  // 气泡背景色
private Color textColor = Color.BLACK;  // 文字颜色
private Font font = new Font("Microsoft YaHei", Font.PLAIN, 14);  // 字体
```

### 修改预设问候语：
编辑 `MainController.java` 中的 greetings 数组：
```java
private String[] greetings = {
    "你好呀！点击我聊天哦~",
    "今天心情怎么样？",
    "想和我聊什么呢？",
    "戳我一下呗！"
};
```
