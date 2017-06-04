package com.github.sc.gennerator;

import com.alibaba.fastjson.JSON;
import com.github.sc.gennerator.jpa.AbstractGeneratorJpa;
import com.github.sc.common.utils.TblUtil;
import com.github.sc.common.utils.VelocityUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by wuyu on 2015/3/29.
 */
public abstract class AbstractGenerator {

    protected String project;

    protected String packagePath;

    protected String applicationName = "";

    protected String username;

    protected String password;

    protected String url;

    protected String driverClass;

    protected List<String> tables;

    protected String serviceProject = "";

    protected String serviceApiProject = "";

    protected String parentProject = "";

    protected String uiProject = "";

    protected String springBootVersion = "1.5.3.RELEASE";

    protected String springCloudVersion = "Dalston.RELEASE";

    protected String tempPath = System.getProperty("java.io.tmpdir");

    protected String basePath = (tempPath + "/" + new Date().getTime() + "/");

    protected Map<String, Object> options = new HashMap<>();

    protected Logger logger = LoggerFactory.getLogger(AbstractGeneratorJpa.class);


    public AbstractGenerator(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClass = driverClass;
        this.project = project;
        this.packagePath = packagePath;
        this.tables = tables;
        for (String s : project.split("-")) {
            this.applicationName += VelocityUtil.firstUp(s);
        }
        this.applicationName += "Application";
    }

    protected void write(String code, String dest) throws IOException {
        if (StringUtils.isBlank(code)) {
            return;
        }
        File file = new File(dest);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(out, "utf-8");
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(code);

        }
    }


    public abstract File generator() throws Exception;

    protected Connection getConnection(String url, String username, String password, String driverClass) throws SQLException, ClassNotFoundException {
        Class.forName(driverClass);
        return DriverManager.getConnection(url, username, password);
    }

    protected Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName(driverClass);
        return DriverManager.getConnection(url, username, password);
    }

    protected String getBasePath() {
        File file = new File(basePath);
        if (!file.exists()) file.mkdirs();
        return basePath;
    }


    protected String generatorPath() {
        this.basePath = (tempPath + "/" + new Date().getTime() + "/");
        new File(basePath).mkdirs();
        return basePath;
    }

    protected String getJavaType(List<MetaData> metaDatas, String columnName) {
        for (MetaData metaData : metaDatas) {
            if (metaData.getColumnName().equalsIgnoreCase(columnName)) {
                return metaData.getTypeName();
            }
        }
        return null;
    }

    protected void writeDockerfile(String srcDockerfile, String destDockerfile, String project, String packagePath) throws IOException {
        Template tempate = VelocityUtil.getTempate(srcDockerfile);
        VelocityContext ctx = new VelocityContext();
        ctx.put("package", packagePath);
        ctx.put("project", project);
        ctx.put("username", this.username);
        ctx.put("password", this.password);
        String host = url.substring(0, url.lastIndexOf("/")).substring(url.substring(0, url.lastIndexOf("/")).lastIndexOf("/") + 1);

        int port = 3306;
        String database = project;
        try {
            database = url.substring(url.lastIndexOf("/") + 1);
            if (host.contains(":")) {
                String[] split = host.split(":");
                port = Integer.parseInt(split[1]);
            }
        } catch (Exception e) {

        }
        ctx.put("port",port);
        ctx.put("database",database);
        ctx.put("application", packagePath + "." + this.applicationName);
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        writer.close();
        write(writer.toString(), destDockerfile);
    }


    protected void writeMain(String srcMain, String destMain, String project, String packagePath) throws IOException {
        Template tempate = VelocityUtil.getTempate(srcMain);
        VelocityContext ctx = new VelocityContext();
        ctx.put("packagePath", packagePath);
        ctx.put("project", project);
        ctx.put("application", this.applicationName);
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        writer.close();
        write(writer.toString(), destMain);
    }

    protected void writeIgnore(String dest) throws IOException {
        Template tempate = VelocityUtil.getTempate("/templates/pom/springboot/gitignore");
        VelocityContext ctx = new VelocityContext();
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        writer.close();
        write(writer.toString(), dest);
    }


    protected void writeLogger(String dest) throws IOException {
        Template tempate = VelocityUtil.getTempate("/templates/pom/springboot/logback-spring.xml");
        VelocityContext ctx = new VelocityContext();
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        writer.close();
        write(writer.toString(), dest + "/logback-spring.xml");
    }


    protected void writeBin(String destDir, String project, String packagePath) throws IOException {
        String binPath = getBinPath();
        List<String> files = getBinFiles();
        for (String fileName : files) {
            String file = binPath + fileName;
            Template template = VelocityUtil.getTempate(file);
            VelocityContext ctx = new VelocityContext();
            ctx.put("packagePath", packagePath);
            ctx.put("project", project);
            if (file.contains("shutdown")) {
                ctx.put("application", this.applicationName);

            } else {
                ctx.put("application", packagePath + "." + this.applicationName);
            }
            StringWriter writer = new StringWriter();
            template.merge(ctx, writer);
            writer.close();
            write(writer.toString(), destDir + fileName);
        }
    }

    protected List<String> getBinFiles() {
        return Arrays.asList("debug.sh", "installToSystem.sh", "shutdown.sh", "startup.bat", "startup.sh", "test.sh");
    }

    protected String getBinPath() {
        return "/templates/bin/";
    }

    protected void writeMaven(String destDir, String project, String packagePath) throws IOException {

        String binDir = "/templates/mvn/";

        new File(destDir + "/.mvn/wrapper/").mkdirs();


        try (FileOutputStream wrapperOut = new FileOutputStream(destDir + "/.mvn/wrapper/maven-wrapper.jar");
             FileOutputStream propertiesOut = new FileOutputStream(destDir + "/.mvn/wrapper/maven-wrapper.properties");
             FileOutputStream mvnwOut = new FileOutputStream(destDir + "/mvnw");
             FileOutputStream mvnwCmdOut = new FileOutputStream(destDir + "/mvnw.cmd")) {

            ClassPathResource wrapper = new ClassPathResource(binDir + "wrapper/maven-wrapper.jar");
            ClassPathResource properties = new ClassPathResource(binDir + "wrapper/maven-wrapper.properties");


            ClassPathResource mvnw = new ClassPathResource(binDir + "mvnw");
            ClassPathResource mvnwCmd = new ClassPathResource(binDir + "mvnw.cmd");


            IOUtils.copy(wrapper.getInputStream(), wrapperOut);
            IOUtils.copy(properties.getInputStream(), propertiesOut);
            IOUtils.copy(mvnw.getInputStream(), mvnwOut);
            IOUtils.copy(mvnwCmd.getInputStream(), mvnwCmdOut);
        }
    }


    protected void writeApplicationConfig(String srcApplication, String destApplication, String project, String packagePath) throws IOException {
        Template tempate = VelocityUtil.getTempate(srcApplication);
        VelocityContext ctx = new VelocityContext();
        ctx.put("project", project);
        ctx.put("driverClass", driverClass);
        ctx.put("username", username);
        ctx.put("password", password);
        ctx.put("packagePath", packagePath);
        ctx.put("url", url);
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        writer.close();
        write(writer.toString(), destApplication);
    }


    protected void writeApplicationConfigs(String destApplicationPath, String project, String packagePath) throws IOException {

        String applicationPath = getSrcApplicationFilesPath();
        List<String> applicationFiles = getApplicationFiles();
        for (String filename : applicationFiles) {
            if (filename.contains("/")) {
                String parentPath = filename.substring(0, filename.lastIndexOf("/"));
                new File(destApplicationPath + "/" + parentPath).mkdirs();
            }

            Template tempate = VelocityUtil.getTempate((applicationPath + "/" + filename).replaceAll("[/]+", "/"));
            VelocityContext ctx = new VelocityContext();
            ctx.put("project", project);
            ctx.put("driverClass", driverClass);
            ctx.put("username", username);
            ctx.put("password", password);
            ctx.put("packagePath", packagePath);
            ctx.put("realPath", packagePath.replace(".", "/"));
            ctx.put("url", url);
            StringWriter writer = new StringWriter();
            tempate.merge(ctx, writer);
            writer.close();
            write(writer.toString(), destApplicationPath + "/" + filename);
        }
    }

    protected String getSrcApplicationFilesPath() {
        return "/templates/pom/springboot/conf/";
    }

    protected List<String> getApplicationFiles() {
        return Arrays.asList("application.yml");
    }


    protected void writePom(String srcPom, String destPom, String project, String packagePath) throws IOException {
        Template tempate = VelocityUtil.getTempate(srcPom);
        VelocityContext ctx = new VelocityContext();
        ctx.put("packagePath", packagePath);
        ctx.put("project", project);
        ctx.put("application", packagePath + "." + this.applicationName);
        ctx.put("driverClass", driverClass);
        ctx.put("serviceProject", serviceProject);
        ctx.put("serviceApiProject", serviceApiProject);
        ctx.put("parentProject", parentProject);
        ctx.put("springCloudVersion", springCloudVersion);
        ctx.put("springBootVersion", springBootVersion);
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            ctx.put(entry.getKey(), entry.getValue());
        }
        StringWriter writer = new StringWriter();
        tempate.merge(ctx, writer);
        writer.close();
        write(writer.toString(), destPom);
    }


    protected String merge(Template template, VelocityContext ctx) throws IOException {
        StringWriter writer = new StringWriter();
        template.merge(ctx, writer);
        writer.close();
        return writer.toString();
    }

    protected List<String> getTables(Connection connection) throws Exception {
        if (tables == null || tables.size() == 0) {
            tables = TblUtil.getTbls(connection);
        }
        if (tables.size() == 0) throw new RuntimeException("can not find table");
        return tables;
    }



    protected String getDockerFile() {
        return "/templates/pom/springboot/Dockerfile";
    }

    protected String getDockerFile2() {
        return "/templates/pom/springboot/Dockerfile2";
    }

    protected String getDockerCompose() {
        return "/templates/pom/springboot/docker_compose/docker-compose.yml";
    }

    public Map<String, Object> getOptions() {
        return options;
    }


}
