package com.homes.backend.domain.property.registry.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class PropertyRegistryRiskScanAsyncConfig {

    public static final String EXECUTOR_NAME = "propertyRegistryRiskScanExecutor";

    @Bean(name = EXECUTOR_NAME)
    public Executor propertyRegistryRiskScanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("property-registry-risk-scan-");
        return executor;
    }
}
