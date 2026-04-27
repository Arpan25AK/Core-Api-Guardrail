package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardrailService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AppConfig appConfig;

    private static final String BOT_COUNT_KEY = "post:%d:bot_count";
    private static final String COOLDOWN_KEY = "cooldown:bot_%d:human_%d";

    public void enforceBotGuardrails(Long postId,Long botId, Long humanId, int depthLevel){
        checkVerticalCap(depthLevel);
        checkCooldown(botId, humanId);
        checkHorizontalCap(postId);
    }

    public void enforceHorizontalAndVerticalCap(Long postId, int depthLevel){
        checkHorizontalCap(postId);
        checkVerticalCap(depthLevel);
    }

    public void checkHorizontalCap(Long postId){
        String key = String.format(BOT_COUNT_KEY,postId);
        Long count = redisTemplate.opsForValue().increment(key);

        if(count > appConfig.getHorizontalCap()){
            redisTemplate.opsForValue().decrement(key);
            log.warn("cap exceeded for post {}",postId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "bot reply limit reached for this post {} " + postId);
        }

        log.debug("horizontal cap passed for the post {} with count {}",postId,count);
    }

    public void checkVerticalCap(int depthLevel){
        if(depthLevel > appConfig.getVerticalCap()){
            log.warn("vertical cap reached : depthLevel {}", depthLevel);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "comment depth too deep : max depth is " + appConfig.getVerticalCap());
        }

        log.debug("current depth level is {}", depthLevel);
    }

    public void checkCooldown(Long botId, Long humanId){
        String key = String.format(COOLDOWN_KEY, botId, humanId);
        Boolean exists = redisTemplate.hasKey(key);

        if(Boolean.TRUE.equals(exists)){
            log.warn("cooldown exists for bot {}", botId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "bot is on cooldown for this user");
        }

        redisTemplate.opsForValue().set(key,"1",appConfig.getBotCooldownTtl(), TimeUnit.SECONDS);
        log.debug("cooldown set for bot {} -> human {}" , botId, humanId);

    }
}
