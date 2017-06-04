package com.github.sc.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Created by wuyu on 2017/5/31.
 */
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private RedisTextWebSocketHandler redisTextWebSocketHandler;

    @Autowired
    private MysqlTextWebSocketHandler mysqlTextWebSocketHandler;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(redisTextWebSocketHandler,"/ws/redis")
                .addHandler(mysqlTextWebSocketHandler,"/ws/mysql");
    }



}
