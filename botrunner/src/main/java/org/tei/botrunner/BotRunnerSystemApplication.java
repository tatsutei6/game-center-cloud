package org.tei.botrunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tei.botrunner.controller.BotRunnerController;

@SpringBootApplication
public class BotRunnerSystemApplication {
    public static void main(String[] args) {
        // 启动Bot Runner线程
        BotRunnerController.BOT_POOL_THREAD.start();
        // Run the application
        SpringApplication.run(BotRunnerSystemApplication.class, args);
    }
}
