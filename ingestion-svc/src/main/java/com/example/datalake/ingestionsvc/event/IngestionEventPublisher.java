package com.example.datalake.ingestionsvc.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IngestionEventPublisher {
    private final KafkaTemplate<String, String> template;

    @Value("${ingestion.topic}")
    private String topic;

    public void publish(String filename, Path path) {
        Map<String, String> payload = Map.of(
                "filename", filename,
                "path", path.toString(),
                "ingestedAt", Instant.now().toString()
        );

        try {
            String json = JsonMapper.builder().build().writeValueAsString(payload);
            template.send(topic, filename, json);
        } catch (JsonProcessingException e) {
            // Add failed log
            throw new RuntimeException("Failed to serialize ingestion event", e);
        }
    }
}
