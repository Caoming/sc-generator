package com.github.sc.model;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "datasource")
public class Datasource implements Serializable {

    private static final long serialVersionUID = 7893301827548325995L;

    @Id
    @GeneratedValue
    private Integer id;

    private String jdbcUrl;

    private String username;

    private String password;

    private String driver;

    private String name;

    @CreationTimestamp
    private Date createdTime;

    public Integer getId() {
        return id;
    }

    public Datasource setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public Datasource setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl == null ? null : jdbcUrl.trim();
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Datasource setUsername(String username) {
        this.username = username == null ? null : username.trim();
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Datasource setPassword(String password) {
        this.password = password == null ? null : password.trim();
        return this;
    }

    public String getDriver() {
        return driver;
    }

    public Datasource setDriver(String driver) {
        this.driver = driver == null ? null : driver.trim();
        return this;
    }

    public String getName() {
        return name;
    }

    public Datasource setName(String name) {
        this.name = name;
        return this;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Datasource setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }
}
