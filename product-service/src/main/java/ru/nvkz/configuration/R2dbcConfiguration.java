package ru.nvkz.configuration;

import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    @Override
    protected List<Object> getCustomConverters() {
        return List.of(mapToJsonConverter(), jsonToMapConverter());
    }

    @Bean
    public MapToJsonConverter mapToJsonConverter() {
        return new MapToJsonConverter(objectMapper);
    }

    @Bean
    public JsonToMapConverter jsonToMapConverter() {
        return new JsonToMapConverter(objectMapper);
    }

    @WritingConverter
    @RequiredArgsConstructor
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {
        private final ObjectMapper mapper;

        @Override
        public Json convert(Map<String, Object> source) {
            try {
                return Json.of(mapper.writeValueAsString(source));
            } catch (Exception e) {
                throw new RuntimeException("Error converting Map to JSON", e);
            }
        }
    }

    @ReadingConverter
    @RequiredArgsConstructor
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {
        private final ObjectMapper mapper;

        @Override
        public Map<String, Object> convert(Json source) {
            if (source == null) return Map.of();
            try {
                return mapper.readValue(source.asString(), new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                throw new RuntimeException("Error converting JSON to Map", e);
            }
        }
    }
}

