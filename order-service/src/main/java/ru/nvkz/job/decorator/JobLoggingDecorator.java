package ru.nvkz.job.decorator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;

@Slf4j
public class JobLoggingDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            String threadName = Thread.currentThread().getName();
            log.info("[DECORATOR] Thread {} STARTS executing a background task", threadName);
            try {
                runnable.run();
            } finally {
                log.info("[DECORATOR] Thread {} COMPLETED the background task", threadName);
            }
        };
    }
}
