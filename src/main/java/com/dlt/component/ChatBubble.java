 package com.dlt.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

/**
 * 对话气泡组件 - 简单风格
 * 显示模式：AI回复气泡（简单白底黑字）
 * 输入模式：用户输入框（简单输入框）
 */
public class ChatBubble {

    private String message;
    private Font font;
    private int maxWidth = 300;
    private int maxHeight = 300;
    private boolean isInputMode;
    private ActionListener onSubmit;

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
        this.font = new Font("Microsoft YaHei", Font.PLAIN, 14);
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
     * 初始化显示模式气泡（JWindow）- 简单风格
     */
    private void initDisplayBubble(Window owner) {
        displayWindow = new JWindow(owner);
        displayWindow.setAlwaysOnTop(true);
        displayWindow.setFocusableWindowState(true);
        displayWindow.setBackground(new Color(0, 0, 0, 0));

        // 主面板：简单白色背景 + 黑色细边框
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // 白色背景
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, w, h);
                // 黑色细边框
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRect(0, 0, w - 1, h - 1);
                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // 关闭按钮
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeButton.setForeground(Color.GRAY);
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(e -> dispose());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);
        topBar.add(closeButton);

        // 文本显示区域
        JTextArea textArea = new JTextArea(message);
        textArea.setFont(font);
        textArea.setForeground(Color.BLACK);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        // 计算合适的文本区域尺寸
        int textAreaWidth = maxWidth - 40;
        textArea.setSize(textAreaWidth, Short.MAX_VALUE);
        Dimension prefSize = textArea.getPreferredSize();
        textArea.setPreferredSize(new Dimension(
            Math.min(prefSize.width + 10, maxWidth - 30),
            Math.min(prefSize.height, maxHeight)
        ));

        // 滚动面板
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        displayWindow.setContentPane(mainPanel);
        displayWindow.pack();

        // 限制最终尺寸
        Dimension winSize = displayWindow.getSize();
        winSize.width = Math.min(winSize.width + 10, maxWidth);
        winSize.height = Math.min(winSize.height + 6, maxHeight + 40);
        displayWindow.setSize(winSize);
    }

    /**
     * 初始化输入模式气泡（JDialog）- 简单风格
     */
    private void initInputBubble(Window owner) {
        inputDialog = new JDialog((Frame) null);
        inputDialog.setUndecorated(true);
        inputDialog.setAlwaysOnTop(true);
        inputDialog.setFocusableWindowState(true);
        // 不使用透明背景，避免IME输入法兼容性问题
        inputDialog.setBackground(Color.WHITE);
        inputDialog.setSize(maxWidth, 44);

        // 主面板 - 简单白色背景 + 灰色边框
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 完全填充白色背景（防止IME重绘残留）
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, w, h);
                
                // 灰色边框
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRect(0, 0, w - 1, h - 1);

                g2d.dispose();
            }
        };
        panel.setOpaque(true); // 改为不透明，避免IME问题
        panel.setBackground(Color.WHITE); // 设置明确背景色
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // 简单输入框
        inputField = new JTextField("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 绘制占位文本
                if (getText().isEmpty()) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setFont(getFont());
                    g2d.setColor(Color.LIGHT_GRAY);
                    FontMetrics fm = g2d.getFontMetrics();
                    Insets ins = getInsets();
                    g2d.drawString("输入消息...", ins.left + 4, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2d.dispose();
                }
            }
        };
        inputField.setFont(font);
        inputField.setForeground(Color.BLACK);
        inputField.setOpaque(true); // 改为不透明
        inputField.setBackground(Color.WHITE); // 明确设置白色背景
        inputField.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

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
