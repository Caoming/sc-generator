package com.github.sc.web;

import com.github.sc.common.utils.FileUtil;
import com.github.sc.common.utils.TblUtil;
import com.github.sc.common.utils.VelocityUtil;
import com.github.sc.db.MysqlExport;
import com.github.sc.gennerator.AbstractGenerator;
import com.github.sc.gennerator.MetaData;
import com.github.sc.gennerator.jpa.GroovyGeneratorJpa;
import com.github.sc.gennerator.jpa.JavaGeneratorJpa;
import com.github.sc.gennerator.jpa.KotlinGeneratorJpa;
import com.github.sc.gennerator.jpa.ScalaGeneratorJpa;
import com.github.sc.gennerator.mybatis.*;
import com.github.sc.model.Datasource;
import com.github.sc.service.DatasourceService;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2015/3/27.
 */
@RequestMapping(value = "/generator/")
@RestController
public class GeneratorController {

    @Autowired
    private DatasourceService datasourceService;

    private static final String tempPath = System.getProperty("java.io.tmpdir");


    @RequestMapping(value = "/springboot", method = RequestMethod.GET)
    public void generatorSpringBoot(HttpServletResponse response,
                                    @RequestParam(value = "id") Integer id,
                                    @RequestParam(value = "type", defaultValue = "java") String type,
                                    @RequestParam(value = "project", defaultValue = "demo") String project,
                                    @RequestParam(value = "packagePath", defaultValue = "com.example") String packagePath,
                                    @RequestParam(value = "tbls", required = false) List<String> tbls,
                                    @RequestParam(value = "mutilProject", defaultValue = "false") boolean mutilProject,
                                    @RequestParam(value = "swagger", defaultValue = "false") boolean swagger,
                                    @RequestParam(value = "commonPackagePath", required = false) String commonPackagePath,
                                    @RequestParam(value = "language", defaultValue = "java") String language) throws Exception {
        Datasource db = datasourceService.findOne(id);
        AbstractGenerator generator;

        if (type.equalsIgnoreCase("jpa")) {
            //其他语言暂时只支持jpa
            if ("kotlin".equalsIgnoreCase(language)) {
                generator = new KotlinGeneratorJpa(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls);
            } else if ("scala".equalsIgnoreCase(language)) {
                generator = new ScalaGeneratorJpa(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls);
            } else if ("groovy".equalsIgnoreCase(language)) {
                generator = new GroovyGeneratorJpa(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls);
            } else {
                generator = new JavaGeneratorJpa(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls);
            }
        } else {
            if (StringUtils.isNotBlank(commonPackagePath)) {
                generator = new SpringBootBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, mutilProject, swagger);
            } else {
                generator = new SpringBootNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, mutilProject, swagger);
            }
        }

        download(response, generator, project);

    }

    @RequestMapping(value = "/springcloud", method = RequestMethod.GET)
    public void generatorSpringCloud(HttpServletResponse response,
                                     @RequestParam(value = "id") Integer id,
                                     @RequestParam(value = "type", defaultValue = "java") String type,
                                     @RequestParam(value = "project", defaultValue = "demo") String project,
                                     @RequestParam(value = "packagePath", defaultValue = "com.example") String packagePath,
                                     @RequestParam(value = "tbls", required = false) List<String> tbls,
                                     @RequestParam(value = "mutilProject", defaultValue = "false") boolean mutilProject,
                                     @RequestParam(value = "swagger", defaultValue = "false") boolean swagger,
                                     @RequestParam(value = "commonPackagePath", required = false) String commonPackagePath,
                                     @RequestParam(value = "language", defaultValue = "java") String language) throws Exception {

        Datasource db = datasourceService.findOne(id);
        AbstractGenerator generator;

        //springcloud暂时只支持 java不支持其他语言
        if (StringUtils.isNotBlank(commonPackagePath)) {
            generator = new SpringCloudBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, mutilProject, swagger);
        } else {
            generator = new SpringCloudNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, mutilProject, swagger);
        }

        download(response, generator, project);

    }

    @RequestMapping(value = "/web", method = RequestMethod.GET)
    public void generatorWeb(HttpServletResponse response,
                             @RequestParam(value = "id") Integer id,
                             @RequestParam(value = "type", defaultValue = "java") String type,
                             @RequestParam(value = "project", defaultValue = "demo") String project,
                             @RequestParam(value = "packagePath", defaultValue = "com.example") String packagePath,
                             @RequestParam(value = "tbls", required = false) List<String> tbls,
                             @RequestParam(value = "swagger", defaultValue = "false") boolean swagger,
                             @RequestParam(value = "commonPackagePath", required = false) String commonPackagePath,
                             @RequestParam(value = "language", defaultValue = "java") String language) throws Exception {

        Datasource db = datasourceService.findOne(id);
        AbstractGenerator generator;

        //springcloud暂时只支持 java不支持其他语言
        if (StringUtils.isNotBlank(commonPackagePath)) {
            generator = new WebBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, swagger);
        } else {
            generator = new WebNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, swagger);
        }

        download(response, generator, project);

    }

    @RequestMapping(value = "/ui", method = RequestMethod.GET)
    public void generatorUI(HttpServletResponse response,
                            @RequestParam(value = "id") Integer id,
                            @RequestParam(value = "type", defaultValue = "java") String type,
                            @RequestParam(value = "project", defaultValue = "demo") String project,
                            @RequestParam(value = "packagePath", defaultValue = "com.example") String packagePath,
                            @RequestParam(value = "tbls", required = false) List<String> tbls,
                            @RequestParam(value = "swagger", defaultValue = "false") boolean swagger,
                            @RequestParam(value = "commonPackagePath", required = false) String commonPackagePath,
                            @RequestParam(value = "language", defaultValue = "angular1") String language) throws Exception {

        Datasource db = datasourceService.findOne(id);
        AbstractGenerator generator;

        //springcloud暂时只支持 java不支持其他语言
        if (language.equalsIgnoreCase("thymeleaf")) {
            if (StringUtils.isNotBlank(commonPackagePath)) {
                generator = new SpringBootThymeleafBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, false, swagger);
            } else {
                generator = new SpringBootThymeleafNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, false, swagger);
            }
        } else if (language.equalsIgnoreCase("html")) {
            if (StringUtils.isNotBlank(commonPackagePath)) {
                generator = new SpringBootHtmlBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, false, swagger);
            } else {
                generator = new SpringBootHtmlNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, false, swagger);
            }
        } else {
            if (StringUtils.isNotBlank(commonPackagePath)) {
                generator = new SpringBootAngular1BaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, false, swagger);
            } else {
                generator = new SpringBootAngular1NoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, false, swagger);
            }
        }
        download(response, generator, project);

    }


    @RequestMapping(value = "/dubbo", method = RequestMethod.GET)
    public void generatorDubbo(HttpServletResponse response,
                               @RequestParam(value = "id") Integer id,
                               @RequestParam(value = "type", defaultValue = "java") String type,
                               @RequestParam(value = "project", defaultValue = "demo") String project,
                               @RequestParam(value = "packagePath", defaultValue = "com.example") String packagePath,
                               @RequestParam(value = "tbls", required = false) List<String> tbls,
                               @RequestParam(value = "container", defaultValue = "springboot") String container,
                               @RequestParam(value = "mutilProject", defaultValue = "false") boolean mutilProject,
                               @RequestParam(value = "swagger", defaultValue = "false") boolean swagger,
                               @RequestParam(value = "commonPackagePath", required = false) String commonPackagePath,
                               @RequestParam(value = "language", defaultValue = "java") String language) throws Exception {

        Datasource db = datasourceService.findOne(id);
        AbstractGenerator generator;

        //dubbo暂时只支持 java不支持其他语言
        if (container.equalsIgnoreCase("springboot")) {
            if (StringUtils.isNotBlank(commonPackagePath)) {
                generator = new SpringBootDubboBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, mutilProject, swagger);
            } else {
                generator = new SpringBootDubboNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, mutilProject, swagger);
            }
        } else {
            if (StringUtils.isNotBlank(commonPackagePath)) {
                generator = new DubboBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, commonPackagePath, mutilProject, swagger);
            } else {
                generator = new DubboNoBaseGeneratorMybatis(db.getJdbcUrl(), db.getUsername(), db.getPassword(), db.getDriver(), project, packagePath, tbls, mutilProject, swagger);
            }
        }


        download(response, generator, project);
    }

    @RequestMapping(value = "/{language}/{id}.{extension}", method = RequestMethod.GET)
    public String sql(@PathVariable(value = "id") Integer id,
                      @PathVariable(value = "extension") String extension,
                      @PathVariable(value = "language") String language,
                      @RequestParam(value = "size", defaultValue = "0") Integer size) throws Exception {
        Datasource datasource = datasourceService.findOne(id);
        Connection connection = DriverManager.getConnection(datasource.getJdbcUrl(), datasource.getUsername(), datasource.getPassword());
        String result = null;
        if (extension.equalsIgnoreCase("sql")) {
            String createDatabaseSql = TblUtil.getCreateDatabaseSql(connection);
            StringBuilder sb = new StringBuilder(createDatabaseSql).append(TblUtil.getCreateTblSql(connection, null));
            if (size != 0) {
                sb.append(TblUtil.limiterInsertSql(connection, null, size));
            }
            result = sb.toString();
        } else if (extension.equalsIgnoreCase("html") || extension.equalsIgnoreCase("doc")) {
            List<String> tbls = TblUtil.getTbls(connection);
            List<String> createTableSqls = new ArrayList<>();
            List<List<Map<String, Object>>> columns = new ArrayList<>();
            for (String tbl : tbls) {
                List<Map<String, Object>> columnAndMeta = TblUtil.getColumnAndMeta(connection, tbl);
                columns.add(columnAndMeta);
                if (datasource.getDriver().equalsIgnoreCase("com.mysql.jdbc.Driver")) {
                    String createTblSql = TblUtil.getCreateTblSql(connection, Collections.singletonList(tbl));
                    createTableSqls.add(createTblSql.substring(0, createTblSql.length() - 4));
                }
            }
            Template template = VelocityUtil.getTempate("/templates/pom/doc/index.vm");
            VelocityContext ctx = new VelocityContext();
            ctx.put("tables", tbls);
            ctx.put("columns", columns);
            ctx.put("sqls", createTableSqls);
            ctx.put("updatedDate", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            ctx.put("project", datasource.getJdbcUrl().substring(datasource.getJdbcUrl().lastIndexOf("/") + 1));
            StringWriter stringWriter = new StringWriter();
            template.merge(ctx, stringWriter);
            result = stringWriter.toString();
            stringWriter.close();
        } else if (extension.equalsIgnoreCase("jdl")) {
            List<String> tbls = TblUtil.getTbls(connection);
            Map<String, Object> modelMap = new LinkedHashMap<>();
            StringBuilder sb = new StringBuilder();
            for (String tbl : tbls) {
                List<MetaData> metaDates = TblUtil.getMetaDates(connection, tbl);
                String modelName = VelocityUtil.tableNameConvertModelName(tbl);
                Map<String, String> filedMap = new LinkedHashMap<>();
                for (MetaData metaDate : metaDates) {
                    String fieldName = VelocityUtil.toHump(metaDate.getColumnName());
                    String fieldType = VelocityUtil.firstUp(metaDate.getTypeName());
                    filedMap.put(fieldName, fieldType);
                }
                modelMap.put(modelName, filedMap);

            }
            StringWriter stringWriter = new StringWriter();
            VelocityContext ctx = new VelocityContext();
            Template template = VelocityUtil.getTempate("/templates/pom/jdl/index.vm");
            ctx.put("modelMap", modelMap);
            template.merge(ctx, stringWriter);
            sb.append(stringWriter.toString());
            result = sb.toString();
        } else {
            result = TblUtil.genMd(connection, datasource.getDriver(), null, language);

        }
        connection.close();
        return result;
    }

    @RequestMapping(value = "/{id}/export", method = RequestMethod.GET)
    public void dump(HttpServletResponse response, @PathVariable(value = "id") Integer id) throws Exception {
        Datasource datasource = datasourceService.findOne(id);
        MysqlExport mysqlExport = new MysqlExport(datasource.getJdbcUrl(), datasource.getUsername(), datasource.getPassword(), null);
//        response.setContentType("text/plain;charset=utf-8");
        String name = (datasource.getName() == null ? datasource.getId() : datasource.getName()) + ".sql";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(name, "UTF-8") + "\"");
        response.setContentType("application/octet-stream;charset=UTF-8");
        mysqlExport.dump(response.getOutputStream());
    }


    @Value("${execute-sql.url:jdbc:mysql://localhost:3306/}")
    private String url;

    @Value("${execute-sql.username:root}")
    private String username;

    @Value("${execute-sql.password:Visualchina123}")
    private String password;

    @RequestMapping(value = "/executeSql", method = RequestMethod.POST)
    public void executeSql(MultipartFile file, HttpServletResponse response) throws SQLException, IOException {
        Connection connection = DriverManager.getConnection(url + "mysql", username, password);
        String db = ("SC" + UUID.randomUUID().toString().substring(0, 5)).toLowerCase();
        List<String> sqls = Arrays.asList("CREATE DATABASE IF NOT EXISTS " + db + " DEFAULT CHARSET utf8",
                "CREATE USER '" + db + "'@'%' IDENTIFIED BY '" + db + "'",
                "grant all privileges on " + db + ".* to '" + db + "'@'%' identified by '" + db + "'");
        for (String sql : sqls) {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
        }

        connection.close();

        String jdbcUrl = url + db;
        connection = DriverManager.getConnection(url, db, db);
        ScriptUtils.executeSqlScript(connection, new InputStreamResource(file.getInputStream()));
        connection.close();

        datasourceService.save(new Datasource()
                .setJdbcUrl(jdbcUrl)
                .setName(file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf(".")))
                .setUsername(db)
                .setPassword(db)
                .setDriver("com.mysql.jdbc.Driver"));
        response.sendRedirect("/");
    }


    private void download(HttpServletResponse response, AbstractGenerator abstractGenerator, String fileName) throws Exception {
        String zipFile = tempPath + new Date().getTime() + ".zip";
        FileUtil.zip(abstractGenerator.generator().getAbsolutePath(), zipFile);
        FileInputStream in = new FileInputStream(zipFile);
        FileUtil.down(response, in, fileName + ".zip");
        in.close();
    }
}
