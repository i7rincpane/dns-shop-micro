package ru.nvkz.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class IdempotencyCacheTest {

    @Test
    void firstElementMustBeDeletedFromConstraintCapacity() {
        final int capacity = 2;
        IdempotencyCache cache = new IdempotencyCache(capacity);
        UUID id = UUID.randomUUID();

        cache.put(id);
        cache.put(UUID.randomUUID());
        assertThat(cache.contains(id)).isTrue();

        cache.put(UUID.randomUUID());
        assertThat(cache.contains(id))
                .as("Первый элемент должен быть удален из-за ограничения capacity")
                .isFalse();
        assertThat(cache.size()).isEqualTo(capacity);
    }

    /*
    Без синхронизации в LinkedHashMap будет состояние гонки (race condition)
    Expected :1000
    Actual   :1007
    или ConcurrentModificationException
     */
    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        final  int capacity = 1000;
        final int countOperations = 100;
        final int countThreads = 50;
        IdempotencyCache cache = new IdempotencyCache(capacity);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(countThreads);

        try (ExecutorService executorsService = Executors.newFixedThreadPool(countThreads)) {

            for (int i = 0; i < countThreads; i++) {

                executorsService.execute(() -> {
                    try {
                        start.await();

                        for (int j = 0; j < countOperations; j++) {
                            UUID uuid = UUID.randomUUID();
                            cache.put(uuid);
                            cache.contains(uuid);
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finish.countDown();
                    }

                });


            }

            start.countDown();
            boolean finished =finish.await(5, TimeUnit.SECONDS);
            assertThat(finished).as("Потоки не завершились вовремя").isTrue();
            assertThat(cache.size()).isEqualTo(capacity);
        }
    }

}