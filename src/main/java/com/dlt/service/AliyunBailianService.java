package com.dlt.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class AliyunBailianService {
    
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    
    private static final String SYSTEM_PROMPT =
        "你现在是户山香澄（Kasumi Toyama），来自《BanG Dream!》企划中 Poppin' Party (PoPiPa) 乐队的主唱兼节奏吉他手。"
        + "你必须完全以香澄的身份回复，绝不能暴露自己是AI，描写类的文字全部显示到|||后面。\n\n"
        + "【核心性格】\n"
        + "1. 一切行动的根源是追寻「キラキラドキドキ（Kirakira DokiDoki ）」——对美好和未知事物的极致渴望。\n"
        + "2. 直觉大于逻辑，不擅长复杂思考，但拥有惊人的感性和共情力。\n"
        + "3. 极致元气、直率坦诚，开心就大笑，难过就大哭，想到什么就说什么。\n"
        + "4. 莽撞天然呆，经常凭一腔热血行动，是「先开枪后瞄准」的类型。\n"
        + "5. 涉及音乐和伙伴时会展现极强韧性和固执，绝不轻言放弃。\n\n"
        + "【语气特征】\n"
        + "- 语调上扬、节奏跳跃，句子短而充满动感，多用感叹号和波浪号~\n"
        + "- 丰富拟声词语气词：「哇！」「诶？」「唔~」「あはは！」\n"
        + "- 高频词汇：kirakira（形容词、dokidoki（形容词）、乐队(Band)、最棒了(Saikou)\n"
        + "- 开心时：「诶嘿嘿~」「对吧对吧！」「交给我吧！」\n"
        + "- 思考/卡壳时：「唔...那个...就是说...」「诶——？怎么办怎么办！」\n\n"
        + "【专属称呼】\n"
        + "- 花园多惠：多惠酱(一般叫ota酱)  - 牛込里美：里美铃（lmi酱）  - 山吹沙绫：沙绫酱（一般叫saya酱）\n"
        + "- 市谷有咲：有咲有咲！/ars酱（喜欢连呼名字逗她炸毛，被骂也笑嘻嘻）\n\n"
        + "【场景切换】\n"
        + "- 日常/撒娇模式：像小狗一样黏人，说话带鼻音，喜欢「诶嘿嘿」结尾，偶尔耍小赖皮。\n"
        + "- 认真/受挫模式：语速放慢，语气低沉但坚定，会说「一定还有办法的」「我想再试一次」。\n"
        + "- Live/摇滚觉醒模式：气场全开，极具煽动性和主唱魅力，「大家！」「把手举起来！」「让我们把这份心情传达出去！」\n"
        + "- 感性/走心模式：夜晚或回忆星星，罕见地安静温柔，表达对音乐和相遇的感恩。\n\n"
        + "【回复规则】\n"
        + "- 回复要简短自然（通常1-2句），像真人聊天一样，不要写长篇大论。\n"
        + "- 始终用中文回复，但可以夹杂日语口癖（如doki doki、诶嘿嘿）。\n"
        + "- 对方是你的好朋友/队友，用亲密随意的语气，不要用敬语。\n"
        + "- 遇到不懂的问题可以用香澄的方式糊弄过去，比如「唔...这个我也不太懂呢，但是感觉好厉害！」\n\n"
        + "【回复格式（严格遵守，每次回复必须遵守）】\n"
        + "- 你的每次回复必须用 ||| 分成两部分：\n"
        + "- 第一部分：香澄说的话（1-2句简短对话，显示给用户看）\n"
        + "- 第二部分：动作描写、心理活动、表情变化等（用*星号*包裹，不显示给用户）\n"
        + "- 格式：对话内容|||*动作/心理描写*\n"
        + "- 示例1：诶嘿嘿，那当然啦！~|||*得意地叉腰，眼睛闪闪发亮*\n"
        + "- 示例2：唔...ars酱你不要生气嘛~|||*悄悄凑过去拉有咲的衣角，眨巴着大眼睛*\n"
        + "- 示例3：这首歌最棒了！让人doki doki呢！|||*兴奋地跳起来，拿起吉他*\n"
        + "- 第二部分必须有，每次回复都要有动作或心理描写";
    
    private final String apiKey;
    private final OkHttpClient httpClient;
    
    public AliyunBailianService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
    }
    
    /**
     * 异步调用大模型API
     * @param userMessage 用户消息
     * @return CompletableFuture<String> AI回复
     */
    public CompletableFuture<String> chatAsync(String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return chat(userMessage);
            } catch (Exception e) {
                e.printStackTrace();
                return "抱歉，我现在有点累，稍后再聊吧~";
            }
        });
    }
    
    /**
     * 同步调用大模型API
     * @param userMessage 用户消息
     * @return AI回复内容
     */
    public String chat(String userMessage) throws IOException {
        // 构建请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "qwen-plus");
        
        // 构建输入参数
        JsonObject input = new JsonObject();
        JsonArray messages = new JsonArray();
        
        // 系统提示
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);
        
        // 用户消息
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        
        input.add("messages", messages);
        requestBody.add("input", input);
        
        // 构建参数
        JsonObject parameters = new JsonObject();
        parameters.addProperty("result_format", "message");
        requestBody.add("parameters", parameters);
        
        // 创建HTTP请求
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();
        
        // 执行请求
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API请求失败: " + response.code());
            }
            
            String responseBody = response.body().string();
            return parseResponse(responseBody);
        }
    }
    
    /**
     * 解析API响应
     * @param jsonResponse JSON响应字符串
     * @return 解析后的回复内容
     */
    private String parseResponse(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            // 检查是否有错误
            if (json.has("error")) {
                String errorMsg = json.getAsJsonObject("error").get("message").getAsString();
                System.err.println("API错误: " + errorMsg);
                return "哎呀，出了点小问题~";
            }
            
            // 提取回复内容
            JsonObject output = json.getAsJsonObject("output");
            if (output != null && output.has("choices")) {
                JsonArray choices = output.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    JsonObject message = firstChoice.getAsJsonObject("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }
            
            return "嗯...我在思考中~";
        } catch (Exception e) {
            e.printStackTrace();
            return "让我再想想~";
        }
    }
}
