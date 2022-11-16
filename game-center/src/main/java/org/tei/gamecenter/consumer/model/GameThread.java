package org.tei.gamecenter.consumer.model;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.tei.gamecenter.consumer.GameSocketServer;
import org.tei.gamecenter.play.pojo.Bot;
import org.tei.gamecenter.play.pojo.GameRecord;
import org.tei.gamecenter.play.pojo.User;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.tei.gamecenter.play.utils.Constants.*;

@Slf4j
public class GameThread extends Thread {

    private static final int ROWS = 13;
    private static final int COLUMNS = 14;
    private final int innerBarrierCount = 16;
    private final Player playerA;
    private final Player playerB;
    private final Lock lock = new ReentrantLock();
    private Integer nextStepA = null;
    private Integer nextStepB = null;
    public final static int[] D_ROWS = {-1, 0, 1, 0};
    public final static int[] D_COLUMNS = {0, 1, 0, -1};
    private String status = PLAYING_STATUS;  // playing -> finished
    private String loser = "";  // all: 平局，A: A输，B: B输
    private final int[][] g;

    private boolean isFirstRound = true;

    public GameThread(Integer playerAId, Integer playerBId, Bot botA, Bot botB) {

        Integer botAId = -1;
        String botACode = "";
        Integer botBId = -1;
        String botBCode = "";
        if (botA != null) {
            botAId = botA.getId();
            botACode = botA.getCode();
        }
        if (botB != null) {
            botBId = botB.getId();
            botBCode = botB.getCode();
        }
        playerA = new Player(playerAId, botAId, botACode, ROWS - 2, 1, new ArrayList<>());
        playerB = new Player(playerBId, botBId, botBCode, 1, COLUMNS - 2, new ArrayList<>());
        g = new int[ROWS][COLUMNS];
    }

    private String getInput(Player player) {
        Player me, rival;
        if (playerA.getId().equals(player.getId())) {
            me = playerA;
            rival = playerB;
        } else {
            me = playerB;
            rival = playerA;
        }

        return getMapString() + "#" +
                me.getRow() + "#" +
                me.getColumn() + "#(" +
                me.getStepsString() + ")#" +
                rival.getRow() + "#" +
                rival.getColumn() + "#(" +
                rival.getStepsString() + ")";

    }

    private void sendToBotRunner(Player player) {
        // 亲自出马，不需要人操作
        if (player.getBotId() == -1) {
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("userId", player.getId().toString());
        request.put("botCode", player.getBotCode());
        request.put("input", getInput(player));
        GameSocketServer.REST_TEMPLATE.postForObject(BOT_RUNNER_URL, request, String.class);
    }

    public int[][] generateGameMapArray() {
        // 生成墙和障碍物的重试次数
        int retryCount = 100;
        // 生成墙，Wall对象加入到GAME_OBJECTS中，是在GameMapObject后加入的
        // 所以Wall的渲染会覆盖掉GameMapObject的渲染
        for (int i = 0; i < retryCount; i++) {
            if (createWallsAndBarriers(g)) {
                break;
            }
            log.info("重新生成墙,障碍物!!!");
        }
        return g;
    }

    private boolean createWallsAndBarriers(int[][] g) {
        // 初始化g
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLUMNS; c++) {
                // 边界位置
                if (isBorder(r, c)) {
                    g[r][c] = 1;
                } else {
                    g[r][c] = 0;
                }
            }
        }
        // 随机生成障碍物的重试次数
        int retryCount = ROWS * COLUMNS;
        Random random = new Random();
        // 生成墙内障碍物所在的位置（随机生成）
        for (int i = 0; i < innerBarrierCount / 2; i++) {
            for (int j = 0; j < retryCount; j++) {
                // 0～rows-1
                int r = random.nextInt(ROWS);
                // 0～columns-1
                int c = random.nextInt(COLUMNS);
                if (!isValidInnerBarrierPosition(g, r, c) ||
                        !isValidInnerBarrierPosition(g, c, r)) {
                    continue;
                }
                // 横线对称（中心对称），地图为长方形可用
                g[r][c] = 1;
                g[ROWS - 1 - r][COLUMNS - 1 - c] = 1;
                break;
            }
        }

        // 为了避免在isConnective中改变g的状态，生成g的副本
        int[][] copyG = new int[ROWS][COLUMNS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(g[r], 0, copyG[r], 0, COLUMNS);
        }

        if (!isConnective(copyG, ROWS - 2, 1, 1, COLUMNS - 2)) {
            return false;
        }

        return true;
    }

    /**
     * 判断两条蛇的初始位置是否连通
     * player1的初始位置为左下，player2的初始位置为右上
     *
     * @param g
     * @param row1
     * @param column1
     * @param row2
     * @param column2
     * @returns {boolean}
     */
    private boolean isConnective(int[][] g, int row1, int column1, int row2, int column2) {
        if (row1 == row2 && column1 == column2) {
            return true;
        }
        g[row1][column1] = 1;
        // 上下左右四个方向
        int[] dRows = new int[]{0, 1, 0, -1};
        int[] dColumns = new int[]{1, 0, -1, 0};

        for (int i = 0; i < dRows.length; i++) {
            int x = row1 + dRows[i];
            int y = column1 + dColumns[i];
            if (x >= 0 && x < ROWS && y >= 0 && y < COLUMNS && g[x][y] == 0) {
                if (isConnective(g, x, y, row2, column2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断是否是合法的内部障碍物位置
     *
     * @param g 地图
     * @param r 行
     * @param c 列
     * @return true:合法 false:非法
     */
    private boolean isValidInnerBarrierPosition(int[][] g, int r, int c) {
        // 边界位置，超过了map的范围，或者该位置已经有障碍物了
        if (isBorder(r, c) || g[r][c] == 1) {
            return false;
        }
        // 两条蛇的初始位置（左下和右上）
        if ((r == ROWS - 2 && c == 1) || (r == 1 && c == COLUMNS - 2)) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否是边界位置
     *
     * @param r
     * @param c
     * @returns {boolean}
     */
    private boolean isBorder(int r, int c) {
        return r == 0 || r >= ROWS - 1 || c == 0 || c >= COLUMNS - 1;
    }

    /**
     * 判断两名玩家下一步操作是否合法
     */
    private void judge() {
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean gameOverA = isGameOver(cellsA, cellsB);
        boolean gameOverB = isGameOver(cellsB, cellsA);

        if (gameOverA && gameOverB) {
            status = FINISHED_STATUS;
            loser = DRAW;
            log.info("平局");
            return;
        }
        if (gameOverA) {
            status = FINISHED_STATUS;
            loser = A_LOSE;
            log.info("A败");
            return;
        }
        if (gameOverB) {
            status = FINISHED_STATUS;
            loser = B_LOSE;
            log.info("B败");
            return;
        }
    }

    /**
     * 接收两名玩家的下一步操作
     * 该函数只处理玩家的输入，不做任何撞墙之类的判断
     *
     * @return 如果有在时限内未输入的player，返回false，否则返回true
     */
    private boolean nextRound() {
        if (isFirstRound) {
            isFirstRound = false;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 是否需要等待玩家输入，还是由bot操作
        sendToBotRunner(playerA);
        sendToBotRunner(playerB);

        for (int i = 0; i < 25; i++) {
            try {
                Thread.sleep(300);
                lock.lock();
                try {
                    if (nextStepA != null && nextStepB != null) {
                        playerA.getSteps().add(nextStepA);
                        playerB.getSteps().add(nextStepB);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * 判断两名玩家的输入是否撞墙，或者赚到对方或本方的身体
     *
     * @param cellsA
     * @param cellsB
     * @return
     */
    private boolean isGameOver(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell headA = cellsA.get(n - 1);
        if (g[headA.row][headA.column] == 1) {
            return true;
        }
        // 判断是否撞到自己的身体
        for (int i = 0; i < n - 1; i++) {
            if (cellsA.get(i).equals(headA)) {
                return true;
            }
        }

        // 判断是否撞到对方的身体
        // 基于地图的设计，两条蛇的头是不会在同一个位置的
        for (int i = 0; i < n - 1; i++) {
            if (cellsB.get(i).equals(headA)) {
                return true;
            }
        }
        return false;
    }

    private void sendMoveActionToClient() {
        log.info("sendMoveActionToClient.aNextStep: {}", nextStepA);
        log.info("sendMoveActionToClient.bNextStep: {}", nextStepB);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("event", MOVE_EVENT);
        lock.lock();
        try {
            jsonResponse.put("aDirection", nextStepA);
            jsonResponse.put("bDirection", nextStepB);
            // 重置nextStepA和nextStepB
            nextStepA = null;
            nextStepB = null;
        } finally {
            lock.unlock();
        }
        sendMessageToClient(jsonResponse.toJSONString());
    }

    private void sendMessageToClient(String message) {
        try {
            if (GameSocketServer.USER_SOCKET_MAP.get(playerA.getId()) != null) {
                GameSocketServer.USER_SOCKET_MAP.get(playerA.getId()).sendMessage(message);
            } else {
                log.info("user {} not in the game.his gameSocketServer not found", playerA.getId());
            }
            if (GameSocketServer.USER_SOCKET_MAP.get(playerB.getId()) != null) {
                GameSocketServer.USER_SOCKET_MAP.get(playerB.getId()).sendMessage(message);
            } else {
                log.info("user {} not in the game.his gameSocketServer not found", playerB.getId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            if (nextRound()) {
                judge();
                if (Objects.equals(status, PLAYING_STATUS)) {
                    // 向两个client广播两名玩家的操作
                    sendMoveActionToClient();
                } else {
                    sendResultToClient();
                    break;
                }
            } else {
                // 16秒以内没有收到玩家的操作，游戏结束
                status = FINISHED_STATUS;
                lock.lock();
                try {
                    if (nextStepA == null && nextStepB == null) {
                        loser = DRAW;
                    } else if (nextStepA == null) {
                        loser = A_LOSE;
                    } else {
                        loser = B_LOSE;
                    }
                } finally {
                    lock.unlock();
                }
                sendResultToClient();
                break;
            }
        }
        log.info("GameThread is finished");
    }

    private void sendResultToClient() {
        JSONObject result = new JSONObject();
        result.put("event", RESULT_EVENT);
        result.put("loser", loser);
        saveGameResult();
        sendMessageToClient(result.toJSONString());
    }

    /**
     * 将游戏过程，信息保存到数据库
     */
    private void saveGameResult() {
        GameRecord record = new GameRecord();

        record.setAId(playerA.getId());
        record.setARow(playerA.getRow());
        record.setAColumn(playerA.getColumn());
        record.setBId(playerB.getId());
        record.setBRow(playerB.getRow());
        record.setBColumn(playerB.getColumn());
        record.setASteps(playerA.getStepsString());
        record.setBSteps(playerB.getStepsString());
        record.setLoser(loser);
        record.setCreateAt(new Date());
        record.setMap(getMapString());
        record.setABotId(playerA.getBotId());
        record.setBBotId(playerB.getBotId());
        GameSocketServer.GAME_RECORD_MAPPER.insert(record);

        // 更新player的score，draw 各减2，win加5，lose减2
        UpdateWrapper<User> updateWrapper1 = new UpdateWrapper<>();
        updateWrapper1.setSql("score=score-2");
        UpdateWrapper<User> updateWrapper2 = new UpdateWrapper<>();
        updateWrapper2.setSql("score=score+5");

        if (Objects.equals(loser, DRAW)) {
            updateWrapper1.in("id", playerA.getId(), playerB.getId());
            GameSocketServer.USER_MAPPER.update(null, updateWrapper1);
        }
        if (Objects.equals(loser, A_LOSE)) {
            // user1
            updateWrapper1.eq("id", playerA.getId());
            GameSocketServer.USER_MAPPER.update(null, updateWrapper1);
            // user2
            updateWrapper2.eq("id", playerB.getId());
            GameSocketServer.USER_MAPPER.update(null, updateWrapper2);
        }
        if (Objects.equals(loser, B_LOSE)) {
            // user2
            updateWrapper1.eq("id", playerB.getId());
            GameSocketServer.USER_MAPPER.update(null, updateWrapper1);
            // user1
            updateWrapper2.eq("id", playerA.getId());
            GameSocketServer.USER_MAPPER.update(null, updateWrapper2);
        }
    }

    private String getMapString() {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                res.append(g[i][j]);
            }
        }
        return res.toString();
    }


    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public Integer getNextStepA() {
        return nextStepA;
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public Integer getNextStepB() {
        return nextStepB;
    }

    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}