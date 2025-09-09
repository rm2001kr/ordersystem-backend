package com.beyond.ordersystem.common.service;


import com.beyond.ordersystem.common.dto.SseMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SseAlarmService implements MessageListener {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final RedisTemplate<String, String> redisTemplate;


//    SseEmitter 연결된 사용자 정보(ip, mac, address 등...)를 의미한다.
    private Map<String, SseEmitter> emitterMap = new HashMap();

    public SseAlarmService(SseEmitterRegistry sseEmitterRegistry, RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
    }

    //    특정 사용자에게 message 발송
    public void publishMessage(String receiver, String sender, Long orderingId){
        SseMessageDto dto = SseMessageDto.builder()
                .sender(sender)
                .receiver(receiver)
                .orderingId(orderingId)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String data = null;
        try{
            data = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

//        emitter 객체를 통해 메세지 전송
        SseEmitter sseEmitter = emitterMap.get(receiver);
//        emitter 객체가 현재 서버에 있으면, 직접 알림 발송, 그렇지 않으면 redis에 publish
        if (sseEmitter != null)
        try{
            sseEmitter.send(SseEmitter.event().name("ordered").data(data));
//            사용자가 로그아웃(새로고침) 후에 다시 화면에 들어왔을 떄 알림메세지가 남아있으려면 DB에 주기적으로 저장 필요
        } catch (IOException e) {
            e.printStackTrace();
        } else {
            redisTemplate.convertAndSend("order-channel", data);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        Message : 실직적인 메시지가 담겨있는 객체
//        pattern : 채널명
        String channel_name = new String(pattern);
        ObjectMapper objectMapper = new ObjectMapper(); // 여러개의 채널명 분기처리
        try{
            SseMessageDto dto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(dto.getReceiver());
            if (sseEmitter != null)
                try{
                    sseEmitter.send(SseEmitter.event().name("ordered").data(dto));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public void removeEmitter(String email) {
        emitterMap.remove(email);
    }

    public void addSseEmitter(String email, SseEmitter sseEmitter){
        emitterMap.put(email, sseEmitter);
        System.out.println(emitterMap);
    }
}
