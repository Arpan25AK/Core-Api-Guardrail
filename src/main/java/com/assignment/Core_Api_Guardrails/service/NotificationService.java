package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AppConfig appConfig;

    private static final String NOTIF_COOLDOWN_KEY = "notif:cooldown:user_%d";
    private static final String PENDING_NOTIFS_KEY = "user:%d:pending_notifs";

    public void handleNotification(Long userId, String botName){
        String cooldownKey = String.format(NOTIF_COOLDOWN_KEY,userId);
        String pendingKey = String.format(PENDING_NOTIFS_KEY,userId);
        String message = "bot" + botName + "has interacted with ur post";

        Boolean Cooldown = redisTemplate.hasKey(cooldownKey);

        if(Boolean.TRUE.equals(Cooldown)){
            redisTemplate.opsForList().rightPush(pendingKey,message);
            log.warn("this bot has already interacted with the user");
        }else {
            log.info("push notification sent to {} : {}",userId,message);
            redisTemplate.opsForValue().set(cooldownKey,"1",appConfig.getNotifCooldownTtl(), TimeUnit.SECONDS);
        }
    }

    public String getPendingNotifsKey(Long userId) {
        return String.format(PENDING_NOTIFS_KEY, userId);
    }
}
