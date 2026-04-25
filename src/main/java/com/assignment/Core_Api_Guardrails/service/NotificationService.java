package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AppConfig appConfig;

    private static final String NOTIF_COOLDOWN_KEY = "notif:cooldown:user_%d";
    private static final String PENDING_NOTIFS_KEY = "user:%d:pending_notifs";

    public void handleNotification(Long userId, String botName){

    }
}
