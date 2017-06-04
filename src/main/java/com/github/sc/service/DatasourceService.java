package com.github.sc.service;

import com.github.sc.dao.DatasourceDao;
import com.github.sc.model.Datasource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatasourceService {

    @Autowired
    private DatasourceDao datasourceDao;

    public Iterable<Datasource> findAll() {
        return datasourceDao.findAll();
    }

    public void save(Datasource datasource) {
        datasourceDao.save(datasource);
    }

    public void delete(Integer id) {
        datasourceDao.delete(id);
    }

    public Datasource findOne(Integer id) {
        return datasourceDao.findOne(id);
    }

}