package com.github.sc.gennerator.mybatis;

import com.github.sc.gennerator.MetaData;
import com.github.sc.common.utils.VelocityUtil;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.util.List;

/**
 * Created by wuyu on 2016/1/30.
 */
public class SpringBootNoBaseGeneratorMybatis extends AbstractGeneratorMybatis {


    public SpringBootNoBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, boolean multiProject,boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables,null, false, multiProject,swagger);
    }

    @Override
    protected String genService(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/springboot/java/mybatis/nobase/Service.vm");
        return merge(template, ctx);
    }

    @Override
    protected String genServiceImpl(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/springboot/java/mybatis/nobase/ServiceImpl.vm");
        return merge(template, ctx);
    }

    @Override
    protected String genController(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/springboot/java/mybatis/nobase/Controller.vm");
        return merge(template, ctx);
    }


}
