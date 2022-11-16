package org.tei.botrunner.model;

import lombok.extern.slf4j.Slf4j;
import org.joor.Reflect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class BotConsumerThread extends Thread {
    private static RestTemplate REST_TEMPLATE;

    private BotPoolElement bot;

    private static final String RECEIVE_BOT_URL = "http://127.0.0.1:8080/play/receive/bot";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        REST_TEMPLATE = restTemplate;
    }

    public void startTimeout(long timeout, BotPoolElement bot) {
        this.bot = bot;
        // 开启线程
        this.start();

        try {
            // 等待线程执行timeout，之后中断线程
            // 将BotConsumerThread单独开辟线程，为了方便控制bot的执行时间
            this.join(timeout);  // 最多等待timeout秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();  // 终端当前线程
        }
    }

    private String addUid(String code, String uid) {  // 在code中的Bot类名后添加uid
        int k = code.indexOf(" implements org.tei.botrunner.model.BotInterface");
        return code.substring(0, k) + uid + code.substring(k);
    }

    @Override
    public void run() {
        UUID uuid = UUID.randomUUID();
        String uid = uuid.toString().substring(0, 8);
        String content = addUid(bot.getBotCode(), uid);
        String className = "org.tei.botrunner.model.Bot" + uid;

        BotInterface botInterface = Reflect.compile(className, content).create().get();
        Integer result = botInterface.nextMove(bot.getInput());
        log.info("userId:{}, bot run result: {}", bot.getUserId(), result);

        Map<String, String> request = new HashMap<>();
        request.put("userId", bot.getUserId().toString());
        request.put("direction", result.toString());
        REST_TEMPLATE.postForObject(RECEIVE_BOT_URL, request, String.class);
    }
}
