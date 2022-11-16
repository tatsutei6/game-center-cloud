package org.tei.botrunner.model;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Slf4j
public class BotPoolThread extends Thread {
    // lock
    private final static Lock LOCK = new ReentrantLock();

    private final static Condition CONDITION = LOCK.newCondition();

    private final Queue<BotPoolElement> botQueue = new LinkedList<>();

    public void addBot(Integer userId, String botCode, String input) {
        log.info("add bot userId:{}", userId);
        LOCK.lock();
        try {
            botQueue.add(new BotPoolElement(userId, botCode, input));
            CONDITION.signalAll();
        } finally {
            LOCK.unlock();
        }
    }


    @Override
    public void run() {
        while (true) {
            LOCK.lock();
            if (botQueue.isEmpty()) {
                try {
                    // await会释放相关联的锁
                    CONDITION.await();
                } catch (InterruptedException e) {
                    LOCK.unlock();
                    log.error(e.getMessage());
                    break;
                }
            } else {
                try {
                    BotPoolElement botPoolElement = botQueue.remove();
                    // 耗费时间的操作，在此之前释放锁
                    consume(botPoolElement);
                } finally {
                    LOCK.unlock();
                }
            }
        }
    }

    private void consume(BotPoolElement bot) {
        BotConsumerThread consumer = new BotConsumerThread();
        consumer.startTimeout(2000, bot);
    }
}
