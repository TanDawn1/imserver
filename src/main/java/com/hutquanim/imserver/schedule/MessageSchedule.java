package com.hutquanim.imserver.schedule;

import com.hutquanim.imserver.common.Constants;
import com.hutquanim.imserver.redis.RedisService;
import com.hutquanim.imserver.service.MessageService;
import com.hutquanim.imserver.socket.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class MessageSchedule {
    private static final Logger logger = LoggerFactory.getLogger(MessageSchedule.class);
    @Autowired
    private RedisService redisService;

    @Autowired
    private MessageService messageService;

    //每日凌晨4点将Redis中的消息转储到MySQL
    //@Scheduled(cron = "0 0 4 * * ?")
    private void saveMessage() {
        try {

            String prefix = Constants.WEBSOCKET_MESSAGE + "*";
            //获取[Message:]开头的所有的key
            Set<String> keys = redisService.getAllKeyByPattern(prefix);
            //遍历所有Message:*
            for (String key : keys) {
                Message message = null;
                List<Message> messages = new ArrayList<>();
                //向左出队，即先进先出
                while ((message = (Message) redisService.leftPop(key)) != null) {
                    messages.add(message);
                }
                if (messages.size() > 0) {
                    //持久化到数据库 TODO
                    messageService.insertMessages(messages);
                }
            }
        } catch (Exception e) {
            logger.error("保存消息到数据库异常 " + e. getMessage());
        }
    }
}
