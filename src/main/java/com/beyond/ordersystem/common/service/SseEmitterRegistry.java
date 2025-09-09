package com.beyond.ordersystem.common.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Component
public class SseEmitterRegistry {
//    SseEmitter는 연결된 사용자정보(ip, mac address 정보 등...)을 의미
//    ConcurrentHashMap은 Thread-Safe한 map (동시성 이슈 x)
    private Map<String, SseEmitter> emitterMap = new HashMap<>();

    public void addSseEmitter(String email, SseEmitter sseEmitter) {
        emitterMap.put(email, sseEmitter);
    }

    public void removeEmitter(String email) {
        emitterMap.remove(email);
    }

    public SseEmitter getEmitter(String email){
       return emitterMap.get(email);
    }
}
