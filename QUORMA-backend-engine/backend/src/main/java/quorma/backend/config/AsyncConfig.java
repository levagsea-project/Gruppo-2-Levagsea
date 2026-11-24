package quorma.backend.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Background work. Telemetry has its own small pool, so it can never take threads from the API. */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String TELEMETRY_EXECUTOR = "telemetryExecutor";

    private static final Logger LOG = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * One or two threads and a bounded queue. When the queue is full the series is dropped with a warning instead of
     * failing the caller (the default would throw into the request that just played the series).
     */
    @Bean(TELEMETRY_EXECUTOR)
    public Executor telemetryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("telemetry-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setRejectedExecutionHandler((task, pool) -> LOG.warn("telemetry queue is full: a series was not aggregated"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
