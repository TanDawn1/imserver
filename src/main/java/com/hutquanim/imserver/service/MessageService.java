package com.hutquanim.imserver.service;

import com.hutquanim.imserver.mapper.IMessageMapper;
import com.hutquanim.imserver.redis.RedisService;
import com.hutquanim.imserver.socket.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 持久化聊天记录
 */
@Service
public class MessageService {

    @Autowired
    private IMessageMapper imessageMapper;

    @Autowired
    RedisService redisService;

    public Message insertMessage(Message message) {
        return imessageMapper.save(message);
    }

    public List<Message> insertMessages(List<Message> messages) {
        Iterable<Message> ret = imessageMapper.saveAll(messages);
        List<Message> res = new ArrayList<>();
        ret.forEach(res::add);
        return res;
    }

    public List<Message> getNoReadMessagesFromDB(Integer userId) {
        return imessageMapper.findByDesUserIdAndAlreadySent(userId, false);
    }
}