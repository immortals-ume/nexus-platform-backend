package com.immortals.notificationservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for high-throughput notification processing
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - minimum threads always alive
        executor.setCorePoolSize(10);
        
        // Max pool size - maximum threads under load
        executor.setMaxPoolSize(50);
        
        // Queue capacity - pending tasks before rejecting
        executor.setQueueCapacity(1000);
        
        // Thread name prefix for debugging
        executor.setThreadNamePrefix("notification-async-");
        
        // Graceful shutdown - wait for tasks to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Rejection policy - caller runs if queue is full (backpressure)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "bulkNotificationExecutor")
    public Executor bulkNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Larger pool for bulk operations
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("bulk-notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}
