package com.github.sc.gennerator.mybatis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wuyu on 2016/1/30.
 */
public class SpringBootDubboBaseGeneratorMybatis extends SpringBootBaseGeneratorMybatis {

    public SpringBootDubboBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, String baseCPackagePrefix, boolean multiProject,boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, baseCPackagePrefix, multiProject,swagger);
    }

    @Override
    protected void writePom(String srcPom, String destPom, String project, String packagePath) throws IOException {
        this.options.put("dubbo", true);
        super.writePom(srcPom, destPom, project, packagePath);
    }

    @Override
    protected String getSrcApplicationFilesPath() {
        return "/templates/pom/springboot/conf/dubbo";
    }

    @Override
    protected List<String> getApplicationFiles() {
        return Arrays.asList("application.yml");
    }
}
