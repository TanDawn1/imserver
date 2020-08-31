package com.hutquanim.imserver.socket;

import com.hutquanim.imserver.common.Constants;
import com.hutquanim.imserver.mapper.IUserMapper;
import com.hutquanim.imserver.pojo.User;
import com.hutquanim.imserver.service.UserService;
import com.hutquanim.imserver.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class WebSocketInterceptor implements HandshakeInterceptor {
    private final Logger logger = LoggerFactory.getLogger(WebSocketInterceptor.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private IUserMapper iUserMapper;
//    @Autowired
//    private AdminService adminService;

    /**
     * 在建立WebSocket的连接前需要做一次身份的验证
     * @param request
     * @param serverHttpResponse
     * @param webSocketHandler
     * @param attributes
     * @return true 继续握手 false 终止握手
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> attributes) throws Exception {
        HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        logger.info("进入ws拦截器");
        logger.info(httpServletRequest.getRequestURL().toString());

        User user = null;

        String userId = httpServletRequest.getParameter("userId");
        String token = httpServletRequest.getParameter("token");
        logger.info(userId + " " + token);
        //判断是否符合用户登录之后所用的Token
        if(redisUtils.hget("userToken",userId).equals(token)){
            //获取user 由于序列化方式的问题，所以不能使用这种方式获取
            //User user = (User) redisUtils.get(httpServletRequest.getHeader("token"));
            //查找数据库
            user = iUserMapper.login(Integer.valueOf(userId));
        }

        if(user != null && user.getUserId() >= 0){
            //把用户数据存储到Map<String, Object> attributes中
            attributes.put(Constants.WEBSOCKET_USER, user);

            logger.info("用户: " + user.getUsername() + "连接成功" + request.getRemoteAddress());

            return true;
        }
        logger.info("认证不通过");
        return  false;

//        if (request instanceof ServletServerHttpRequest) {
//            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
//            Long userId = Long.parseLong(httpServletRequest.getParameter("userId").toString());
//            String password = httpServletRequest.getParameter("password");
//            if(userId > 0) {
//                User user = userService.getUserById(userId);
//                if (null != user && user.getPassword().equals(password)) {
//                    attributes.put(Constants.WEBSOCKET_USER, user);
//                    logger.info("用户" + user.getNickName() + "连接并验证成功" + request.getRemoteAddress());
//                    return true;
//                }
//            }
//        }
//        return false;
    }

    /**
     * 在握手之后执行该方法， 无论是否握手成功都指明了响应状态码和相应头
     * @param serverHttpRequest
     * @param serverHttpResponse
     * @param webSocketHandler
     * @param e
     */
    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {

    }
}
