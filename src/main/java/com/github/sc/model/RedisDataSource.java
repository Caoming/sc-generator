package com.github.sc.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by wuyu on 2017/4/15.
 */
@Entity(name = "redisDataSource")
public class RedisDataSource implements Serializable {

    private static final long serialVersionUID = 7893301827518325991L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String name;

    private String url;

    private String auth;

    private String path;

    @CreationTimestamp
    private Date createdTime;

    @UpdateTimestamp
    private Date updatedTime;


    public RedisDataSource(Integer id, String name, String url, String auth, String path, Date createdTime, Date updatedTime) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.auth = auth;
        this.path = path;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }

    public RedisDataSource() {
    }

    public Integer getId() {
        return id;
    }

    public RedisDataSource setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public RedisDataSource setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public RedisDataSource setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getAuth() {
        return auth;
    }

    public RedisDataSource setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RedisDataSource setPath(String path) {
        this.path = path;
        return this;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public RedisDataSource setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public RedisDataSource setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }
}
