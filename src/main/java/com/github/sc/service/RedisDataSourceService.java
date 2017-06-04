package com.github.sc.service;

import com.github.sc.dao.RedisDataSourceDao;
import com.github.sc.model.RedisDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by wuyu on 2017/4/15.
 */
@Service
public class RedisDataSourceService {

    @Autowired
    private RedisDataSourceDao redisDataSourceDao;

    public RedisDataSource findOne(Integer id) {
        return redisDataSourceDao.findOne(id);
    }

    public void delete(Integer id) {
        redisDataSourceDao.delete(id);
    }

    public Iterable<RedisDataSource> findAll() {
        return redisDataSourceDao.findAll();
    }

    public void save(RedisDataSource redisDataSource) {
        redisDataSourceDao.save(redisDataSource);
    }
}
