package com.dlt.controller;

import com.dlt.component.ChatBubble;
import com.dlt.component.GlobalMouseHook;
import com.dlt.service.AliyunBailianService;
import com.dlt.service.IdleChatService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.dlt.service.TranslateService;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.concurrent.CompletableFuture;

public class MainController {

    // 预设问候语
    private String[] greetings = {
        "吃早餐了吗？没吃的话香澄分你一半星星饼干哦！",
        "呀吼——！正好正好，ksm正打算去Circle练习呢！你要不要也一起来？多惠和里美她们也都在哦，大家一起演奏绝对会超级开心的！",
        "哇啊，你看你看,今天的晚霞是粉红色的！这种时候最适合一边散步一边寻找新歌的灵感了",
        "今天也玩得很开心！你看，天上的星星在眨眼睛哦！",
        "大家准备好了吗？让我们一起创造出最kirakira的舞台吧"
    };

    private JWindow petWindow;
    private JLabel petLabel;
    private Image petImage; // 当前显示的图片
    private BufferedImage image001; // 001.png原始图片
    private BufferedImage image002; // 002.jpg原始图片
    private int petWidth = 250;
    private int petHeight = 250;
    private boolean petVisible = true;
    // 拖动相关
    private Point dragStartPoint;
    private Point frameStartLocation;
    // 对话气泡
    private ChatBubble currentBubble;
    // AI服务
    private AliyunBailianService aiService;
    private String apiKey;
    // 自言自语服务
    private IdleChatService idleChatService;
    // 翻译服务
    private TranslateService translateService;
    // 全局鼠标钩子
    private GlobalMouseHook mouseHook;
    // 跳跃动画相关
    private boolean isJumping = false;
    private Timer jumpTimer;
    // 转身动画相关
    private boolean isTurning = false;
    private double currentAngle = 0; // 当前旋转角度（弧度）
    private Timer turnTimer;
    // 系统托盘
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    public MainController() {
        //initApiKey();
        apiKey = "sk-9ebdc142aef749e296e6a6beef33c4d4";
        translateService = new TranslateService();
        aiService = new AliyunBailianService(apiKey);

        initPetWindow();
        initMouseHook();
        initSystemTray();
        initIdleChat();
    }

    /**
     * 初始化自言自语功能
     * 宠物无聊时会随机自言自语或找人互动，内容由AI生成
     */
    private void initIdleChat() {
        idleChatService = new IdleChatService(aiService, message -> {
            SwingUtilities.invokeLater(() -> {
                if (petVisible) {
                    showTimedBubble(message);
                }
            });
        });
        idleChatService.start();
    }

    /**
     * 初始化宠物窗口
     */
    private void initPetWindow() {
        petWindow = new JWindow();
        petWindow.setAlwaysOnTop(true);
        petWindow.setBackground(new Color(0, 0, 0, 0));
        petWindow.setSize(petWidth, petHeight);
        // 加载宠物图片（直接使用PNG原始尺寸，不缩放）
        try {
            // 加载001.png
            java.io.InputStream is1 = getClass().getResourceAsStream("/resource/001.png");
            if (is1 == null) {
                throw new Exception("找不到图片资源: /resource/001.png");
            }
            image001 = ImageIO.read(is1);
            
            // 加载002.png
            java.io.InputStream is2 = getClass().getResourceAsStream("/resource/002.png");
            if (is2 == null) {
                throw new Exception("找不到图片资源: /resource/002.png");
            }
            image002 = ImageIO.read(is2);
            
            // 窗口固定250x250，两张图片都绘制为窗口大小
            petWindow.setSize(petWidth, petHeight);
            
            // 初始使用001.png
            petImage = image001;
        } catch (Exception e) {
            System.err.println("加载宠物图片失败: " + e.getMessage());
        }

        // 宠物标签 - 支持3D转身效果（通过X轴缩放模拟）
        petLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                // 不调用super.paintComponent，避免Windows LAF绘制默认背景矩形
                Graphics2D g2d = (Graphics2D) g.create();
                
                // 设置高质量渲染
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (currentAngle != 0) {
                    // 模拟3D转身：通过X轴缩放实现
                    int centerX = getWidth() / 2;
                    int centerY = getHeight() / 2;
                    
                    // 计算X轴缩放比例：cos(angle)
                    // 0° -> 1.0, 90° -> 0.0, 180° -> -1.0
                    double scaleX = Math.cos(currentAngle);
                    
                    // 平移到中心点
                    g2d.translate(centerX, centerY);
                    // X轴缩放（负值会自动水平翻转）
                    g2d.scale(scaleX, 1.0);
                    // 平移回原点
                    g2d.translate(-centerX, -centerY);
                }
                
                g2d.drawImage(petImage, 0, 0, getWidth(), getHeight(), this);
                g2d.dispose();
            }
        };
        petLabel.setOpaque(false);
        //petLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // 鼠标交互
        petLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
                frameStartLocation = petWindow.getLocation();
            }
        });
        petLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = petWindow.getLocation();
                int newX = current.x + e.getX() - dragStartPoint.x;
                int newY = current.y + e.getY() - dragStartPoint.y;
                petWindow.setLocation(newX, newY);
                // 对话气泡跟随宠物移动
                if (currentBubble != null && currentBubble.isVisible()) {
                    int centerX = newX + petWidth / 2;
                    int topY = newY;
                    currentBubble.updatePosition(centerX, topY);
                }
            }
        });
        // 窗口位置：屏幕右下角
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - petWidth - 100;
        int y = screenSize.height - petHeight - 80;
        petWindow.setLocation(x, y);
        petWindow.setVisible(true);

        // 右键菜单
        JPopupMenu popupMenu = createPopupMenu();
        petLabel.setComponentPopupMenu(popupMenu);

        petWindow.setContentPane(petLabel);
    }

    /**
     * 创建右键菜单
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem showHideItem = new JMenuItem("显示/隐藏");
        showHideItem.addActionListener(e -> togglePetVisibility());
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> exitApp());

        menu.add(showHideItem);
        menu.addSeparator();
        menu.add(exitItem);

        return menu;
    }

    /**
     * 显示随机问候语
     */
    private void showGreeting() {
        if (!petVisible) return;
        int index = (int) (Math.random() * greetings.length);
        String greeting = greetings[index];
        showTimedBubble(greeting);

        // 异步调用AI获取回复
        CompletableFuture<String> future = aiService.chatAsync(greeting);
        future.thenAccept(reply -> {
            SwingUtilities.invokeLater(() -> {
                // 解析回复：||| 前是显示内容
                String displayText = parseDisplayText(reply);
                if (displayText != null && !displayText.isEmpty()) {
                    showTimedBubble(displayText);
                }
            });
        });
    }

    /**
     * 显示输入对话框
     */
    private void showInputDialog() {
        if (currentBubble != null) {
            currentBubble.dispose();
        }
        
        // 如果正在转身，停止转身动画并恢复001.png
        if (isTurning) {
            stopTurnAnimation();
        }
        // 直接切换回001.png
        petImage = image001;
        currentAngle = 0;
        petLabel.repaint();
        
        // 触发跳跃动画
        jumpPet();

        Point petLoc = petWindow.getLocation();
        int centerX = petLoc.x + petWidth / 2;
        int topY = petLoc.y;

        ChatBubble inputBubble = new ChatBubble(petWindow, e -> {
            String userMessage = e.getActionCommand();
            if (userMessage != null && !userMessage.isEmpty()) {
                handleUserMessage(userMessage);
            }
        });
        currentBubble = inputBubble;
        inputBubble.showAt(centerX, topY);
    }

    /**
     * 处理用户输入的消息
     */
    private void handleUserMessage(String message) {
        // 先显示思考中的提示（短暂自动消失）
        showTimedBubble("思考中...");
        
        // 异步调用AI
        CompletableFuture<String> future = aiService.chatAsync(message);
        future.thenAccept(reply -> {
            SwingUtilities.invokeLater(() -> {
                String displayText = parseDisplayText(reply);
                if (displayText != null && !displayText.isEmpty()) {
                    // 收到AI回复后，触发表情切换动画
                    turnAndSwitchExpression(displayText);
                } else {
                    currentBubble.dispose();
                }
            });
        });
    }

    /**
     * 解析AI回复，提取显示文本（|||前的内容）
     */
    private String parseDisplayText(String reply) {
        if (reply == null || reply.isEmpty()) {
            return "模型想不出来呢....";
        }
        String[] parts = reply.split("\\|\\|\\|", 2);
        return parts[0].trim();
    }

    /**
     * 显示持久对话气泡（AI回复，不自动消失，仅通过X按钮或下次输入时关闭）
     */
    private void showPersistentBubble(String message) {
        if (currentBubble != null) {
            currentBubble.dispose();
        }

        Point petLoc = petWindow.getLocation();
        int centerX = petLoc.x + petWidth / 2;
        int topY = petLoc.y;

        ChatBubble bubble = new ChatBubble(petWindow, message);
        currentBubble = bubble;
        bubble.showAt(centerX, topY);
        // 不设置自动消失计时器，由X按钮或下次触发输入框时关闭
    }

    /**
     * 显示定时对话气泡（自言自语，5秒后自动消失）
     */
    private void showTimedBubble(String message) {
        if (currentBubble != null) {
            currentBubble.dispose();
        }

        Point petLoc = petWindow.getLocation();
        int centerX = petLoc.x + petWidth / 2;
        int topY = petLoc.y;

        ChatBubble bubble = new ChatBubble(petWindow, message);
        currentBubble = bubble;
        bubble.showAt(centerX, topY);

        // 5秒后自动消失
        Timer timer = new Timer(5000, e -> {
            bubble.dispose();
            if (currentBubble == bubble) {
                currentBubble = null;
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 显示定时对话气泡（自定义时长后自动消失）
     */
    private void showTimedBubble(String message, int durationMs) {
        if (currentBubble != null) {
            currentBubble.dispose();
        }

        Point petLoc = petWindow.getLocation();
        int centerX = petLoc.x + petWidth / 2;
        int topY = petLoc.y;

        ChatBubble bubble = new ChatBubble(petWindow, message);
        currentBubble = bubble;
        bubble.showAt(centerX, topY);

        Timer timer = new Timer(durationMs, e -> {
            bubble.dispose();
            if (currentBubble == bubble) {
                currentBubble = null;
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 切换宠物显示/隐藏
     */
    private void togglePetVisibility() {
        petVisible = !petVisible;
        petWindow.setVisible(petVisible);
        if (!petVisible && currentBubble != null) {
            currentBubble.dispose();
            currentBubble = null;
        }
    }

    /**
     * 初始化全局鼠标钩子（侧键触发对话）
     */
    private void initMouseHook() {
        mouseHook = new GlobalMouseHook(button -> {
            SwingUtilities.invokeLater(() -> {
                if (button == 4) { // 后退侧键 → 翻译选中文本
                    if (petVisible) {
                        handleTranslateSelection();
                    }
                } else if (button == 5) { // 前进侧键 → 触发对话
                    if (petVisible) {
                        showInputDialog();
                    } else {
                        togglePetVisibility();
                        showInputDialog();
                    }
                }
            });
        });
        mouseHook.install();
    }

    /**
     * 高质量缩放图片（避免 getScaledInstance 导致的模糊）
     */
    private BufferedImage scaleImage(BufferedImage original, int targetWidth, int targetHeight) {
        // 创建目标图像
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        
        // 设置最高质量的渲染参数
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // 绘制图片
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return scaled;
    }

    /**
     * 转身并切换表情动画（模拟3D转身效果）
     * 从0度旋转到90度（变成一条线），切换图片，然后继续旋转到180度
     */
    private void turnAndSwitchExpression(String displayText) {
        if (isTurning) {
            return; // 正在转身中，不重复触发
        }
        isTurning = true;
        currentAngle = 0;
        
        final int totalDuration = 400; // 总时长（毫秒）
        final int frameDelay = 15; // 帧间隔（毫秒）
        final int totalFrames = totalDuration / frameDelay;
        final double anglePerFrame = Math.PI / totalFrames; // 每帧旋转角度（从0到π）
        
        // 先显示AI回复的气泡
        showPersistentBubble(displayText);
        
        turnTimer = new Timer(frameDelay, null);
        turnTimer.addActionListener(e -> {
            currentAngle += anglePerFrame;
            
            // 当旋转到90度（π/2）时，切换图片（直接使用002.png原始图片）
            if (currentAngle >= Math.PI / 2 && currentAngle < Math.PI / 2 + anglePerFrame) {
                petImage = image002; // 直接使用002.png原始图片
            }
            
            petLabel.repaint();
            
            // 更新窗口形状，裁剪掉透明区域，避免转身时出现矩形
            double scaleX = Math.abs(Math.cos(currentAngle));
            int visibleWidth = Math.max(1, (int)(petWidth * scaleX));
            int shapeX = (petWidth - visibleWidth) / 2;
            petWindow.setShape(new java.awt.Rectangle(shapeX, 0, visibleWidth, petHeight));
            
            // 完成旋转（180度）
            if (currentAngle >= Math.PI) {
                currentAngle = 0; // 重置角度为0，此时显示的是002.jpg的正面
                petWindow.setShape(null); // 恢复正常矩形
                turnTimer.stop();
                isTurning = false;
                petLabel.repaint();
            }
        });
        turnTimer.start();
    }
    
    /**
     * 停止转身动画
     */
    private void stopTurnAnimation() {
        if (turnTimer != null && turnTimer.isRunning()) {
            turnTimer.stop();
        }
        isTurning = false;
        currentAngle = 0;
        petWindow.setShape(null); // 恢复正常矩形
    }

    /**
     * 宠物跳跃动画（跳两下）
     */
    private void jumpPet() {
        if (isJumping) {
            return; // 正在跳跃中，不重复触发
        }
        isJumping = true;

        final int jumpHeight = 20;      // 跳跃高度（像素）
        final int jumpDuration = 200;   // 单次跳跃时长（毫秒）
        final int totalJumps = 2;       // 跳跃次数
        final int frameDelay = 15;      // 帧间隔（毫秒）

        final Point originalPos = petWindow.getLocation();
        final int[] currentJump = {0};
        final int[] frame = {0};
        final int framesPerJump = jumpDuration / frameDelay;

        jumpTimer = new Timer(frameDelay, null);
        jumpTimer.addActionListener(e -> {
            frame[0]++;
            int jumpFrame = frame[0] % framesPerJump;
            double progress = (double) jumpFrame / framesPerJump;

            // 线性三角跳跃：前半段上升，后半段下降
            int offsetY;
            if (progress < 0.5) {
                offsetY = (int) (-jumpHeight * (progress * 2));
            } else {
                offsetY = (int) (-jumpHeight * (2 - progress * 2));
            }
            petWindow.setLocation(originalPos.x, originalPos.y + offsetY);

            // 完成一次跳跃
            if (jumpFrame == 0 && frame[0] > 0) {
                currentJump[0]++;
            }

            // 完成所有跳跃
            if (currentJump[0] >= totalJumps) {
                petWindow.setLocation(originalPos.x, originalPos.y);
                jumpTimer.stop();
                isJumping = false;
            }
        });
        jumpTimer.start();
    }

    /**
     * 初始化系统托盘
     */
    private void initSystemTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("系统不支持系统托盘");
            return;
        }
        systemTray = SystemTray.getSystemTray();

        // 获取系统推荐的托盘图标尺寸（高DPI下可能大于16）
        Dimension trayIconSize = systemTray.getTrayIconSize();
        int traySize = Math.max(trayIconSize.width, trayIconSize.height);
        System.out.println("系统托盘图标推荐尺寸: " + traySize + "x" + traySize);

        BufferedImage trayImage = null;
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/resource/001.png");
            if (is == null) {
                throw new Exception("找不到图片资源: /resource/001.png");
            }
            BufferedImage original = ImageIO.read(is);
            trayImage = scaleImage(original, traySize, traySize);
        } catch (Exception e) {
            System.err.println("加载托盘图标失败: " + e.getMessage());
            // 回退：创建一个简单的彩色图标
            trayImage = new BufferedImage(traySize, traySize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = trayImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(255, 182, 193));
            g2d.fillOval(0, 0, traySize, traySize);
            g2d.dispose();
        }

        trayIcon = new TrayIcon(trayImage, "KSM 桌面宠物");
        trayIcon.setImageAutoSize(true); // 允许系统在需要时自动适配

        // 托盘右键菜单
        java.awt.PopupMenu trayMenu = new java.awt.PopupMenu();

        java.awt.MenuItem showItem = new java.awt.MenuItem("显示/隐藏");
        showItem.addActionListener(e -> togglePetVisibility());

        java.awt.MenuItem chatItem = new java.awt.MenuItem("和香澄聊天");
        chatItem.addActionListener(e -> {
            if (!petVisible) togglePetVisibility();
            showInputDialog();
        });

        java.awt.MenuItem exitItem = new java.awt.MenuItem("退出");
        exitItem.addActionListener(e -> exitApp());

        trayMenu.add(showItem);
        trayMenu.add(chatItem);
        trayMenu.addSeparator();
        trayMenu.add(exitItem);

        trayIcon.setPopupMenu(trayMenu);

        // 双击托盘图标显示/隐藏宠物
        trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    togglePetVisibility();
                }
            }
        });

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("添加系统托盘图标失败: " + e.getMessage());
        }
    }

    /**
     * 退出应用
     */
    private void exitApp() {
        if (idleChatService != null) {
            idleChatService.stop();
        }
        if (mouseHook != null) {
            mouseHook.uninstall();
        }
        if (currentBubble != null) {
            currentBubble.dispose();
        }
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
        petWindow.dispose();
        System.exit(0);
    }

    /**
     * 获取当前系统选中的文本
     * 通过模拟 Ctrl+C 复制并读取剪贴板实现
     */
    private String getSelectedText() {
        try {
            // 保存当前剪贴板内容
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable oldContent = clipboard.getContents(null);

            // 模拟 Ctrl+C
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_C);
            robot.keyRelease(KeyEvent.VK_C);
            robot.keyRelease(KeyEvent.VK_CONTROL);

            // 等待复制完成
            Thread.sleep(150);

            // 读取剪贴板
            String selectedText = (String) clipboard.getData(DataFlavor.stringFlavor);

            // 恢复原剪贴板内容
            if (oldContent != null) {
                clipboard.setContents(oldContent, null);
            }

            return selectedText;
        } catch (Exception e) {
            System.err.println("获取选中文本失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 处理翻译选中文本
     * 在 Windows 上，没有直接的 Java API 可以获取其他应用程序中选中的文本。
     * 所有应用（包括浏览器、编辑器等）都不会把"当前选中文本"暴露给外部程序，
     * 唯一的通用途径就是通过剪贴板（模拟 Ctrl+C）。
     */
    private void handleTranslateSelection() {
        showTimedBubble("翻译中...");

        // 在EDT上获取选中文本（Robot模拟Ctrl+C需要在EDT执行）
        final String selectedText = getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            showTimedBubble("没有选中文本哦~");
            return;
        }

        // 异步调用翻译API
        CompletableFuture.runAsync(() -> {
            String targetLang = "zh-CN";
            String result = translateService.translate(selectedText.trim(), targetLang);

            SwingUtilities.invokeLater(() -> {
                if (result != null && !result.isEmpty()) {
                    showTimedBubble(result, 3000);
                } else {
                    showTimedBubble("翻译失败了诶...");
                }
            });
        });
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainController();
        });
    }
}
