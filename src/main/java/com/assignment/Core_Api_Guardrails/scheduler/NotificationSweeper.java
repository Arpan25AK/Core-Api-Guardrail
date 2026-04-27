package com.assignment.Core_Api_Guardrails.scheduler;

import com.assignment.Core_Api_Guardrails.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSweeper {

    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.scheduler.notif-sweep-cron}")
    public void sweepPendingNotification(){
        log.info("sweeping operation started!!!");

        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");

        if(keys == null || keys.isEmpty()){
            log.info("NO pending notifications");
        }

        for(String key : keys){
            List<String> messages = redisTemplate.opsForList().range(key,0,-1);

            if(messages == null || messages.isEmpty()){
                continue;
            }

            int count = messages.size();
            String firstMessage = messages.get(0);

            String botName = firstMessage.replace(" replied to your post", "");

            if (count == 1) {
                log.info("Summarized Push Notification: {} interacted with your posts.", botName);
            } else {
                log.info("Summarized Push Notification: {} and [{}] others interacted with your posts.",
                        botName, count - 1);
            }
            redisTemplate.delete(key);
        }
        log.info("sweeper operation ended!!");
    }


}
