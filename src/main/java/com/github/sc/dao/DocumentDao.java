package com.github.sc.dao;

import com.github.sc.model.Document;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.Date;

/**
 * Created by wuyu on 2017/4/3.
 */
public interface DocumentDao extends CrudRepository<Document, Integer> {

    @Modifying
    @Transactional
    @Query(value = "update Document doc set doc.content=?2,doc.updatedTime=?3 where doc.id=?1")
    void update(Integer id, String content, Date updatedTime);

}
