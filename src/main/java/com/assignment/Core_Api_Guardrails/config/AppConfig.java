package com.assignment.Core_Api_Guardrails.config;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {
    @Value("${app.redis.bot-cooldown-ttl}")
    private long botCooldownTtl;

    @Value("${app.redis.notif-cooldown-ttl}")
    private long notifCooldownTtl;

    @Value("${app.redis.horizontal-cap}")
    private long horizontalCap;

    @Value("${app.redis.vertical-cap}")
    private int verticalCap;
}
