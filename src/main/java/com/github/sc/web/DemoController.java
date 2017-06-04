package com.github.sc.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.sc.common.utils.TblUtil;
import com.github.sc.common.utils.VelocityUtil;
import com.github.sc.gennerator.MetaData;
import com.github.sc.model.Datasource;
import com.github.sc.service.DatasourceService;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by wuyu on 2017/5/23.
 */
@RestController
@RequestMapping(value = "/demo")
public class DemoController {

    @Autowired
    private DatasourceService datasourceService;

    private Logger logger = LoggerFactory.getLogger(DemoController.class);


    @RequestMapping(value = "/{id}/{table}.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String html(@PathVariable(value = "id") Integer id, @PathVariable(value = "table") String table) throws Exception {
        Datasource one = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());
        List<String> modelNames = new ArrayList<>();
        List<String> varModelNames = new ArrayList<>();
        List<String> tbls = TblUtil.getTbls(connection);
        if (!tbls.contains(table) && tbls.size() > 0) {
            table = tbls.get(0);
        }

        for (String tbl : tbls) {
            modelNames.add(VelocityUtil.tableNameConvertModelName(tbl));
            varModelNames.add(VelocityUtil.firstLow(VelocityUtil.tableNameConvertModelName(tbl)));
        }

        List<MetaData> metaDates = getMetaDates(connection, table);

        List<String> filedNames = new ArrayList<>();
        List<String> varFiledNames = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        for (MetaData metaDate : metaDates) {
            filedNames.add(VelocityUtil.tableNameConvertModelName(metaDate.getColumnName()));
            varFiledNames.add(VelocityUtil.toHump(metaDate.getColumnName()));
            columns.add(metaDate.getColumnName());
        }

        String modelName = VelocityUtil.tableNameConvertModelName(table);
        String columnKey = TblUtil.getPrimaryKey(connection, table);
        String primaryKey = VelocityUtil.toHump(columnKey);
        String primaryType = "Object";
        String varModelName = VelocityUtil.firstLow(modelName);
        if (StringUtils.isNotBlank(columnKey)) {
            primaryType = TblUtil.getJavaType(connection, table, columnKey);
        }
        VelocityContext ctx = new VelocityContext();
        ctx.put("modelName", modelName);
        ctx.put("primaryKey", primaryKey);
        ctx.put("primaryType", primaryType);
        ctx.put("varModelName", varModelName);
        ctx.put("columns", columns);
        ctx.put("modelNames", modelNames);
        ctx.put("tables", tbls);
        ctx.put("table", table);
        ctx.put("filedNames", filedNames);
        ctx.put("varFiledNames", varFiledNames);
        ctx.put("primaryKey", primaryKey);
        ctx.put("varModelName", VelocityUtil.toHump(table));
        ctx.put("varModelNames", varModelNames);
        ctx.put("id", id);
        String project = one.getJdbcUrl().substring(one.getJdbcUrl().lastIndexOf("/") + 1);
        ctx.put("project", VelocityUtil.tableNameConvertModelName(project.split("-")[0]));
        ctx.put("miniProject", VelocityUtil.tableNameConvertModelName(project.split("-")[0]).substring(0, 1));

        Template tempate = VelocityUtil.getTempate("/templates/pom/ui/demo/index.vm");
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        connection.close();

        return writer.toString();
    }

    @RequestMapping(value = "/{id}/{table}/list", method = RequestMethod.GET)
    @ResponseBody
    public Object list(@PathVariable(value = "id") Integer id,
                       @PathVariable(value = "table") String table,
                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) throws SQLException {

        String sql = "select * from `" + table + "` limit " + ((pageNum - 1) * pageSize) + "," + pageSize;
        String count = "select count(1) from `" + table + "`";
        JSONObject data = new JSONObject();

        logger.info(sql);
        Datasource one = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
        ResultSetMetaData metaData = statement.getMetaData();
        ResultSet statementResultSet = statement.getResultSet();
        List<JSONObject> list = new ArrayList<>();
        while (statementResultSet.next()) {
            JSONObject row = new JSONObject();
            for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
                String columnName = metaData.getColumnName(i);
                String fieldName = VelocityUtil.toHump(columnName);
                Object object = statementResultSet.getObject(columnName);
                row.put(fieldName, object);
            }
            list.add(row);
        }
        data.put("list", list);

        statementResultSet.close();
        statement.close();

        statement = connection.prepareStatement(count);
        statement.execute();
        statementResultSet = statement.getResultSet();
        while (statementResultSet.next()) {
            Object object = statementResultSet.getObject(1);
            data.put("total", object);
            data.put("pageNum", pageNum);
            long l = Long.parseLong(object.toString());
            data.put("pages", (l / pageSize) + (l % pageSize == 0 ? 0 : 1));
        }

        statementResultSet.close();
        statement.close();
        connection.close();
        return data;
    }

    @RequestMapping(value = "/{id}/{table}/{primaryKey}", method = RequestMethod.GET)
    @ResponseBody
    public Object selectByPrimaryKey(@PathVariable(value = "id") Integer id,
                                     @PathVariable(value = "table") String table,
                                     @PathVariable(value = "primaryKey") String primaryKey) throws Exception {
        Datasource one = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());
        String column = TblUtil.getPrimaryKey(connection, table);
        String sql = "select * from `" + table + "` where `" + column + "`= '" + primaryKey + "'";
        if (TblUtil.isNumber(TblUtil.getColunmType(connection, table, column))) {
            sql = "select * from `" + table + "` where `" + column + "`= " + primaryKey;

        }

        logger.info(sql);

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
        ResultSetMetaData metaData = statement.getMetaData();
        ResultSet statementResultSet = statement.getResultSet();
        JSONObject row = new JSONObject();

        while (statementResultSet.next()) {
            for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
                String columnName = metaData.getColumnName(i);
                String fieldName = VelocityUtil.toHump(columnName);
                Object object = statementResultSet.getObject(columnName);
                row.put(fieldName, object);
            }
        }

        statementResultSet.close();
        statement.close();
        connection.close();
        return row.size() > 0 ? row : null;

    }

    @RequestMapping(value = "/{id}/{table}/{primaryKey}", method = RequestMethod.DELETE)
    public void deleteByPrimaryKey(@PathVariable(value = "id") Integer id,
                                   @PathVariable(value = "table") String table,
                                   @PathVariable(value = "primaryKey") String primaryKey) throws Exception {
        Datasource one = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());
        String column = TblUtil.getPrimaryKey(connection, table);
        String sql = "DELETE  FROM `" + table + "` where `" + column + "`= '" + primaryKey + "'";
        if (TblUtil.isNumber(TblUtil.getColunmType(connection, table, column))) {
            sql = "DELETE  FROM `" + table + "` where `" + column + "`= " + primaryKey;
        }

        logger.info(sql);

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
        statement.close();
        connection.close();
    }


    @RequestMapping(value = "/{id}/{table}/", method = RequestMethod.PUT)
    public void updateByPrimaryKey(@PathVariable(value = "id") Integer id,
                                   @PathVariable(value = "table") String table,
                                   @RequestBody Map<String, Object> data) throws Exception {


        Datasource one = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());
        String primaryKey = TblUtil.getPrimaryKey(connection, table);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        Object primaryValue = data.get(primaryKey);

        String sql = "UPDATE `" + table + "` set ";

        List<String> columns = TblUtil.getColunms(connection, table);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            for (String column : columns) {
                if (VelocityUtil.toHump(column).equalsIgnoreCase(entry.getKey())) {
                    if (entry.getValue() != null && !"".equalsIgnoreCase(entry.getValue().toString().trim())) {
                        if (TblUtil.isNumber(TblUtil.getColunmType(connection, table, column))) {
                            sql += "`" + column + "` = " + entry.getValue() + ",";
                        } else if (TblUtil.isDate(TblUtil.getColunmType(connection, table, column))) {
                            sql += "`" + column + "` = '" + simpleDateFormat.format(new Date(Long.parseLong(entry.getValue().toString()))) + "',";
                        } else {
                            sql += "`" + column + "` = '" + entry.getValue() + "',";
                        }
                    }
                }
            }
        }

        if (TblUtil.isNumber(TblUtil.getColunmType(connection, table, primaryKey))) {
            sql = sql.substring(0, sql.length() - 1) + " where `" + primaryKey + "`= " + primaryValue;
        } else {
            sql = sql.substring(0, sql.length() - 1) + " where `" + primaryKey + "`= '" + primaryValue + "'";
        }

        logger.info(sql);

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
        statement.close();
        connection.close();
    }


    @RequestMapping(value = "/{id}/{table}/", method = RequestMethod.POST)
    public void insert(@PathVariable(value = "id") Integer id,
                       @PathVariable(value = "table") String table,
                       @RequestBody Map<String, Object> data) throws Exception {


        Datasource one = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(one.getJdbcUrl(), one.getUsername(), one.getPassword());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String sql = "INSERT INTO `" + table + "`(%s) VALUES (";
        List<String> insertColumns = new ArrayList<>();
        String primaryKey = TblUtil.getPrimaryKey(connection, table);

        List<String> columns = TblUtil.getColunms(connection, table);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            for (String column : columns) {
                if (VelocityUtil.toHump(column).equalsIgnoreCase(entry.getKey())) {
                    insertColumns.add(column);
                    if (entry.getValue() == null && "".equalsIgnoreCase(entry.getValue().toString().trim())) {
                        sql += "null,";
                    } else if (TblUtil.isNumber(TblUtil.getColunmType(connection, table, column))) {
                        sql += entry.getValue() + ",";
                    } else if (TblUtil.isDate(TblUtil.getColunmType(connection, table, column))) {
                        sql += "'" + simpleDateFormat.format(new Date(Long.parseLong(entry.getValue().toString()))) + "',";
                    } else {
                        sql += "'" + entry.getValue() + "',";
                    }

                }
            }
        }

        String columnStr = StringUtils.join(insertColumns, ",");

        sql = String.format((sql.substring(0, sql.length() - 1) + ")"), columnStr);

        logger.info(sql);

        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
        statement.close();
        connection.close();
    }

    protected List<MetaData> getMetaDates(Connection connection, String tableName) throws Exception {
        List<Map<String, Object>> columnAndMeta = TblUtil.getColumnAndMeta(connection, tableName);
        return JSON.parseArray(JSON.toJSONString(columnAndMeta), MetaData.class);
    }
}
