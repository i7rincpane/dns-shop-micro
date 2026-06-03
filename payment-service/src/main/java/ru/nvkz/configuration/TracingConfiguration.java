package ru.nvkz.configuration;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.nvkz.common.ReactiveTraceExecutor;

@Configuration
public class TracingConfiguration {
    @Bean
    public ReactiveTraceExecutor reactiveTraceExecutor(
            Tracer tracer,
            Propagator propagator,
            ObservationRegistry observationRegistry) {
        return new ReactiveTraceExecutor(tracer, propagator, observationRegistry);
    }
}
