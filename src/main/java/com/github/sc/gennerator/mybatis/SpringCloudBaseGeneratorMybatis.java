package com.github.sc.gennerator.mybatis;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wuyu on 2017/3/30.
 */
public class SpringCloudBaseGeneratorMybatis extends SpringBootBaseGeneratorMybatis {


    public SpringCloudBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, String baseCPackagePrefix, boolean multiProject,boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, baseCPackagePrefix, multiProject,swagger);
    }

    @Override
    protected String getSrcParentProjectPom() {
        return "/templates/pom/springcloud/parentPom.vm";
    }


    @Override
    protected String getSrcServiceProjectPom() {
        return "/templates/pom/springcloud/servicePom.vm";
    }

    @Override
    protected String getSrcInterfaceProjectPom() {
        return "/templates/pom/springcloud/interfacePom.vm";
    }

    @Override
    protected String getSrcPomFilePath() {
        return "/templates/pom/springcloud/singlePom.vm";
    }

    @Override
    protected String getMainPath() {
        return "/templates/pom/springcloud/Main.vm";
    }

    @Override
    protected String getSrcApplicationFilesPath() {
        return "/templates/pom/springcloud/conf/";
    }

    @Override
    protected List<String> getApplicationFiles() {
        return Arrays.asList("application.yml",
                "application-dev.yml",
                "application-prod.yml",
                "application-test.yml",
                "bootstrap.yml"
        );
    }
}
