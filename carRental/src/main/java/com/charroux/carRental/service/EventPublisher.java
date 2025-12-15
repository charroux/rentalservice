package com.charroux.carRental.service;

import com.charroux.carRental.config.KafkaProducerConfig;
import com.charroux.carRental.event.CarRentedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing events to Kafka
 */
@Service
public class EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publish a CarRentedEvent to Kafka
     * @param event The event to publish
     */
    public void publishCarRentedEvent(CarRentedEvent event) {
        logger.info("Publishing CarRentedEvent to Kafka: {}", event);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaProducerConfig.CAR_RENTED_TOPIC, event.getPlateNumber(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Successfully sent CarRentedEvent with key={} to topic={}, partition={}, offset={}",
                        event.getPlateNumber(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to send CarRentedEvent with key={}: {}", 
                        event.getPlateNumber(), ex.getMessage(), ex);
            }
        });
    }
}
