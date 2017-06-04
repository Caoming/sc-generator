package com.github.sc.websocket;

import com.github.sc.service.DatasourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Created by wuyu on 2017/5/31.
 */
@Component
public class MysqlTextWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private DatasourceService datasourceService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
    }
}
