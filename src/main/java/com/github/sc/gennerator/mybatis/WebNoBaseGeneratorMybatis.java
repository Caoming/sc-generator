package com.github.sc.gennerator.mybatis;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wuyu on 2015/1/1.
 */
public class WebNoBaseGeneratorMybatis extends SpringBootNoBaseGeneratorMybatis {


    public WebNoBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, false, swagger);
    }

    @Override
    protected String getSrcPomFilePath() {
        return "/templates/pom/web/pom.vm";
    }


    @Override
    protected String getSrcApplicationFilesPath() {
        return "/templates/pom/web/resources";
    }

    @Override
    protected List<String> getApplicationFiles() {
        return Arrays.asList("/config/anotation.xml",
                "/config/dubbo-service.xml",
                "/config/jdbc.xml",
                "/config/mybatis.xml",
                "/config/property.xml",
                "/config/redis.xml",
                "/config/transation.xml",
                "/config/restful.xml",
                "/properties/db.properties",
                "/properties/dubbo.properties",
                "/properties/redis.properties",
                "springmvc.xml",
                "applicationContext.xml",
                "log4j.properties"
        );
    }

    @Override
    protected void writeLogger(String dest) throws IOException {

    }

    @Override
    protected String getDockerFile() {
        return "/templates/pom/web/Dockerfile";
    }


    @Override
    protected List<String> getBinFiles() {
        return Arrays.asList("shutdown.sh", "startup.bat", "startup.sh");
    }

    protected String getBinPath() {
        return "/templates/pom/web/bin/";
    }

    @Override
    protected void writeMain(String srcMain, String destMain, String project, String packagePath) throws IOException {

    }

    @Override
    protected void writeWebXml() throws IOException {
        File file = new File(getDestProjectPath() + "/src/main/webapp/WEB-INF/");
        file.mkdirs();
        InputStream in = new ClassPathResource("/templates/pom/web/webapp/WEB-INF/web.xml").getInputStream();
        FileOutputStream out = new FileOutputStream(file.getAbsolutePath() + "/web.xml");
        org.apache.commons.io.IOUtils.copy(in, out);
        out.close();
        in.close();
    }

}
