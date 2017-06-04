package com.github.sc.db;

import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.io.RawByteListener;
import com.moilioncircle.redis.replicator.rdb.RdbListener;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by wuyu on 2016/7/14.
 */
public class RedisExport {

    private String host = "127.0.0.1";

    private int port = 6379;

    private String password;

    public RedisExport(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void dump(final OutputStream out) throws IOException {
        final RawByteListener rawByteListener = new RawByteListener() {
            @Override
            public void handle(byte... rawBytes) {
                try {
                    out.write(rawBytes);
                } catch (IOException ignore) {
                }
            }
        };

        //save rdb from remote server
        Replicator replicator = new RedisReplicator(this.host, this.port, Configuration.defaultSetting().setAuthPassword(password));
        replicator.addRdbListener(new RdbListener() {
            @Override
            public void preFullSync(Replicator replicator) {
                replicator.addRawByteListener(rawByteListener);
            }

            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                replicator.removeRawByteListener(rawByteListener);
                try {
                    out.close();
                    replicator.close();
                } catch (IOException ignore) {
                }
            }
        });
        replicator.open();
    }

}
