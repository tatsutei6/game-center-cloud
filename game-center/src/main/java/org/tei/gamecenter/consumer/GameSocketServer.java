package org.tei.gamecenter.consumer;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.tei.gamecenter.consumer.model.GameThread;
import org.tei.gamecenter.play.mapper.BotMapper;
import org.tei.gamecenter.play.mapper.GameRecordMapper;
import org.tei.gamecenter.play.mapper.UserMapper;
import org.tei.gamecenter.play.pojo.Bot;
import org.tei.gamecenter.play.pojo.User;
import org.tei.gamecenter.play.utils.JwtUtil;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.tei.gamecenter.play.utils.Constants.*;

// 客户端每新建立一个socket连接，就会创建一个新的WebSocket对象
@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
@Slf4j
public class GameSocketServer {
    public static final Map<Integer, GameSocketServer> USER_SOCKET_MAP = new ConcurrentHashMap<>();

    // 在spring boot的websocket中，无法为实例属性直接注入bean，只能将要注入的属性设置为static
    public static UserMapper USER_MAPPER;

    public static GameRecordMapper GAME_RECORD_MAPPER;

    public static BotMapper BOT_MAPPER;

    public static RestTemplate REST_TEMPLATE;

    private Session session;

    private User user;

    private GameThread game;

    public GameThread getGame() {
        return game;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        GameSocketServer.REST_TEMPLATE = restTemplate;
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        GameSocketServer.USER_MAPPER = userMapper;
    }

    @Autowired
    public void setRecordMapper(GameRecordMapper gameRecordMapper) {
        GameSocketServer.GAME_RECORD_MAPPER = gameRecordMapper;
    }

    @Autowired
    public void setBotMapper(BotMapper botMapper) {
        GameSocketServer.BOT_MAPPER = botMapper;
    }


    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        log.info("GameSocketServer HashCode: {}", this.hashCode());
        log.info("GameSocketServer Session HashCode: {}", session.hashCode());
        // 建立连接
        this.session = session;
        log.info("connected!!!");
        Integer userId = JwtUtil.getUserIdFromJwt(token);
        user = USER_MAPPER.selectById(userId);
        log.info("user = {}", user);
        if (user != null) {
            USER_SOCKET_MAP.put(userId, this);
        } else {
            log.info("userid {} not found", userId);
            session.close();
        }
    }

    @OnClose
    public void onClose() {
        // 关闭链接
        log.info("disconnected!!!");
        if (user != null) {
            USER_SOCKET_MAP.remove(user.getId());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 从Client接收消息
        JSONObject jsonObject = JSONObject.parseObject(message);
        String event = jsonObject.getString("event");
        String botId = jsonObject.getString("botId");

        if (Objects.equals(event, START_MATCHING_EVENT)) {
            // 开始匹配
            log.info("start matching!!!");
            onStartMatching(botId);
        }

        if (Objects.equals(event, STOP_MATCHING_EVENT)) {
            // 停止匹配
            log.info("stop matching!!!");
            onStopMatching();
        }

        if (Objects.equals(event, MOVE_EVENT)) {
            // 移动
            log.info("move!!!");
            onMove(jsonObject.getInteger("direction"));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    /**
     * 向客户端发送消息
     *
     * @param message
     * @throws Exception
     */
    public synchronized void sendMessage(String message) throws Exception {
        log.info("send message {} to client {}", message, user.getId());
        session.getBasicRemote().sendText(message);
    }

    /**
     * 接收来自客户端的移动输入
     *
     * @param direction
     */
    private void onMove(int direction) {
        log.info("user {} onMove direction = {}", user.getId(), direction);
        // 判断是由哪一个player发出的指令
        if (game.getPlayerA().getId().equals(user.getId())) {
            // 如果为亲自出马
            if (game.getPlayerA().getBotId() == -1) {
                game.setNextStepA(direction);
            }
        } else if (game.getPlayerB().getId().equals(user.getId())) {
            // 如果为亲自出马
            if (game.getPlayerB().getBotId() == -1) {
                game.setNextStepB(direction);
            }
        } else {
            log.warn("user {} is not in game", user.getId());
        }
    }

    public static void startGame(Integer aId, Integer bId, Integer aBotId, Integer bBotId) {
        log.info("start game aId = {}, bId = {}, aBotId = {}, bBotId = {}", aId, bId, aBotId, bBotId);
        User userA = USER_MAPPER.selectById(aId);
        User userB = USER_MAPPER.selectById(bId);
        Bot botA = BOT_MAPPER.selectById(aBotId);
        Bot botB = BOT_MAPPER.selectById(bBotId);
        GameThread _game = new GameThread(userA.getId(), userB.getId(), botA, botB);
        JSONObject gameInfoResponse = new JSONObject();

        GameSocketServer userAGameSocketServer = USER_SOCKET_MAP.get(userA.getId());
        if (userAGameSocketServer != null)
            userAGameSocketServer.game = _game;
        else {
            log.info("user {} gameSocketServer not found", userA.getId());
            // TODO 通知用户匹配失败
            return;
        }
        GameSocketServer userBGameSocketServer = USER_SOCKET_MAP.get(userB.getId());
        if (userBGameSocketServer != null)
            userBGameSocketServer.game = _game;
        else {
            // TODO 通知用户匹配失败
            log.info("user {} gameSocketServer not found", userB.getId());
            return;
        }

        int[][] gameMapArray = _game.generateGameMapArray();

        // 开启线程
        _game.start();
        gameInfoResponse.put("aId", userA.getId());
        gameInfoResponse.put("aName", userA.getUsername());
        gameInfoResponse.put("aRow", _game.getPlayerA().getRow());
        gameInfoResponse.put("aColumn", _game.getPlayerA().getColumn());
        gameInfoResponse.put("aPhoto", userA.getAvatarUrl());

        gameInfoResponse.put("bId", userB.getId());
        gameInfoResponse.put("bName", userB.getUsername());
        gameInfoResponse.put("bRow", _game.getPlayerB().getRow());
        gameInfoResponse.put("bColumn", _game.getPlayerB().getColumn());
        gameInfoResponse.put("bPhoto", userB.getAvatarUrl());

        gameInfoResponse.put("gameMapArray", gameMapArray);

        JSONObject userAResponse = new JSONObject();
        JSONObject userBResponse = new JSONObject();
        userAResponse.put("event", MATCH_SUCCESS);

        userAResponse.put("opponentName", userB.getUsername());
        userAResponse.put("opponentPhoto", userB.getAvatarUrl());
        userAResponse.put("gameInfo", gameInfoResponse);


        userBResponse.put("event", MATCH_SUCCESS);
        userBResponse.put("opponentName", userA.getUsername());
        userBResponse.put("opponentPhoto", userA.getAvatarUrl());
        userBResponse.put("gameInfo", gameInfoResponse);

        // 通知双方匹配成功
        try {
            userAGameSocketServer.sendMessage(userAResponse.toJSONString());
            userBGameSocketServer.sendMessage(userBResponse.toJSONString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onStartMatching(String botId) {
        Map<String, String> request = new HashMap<>();
        request.put("userId", user.getId().toString());
        request.put("score", user.getScore().toString());
        request.put("botId", botId);
        REST_TEMPLATE.postForObject(ADD_PLAYER_URL, request, String.class);
    }

    private void onStopMatching() {
        log.info("onStopMatching!!!");
        // 通知匹配服务器，删除该用户
        REST_TEMPLATE.postForObject(REMOVE_PLAYER_URL + user.getId(), null, String.class);
    }
}
