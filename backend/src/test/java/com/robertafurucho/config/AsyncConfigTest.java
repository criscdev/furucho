package com.robertafurucho.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AsyncConfig}.
 */
@DisplayName("AsyncConfig")
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    @DisplayName("creates whatsappExecutor with correct pool settings")
    void executorSettings() {
        Executor executor = config.whatsappExecutor();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaxPoolSize()).isEqualTo(5);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("whatsapp-");
    }

    @Test
    @DisplayName("provides an AsyncUncaughtExceptionHandler")
    void uncaughtExceptionHandler() {
        assertThat(config.getAsyncUncaughtExceptionHandler()).isNotNull();
    }
}
