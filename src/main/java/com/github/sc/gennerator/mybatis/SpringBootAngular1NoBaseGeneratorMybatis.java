package com.github.sc.gennerator.mybatis;

import com.github.sc.common.utils.VelocityUtil;
import com.github.sc.gennerator.MetaData;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by wuyu on 2016/1/30.
 */
public class SpringBootAngular1NoBaseGeneratorMybatis extends SpringBootNoBaseGeneratorMybatis {


    public SpringBootAngular1NoBaseGeneratorMybatis(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables, boolean multiProject, boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables, multiProject, swagger);
    }

    @Override
    protected void writePom(String srcPom, String destPom, String project, String packagePath) throws IOException {
        super.writePom(srcPom, destPom, project, packagePath);
    }

    @Override
    protected String getUIDestDirectoryPath() {
        return "/static";
    }

    @Override
    protected String getUISrcFilePath() {
        return "/templates/pom/ui/angular1/view.vm";
    }

    @Override
    protected String getSrcPomFilePath() {
        return "/templates/pom/ui/angular1/singlePom.vm";
    }

    @Override
    protected String genController(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/ui/angular1/Controller.vm");
        return merge(template, ctx);
    }

    @Override
    protected String getUIComponentFilesDestDirectoryPath() {
        return "/static/js/";
    }

    @Override
    protected List<String> getUIComponentFilesPath() {
        return Collections.singletonList("/templates/pom/ui/angular1/view.js");
    }

    @Override
    protected void writeUIOther(String destResourcePath, List<String> tables, List<String> modelNames, List<String> varModelNames) throws IOException {
        Template indexHtml = VelocityUtil.getTempate("/templates/pom/ui/angular1/index.vm");
        Template indexJs = VelocityUtil.getTempate("/templates/pom/ui/angular1/indexjs.vm");
        Template confirmModal = VelocityUtil.getTempate("/templates/pom/ui/angular1/confirm_modal.html");
        Template messageModal = VelocityUtil.getTempate("/templates/pom/ui/angular1/message_modal.html");
        VelocityContext ctx = new VelocityContext();
        ctx.put("modelNames", modelNames);
        ctx.put("tables", tables);
        ctx.put("varModelNames", varModelNames);
        ctx.put("project", this.project);
        ctx.put("project", VelocityUtil.tableNameConvertModelName(this.project.split("-")[0]));
        ctx.put("miniProject", VelocityUtil.tableNameConvertModelName(this.project.split("-")[0]).substring(0, 1));
        write(merge(indexHtml, ctx), destResourcePath + "/static/index.html");
        write(merge(indexJs, ctx), destResourcePath + "/static/js/app.js");
        write(merge(confirmModal, ctx), destResourcePath + "/static/confirm_modal.html");
        write(merge(messageModal, ctx), destResourcePath + "/static/message_modal.html");
    }

}
