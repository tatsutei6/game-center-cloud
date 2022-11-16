package org.tei.matching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tei.matching.controller.MatchingController;

@SpringBootApplication
public class MatchingSystemApplication {
    public static void main(String[] args) {
        // 启动匹配线程
        MatchingController.MATCHING_POOL_THREAD.start();
        SpringApplication.run(MatchingSystemApplication.class, args);
    }
}