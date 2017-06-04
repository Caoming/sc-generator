package com.github.sc.gennerator.mybatis;

import com.github.sc.gennerator.MetaData;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wuyu on 2015/6/11.
 */
public class DubboBaseGeneratorMybatis extends SpringBootBaseGeneratorMybatis {


    public DubboBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, String baseCPackagePrefix, boolean multiProject,boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, baseCPackagePrefix, multiProject,swagger);
    }

    @Override
    protected String getSrcPomFilePath() {
        return "/templates/pom/dubbo/singlePom.vm";
    }

    @Override
    protected String getSrcApplicationFilesPath() {
        return "/templates/pom/dubbo/conf/";
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
                "applicationContext.xml",
                "log4j.properties"
        );
    }

    @Override
    protected String getSrcParentProjectPom() {
        return "/templates/pom/dubbo/parentPom.vm";
    }

    @Override
    protected String getSrcServiceProjectPom() {
        return "/templates/pom/dubbo/servicePom.vm";
    }

    @Override
    protected void writeLogger(String dest) throws IOException {
    }

    @Override
    protected String getSrcInterfaceProjectPom() {
        return "/templates/pom/dubbo/interfacePom.vm";
    }

    @Override
    protected String getMainPath() {
        return "/templates/pom/dubbo/Main.vm";
    }

    @Override
    protected String genController(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        return null;
    }
}
