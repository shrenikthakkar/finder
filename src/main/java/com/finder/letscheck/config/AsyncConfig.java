package com.finder.letscheck.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Central async executor configuration.
 *
 * Why this exists:
 * - avoids relying on Spring's default async executor
 * - gives controlled concurrency for background tasks
 * - prevents unbounded async behavior during traffic spikes
 *
 * Important:
 * - this pool is for background async tasks only
 * - it does not replace Tomcat request thread tuning
 * - it does not guarantee capacity by itself
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Main async executor for application background tasks.
     *
     * Sizing rationale for current launch stage:
     * - app server target: ~2 cores, ~2 GB RAM
     * - background tasks today are relatively lightweight
     * - queue is bounded to avoid runaway memory growth
     */
    @Bean(name = "appTaskExecutor")
    public Executor appTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Threads kept ready for normal async traffic
        executor.setCorePoolSize(4);

        // Max burst threads for temporary spikes
        executor.setMaxPoolSize(8);

        // Bounded queue to avoid unbounded memory usage
        executor.setQueueCapacity(500);

        // Thread naming helps debugging in logs and thread dumps
        executor.setThreadNamePrefix("finder-async-");

        // Graceful shutdown behavior
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Idle thread keep-alive
        executor.setKeepAliveSeconds(60);

        // Helps preserve useful logging context in async execution
        executor.setTaskDecorator(new LoggingContextTaskDecorator());

        executor.initialize();
        return executor;
    }

    /**
     * Minimal task decorator.
     *
     * Useful later if you want MDC / request tracing propagation.
     * For now it simply wraps execution safely.
     */
    static class LoggingContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            return () -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    log.error("Unhandled async task error", ex);
                    throw ex;
                }
            };
        }
    }
}