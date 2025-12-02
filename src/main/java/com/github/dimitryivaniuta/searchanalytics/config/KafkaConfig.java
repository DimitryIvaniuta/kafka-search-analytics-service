package com.github.dimitryivaniuta.searchanalytics.config;

import com.github.dimitryivaniuta.searchanalytics.model.SearchEventPayload;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Central Kafka configuration:
 *  - ConsumerFactory & ListenerContainerFactory for SearchEventPayload
 *  - ProducerFactory & KafkaTemplate for JSON messages
 *  - Uses manual acks and concurrency=1 so one consumer instance gets all partitions.
 */
@Configuration
@EnableConfigurationProperties({
        KafkaProperties.class,
        KafkaTopicsProperties.class
})
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * ConsumerFactory for SearchEventPayload.
     * Uses KafkaProperties to build the base config and then adjusts value deserializer type.
     */
    @Bean
    public ConsumerFactory<String, SearchEventPayload> searchEventConsumerFactory() {
        Map<String, Object> consumerProps = kafkaProperties.buildConsumerProperties();

        // Override value deserializer with a strongly-typed JsonDeserializer<SearchEventPayload>
        JsonDeserializer<SearchEventPayload> valueDeserializer =
                new JsonDeserializer<>(SearchEventPayload.class);
        valueDeserializer.addTrustedPackages("com.github.dimitryivaniuta.searchanalytics.model");

        return new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    /**
     * Listener container factory:
     *  - concurrency = 1   -> exactly one consumer instance per app instance
     *  - MANUAL ack mode   -> offsets committed only after successful processing
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SearchEventPayload> searchEventsKafkaListenerContainerFactory(
            ConsumerFactory<String, SearchEventPayload> searchEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, SearchEventPayload> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(searchEventConsumerFactory);

        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    /**
     * ProducerFactory for generic JSON STRING payloads (used by DLT / outbox publisher).
     * Uses spring.kafka.producer.* properties.
     */
    @Bean
    public ProducerFactory<String, String> jsonStringProducerFactory() {
        Map<String, Object> producerProps = kafkaProperties.buildProducerProperties();

        // Force plain String value serialization for this template
        producerProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(
                producerProps,
                new StringSerializer(),
                new StringSerializer()
        );
    }

    @Bean
    public KafkaTemplate<String, String> jsonStringKafkaTemplate(
            ProducerFactory<String, String> jsonStringProducerFactory
    ) {
        return new KafkaTemplate<>(jsonStringProducerFactory);
    }

    /**
     * Optional: KafkaTemplate for publishing SearchEventPayload as JSON.
     */
    @Bean
    public ProducerFactory<String, SearchEventPayload> searchEventProducerFactory() {
        Map<String, Object> producerProps = kafkaProperties.buildProducerProperties();

        // JsonSerializer for values
        producerProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(
                producerProps,
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, SearchEventPayload> searchEventKafkaTemplate(
            ProducerFactory<String, SearchEventPayload> searchEventProducerFactory
    ) {
        return new KafkaTemplate<>(searchEventProducerFactory);
    }
}
