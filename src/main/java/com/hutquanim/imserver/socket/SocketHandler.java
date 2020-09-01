package com.hutquanim.imserver.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.hutquanim.imserver.common.Constants;
import com.hutquanim.imserver.pojo.User;
import com.hutquanim.imserver.redis.Receiver;
import com.hutquanim.imserver.redis.RedisService;
import com.hutquanim.imserver.service.MessageService;
import com.hutquanim.imserver.service.UserService;
import com.hutquanim.imserver.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class SocketHandler implements WebSocketHandler {

    @Autowired
    //遗留处理方式
    private RedisService redisService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserService userService;
//    @Autowired
//    private PushService pushService;
    @Autowired
    private MessageService messageService;

    @Autowired
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);
    //在线用户列表
    private static final Map<Integer, Receiver> users;

    static {
        users = new ConcurrentHashMap<>();
    }

    /**
     * afterConnectionEstablished 方法
     * 功能在于将前一步拦截器处理验证后将用户和用户所使用的连接存入缓存，
     * 其功能和 @ServerEndPoint 实现类中的 onOpen () 完成的内容基本一致
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        //在ws拦截器中已经把用户数据存储到了 ws_session中
        Object user = session.getAttributes().get(Constants.WEBSOCKET_USER);

        if (user != null) {
            if(user instanceof User) {

                Receiver receiver = new Receiver();
                receiver.setSession(session);

                //设置订阅topic Message:*:selfUserId
                //这样订阅发送消息给自己的用户
                redisMessageListenerContainer.
                        addMessageListener(receiver,
                                new PatternTopic(Constants.WEBSOCKET_MESSAGE + "*:" + ((User)user).getUserId()));

                //判断user是否已经存在连接在用户列表中，在的话关闭
                if(users.containsKey(((User)user).getUserId())) {
                    users.get(((User)user).getUserId()).getSession().close();
                }
                //把用户数据存储进Map维护的用户列表
                users.put(((User)user).getUserId(), receiver);
                //将用户存储进Redis中，表示用户在线
                redisUtils.hset(Constants.WEBSOCKET_USER,((User)user).getUserId().toString(),true);

                //未读消息转发
                sendNoReadMessage(((User)user).getUserId(), session);
            }
            //session.sendMessage(new TextMessage("成功建立socket连接"));
            logger.info(user + "成功连接！");
        }
        logger.info("当前在线人数："+users.size());
    }


    //用户上线时将所有未发送消息发送给用户
    public void sendNoReadMessage(Integer id, WebSocketSession session) {
        try {
            List<Message> messages = new ArrayList<>();
            //从Redis中获取未发送的消息
            String pattern = Constants.WEBSOCKET_MESSAGE_NO + "*:" + id;
            //匹配所有指定的key -> Message:*:id
            Set<String> keys = redisService.getAllKeyByPattern(pattern);

            for (String key : keys) {
                Message msg = null;
                //先进先出
                while((msg = (Message) redisService.leftPop(key)) != null) {
                    messages.add(msg);
                }
            }
            //查找数据库中未读消息
            //原因: 每天的凌晨3点会将Redis中的所有聊天数据都转存到Mysql中
            //TODO 未采用这种方式
            //messages.addAll(messageService.getNoReadMessagesFromDB(id));

//            for (Message m : messages) {
//                //判断消息是否已发送 只转发未读的
//                if(!m.isAlreadySent()) {
//                    session.sendMessage(new TextMessage(JSONObject.toJSONString(m)));
//                }else{
//                    //如果查询到消息已发送，则可以判断之后的消息都是已经发送过的
//                    return;
//                }
//            }
            for(Message m : messages) {
//                //判断一遍是否是未发送的 -> 其实这个判断无意义
//                if(!m.isAlreadySent()) {
//                    session.sendMessage(new TextMessage(JSONObject.toJSONString(m)));
//                }
                //将消息置已发送
                m.setAlreadySent(true);
                //存储入已发送Redis中  到了这一步表示用户已经订阅了Redis相应Key
                //所以我发布进Redis就会被监听到，进而通过receiver转发
                redisService.convertAndSend(m.buildTopic(), JSONObject.toJSONString(m));
                logger.info("未读消息转发：" + m);
                //存储进Redis 已发布key
                redisUtils.set(Constants.WEBSOCKET_MESSAGE + m.getSrcUserId()+":"+id,m);
            }
            logger.info(id+"未读消息转发完成");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * handleMessage 方法
     * 作用在于接收用户发来的消息，其功能与 @ServerEndPoint 实现类中的 onMessage () 完成的内容基本一致
     * @param webSocketSession
     * @param webSocketMessage
     * @throws Exception
     */
    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {
        try{
            if(webSocketMessage instanceof TextMessage) {
                try {
                    //判断是否是合法的JSON
                    if(JSONObject.isValid(((TextMessage) webSocketMessage).getPayload())) {
                        //将JSON转化反序列化为对象
                        Message msg = JSONObject.parseObject(((TextMessage) webSocketMessage).getPayload(), Message.class);
                        logger.info("收到用户消息：" + msg);
                        msg.setTime(Instant.now().getEpochSecond());
                        //调用发送方法
                        sendMessageToUser(msg);
                    } else {
                        logger.info("收到其他消息：" + ((TextMessage) webSocketMessage).getPayload());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.info("收到其他消息：" + ((TextMessage) webSocketMessage).getPayload());
                }
            } else {
                logger.info("收到其他消息：" + webSocketMessage.getPayload());
            }
        }catch(Exception e){
            logger.error("消息处理失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送信息给指定用户n
     src = 633 表示系统消息
     */
    public void sendMessageToUser(Message message) {
        System.out.println(message);
        message.setTime(Instant.now().getEpochSecond());
        //获取发送人Id
        Integer srcUserId = message.getSrcUserId();
        //获取接收人Id
        Integer userId = message.getDesUserId();
        //633 是系统消息，发送给所有用户 未实现
        if(srcUserId == 633 && userId == 633) {
            sendMessageToAllUsers(new TextMessage(JSON.toJSONString(message)));
        } else {
            //判断是否在线
            if(Boolean.TRUE.equals(redisUtils.hget(Constants.WEBSOCKET_USER,userId.toString()))){
                //消息接收者在线 直接通过 Redis发布/订阅的方式 发布
                redisService.convertAndSend(message.buildTopic(), JSONObject.toJSONString(message));
                logger.info("消息转发：" + message);
                //成功发送
                message.setAlreadySent(true);
                //存储入Redis 已发送Key
                String key = Constants.WEBSOCKET_MESSAGE + message.getSrcUserId() + ":" + message.getDesUserId();
                redisService.rightPush(key, message);
            } else {
                //消息接收者不在线
                //TODO 本该判断接收人是否存在，但是没啥必要，在移动端处理之后，不会出现这种情况
                //User user = userService.getUserById(userId);
                //TODO 这里可以使用移动端相关的推送平台
                logger.info("消息未转发：" + message);
                message.setAlreadySent(false);
                //存储入Redis 未发送Key
                String key = Constants.WEBSOCKET_MESSAGE_NO + message.getSrcUserId() + ":" + message.getDesUserId();
                redisService.rightPush(key, message);
            }
        }

    }

    /**
     * 广播信息
     * @param message
     * @return
     */
    public boolean sendMessageToAllUsers(TextMessage message) {
        //这个没必要
        return true;
    }

    /**
     * 和@OnError是一样的
     * @param session
     * @param exception
     * @throws Exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close();
        }
        System.out.println("连接出错");
        removeDisconnectedUser(getUserId(session));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("连接已关闭：" + status);
        //移除连接
        removeDisconnectedUser(getUserId(session));
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 获取用户标识
     * @param session
     * @return
     */
    private Integer getUserId(WebSocketSession session) {
        try {
            Object user = session.getAttributes().get(Constants.WEBSOCKET_USER);
            if(user instanceof User) {
                return ((User)user).getUserId();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void removeDisconnectedUser(Integer uid) {
        if(users.containsKey(uid)) {
            //移除订阅
            redisMessageListenerContainer.removeMessageListener(users.get(uid));
        }
        users.remove(uid);
        logger.info("设置用户"+uid+"在线状态为false");
        //设置用户不在线
        redisUtils.hset(Constants.WEBSOCKET_USER,uid.toString(),false);
    }
}

