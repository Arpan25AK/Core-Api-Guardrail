package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

    private static final String HORIZONTAL_CAP_SCRIPT =
            "local count = redis.call('INCR', KEYS[1]) " +
                    "if count > tonumber(ARGV[1]) then " +
                    "  redis.call('DECR', KEYS[1]) " +
                    "  return -1 " +
                    "end " +
                    "return count";

    public void checkHorizontalCap(Long postId){
        String key = String.format(BOT_COUNT_KEY, postId);

        RedisScript<Long> script = RedisScript.of(HORIZONTAL_CAP_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, List.of(key), String.valueOf(appConfig.getHorizontalCap()));

        if (result == null || result == -1L) {
            log.warn("Horizontal cap exceeded for post {}", postId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bot reply limit reached for post " + postId);
        }

        log.debug("Horizontal cap passed for post {} with count {}", postId, result);
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

        Boolean set = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", appConfig.getBotCooldownTtl(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(set)) {
            log.warn("Cooldown active for bot {} -> human {}", botId, humanId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bot is on cooldown for this user");
        }

        log.debug("Cooldown set for bot {} -> human {}", botId, humanId);
    }
}
