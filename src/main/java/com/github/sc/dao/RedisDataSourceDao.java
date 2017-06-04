package com.github.sc.dao;

import com.github.sc.model.RedisDataSource;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by wuyu on 2017/4/15.
 */
public interface RedisDataSourceDao extends CrudRepository<RedisDataSource,Integer>{
}
