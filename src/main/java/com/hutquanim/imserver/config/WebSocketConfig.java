package com.hutquanim.imserver.config;

import com.hutquanim.imserver.socket.SocketHandler;
import com.hutquanim.imserver.socket.WebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private SocketHandler socketHandler;

    // 在建立连接前需要做一次身份认证，通过请求头的token来验证
    @Autowired
    private WebSocketInterceptor webSocketInterceptor;

    /**
     * 这是一个核心实现方法，配置websocket入口，允许访问的域、注册Handler、SockJs支持和拦截器。
     * @param registry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //registry.addHandler注册和路由的功能，当客户端发起websocket连接时->
        //-把/path交给对应的handler处理，而不实现具体的业务逻辑，可以理解为收集和任务分发中心。
        //setAllowedOrigins(String[] domains),允许指定的域名或IP(含端口号)建立长连接，如果只允许自家域名访问，
        //这里轻松设置。如果不限时使用"*"号，如果指定了域名，则必须要以http或https开头。
        //addInterceptors，顾名思义就是为handler添加拦截器，可以在调用handler前后加入我们自己的逻辑代码。
        registry.addHandler(socketHandler, "/ChatServer")
               .setAllowedOrigins("*")
                .addInterceptors(webSocketInterceptor);
    }
}
