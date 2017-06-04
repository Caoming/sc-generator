package com.github.sc.web;

import com.github.sc.db.RedisExport;
import com.github.sc.model.RedisDataSource;
import com.github.sc.service.RedisDataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wuyu on 2017/4/15.
 */
@RestController
@RequestMapping(value = "/redisDataSource/")
public class RedisDataSourceController {

    @Autowired
    private RedisDataSourceService redisDataSourceService;

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public RedisDataSource findOne(@PathVariable(value = "id") Integer id) {
        return redisDataSourceService.findOne(id);
    }

    @RequestMapping(value = "findAll", method = RequestMethod.GET)
    public Iterable<RedisDataSource> findAll() {
        return redisDataSourceService.findAll();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable(value = "id") Integer id) {
        redisDataSourceService.delete(id);
    }

    @RequestMapping(value = "/",method = RequestMethod.POST)
    public void save(@RequestBody RedisDataSource redisDataSource){
        redisDataSourceService.save(redisDataSource);
    }

    @RequestMapping(value = "/{id}/export", method = RequestMethod.GET)
    public void dump(HttpServletResponse response, @PathVariable(value = "id") Integer id) throws IOException {
        RedisDataSource redisDataSource = redisDataSourceService.findOne(id);
        String[] split = redisDataSource.getUrl().split(":");
        RedisExport redisExport = new RedisExport(split[0], Integer.parseInt(split[1]),redisDataSource.getAuth());
        response.setHeader("Content-Disposition", "attachment; filename=dump.rdb");
        response.setContentType("application/octet-stream;charset=UTF-8");
        redisExport.dump(response.getOutputStream());
    }
}
