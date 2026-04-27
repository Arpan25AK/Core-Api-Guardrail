package com.assignment.Core_Api_Guardrails.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViralityService {

    private final RedisTemplate<String ,String> redisTemplate;

    private static final String VIRALITY_KEY = "post:%d:virality_score";

    public void addBotReply(Long postId){
        String key = String.format(VIRALITY_KEY,postId);
        redisTemplate.opsForValue().increment(key,1);
    }

    public void addHumanLike(Long postId){
        String key = String.format(VIRALITY_KEY,postId);
        redisTemplate.opsForValue().increment(key,20);
    }

    public void addHumanComment(Long postId){
        String key = String.format(VIRALITY_KEY, postId);
        redisTemplate.opsForValue().increment(key,50);
    }

    public Long getViralityScore(Long postId){
        String key = String.format(VIRALITY_KEY, postId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }
}
