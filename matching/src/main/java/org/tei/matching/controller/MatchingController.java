package org.tei.matching.controller;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tei.matching.model.MatchingPoolThread;

import java.util.Map;

@RestController
@Slf4j
public class MatchingController {
    public static final MatchingPoolThread MATCHING_POOL_THREAD = new MatchingPoolThread();

    // !!!接收@RequestBody的json数据，必须是Map,不能是MultiValueMap，否则会报415错误!!!
    @PostMapping("/player/add")
    public String addPlayer(@RequestBody Map<String, String> request) {
        Integer userId = NumberUtil.parseInt(request.get("userId"));
        Integer score = NumberUtil.parseInt(request.get("score"));
        Integer botId = NumberUtil.parseInt(request.get("botId"));
        log.info("add player: userId={}, score={}, botId={}", userId, score, botId);
        MATCHING_POOL_THREAD.addPlayer(userId, score,botId);
        return "add success userId:" + userId + " score:" + score;
    }

    @PostMapping("/player/remove/{userId}")
    public String removePlayer(@PathVariable("userId") Integer userId) {
        log.info("remove player: userId={}", userId);
        MATCHING_POOL_THREAD.removePlayer(userId);
        return "remove success userId: " + userId;
    }
}
