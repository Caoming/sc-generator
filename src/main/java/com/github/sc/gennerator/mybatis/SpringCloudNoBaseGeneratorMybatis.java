package com.github.sc.gennerator.mybatis;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wuyu on 2016/1/30.
 */
public class SpringCloudNoBaseGeneratorMybatis extends SpringBootNoBaseGeneratorMybatis {


    public SpringCloudNoBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, boolean multiProject,boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, multiProject,swagger);
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
        return "/templates/pom/springcloud/conf";
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
