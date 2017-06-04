package com.github.sc.service;

import com.github.sc.model.Document;
import com.github.sc.dao.DocumentDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by wuyu on 2017/4/3.
 */
@Service
public class DocumentService {

    @Autowired
    private DocumentDao documentDao;

    public Document findOne(Integer id) {
        return documentDao.findOne(id);
    }

    public Iterable<Document> findAll() {
        return documentDao.findAll();
    }

    public void save(Document document) {
        documentDao.save(document);
    }

    public void delete(Integer id) {
        documentDao.delete(id);
    }

    public void update(Integer id, String content, Date updatedTime) {
        documentDao.update(id,content,updatedTime);
    }
}
