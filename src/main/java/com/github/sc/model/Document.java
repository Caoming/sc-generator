package com.github.sc.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Table(name = "document")
@Entity
public class Document implements Serializable {

    private static final long serialVersionUID = 7893301827548325991L;

    @Id
    @GeneratedValue
    private Integer id;

    @Column(columnDefinition = "CLOB")
    private String content;

    @Column(length = 1024)
    private String url;

    private String title;

    @CreationTimestamp
    private Date createdTime;

    @UpdateTimestamp
    private Date updatedTime;

    public String getContent() {
        return content;
    }

    public Document setContent(String content) {
        this.content = content;
        return this;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Document setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public Document setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Document setTitle(String title) {
        this.title = title;
        return this;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public Document setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }
}
