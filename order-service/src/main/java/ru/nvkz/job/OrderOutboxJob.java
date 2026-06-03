package ru.nvkz.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import ru.nvkz.job.decorator.CompositeTaskDecorator;
import ru.nvkz.job.decorator.JobLoggingDecorator;
import ru.nvkz.job.decorator.JobTimingDecorator;
import ru.nvkz.service.OutboxService;

import javax.sql.DataSource;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class OrderOutboxJob {

    private final OutboxService outboxService;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay}",
            scheduler = "orderOutboxTaskScheduler")
    @SchedulerLock(
            name = "OrderOutboxJob_processOutbox",
            lockAtMostFor = "PT5M", //сколько держим лок максимум
            lockAtLeastFor = "PT5S" //минимум удерживаем
    )
    public void processOutbox() {
        log.info("Outbox background processing task started");

        outboxService.processPendingEvents()
                .doOnError(ex -> log.error("Outbox Job fails: {}", ex.getMessage()))
                .subscribe();
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate shedlockJdbcTemplate) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(shedlockJdbcTemplate)
                        .build()
        );
    }

    @Bean
    public ThreadPoolTaskScheduler orderOutboxTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(2);

        scheduler.setThreadNamePrefix("order-outbox-scheduler-");

        TaskDecorator compositeDecorator = new CompositeTaskDecorator(
                List.of(
                        new JobLoggingDecorator(),
                        new JobTimingDecorator()
                ));

        scheduler.setTaskDecorator(compositeDecorator);

        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public DataSource shedlockDataSource(R2dbcProperties r2dbcProperties) {
        String jdbcUrl = r2dbcProperties.getUrl()
                .replace("r2dbc:", "jdbc:")
                .replace("pooled:", "");

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(r2dbcProperties.getUsername())
                .password(r2dbcProperties.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public JdbcTemplate shedlockJdbcTemplate(DataSource shedlockDataSource) {
        return new JdbcTemplate(shedlockDataSource);
    }
}
