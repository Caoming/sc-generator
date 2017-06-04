package com.github.sc.websocket;

import com.github.sc.model.RedisDataSource;
import com.github.sc.service.RedisDataSourceService;
import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.io.RawByteListener;
import com.moilioncircle.redis.replicator.rdb.RdbListener;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.redis.*;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import rx.Observable;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/5/31.
 */
@Component
public class RedisTextWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RedisDataSourceService redisDataSourceService;

    private Logger logger = LoggerFactory.getLogger(RedisTextWebSocketHandler.class);

    private final Map<String, Replicator> replicatorMap = new ConcurrentHashMap<>();

    private final Map<String, RxClient> rxClientMap = new ConcurrentHashMap<>();

    private final Map<String, Observable<ObservableConnection<Object, Object>>> observableMap = new ConcurrentHashMap<>();

    private final Map<String, Integer> idMap = new ConcurrentHashMap<>();

    private final Map<String, String> authMap = new ConcurrentHashMap<>();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        if (rxClientMap.get(session.getId()) != null) {
            RxClient rxClient = rxClientMap.remove(session.getId());
            rxClient.shutdown();
        }


        String query = session.getUri().getQuery();
        for (String param : query.substring(query.indexOf("?") + 1).split("&")) {
            if (param.contains("id")) {
                int id = Integer.parseInt(param.split("=")[1]);
                RedisDataSource redisDataSource = redisDataSourceService.findOne(id);
                String[] split = redisDataSource.getUrl().split(":");
                RxClient<Object, Object> rxClient = RxNetty.newTcpClientBuilder(split[0], Integer.parseInt(split[1]))
                        .channelOption(ChannelOption.SO_KEEPALIVE, true)
                        .pipelineConfigurator(pipeline -> {
                            pipeline.addLast(new RedisDecoder());
                            pipeline.addLast(new RedisEncoder());
                        })
                        .build();

                Observable<ObservableConnection<Object, Object>> connect = rxClient.connect();
                rxClientMap.put(session.getId(), rxClient);
                observableMap.put(session.getId(), connect);
                idMap.put(session.getId(), id);

                if (redisDataSource.getAuth() != null) {
                    authMap.put(session.getId(), redisDataSource.getAuth());
                }
            }

        }
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        RxClient rxClient = rxClientMap.get(session.getId());
        if (rxClient == null) {
            session.close();
            return;
        }
        final String command = message.getPayload();
        if (command.equalsIgnoreCase("sync")) {
            sync(session);
        } else {
            String auth = authMap.get(session.getId());

            observableMap.get(session.getId())
                    .subscribe(connection -> {
                        if (auth != null) {
                            connection.writeAndFlush("auth " + auth + "\r\n" + command + "\r\n");
                        } else {
                            connection.writeAndFlush(command + "\r\n");
                        }
                        connection.getInput()
                                .distinct()
                                .subscribe(response -> {
                                    String requestMessage;
                                    if (response instanceof DefaultLastBulkStringRedisContent) {
                                        DefaultLastBulkStringRedisContent content = (DefaultLastBulkStringRedisContent) response;
                                        byte[] bytes = new byte[content.content().readableBytes()];
                                        content.content().readBytes(bytes);
                                        requestMessage = new String(bytes);
                                    } else if (response instanceof IntegerRedisMessage) {
                                        requestMessage = String.valueOf(((IntegerRedisMessage) response).value());
                                    } else if (response instanceof ErrorRedisMessage) {
                                        requestMessage = ((ErrorRedisMessage) response).content();
                                    } else if (response instanceof FullBulkStringRedisMessage) {
                                        requestMessage = "nil";
                                    } else if (response instanceof SimpleStringRedisMessage) {
                                        requestMessage = ((SimpleStringRedisMessage) response).content();
                                    } else if (response instanceof BulkStringHeaderRedisMessage || response instanceof DefaultBulkStringRedisContent || response instanceof ArrayHeaderRedisMessage) {
                                        return;
                                    } else {
                                        requestMessage = response.toString();
                                    }

                                    try {
                                        session.sendMessage(new TextMessage(requestMessage));
                                    } catch (IOException e) {
                                        logger.warn(e.getMessage());
                                    }
                                });
                    });
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        close(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        close(session);
    }

    public void sync(WebSocketSession session) throws IOException {
        Integer id = idMap.get(session.getId());
        RedisDataSource redisDataSource = redisDataSourceService.findOne(id);
        String[] split = redisDataSource.getUrl().split(":");
        if (replicatorMap.get(session.getId()) != null) {
            Replicator replicator = replicatorMap.remove(session.getId());
            replicator.close();
        }
        final RawByteListener rawByteListener = rawBytes -> {
            try {
                session.sendMessage(new TextMessage(new String(rawBytes, "utf-8")));
            } catch (IOException ignore) {
                logger.warn(ignore.getMessage());
            }
        };

        Replicator replicator = new RedisReplicator(split[0], Integer.parseInt(split[1]), Configuration.defaultSetting().setAuthPassword(authMap.get(session.getId())));
        replicator.addRdbListener(new RdbListener() {
            @Override
            public void preFullSync(Replicator replicator) {
            }

            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                replicator.addRawByteListener(rawByteListener);
            }
        });
        replicator.open();
        replicatorMap.put(session.getId(), replicator);
    }

    public void close(WebSocketSession webSocketSession) {
        Replicator replicator = replicatorMap.remove(webSocketSession.getId());
        if (replicator != null) {
            try {
                replicator.close();
                webSocketSession.close();
            } catch (IOException e) {
                logger.warn(e.getMessage());
            }
        }
        RxClient rxClient = rxClientMap.get(webSocketSession.getId());
        if (rxClient != null) {
            rxClient.shutdown();
        }
        observableMap.remove(webSocketSession.getId());
        authMap.remove(webSocketSession.getId());
    }

    @PreDestroy
    public void destroy() {
        observableMap.clear();
        for (RxClient rxClient : rxClientMap.values()) {
            try {
                rxClient.shutdown();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
        for (Replicator replicator : replicatorMap.values()) {
            try {
                replicator.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }

        authMap.clear();

    }

}
