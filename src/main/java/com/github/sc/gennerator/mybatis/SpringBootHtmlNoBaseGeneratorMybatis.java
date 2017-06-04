package com.github.sc.gennerator.mybatis;

import com.github.sc.common.utils.VelocityUtil;
import com.github.sc.gennerator.MetaData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wuyu on 2016/1/30.
 */
public class SpringBootHtmlNoBaseGeneratorMybatis extends SpringBootAngular1NoBaseGeneratorMybatis {


    public SpringBootHtmlNoBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, boolean multiProject, boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, multiProject, swagger);
    }

    @Override
    protected String getUISrcFilePath() {
        return "/templates/pom/ui/html/view.vm";
    }

    protected List<String> getUIComponentFilesPath() {
        return new ArrayList<>();
    }

    @Override
    protected void writeUIOther(String destResourcePath, List<String> tables, List<String> modelNames, List<String> varModelNames) throws IOException {
        Template indexHtml = VelocityUtil.getTempate("/templates/pom/ui/html/index.vm");
        VelocityContext ctx = new VelocityContext();
        ctx.put("modelNames", modelNames);
        ctx.put("tables", tables);
        ctx.put("varModelNames", varModelNames);
        ctx.put("project", VelocityUtil.tableNameConvertModelName(this.project.split("-")[0]));
        ctx.put("miniProject", VelocityUtil.tableNameConvertModelName(this.project.split("-")[0]).substring(0, 1));
        write(merge(indexHtml, ctx), destResourcePath + "/static/index.html");
    }


}
