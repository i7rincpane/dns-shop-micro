package ru.nvkz.job.decorator;

import org.springframework.core.task.TaskDecorator;
import org.springframework.util.StopWatch;

import static reactor.netty.http.HttpConnectionLiveness.log;

public class JobTimingDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                runnable.run();
            } finally {
                stopWatch.stop();
                log.info("[DECORATOR] The time to execute the job was: {} ms", stopWatch.getTotalTimeMillis());
            }
        };
    }
}
