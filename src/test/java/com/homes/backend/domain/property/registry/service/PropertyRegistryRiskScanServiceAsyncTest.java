package com.homes.backend.domain.property.registry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.homes.backend.domain.property.event.PropertySavedEvent;
import com.homes.backend.domain.property.registry.config.PropertyRegistryRiskScanAsyncConfig;
import com.homes.backend.domain.property.registry.repository.PropertyRegistryRiskRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class PropertyRegistryRiskScanServiceAsyncTest {

    @Test
    void handlesPropertySavedOnDedicatedExecutor() throws InterruptedException {
        PropertyRepository propertyRepository = mock(PropertyRepository.class);
        PropertyRegistryRiskRepository registryRiskRepository = mock(PropertyRegistryRiskRepository.class);
        RegistryRiskScanner registryRiskScanner = mock(RegistryRiskScanner.class);
        CountDownLatch repositoryCallStarted = new CountDownLatch(1);
        CountDownLatch releaseRepositoryCall = new CountDownLatch(1);
        AtomicReference<String> workerThreadName = new AtomicReference<>();

        when(propertyRepository.findById(1L)).thenAnswer(invocation -> {
            workerThreadName.set(Thread.currentThread().getName());
            repositoryCallStarted.countDown();
            releaseRepositoryCall.await(5, TimeUnit.SECONDS);
            return Optional.empty();
        });

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(PropertyRegistryRiskScanAsyncConfig.class);
            context.registerBean(PropertyRegistryRiskScanService.class,
                    () -> new PropertyRegistryRiskScanService(
                            propertyRepository, registryRiskRepository, registryRiskScanner));
            context.refresh();

            PropertyRegistryRiskScanService service = context.getBean(PropertyRegistryRiskScanService.class);
            service.handlePropertySaved(new PropertySavedEvent(1L));

            assertThat(repositoryCallStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(workerThreadName.get()).startsWith("property-registry-risk-scan-");
            assertThat(releaseRepositoryCall.getCount()).isEqualTo(1);
            releaseRepositoryCall.countDown();
        } finally {
            releaseRepositoryCall.countDown();
        }
    }
}
