package com.dlt.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.RoundRectangle2D;

/**
 * 对话气泡组件 - Ant Design 风格
 * 显示模式：AI回复气泡（带阴影、圆角、优雅关闭按钮）
 * 输入模式：用户输入气泡（现代输入框、蓝色聚焦边框）
 */
public class ChatBubble {

    private String message;
    private Font font;
    private int maxWidth = 340;
    private int maxHeight = 400;
    private boolean isInputMode;
    private ActionListener onSubmit;

    // Ant Design 配色
    private static final Color COLOR_BG_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_TEXT_PRIMARY = new Color(0, 0, 0, 218);     // rgba(0,0,0,0.85)
    private static final Color COLOR_TEXT_SECONDARY = new Color(0, 0, 0, 115);   // rgba(0,0,0,0.45)
    private static final Color COLOR_TEXT_DISABLED = new Color(0, 0, 0, 64);     // rgba(0,0,0,0.25)
    private static final Color COLOR_BORDER = new Color(217, 217, 217);          // #d9d9d9
    private static final Color COLOR_PRIMARY = new Color(22, 119, 255);          // #1677ff
    private static final Color COLOR_PRIMARY_HOVER = new Color(64, 150, 255);    // #4096ff
    private static final Color COLOR_FILL_SECONDARY = new Color(0, 0, 0, 10);    // rgba(0,0,0,0.04)
    private static final Color COLOR_INPUT_PLACEHOLDER = new Color(0, 0, 0, 64); // rgba(0,0,0,0.25)

    private static final int BORDER_RADIUS = 12;
    private static final int SHADOW_SIZE = 8;

    // 显示模式对话框（改用 JDialog，配合 setShape 实现圆角）
    private JDialog displayDialog;
    /* [旧] 显示模式使用 JWindow，无法使用 setShape 圆角且不支持透明背景
    private JWindow displayWindow;
    */


    // 输入模式对话框,用JDialog
    private JDialog inputDialog;
    // 输入框,用JTextField
    private JTextField inputField;

     // 专用隐藏 Frame 作为 JDialog 的稳定 owner，避免 IME 输入法窗口抢占 owner 关系
    private static Frame stableOwnerFrame;

    private static Frame getStableOwnerFrame() {
        if (stableOwnerFrame == null) {
            stableOwnerFrame = new Frame();
            stableOwnerFrame.setUndecorated(true);
            stableOwnerFrame.setSize(0, 0);
        }
        return stableOwnerFrame;
    }

    /**
     * 判断窗口是否是 IME 输入法窗口
     * IME 窗口通常类名包含 "InputMethod" 或是 Window 但不是 Frame/Dialog
     */
    private static boolean isIMEWindow(Window w) {
        if (w == null) return false;
        String className = w.getClass().getName();
        return className.contains("InputMethod") || className.contains("IME");
    }

    /**
     * 构造函数 - 显示模式（AI回复）
     */
    public ChatBubble(Window owner, String message) {
        this.message = message;
        this.font = new Font("Microsoft YaHei", Font.PLAIN, 14);//字体设置要new一个Font对象
        this.isInputMode = false;
        initDisplayBubble(owner);
    }

    /**g
     * 构造函数 - 输入模式
     */
    public ChatBubble(Window owner, ActionListener onSubmit) {
        this.message = "";
        this.font = new Font("Microsoft YaHei", Font.PLAIN, 14);
        this.isInputMode = true;
        this.onSubmit = onSubmit;
        initInputBubble(owner);
    }

    /**
     * 初始化显示模式气泡（JDialog）- Ant Design 风格
     * 改用 JDialog + setShape 实现圆角，不依赖透明背景
     */
    private void initDisplayBubble(Window owner) {
        /* [旧] 使用 JWindow，透明背景绘制阴影+圆角
        displayWindow = new JWindow(owner);
        displayWindow.setAlwaysOnTop(true);
        displayWindow.setFocusableWindowState(true);
        displayWindow.setBackground(new Color(0, 0, 0, 0));
        */
        displayDialog = new JDialog(getStableOwnerFrame());
        displayDialog.setUndecorated(true);
        displayDialog.setAlwaysOnTop(true);
        displayDialog.setFocusableWindowState(true);
        displayDialog.setBackground(COLOR_BG_WHITE); // 不透明背景

        // 主面板：不透明 + 圆角绘制，配合 setShape 裁剪
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int r = BORDER_RADIUS * 2;

                // 绘制白色圆角背景
                g2d.setColor(COLOR_BG_WHITE);
                g2d.fill(new RoundRectangle2D.Float(0, 0, w, h, r, r));

                // 绘制极细边框（Ant Design 的 subtle border）
                g2d.setColor(new Color(0, 0, 0, 15));
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, r, r));

                g2d.dispose();
            }
        };
        mainPanel.setOpaque(true);
        mainPanel.setBackground(COLOR_BG_WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // 顶部栏：关闭按钮在左上角（Ant Design 风格 - 灰色小图标）
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topBar.setOpaque(false);

        // Ant Design 风格的关闭按钮：灰色 × 悬停变深色
        JButton closeButton = new JButton("×") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 悬停时显示淡灰色背景
                if (getModel().isRollover()) {
                    g2d.setColor(COLOR_FILL_SECONDARY);
                    g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                }
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        closeButton.setForeground(COLOR_TEXT_DISABLED);
        closeButton.setPreferredSize(new Dimension(22, 22));
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        // 悬停变色
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(COLOR_TEXT_SECONDARY);
                closeButton.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(COLOR_TEXT_DISABLED);
                closeButton.repaint();
            }
        });
        topBar.add(closeButton);

        // 文本显示区域
        JTextArea textArea = new JTextArea(message);
        textArea.setFont(font);
        textArea.setForeground(COLOR_TEXT_PRIMARY);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFocusable(true);
        // 组件级 setOpaque(false) 不影响 IME（显示模式不涉及输入法）
        textArea.setOpaque(false);
        textArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        // 计算合适的文本区域尺寸
        int textAreaWidth = maxWidth - 80;
        textArea.setSize(textAreaWidth, Short.MAX_VALUE);
        Dimension prefSize = textArea.getPreferredSize();
        textArea.setPreferredSize(new Dimension(
            Math.min(prefSize.width + 10, maxWidth - 60),
            Math.min(prefSize.height, maxHeight)
        ));

        // 滚动面板（Ant Design 风格滚动条）
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);
        scrollPane.getVerticalScrollBar().setOpaque(false);

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        displayDialog.setContentPane(mainPanel);
        displayDialog.pack();

        // 限制最终尺寸
        Dimension winSize = displayDialog.getSize();
        winSize.width = Math.min(winSize.width + 10, maxWidth);
        winSize.height = Math.min(winSize.height + 6, maxHeight + 40);
        displayDialog.setSize(winSize);

        // 使用 setShape 裁剪为圆角矩形
        displayDialog.setShape(new RoundRectangle2D.Float(0, 0, displayDialog.getWidth(), displayDialog.getHeight(), BORDER_RADIUS * 2, BORDER_RADIUS * 2));

        // 窗口尺寸变化时更新圆角形状
        displayDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                displayDialog.setShape(new RoundRectangle2D.Float(0, 0, displayDialog.getWidth(), displayDialog.getHeight(), BORDER_RADIUS * 2, BORDER_RADIUS * 2));
                mainPanel.repaint();
            }
        });
    }

    /**
     * 初始化输入模式气泡（JDialog）- Ant Design 风格
     */
    private void initInputBubble(Window owner) {
        /* [旧] 使用 (Frame) null 作为 owner，Swing 内部使用共享隐藏 Frame，IME 激活时可能干扰 owner 关系
        inputDialog = new JDialog((Frame) null);
        */
        // 使用专用隐藏 Frame 作为稳定 owner，防止 IME 输入法窗口抢占 owner 关系
        inputDialog = new JDialog(getStableOwnerFrame());
        inputDialog.setUndecorated(true);
        inputDialog.setAlwaysOnTop(true);
        inputDialog.setFocusableWindowState(true);
        /* [旧] 透明背景导致IME候选框残留
        inputDialog.setBackground(new Color(0, 0, 0, 0));
        */
        inputDialog.setBackground(COLOR_BG_WHITE); // 不透明背景，配合 setShape 实现圆角
        inputDialog.setSize(348, 55);
        // 使用 setShape 裁剪为圆角矩形，无需透明背景
        inputDialog.setShape(new RoundRectangle2D.Float(0, 0, inputDialog.getWidth(), inputDialog.getHeight(), BORDER_RADIUS * 2, BORDER_RADIUS * 2));

        // 主面板 - 带阴影
        /* [旧] IME触发时透明窗口重绘残留导致直角边，缺少AlphaComposite清除像素
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                int w = getWidth();
                int h = getHeight();
                int r = BORDER_RADIUS;
                int s = SHADOW_SIZE;
                // 绘制柔和阴影
                for (int i = s; i > 0; i--) {
                    float alpha = 0.04f * (s - i + 1) / s;
                    g2d.setColor(new Color(0, 0, 0, Math.min(255, (int)(alpha * 255))));
                    g2d.fill(new RoundRectangle2D.Float(
                            s - i, s - (float) i / 2, w - 2 * s + 2 * i, h - 2 * s + i * 2, r + i, r + i));
                }
                // 白色圆角背景
                g2d.setColor(COLOR_BG_WHITE);
                g2d.fill(new RoundRectangle2D.Float(s, s, w - 2 * s, h - 2 * s, r, r));

                g2d.dispose();
            }
        };
        */
        /* [旧] 透明背景方案：需要 AlphaComposite.Clear + setOpaque(false)，但会导致 IME 候选框残留
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int r = BORDER_RADIUS;
                int s = SHADOW_SIZE;

                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, w, h);

                g2d.setComposite(AlphaComposite.SrcOver);
                for (int i = s; i > 0; i--) {
                    float alpha = 0.04f * (s - i + 1) / s;
                    g2d.setColor(new Color(0, 0, 0, Math.min(255, (int)(alpha * 255)));
                    g2d.fill(new RoundRectangle2D.Float(
                            s - i, s - (float) i / 2, w - 2 * s + 2 * i, h - 2 * s + i * 2, r + i, r + i));
                }
                g2d.setColor(COLOR_BG_WHITE);
                g2d.fill(new RoundRectangle2D.Float(s, s, w - 2 * s, h - 2 * s, r, r));

                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        */
        // 不透明面板方案：配合 JDialog.setShape() 实现圆角，避免 IME 兼容问题
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 填充白色圆角背景
                g2d.setColor(COLOR_BG_WHITE);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), BORDER_RADIUS * 2, BORDER_RADIUS * 2));
                // 绘制极细边框
                g2d.setColor(new Color(0, 0, 0, 15));
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, BORDER_RADIUS * 2, BORDER_RADIUS * 2));
                g2d.dispose();
            }
        };
        panel.setOpaque(true);
        panel.setBackground(COLOR_BG_WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(7, 11, 7, 11));

        // Ant Design 风格输入框
        inputField = new JTextField("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // 绘制占位文本
                if (getText().isEmpty()) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setFont(getFont());
                    g2d.setColor(COLOR_INPUT_PLACEHOLDER);
                    FontMetrics fm = g2d.getFontMetrics();
                    Insets ins = getInsets();
                    g2d.drawString("输入消息...", ins.left + 4, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2d.dispose();
                }
            }
        };
        inputField.setFont(font);
        inputField.setForeground(COLOR_TEXT_PRIMARY);
        inputField.setCaretColor(COLOR_PRIMARY);
        inputField.setSelectionColor(new Color(22, 119, 255, 50));
        /* [旧] 输入框不透明会导致IME时出现"两个输入框"的视觉问题，让面板圆角背景正确显示
        inputField.setOpaque(true);
        inputField.setBackground(Color.WHITE);
        */
        /* [旧] 透明背景配合透明面板，但会导致IME候选框残留
        inputField.setOpaque(false);
        inputField.setBackground(new Color(0, 0, 0, 0));
        */
        // 输入框不画自身背景（setOpaque false），露出面板的白色背景
        // 注意：组件级 setOpaque(false) 不同于窗口级透明背景，不会导致 IME 候选框残留
        inputField.setOpaque(false);
        inputField.setBackground(new Color(0, 0, 0, 0));
        // Ant Design 风格边框：默认灰色，聚焦蓝色（通过自定义 Border 实现）
        inputField.setBorder(new javax.swing.border.AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 8;
                if (c.hasFocus()) {
                    // 蓝色外发光
                    g2d.setColor(new Color(22, 119, 255, 40));
                    g2d.setStroke(new BasicStroke(3.0f));
                    g2d.draw(new RoundRectangle2D.Float(x + 1, y + 1, w - 2, h - 2, r, r));
                    // 蓝色主边框
                    g2d.setColor(COLOR_PRIMARY);
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, r, r));
                } else {
                    g2d.setColor(COLOR_BORDER);
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, r, r));
                }
                g2d.dispose();
            }
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(5, 11, 5, 11);
            }
        });

        // 聚焦状态追踪
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                inputField.repaint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                inputField.repaint();
            }
        });

        // 按回车提交
        inputField.addActionListener(e -> submitInput());

        // ESC关闭
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        panel.add(inputField, BorderLayout.CENTER);
        inputDialog.setContentPane(panel);

        /* [旧] DISPOSE_DELAY=1ms太短，IME激活时焦点闪烁来不及取消就dispose了，导致渲染异常
        // 失去焦点时关闭输入框（带延迟防止IME输入法弹窗导致误关闭）
        final int DISPOSE_DELAY = 1; // 毫秒
        final Timer[] disposeTimer = {null};
        inputDialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // 重新获得焦点时取消关闭
                if (disposeTimer[0] != null && disposeTimer[0].isRunning()) {
                    disposeTimer[0].stop();
                }
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
                // 延迟关闭，给IME弹窗等短暂失焦一个恢复机会
                if (disposeTimer[0] != null && disposeTimer[0].isRunning()) {
                    disposeTimer[0].stop();
                }
                disposeTimer[0] = new Timer(DISPOSE_DELAY, ev -> dispose());
                disposeTimer[0].setRepeats(false);
                disposeTimer[0].start();
            }
        });
        */
        /* [旧] 未识别 IME 窗口，IME 激活时触发不必要的 dispose 和重绘
        inputDialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // 重新获得焦点时取消关闭，并重绘面板恢复圆角
                if (disposeTimer[0] != null && disposeTimer[0].isRunning()) {
                    disposeTimer[0].stop();
                }
                panel.repaint();
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
                // 延迟关闭，给IME弹窗等短暂失焦一个恢复机会
                if (disposeTimer[0] != null && disposeTimer[0].isRunning()) {
                    disposeTimer[0].stop();
                }
                disposeTimer[0] = new Timer(DISPOSE_DELAY, ev -> {
                    if (!inputDialog.isFocused()) {
                        dispose();
                    }
                });
                disposeTimer[0].setRepeats(false);
                disposeTimer[0].start();
            }
        });
        */
        // 失去焦点时关闭输入框（带延迟防止IME输入法弹窗导致误关闭）
        final int DISPOSE_DELAY = 300; // 毫秒
        final Timer[] disposeTimer = {null};
        inputDialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // 重新获得焦点时取消关闭
                if (disposeTimer[0] != null && disposeTimer[0].isRunning()) {
                    disposeTimer[0].stop();
                }
                // 从 IME 输入法恢复焦点时不重绘，避免 IME 作为 owner 时重绘异常
                if (!isIMEWindow(e.getOppositeWindow())) {
                    panel.repaint();
                }
            }
            @Override
            public void windowLostFocus(WindowEvent e) {
                // IME 输入法窗口夺走焦点时，不应触发关闭逻辑
                if (isIMEWindow(e.getOppositeWindow())) {
                    return;
                }
                // 延迟关闭，给IME弹窗等短暂失焦一个恢复机会
                if (disposeTimer[0] != null && disposeTimer[0].isRunning()) {
                    disposeTimer[0].stop();
                }
                disposeTimer[0] = new Timer(DISPOSE_DELAY, ev -> {
                    if (!inputDialog.isFocused()) {
                        dispose();
                    }
                });
                disposeTimer[0].setRepeats(false);
                disposeTimer[0].start();
            }
        });
        // 窗口尺寸变化时更新圆角形状
        inputDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                inputDialog.setShape(new RoundRectangle2D.Float(0, 0, inputDialog.getWidth(), inputDialog.getHeight(), BORDER_RADIUS * 2, BORDER_RADIUS * 2));
                panel.repaint();
            }
        });
    }

    /**
     * 提交输入内容
     */
    private void submitInput() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && onSubmit != null) {
            onSubmit.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, text));
        }
        dispose();
    }

    /**
     * 显示气泡在指定位置
     */
    public void showAt(int x, int y) {
        if (isInputMode) {
            showInputAt(x, y);
        } else {
            showDisplayAt(x, y);
        }
    }

    /**
     * 显示输入气泡（JDialog）
     */
    private void showInputAt(int x, int y) {
        int bubbleX = x - inputDialog.getWidth() / 2;
        int bubbleY = y - inputDialog.getHeight() - 10;

        if (bubbleY < 0) {
            bubbleY = y + 20;
        }

        inputDialog.setLocation(bubbleX, bubbleY);
        inputDialog.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            inputField.requestFocusInWindow();
        });
    }

    /**
     * 显示对话气泡（JDialog）
     */
    private void showDisplayAt(int x, int y) {
        int bubbleX = x - displayDialog.getWidth() / 2;
        int bubbleY = y - displayDialog.getHeight() - 10;

        if (bubbleY < 0) {
            bubbleY = y + 20;
        }

        displayDialog.setLocation(bubbleX, bubbleY);
        displayDialog.setVisible(true);
    }

    /**
     * 更新气泡位置（跟随宠物移动）
     */
    public void updatePosition(int x, int y) {
        Window window = isInputMode ? inputDialog : displayDialog;
        if (window == null || !window.isVisible()) {
            return;
        }

        int bubbleX = x - window.getWidth() / 2;
        int bubbleY = y - window.getHeight() - 10;

        if (bubbleY < 0) {
            bubbleY = y + 20;
        }

        window.setLocation(bubbleX, bubbleY);
    }

    /**
     * 释放气泡
     */
    public void dispose() {
        /* [旧] JWindow 方案
        if (displayWindow != null) {
            displayWindow.dispose();
        }
        */
        if (displayDialog != null) {
            displayDialog.dispose();
        }
        if (inputDialog != null) {
            inputDialog.dispose();
        }
    }

    /**
     * 是否可见
     */
    public boolean isVisible() {
        if (isInputMode && inputDialog != null) {
            return inputDialog.isVisible();
        }
        return displayDialog != null && displayDialog.isVisible();
    }

    /**
     * 获取宽度
     */
    public int getWidth() {
        Window window = isInputMode ? inputDialog : displayDialog;
        return window != null ? window.getWidth() : 0;
    }

    /**
     * 获取高度
     */
    public int getHeight() {
        Window window = isInputMode ? inputDialog : displayDialog;
        return window != null ? window.getHeight() : 0;
    }
}
