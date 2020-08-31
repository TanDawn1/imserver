package com.hutquanim.imserver.redis;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 接收Redis中订阅的消息，并及时转发到客户端
 */
@Component
public class Receiver implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private WebSocketSession session;

    @Override
    public void onMessage(Message message, byte[] bytes) {

        logger.info("收到订阅的消息" + message);

        try {
            session.sendMessage(new TextMessage(JSONObject.parse(message.toString()).toString()));
        } catch (IOException e) {
            logger.error("发送消息到客户端失败" + message);
        }
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }
}