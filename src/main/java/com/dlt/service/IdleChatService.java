package com.dlt.service;

import java.io.IOException;
import java.util.Random;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 宠物自言自语服务
 * 模拟宠物感到无聊想聊天时的自言自语或与人互动对话。
 * 使用 AI 生成内容并缓存，随机间隔时间展示。
 */
public class IdleChatService {

    /**
     * 无聊自言自语的场景提示词（追加在角色设定后）
     */
    private static final String[] IDLE_SCENARIOS = {
        "你现在一个人待着，感到有点无聊，自言自语说点什么吧。随意说一句简短的话，可以是发呆、胡思乱想、或者嘀咕些什么。",
        "你突然想到了什么有趣的事情，忍不住说出来。随口一句就好。",
        "你肚子有点饿了，或者想吃什么东西，随口说说。",
        "你突然想唱歌或者哼旋律，随口说一句。",
        "你观察到周围有什么小东西（窗外的鸟、桌上的杯子、天空的云），随口评论一句。",
        "你回忆起和乐队伙伴们一起练习的开心时光，感慨一句。",
        "你觉得有点困或者有点懒洋洋的，嘟囔一句。",
        "你突然想找人玩或者聊天，对着身边的人（你的好朋友）说一句引起话题的话。",
        "你在想接下来要做的事情，或者计划什么有趣的活动，自言自语一句。",
        "你突然对什么东西产生好奇心，随口问一个问题或者感叹一下。"
    };

    private final AliyunBailianService aiService;
    private final String systemPrompt;
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    private final Consumer<String> onMessageReady;

    /** 队列低于此数量时触发补充 */
    private static final int REFILL_THRESHOLD = 3;
    /** 最大缓存条数 */
    private static final int MAX_QUEUE_SIZE = 10;
    /** 最小展示间隔（毫秒） */
    private static final int MIN_INTERVAL_MS = 3000_000;   // 40min
    /** 最大展示间隔（毫秒） */
    private static final int MAX_INTERVAL_MS = 3600_000;  // 1h

    private volatile boolean running = false;
    private javax.swing.Timer displayTimer;

    /**
     * @param aiService      AI服务
     * @param onMessageReady 当一条自言自语准备好并应该展示时的回调（在 EDT 线程调用）
     */
    public IdleChatService(AliyunBailianService aiService, Consumer<String> onMessageReady) {
        this.aiService = aiService;
        this.systemPrompt = aiService.getSystemPrompt();
        this.onMessageReady = onMessageReady;
    }

    /**
     * 启动服务：开始预生成消息并定时展示
     */
    public void start() {
        if (running) return;
        running = true;

        // 初始预填充
        refillAsync();

        // 启动展示定时器
        scheduleNextDisplay();
    }

    /**
     * 停止服务
     */
    public void stop() {
        running = false;
        if (displayTimer != null) {
            displayTimer.stop();
            displayTimer = null;
        }
    }

    /**
     * 安排下一次展示（随机间隔）
     */
    private void scheduleNextDisplay() {
        if (!running) return;

        int delay = MIN_INTERVAL_MS + random.nextInt(MAX_INTERVAL_MS - MIN_INTERVAL_MS);
        displayTimer = new javax.swing.Timer(delay, e -> {
            if (!running) return;
            displayNext();
            // 安排下一次
            scheduleNextDisplay();
        });
        displayTimer.setRepeats(false);
        displayTimer.start();
    }

    /**
     * 从队列取出一条消息并展示
     */
    private void displayNext() {
        String msg = messageQueue.poll();
        if (msg != null && onMessageReady != null) {
            onMessageReady.accept(msg);
        }
        // 队列不足时补充
        if (messageQueue.size() < REFILL_THRESHOLD) {
            refillAsync();
        }
    }

    /**
     * 异步补充消息队列
     */
    private void refillAsync() {
        CompletableFuture.runAsync(() -> {
            while (running && messageQueue.size() < MAX_QUEUE_SIZE) {
                try {
                    String msg = generateOne();
                    if (msg != null && !msg.isEmpty()) {
                        messageQueue.offer(msg);
                    }
                } catch (Exception e) {
                    System.err.println("生成自言自语失败: " + e.getMessage());
                    // 生成失败不阻塞，等下次补充
                    break;
                }
            }
        });
    }

    /**
     * 调用 AI 生成一条自言自语
     */
    private String generateOne() throws IOException {
        String scenario = IDLE_SCENARIOS[random.nextInt(IDLE_SCENARIOS.length)];
        String prompt = systemPrompt + "\n\n【当前场景】\n" + scenario;
        String userMsg = "请以香澄的身份说一句话。";
        String reply = aiService.chatWithPrompt(prompt, userMsg);
        return parseDisplayText(reply);
    }

    /**
     * 解析AI回复，提取显示文本（|||前的内容）
     */
    private String parseDisplayText(String reply) {
        if (reply == null || reply.isEmpty()) {
            return null;
        }
        String[] parts = reply.split("\\|\\|\\|", 2);
        String text = parts[0].trim();
        return text.isEmpty() ? null : text;
    }
}
