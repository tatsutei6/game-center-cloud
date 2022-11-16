package org.tei.botrunner.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tei.botrunner.model.BotPoolThread;

import java.util.Map;

@RestController
@Slf4j
public class BotRunnerController {
    public static final BotPoolThread BOT_POOL_THREAD = new BotPoolThread();
    @PostMapping("/bot/run")
    public String runBotCode(@RequestBody Map<String, String> request) {
        // get userId, input, botCode
        String botCode = request.get("botCode");
        String input = request.get("input");
        String userId = request.get("userId");
        log.info("userId:{}, input:{}", userId, input);
        BOT_POOL_THREAD.addBot(Integer.valueOf(userId), botCode, input);
        return "Greetings from Spring Boot!";
    }
}
