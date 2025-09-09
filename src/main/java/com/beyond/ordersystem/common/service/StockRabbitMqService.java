package com.beyond.ordersystem.common.service;

import com.beyond.ordersystem.common.dto.StockRabbitMqDto;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockRabbitMqService {
    private final RabbitTemplate rabbitTemplate;
    private final ProductRepository productRepository;

//    rabbitmq에 메세지 발행
    public void publish(Long productId, int productCount){
        StockRabbitMqDto stockRabbitMqDto = StockRabbitMqDto.builder()
                .productId(productId)
                .productCount(productCount)
                .build();
        rabbitTemplate.convertAndSend("stockDecreaseQueue", stockRabbitMqDto);
    }

//    rabbitmq에 발행된 메세지를 수신
//    listener는 단일스레드로 메세지를 처리하므로, 동시성 이슈 발생X

    @RabbitListener(queues = "stockDecreaseQueue")
    @Transactional
        public void subscribe(Message message) throws JsonProcessingException {
        String messageBody = new String(message.getBody());
        ObjectMapper objectMapper = new ObjectMapper();
        StockRabbitMqDto dto = objectMapper.readValue(messageBody, StockRabbitMqDto.class);
        Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("오류"));
        product.updateStockQuantity(dto.getProductCount());
    }
}
