package org.tei.matching.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class MatchingPoolThread extends Thread {
    private static final Lock LOCK = new ReentrantLock();

    private static List<MatchingPlayer> MATCHING_PLAYER_LIST = new ArrayList<>();

    private static RestTemplate REST_TEMPLATE;

    private final static String START_GAME_URL = "http://127.0.0.1:8080/play/start/game";


    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        MatchingPoolThread.REST_TEMPLATE = restTemplate;
    }

    public void addPlayer(Integer userId, Integer score, Integer botId) {
        LOCK.lock();
        try {
            MATCHING_PLAYER_LIST.add(new MatchingPlayer(userId, score, 0, botId));
        } finally {
            LOCK.unlock();
        }
    }

    public void removePlayer(Integer userId) {
        LOCK.lock();
        try {
            List<MatchingPlayer> newPlayers = new ArrayList<>();
            for (MatchingPlayer player : MATCHING_PLAYER_LIST) {
                if (!player.getUserId().equals(userId)) {
                    newPlayers.add(player);
                }
            }
            MATCHING_PLAYER_LIST = newPlayers;
        } finally {
            LOCK.unlock();
        }
    }

    private void increaseWaitingTime() {
        for (MatchingPlayer matchingPlayer : MATCHING_PLAYER_LIST) {
            matchingPlayer.setWaitingTime(matchingPlayer.getWaitingTime() + 1);
        }
    }

    private void matchPlayers() {
        if (MATCHING_PLAYER_LIST.size() < 2) {
            return;
        }
        log.info("Matching players... {}", MATCHING_PLAYER_LIST);

        boolean[] matched = new boolean[MATCHING_PLAYER_LIST.size()];
        for (int i = 0; i < MATCHING_PLAYER_LIST.size(); i++) {
            if (matched[i]) {
                continue;
            }
            for (int j = i + 1; j < MATCHING_PLAYER_LIST.size(); j++) {
                if (matched[j]) {
                    continue;
                }
                MatchingPlayer player1 = MATCHING_PLAYER_LIST.get(i);
                MatchingPlayer player2 = MATCHING_PLAYER_LIST.get(j);
                if (isMatch(player1, player2)) {
                    matched[i] = true;
                    matched[j] = true;
                    log.info("matched: player1 {} player2 {}", player1.getUserId(), player2.getUserId());
                    sendMatchResult(player1, player2);
                    break;
                }
            }
        }

        List<MatchingPlayer> newPlayers = new ArrayList<>();
        for (int i = 0; i < MATCHING_PLAYER_LIST.size(); i++) {
            if (!matched[i]) {
                newPlayers.add(MATCHING_PLAYER_LIST.get(i));
            }
        }
        MATCHING_PLAYER_LIST = newPlayers;
    }

    private boolean isMatch(MatchingPlayer player1, MatchingPlayer player2) {
        Integer score1 = player1.getScore();
        Integer score2 = player2.getScore();
        int scoreDelta = Math.abs(score1 - score2);
        int minWaitingTime = Math.min(player1.getWaitingTime(), player2.getWaitingTime());
        return scoreDelta <= minWaitingTime * 10;
    }

    private void sendMatchResult(MatchingPlayer player1, MatchingPlayer player2) {
        log.info("send match result: " + player1 + " " + player2);
        Map<String,String> request = new HashMap<>();
        request.put("aId", player1.getUserId().toString());
        request.put("bId", player2.getUserId().toString());
        request.put("aBotId", player1.getBotId().toString());
        request.put("bBotId", player2.getBotId().toString());
        REST_TEMPLATE.postForObject(START_GAME_URL, request, String.class);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("MatchingPoolThread sleep error: {}", e.getMessage());
                break;
            }
            LOCK.lock();
            try {
                increaseWaitingTime();
                matchPlayers();
            } finally {
                LOCK.unlock();
            }
        }
    }
}

