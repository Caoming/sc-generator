package com.github.sc.web;

import com.github.sc.common.utils.TblUtil;
import com.github.sc.service.DatasourceService;
import com.github.sc.model.Datasource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/db")
public class DatasourceController {

    @Autowired
    private DatasourceService datasourceService;

    private final Cache<Integer, List<String>> tableCache = CacheBuilder.newBuilder()
            .maximumSize(60000)
            .expireAfterAccess(20, TimeUnit.SECONDS)
            .build();

    @RequestMapping(value = "/findAll", method = RequestMethod.GET)
    public Iterable<Datasource> findAll() {
        return datasourceService.findAll();
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public void save(@RequestBody Datasource datasource) {
        datasourceService.save(datasource);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("id") Integer id) {
        datasourceService.delete(id);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Datasource findOne(@PathVariable("id") Integer id) {
        return datasourceService.findOne(id);
    }


    @RequestMapping(value = "/{id}/tables", method = RequestMethod.GET)
    public List<String> listTables(@PathVariable("id") Integer id, @RequestParam(value = "q", required = false) String q) throws Exception {
        Datasource one = datasourceService.findOne(id);
        List<String> tables = tableCache.getIfPresent(id);
        if (tables == null) {
            Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());
            tables = TblUtil.getTbls(connection);
            tableCache.put(id, tables);
            connection.close();
        }

        if (StringUtils.isNotBlank(q)) {
            return tables.stream()
                    .filter(s -> s.contains(q))
                    .collect(Collectors.toList());
        }

        return tables;
    }

}
