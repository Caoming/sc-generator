package com.github.sc.gennerator.mybatis;

import com.github.sc.gennerator.AbstractGenerator;
import com.github.sc.gennerator.MetaData;
import com.github.sc.common.utils.TblUtil;
import com.github.sc.common.utils.VelocityUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.springframework.stereotype.Component;

import java.io.*;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by wuyu on 2015/1/29.
 */
public abstract class AbstractGeneratorMybatis extends AbstractGenerator {

    private String baseCPackagePrefix = "com.vcg.common.base";

    private static List<String> drivers = Arrays.asList("net.sourceforge.jtds.jdbc.Driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "com.mysql.jdbc.Driver", "org.mariadb.jdbc.Driver");

    private String baseDao = baseCPackagePrefix + ".BaseDao";
    private String baseModel = baseCPackagePrefix + ".BaseModel";
    private String baseService = baseCPackagePrefix + ".BaseService";
    private String baseRestService = baseCPackagePrefix + ".BaseRest";
    private String baseRestServiceImpl = baseCPackagePrefix + ".BaseRestImpl";
    private String baseServiceImpl = baseCPackagePrefix + ".BaseServiceImpl";
    private String baseExample = baseCPackagePrefix + ".BaseExample";

    private boolean extendsBase = false;
    private boolean multilProject = false;

    private boolean swagger = false;

    public AbstractGeneratorMybatis(String url, String username, String password, String driverClass,
                                    String project, String packagePath, List<String> tables, String baseCPackagePrefix, boolean extendsBase, boolean multiProject, boolean swagger) {
        super(url, username, password, driverClass, project, packagePath, tables);
        if (baseCPackagePrefix != null) {
            this.baseCPackagePrefix = baseCPackagePrefix;
            this.baseDao = baseCPackagePrefix + ".BaseDao";
            this.baseModel = baseCPackagePrefix + ".BaseModel";
            this.baseService = baseCPackagePrefix + ".BaseService";
            this.baseRestService = baseCPackagePrefix + ".BaseRest";
            this.baseRestServiceImpl = baseCPackagePrefix + ".BaseRestImpl";
            this.baseServiceImpl = baseCPackagePrefix + ".BaseServiceImpl";
            this.baseExample = baseCPackagePrefix + ".BaseExample";
        }
        this.extendsBase = extendsBase;
        this.multilProject = multiProject;
        this.serviceProject = project + "-service";
        this.serviceApiProject = project + "-api";
        this.uiProject = project + "-ui";
        this.parentProject = project;
        this.swagger = swagger;
    }

    @Override
    public File generator() throws Exception {
        Connection connection = getConnection();
        List<String> tables = getTables(connection);
        List<String> modelNames = new ArrayList<>();
        List<String> varModelNames = new ArrayList<>();

        String destProject = getDestProjectPath();
        String destSourcePath = getJavaSourcePath(destProject);
        String destResourcePath = getResourcesPath(destProject);

        if (multilProject) {
            destProject = getDestServiceProjectPath();
            String interfaceProject = getDestServiceInterfaceProjectPath();
            destSourcePath = getJavaSourcePath(destProject);
            destResourcePath = getResourcesPath(destProject);

            new File(interfaceProject + "/src/main/java/").mkdirs();

            writeBin(getDestProjectPath(), this.serviceProject, packagePath);
            writePom(getSrcServiceProjectPom(), getDestServicePom(), project, packagePath);
            writePom(getSrcInterfaceProjectPom(), getDestServiceInterfacePom(), project, packagePath);
            writePom(getSrcParentProjectPom(), getDestParentProjectPom(), project, packagePath);
            writeLogger(destResourcePath);
            String destDockerFilePath = getDestServiceProjectPath() + "Dockerfile";
            writeDockerfile(getDockerFile(), destDockerFilePath, project, packagePath);
            writeDockerfile(getDockerFile2(), this.getDestDockerFilePath(), project, packagePath);
            writeDockerfile(getDockerCompose(), this.getDestDockerComposePath(), project, packagePath);

        } else {
            String destDockerFilePath = getDestDockerFilePath();
            writeDockerfile(getDockerFile(), destDockerFilePath, this.serviceProject, packagePath);
            writeDockerfile(getDockerCompose(), this.getDestDockerComposePath(), project, packagePath);
            writePom(getSrcPomFilePath(), getDestPomFilePath(), project, packagePath);
            writeBin(getDestProjectPath(), "", packagePath);
            writeLogger(destResourcePath);
        }

        new File(destProject).mkdirs();
        new File(destSourcePath).mkdirs();
        new File(destResourcePath).mkdirs();
        new File(destResourcePath + "/templates").mkdirs();
        new File(destResourcePath + "/static").mkdirs();
        String configStr = VelocityUtil.writerGeneratorConfig(url, username, password, driverClass, packagePath, destSourcePath, tables);


        List<String> warnings = new ArrayList<>();
        ConfigurationParser cp = new ConfigurationParser(warnings);
        Configuration config = cp.parseConfiguration(new InputStreamReader(new ByteArrayInputStream(configStr.getBytes())));
        DefaultShellCallback callback = new DefaultShellCallback(true);
        MapperGenerator myBatisGenerator = new MapperGenerator(config, callback, warnings);
        myBatisGenerator.setBaseDao(baseDao);
        myBatisGenerator.setBaseModel(baseModel);
        myBatisGenerator.setBaseExample(baseExample);
        myBatisGenerator.setExtendsBase(extendsBase);
        myBatisGenerator.setSwagger(swagger);
        myBatisGenerator.setSupport(drivers.contains(driverClass));
        myBatisGenerator.generate(null, null, null);

        copyMybatisXml(destSourcePath, destResourcePath);


        for (String tableName : tables) {

            List<MetaData> metaDates = TblUtil.getMetaDates(connection, tableName);
            String modelName = VelocityUtil.tableNameConvertModelName(tableName);
            String columnKey = TblUtil.getPrimaryKey(connection, tableName);
            String primaryKey = VelocityUtil.toHump(columnKey);
            String primaryType = "Object";
            String varModelName = VelocityUtil.firstLow(modelName);
            if (StringUtils.isNotBlank(columnKey)) {
                primaryType = TblUtil.getJavaType(connection, tableName, columnKey);
            }
            modelNames.add(modelName);
            varModelNames.add(varModelName);
            String destServicePath = getDestServicePath(destSourcePath, modelName, primaryKey, primaryType);
            String destControllerPath = getDestControllerPath(destSourcePath, modelName, primaryKey, primaryType);
            String destServiceImplPath = getDestServiceImplPath(destSourcePath, modelName, primaryKey, primaryType);

            VelocityContext ctx = new VelocityContext();
            ctx.put("packagePath", packagePath);
            ctx.put("modelName", modelName);
            ctx.put("primaryKey", primaryKey);
            ctx.put("primaryType", primaryType);
            ctx.put("varModelName", varModelName);
            ctx.put("commonPath", baseCPackagePrefix);
            ctx.put("createdTime", new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()));
            ctx.put("application", applicationName);
            ctx.put("tableName", tableName);

            String service;
            String serviceImpl;
            String controller;

            List<String> importedKeys = TblUtil.getImportedKeys(connection, tableName);

            if (drivers.contains(driverClass) && importedKeys.size() == 0 && TblUtil.getPrimaryKeys(connection, tableName).size() == 1) {
                service = genService(ctx, destServicePath, destSourcePath, packagePath, metaDates, primaryKey, primaryType, modelName);
                serviceImpl = genServiceImpl(ctx, destServiceImplPath, destSourcePath, packagePath, metaDates, primaryKey, primaryType, modelName);
                controller = genController(ctx, destControllerPath, destSourcePath, packagePath, metaDates, primaryKey, primaryType, modelName);
                if (extendsBase) {
                    writeCommon(destSourcePath);
                }
            } else {
                service = genEmptyService(ctx, destServicePath, destSourcePath, packagePath, metaDates, primaryKey, primaryType, modelName);
                serviceImpl = genEmptyServiceImpl(ctx, destServiceImplPath, destSourcePath, packagePath, metaDates, primaryKey, primaryType, modelName);
                controller = genEmptyController(ctx, destControllerPath, destSourcePath, packagePath, metaDates, primaryKey, primaryType, modelName);
            }
            write(service, destServicePath);
            write(serviceImpl, destServiceImplPath);
            write(controller, destControllerPath);
            writeUI(destResourcePath, ctx, tableName, primaryKey, metaDates);
        }
        writeConfiguration(destSourcePath, project, packagePath);
        new File(destProject + "/src/test/java").mkdirs();
        String destMainPath = getDestMainPath(destProject);

        writeUIOther(destResourcePath, tables, modelNames, varModelNames);
        writeMain(getMainPath(), destMainPath, destProject, packagePath);
        writeIgnore(getDestProjectPath() + "/gitignore");
        writeMaven(getDestProjectPath(), destProject, packagePath);
        writeApplicationConfigs(destResourcePath, project, packagePath);
        writeWebXml();
        TblUtil.genMd(connection,driverClass,getDestProjectPath(),null);
        if (drivers.contains("com.mysql.jdbc.Driver") || drivers.contains("org.mariadb.jdbc.Driver")) {
            //暂时只支持mysql建表语句
            writeSql(destProject, tables);
        }
        logger.info(basePath);
        connection.close();
        return new File(basePath);
    }

    protected void writeCommon(String destProject) throws IOException {
        String commonBasePath = getSrcCommonPath();
        for (String filename : getSrcCommonFiles()) {
            String vmFile = (commonBasePath + "/" + filename).replaceAll("[/]+", "/");
            VelocityContext ctx = new VelocityContext();
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                ctx.put(entry.getKey(), entry.getValue());
            }
            ctx.put("commonPath", baseCPackagePrefix);
            String destFileName = getDestCommonPath(destProject) + filename.replace(".vm", ".java");
            Template tempate = VelocityUtil.getTempate(vmFile);
            String merge = merge(tempate, ctx);
            write(merge, destFileName);
        }
    }

    protected void writeConfiguration(String destProject, String project, String packagePath) throws IOException {
        String configurationPath = getSrcConfigurationPath();
        if (configurationPath == null) return;

        for (String filename : getSrcConfigurationFiles()) {
            String vmFile = (configurationPath + "/" + filename).replaceAll("[/]+", "/");
            VelocityContext ctx = new VelocityContext();
            ctx.put("packagePath", packagePath);
            ctx.put("project", project);
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                ctx.put(entry.getKey(), entry.getValue());
            }
            String destFileName = destProject + "/" + (packagePath + "/config/").replace(".", "/") + "/" + filename.replace(".vm", ".java");
            Template tempate = VelocityUtil.getTempate(vmFile);
            String merge = merge(tempate, ctx);
            write(merge, destFileName);
        }
    }


    protected void writeWebXml() throws IOException {
    }

    protected abstract String genService(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception;

    protected abstract String genServiceImpl(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception;

    protected abstract String genController(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception;

    protected String genEmptyService(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/springboot/java/mybatis/empty/Service.vm");
        return merge(template, ctx);
    }

    protected String genEmptyServiceImpl(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/springboot/java/mybatis/empty/ServiceImpl.vm");
        return merge(template, ctx);
    }

    protected String genEmptyController(VelocityContext ctx, String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) throws Exception {
        Template template = VelocityUtil.getTempate("/templates/pom/springboot/java/mybatis/empty/Controller.vm");
        return merge(template, ctx);
    }


    protected String getDestServicePath(String destSource, String modelName, String primaryKey, String primaryType) {
        return destSource + packagePath.replace(".", "/") + "/service/" + modelName + "Service." + getExt();
    }

    protected String getDestServiceImplPath(String destSource, String modelName, String primaryKey, String primaryType) {
        return destSource + packagePath.replace(".", "/") + "/service/impl/" + modelName + "ServiceImpl." + getExt();
    }

    protected String getDestControllerPath(String destSource, String modelName, String primaryKey, String primaryType) {
        return destSource + packagePath.replace(".", "/") + "/web/" + modelName + "Controller." + getExt();
    }

    protected String getDestCommonPath(String destPath) {
        return destPath + baseCPackagePrefix.replace(".", "/") + "/";
    }

    protected String getSrcCommonPath() {
        return "/templates/pom/springboot/java/mybatis/hasbase/base/";
    }

    protected String getSrcConfigurationPath() {
        return null;
    }

    protected List<String> getSrcConfigurationFiles() {
        return new ArrayList<>();
    }

    protected List<String> getSrcCommonFiles() {
        return Arrays.asList("BaseDao.vm", "BaseExample.vm", "BaseModel.vm", "BaseRest.vm", "BaseRestImpl.vm", "BaseService.vm", "BaseServiceImpl.vm");

    }


    protected String getDestDockerFilePath() {
        return basePath + "/" + project + "/Dockerfile";
    }

    protected String getDestDockerComposePath() {
        return basePath + "/" + project + "/docker-compose.yml";
    }

    protected String getDestPomFilePath() {
        return basePath + "/" + project + "/pom.xml";
    }

    protected String getSrcPomFilePath() {
        return "/templates/pom/springboot/java/mybatis/singlePom.vm";
    }

    protected String getSrcApplicationFilesPath() {
        return "/templates/pom/springboot/conf";
    }


    protected String getDestProjectPath() {
        return basePath + "/" + project + "/";
    }


    protected String getDestServiceProjectPath() {
        return basePath + "/" + this.project + "/" + this.serviceProject + "/";
    }

    protected String getDestServicePom() {
        return getDestServiceProjectPath() + "/pom.xml";
    }

    protected String getDestServiceInterfacePom() {
        return getDestServiceInterfaceProjectPath() + "/pom.xml";
    }


    protected String getSrcServiceProjectPom() {
        return "/templates/pom/springboot/java/mybatis/servicePom.vm";
    }

    protected String getDestServiceInterfaceProjectPath() {
        return basePath + "/" + this.project + "/" + this.serviceApiProject + "/";
    }

    protected String getSrcInterfaceProjectPom() {
        return "/templates/pom/springboot/java/mybatis/interfacePom.vm";
    }


    protected String getDestParentProjectPath() {
        return basePath + "/" + this.parentProject + "/";
    }

    protected String getSrcParentProjectPom() {
        return "/templates/pom/springboot/java/mybatis/parentPom.vm";
    }

    protected String getDestParentProjectPom() {
        return getDestParentProjectPath() + "/pom.xml";
    }


    protected String getDestMainPath(String dest) {
        return getJavaSourcePath(dest) + packagePath.replace(".", "/") + "/" + applicationName + "." + getExt();
    }

    protected String getExt() {
        return "java";
    }

    protected String getMainPath() {
        return "/templates/pom/springboot/Main.vm";
    }

    public String getJavaSourcePath(String destProject) {
        return destProject + "/src/main/java/";
    }

    public String getResourcesPath(String destProject) {
        return destProject + "/src/main/resources/";
    }

    protected void copyMybatisXml(String src, String dest) throws IOException {
        final List<File> oldXmlFile = new ArrayList<File>();
        FileUtils.copyDirectory(new File(src + packagePath.replace(".", "/") + "/dao"),
                new File(dest + "/" + packagePath.replace(".", "/") + "/dao"), new FileFilter() {
                    public boolean accept(File pathname) {
                        int lastIndexOf = pathname.getName().lastIndexOf(".xml");
                        if (lastIndexOf != -1) {
                            oldXmlFile.add(pathname);
                        }
                        return lastIndexOf != -1;
                    }
                });
        for (File file : oldXmlFile) {
            file.delete();
        }
    }




    protected void writeSql(String destProject, List<String> tables) throws Exception {
        String path = getResourcesPath(destProject) + "/db/migration/V1__" + project + ".sql";
        String createDatabaseSql = TblUtil.getCreateDatabaseSql(this.getConnection());
        String createTblSql = TblUtil.getCreateTblSql(this.getConnection(), tables);
        write(createDatabaseSql+createTblSql, path);
    }


    protected String getUISrcFilePath() {
        return null;
    }

    protected String getUIDestDirectoryPath() {
        return null;
    }

    protected List<String> getUIComponentFilesPath() {
        return new ArrayList<>();
    }

    protected String getUIComponentFilesDestDirectoryPath() {
        return null;
    }

    protected void writeUI(String destResourcePath, VelocityContext ctx, String tableName, String primaryKey, List<MetaData> metaDates) throws IOException {
        String uiSrcFilePath = getUISrcFilePath();
        if (uiSrcFilePath == null) {
            return;
        }
        Template template = VelocityUtil.getTempate(uiSrcFilePath);
        List<String> filedNames = new ArrayList<>();
        List<String> varFiledNames = new ArrayList<>();
        for (MetaData metaDate : metaDates) {
            filedNames.add(VelocityUtil.tableNameConvertModelName(metaDate.getColumnName()));
            varFiledNames.add(VelocityUtil.toHump(metaDate.getColumnName()));
        }
        ctx.put("filedNames", filedNames);
        ctx.put("varFiledNames", varFiledNames);
        ctx.put("primaryKey", primaryKey);
        ctx.put("modelName", VelocityUtil.tableNameConvertModelName(tableName));
        ctx.put("varModelName", VelocityUtil.toHump(tableName));

        if (getUIComponentFilesDestDirectoryPath() != null) {
            for (String componentFile : getUIComponentFilesPath()) {
                Template tc = VelocityUtil.getTempate(componentFile);
                int ext = componentFile.lastIndexOf(".");
                write(merge(tc, ctx), (destResourcePath + "/" + getUIComponentFilesDestDirectoryPath()).replaceAll("[/]+", "/") + "/" + tableName + componentFile.substring(ext));
            }
        }
        write(merge(template, ctx), (destResourcePath + "/" + getUIDestDirectoryPath()).replaceAll("[/]+", "/") + "/" + tableName + ".html");
    }

    protected void writeUIOther(String destResourcePath, List<String> tables, List<String> modelNames, List<String> varModelNames) throws IOException {

    }
}
