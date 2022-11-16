package org.tei.gamecenter.play.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tei.gamecenter.consumer.GameSocketServer;
import org.tei.gamecenter.consumer.model.GameThread;

import java.util.Map;

@RestController
@Slf4j
public class GameController {

    @PostMapping("/play/start/game")
    public String startGame(@RequestBody Map<String, String> request) {
        Integer aId = Integer.parseInt(request.get("aId"));
        Integer bId = Integer.parseInt(request.get("bId"));
        Integer aBotId = Integer.parseInt(request.get("aBotId"));
        Integer bBotId = Integer.parseInt(request.get("bBotId"));

        log.info("start game: aId={}, bId={}, aBotId={}, bBotId={}", aId, bId, aBotId, bBotId);
        GameSocketServer.startGame(aId, bId, aBotId, bBotId);
        return "start game success";
    }

    @PostMapping("/play/receive/bot")
    public String receiveBotMove(@RequestBody Map<String, String> request) {
        Integer userId = Integer.parseInt(request.get("userId"));
        Integer direction = Integer.parseInt(request.get("direction"));
        if (GameSocketServer.USER_SOCKET_MAP.get(userId) != null) {
            GameThread game = GameSocketServer.USER_SOCKET_MAP.get(userId).getGame();
            if (game != null) {
                if (game.getPlayerA().getId().equals(userId)) {
                    log.info("playerA:{} receive bot move direction:{}", userId, direction);
                    game.setNextStepA(direction);
                } else if (game.getPlayerB().getId().equals(userId)) {
                    log.info("playerB:{} receive bot move direction:{}", userId, direction);
                    game.setNextStepB(direction);
                }
            }
        }
        log.info("user {} gameSocketServer not found direction:{}", userId, direction);
        return "receive bot move success";
    }
}
