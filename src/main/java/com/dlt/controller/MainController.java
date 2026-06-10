package com.dlt.controller;

import com.dlt.component.ChatBubble;
import com.dlt.component.GlobalMouseHook;
import com.dlt.service.AliyunBailianService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
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
    private Image petImage;
    private int petWidth = 180;
    private int petHeight = 180;
    private boolean petVisible = true;
    // 拖动相关
    private Point dragStartPoint;
    private Point frameStartLocation;
    // 对话气泡
    private ChatBubble currentBubble;
    // AI服务
    private AliyunBailianService aiService;
    private String apiKey;
    // 全局鼠标钩子
    private GlobalMouseHook mouseHook;
    // 跳跃动画相关
    private boolean isJumping = false;
    private Timer jumpTimer;
    // 系统托盘
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    public MainController() {
        //initApiKey();
        apiKey = "sk-9ebdc142aef749e296e6a6beef33c4d4";
        aiService = new AliyunBailianService(apiKey);

        initPetWindow();
        initMouseHook();
        initSystemTray();
    }

    /**
     * 初始化宠物窗口
     */
    private void initPetWindow() {
        petWindow = new JWindow();
        petWindow.setAlwaysOnTop(true);
        petWindow.setBackground(new Color(0, 0, 0, 0));
        petWindow.setSize(petWidth, petHeight);
        // 加载宠物图片
        try {
            //用 File 加载的是文件系统路径，而不是 classpath 资源路径。应该用 getClass().getResourceAsStream() 来加载打包在 jar 里的资源。
            java.io.InputStream is = getClass().getResourceAsStream("/resource/001.png");
            if (is == null) {
                throw new Exception("找不到图片资源: /com/dlt/img/001.png");
            }
            BufferedImage img = ImageIO.read(is);
            petImage = scaleImage(img, petWidth, petHeight);
        } catch (Exception e) {
            System.err.println("加载宠物图片失败: " + e.getMessage());
        }

        // 宠物标签
        petLabel = new JLabel() {
            @Override
            //g.drawImage(petImage, 0, 0, getWidth(), getHeight(), this);
            //直接用原始 Graphics 对象绘制,没有任何渲染提示，Java 使用默认的最近邻插值（Nearest Neighbor），缩放时像素直接"拉伸"，边缘锯齿明显、图像模糊
            //Bicubic（双三次插值），取周围 16 个像素加权计算，边缘平滑清晰
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);//保留 super.paintComponent(g)：是 Swing 重写 paintComponent 的标准做法，保证未来如果给 JLabel 加了文字、图标、或改成 opaque，不会出 bu
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
        showDisplayBubble(greeting);

        // 异步调用AI获取回复
        CompletableFuture<String> future = aiService.chatAsync(greeting);
        future.thenAccept(reply -> {
            SwingUtilities.invokeLater(() -> {
                // 解析回复：||| 前是显示内容
                String displayText = parseDisplayText(reply);
                if (displayText != null && !displayText.isEmpty()) {
                    showDisplayBubble(displayText);
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
        // 先显示用户消息的确认
        showDisplayBubble("em..em...");
        // 异步调用AI
        CompletableFuture<String> future = aiService.chatAsync(message);
        future.thenAccept(reply -> {
            SwingUtilities.invokeLater(() -> {
                String displayText = parseDisplayText(reply);
                if (displayText != null && !displayText.isEmpty()) {
                    showDisplayBubble(displayText);
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
     * 显示对话气泡
     */
    private void showDisplayBubble(String message) {
        if (currentBubble != null) {
            currentBubble.dispose();
        }

        Point petLoc = petWindow.getLocation();
        int centerX = petLoc.x + petWidth / 2;
        int topY = petLoc.y;

        ChatBubble bubble = new ChatBubble(petWindow, message);
        currentBubble = bubble;
        bubble.showAt(centerX, topY);

        // 3秒后自动消失
        Timer timer = new Timer(3000, e -> {
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
                if (petVisible) {
                    showInputDialog();
                }
                else{
                    togglePetVisibility();
                    showInputDialog();
                }
            });
        });
        mouseHook.install();
    }

    /**
     * 高质量缩放图片（避免 getScaledInstance 导致的模糊）
     */
    private BufferedImage scaleImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return scaled;
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

        // 使用 Graphics2D + BICUBIC 高质量缩放托盘图标
        BufferedImage trayImage = null;
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/resource/001.png");
            if (is == null) {
                throw new Exception("找不到图片资源: /resource/001.png");
            }
            BufferedImage original = ImageIO.read(is);
            int traySize = 16;
            trayImage = scaleImage(original, traySize, traySize);
        } catch (Exception e) {
            System.err.println("加载托盘图标失败: " + e.getMessage());
            // 回退：创建一个简单的彩色图标
            trayImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = trayImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(255, 182, 193));
            g2d.fillOval(0, 0, 16, 16);
            g2d.dispose();
        }

        trayIcon = new TrayIcon(trayImage, "KSM 桌面宠物");
        trayIcon.setImageAutoSize(false); // 禁用系统自动缩放，保持 BICUBIC 效果

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
