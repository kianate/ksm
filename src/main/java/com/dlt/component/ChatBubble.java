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

    // 显示模式窗口
    private JWindow displayWindow;


    // 输入模式对话框,用JDialog
    private JDialog inputDialog;
    // 输入框,用JTextField
    private JTextField inputField;

    /**
     * 构造函数 - 显示模式（AI回复）
     */
    public ChatBubble(Window owner, String message) {
        this.message = message;
        this.font = new Font("Microsoft YaHei", Font.PLAIN, 14);//字体设置要new一个Font对象
        this.isInputMode = false;
        initDisplayBubble(owner);
    }

    /**
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
     * 初始化显示模式气泡（JWindow）- Ant Design 风格
     */
    private void initDisplayBubble(Window owner) {
        displayWindow = new JWindow(owner);
        displayWindow.setAlwaysOnTop(true);
        displayWindow.setFocusableWindowState(true);
        displayWindow.setBackground(new Color(0, 0, 0, 0));

        // 主面板：Ant Design 风格 - 白色背景 + 柔和阴影
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int r = BORDER_RADIUS;
                int s = SHADOW_SIZE;

                // 绘制柔和阴影（多层渐变）
                for (int i = s; i > 0; i--) {
                    float alpha = 0.04f * (s - i + 1) / s;
                    g2d.setColor(new Color(0, 0, 0, Math.min(255, (int)(alpha * 255))));
                    g2d.fill(new RoundRectangle2D.Float(
                        s - i, s - i / 2, w - 2 * s + 2 * i, h - 2 * s + i * 2, r + i, r + i));
                }

                // 绘制白色圆角背景
                g2d.setColor(COLOR_BG_WHITE);
                g2d.fill(new RoundRectangle2D.Float(s, s, w - 2 * s, h - 2 * s, r, r));

                // 绘制极细边框（Ant Design 的 subtle border）
                g2d.setColor(new Color(0, 0, 0, 15)); // 非常淡的边框
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw(new RoundRectangle2D.Float(s + 0.5f, s + 0.5f, w - 2 * s - 1, h - 2 * s - 1, r, r));

                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
            SHADOW_SIZE + 12, SHADOW_SIZE + 16, SHADOW_SIZE + 12, SHADOW_SIZE + 16));

        // 顶部栏：关闭按钮在右上角（Ant Design 风格 - 灰色小图标）
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
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

        displayWindow.setContentPane(mainPanel);
        displayWindow.pack();

        // 限制最终尺寸
        Dimension winSize = displayWindow.getSize();
        winSize.width = Math.min(winSize.width + 10, maxWidth + SHADOW_SIZE * 2);
        winSize.height = Math.min(winSize.height + 6, maxHeight + 40 + SHADOW_SIZE * 2);
        displayWindow.setSize(winSize);
    }

    /**
     * 初始化输入模式气泡（JDialog）- Ant Design 风格
     */
    private void initInputBubble(Window owner) {
        // JWindow不能作为JDialog的owner，使用null代替（已通过setAlwaysOnTop保持层级）
        inputDialog = new JDialog((Frame) null);
        inputDialog.setUndecorated(true);
        inputDialog.setAlwaysOnTop(true);
        inputDialog.setFocusableWindowState(true);
        inputDialog.setBackground(new Color(0, 0, 0, 0));//颜色使用需要new颜色对象
        inputDialog.setSize(maxWidth + SHADOW_SIZE * 2, 52 + SHADOW_SIZE * 2);

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
                    g2d.setColor(new Color(0, 0, 0, Math.min(255, (int)(alpha * 255))));
                    g2d.fill(new RoundRectangle2D.Float(
                            s - i, s - (float) i / 2, w - 2 * s + 2 * i, h - 2 * s + i * 2, r + i, r + i));
                }
                g2d.setColor(COLOR_BG_WHITE);
                g2d.fill(new RoundRectangle2D.Float(s, s, w - 2 * s, h - 2 * s, r, r));

                g2d.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(
            SHADOW_SIZE + 8, SHADOW_SIZE + 12, SHADOW_SIZE + 8, SHADOW_SIZE + 12));

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
                return new Insets(6, 12, 6, 12);
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
        // 失去焦点时关闭输入框（带延迟防止IME输入法弹窗导致误关闭）
        final int DISPOSE_DELAY = 300; // 毫秒
        final Timer[] disposeTimer = {null};
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
        // IME可能导致窗口尺寸变化，触发重绘
        inputDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
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
     * 显示普通气泡（JWindow）
     */
    private void showDisplayAt(int x, int y) {
        int bubbleX = x - displayWindow.getWidth() / 2;
        int bubbleY = y - displayWindow.getHeight() - 10;

        if (bubbleY < 0) {
            bubbleY = y + 20;
        }

        displayWindow.setLocation(bubbleX, bubbleY);
        displayWindow.setVisible(true);
    }

    /**
     * 更新气泡位置（跟随宠物移动）
     */
    public void updatePosition(int x, int y) {
        Window window = isInputMode ? inputDialog : displayWindow;
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
        if (displayWindow != null) {
            displayWindow.dispose();
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
        return displayWindow != null && displayWindow.isVisible();
    }

    /**
     * 获取宽度
     */
    public int getWidth() {
        Window window = isInputMode ? inputDialog : displayWindow;
        return window != null ? window.getWidth() : 0;
    }

    /**
     * 获取高度
     */
    public int getHeight() {
        Window window = isInputMode ? inputDialog : displayWindow;
        return window != null ? window.getHeight() : 0;
    }
}
