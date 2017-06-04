package com.github.sc.gennerator.jpa;

import com.github.sc.gennerator.AbstractGenerator;
import com.github.sc.gennerator.MetaData;
import com.github.sc.common.utils.TblUtil;
import com.github.sc.common.utils.VelocityUtil;

import java.io.File;
import java.sql.Connection;
import java.util.List;

/**
 * Created by wuyu on 2015/6/3.
 */
public abstract class AbstractGeneratorJpa extends AbstractGenerator {

    public AbstractGeneratorJpa(String url, String username, String password, String driverClass, String project, String packagePath, List<String> tables) {
        super(url, username, password, driverClass, project, packagePath, tables);
    }

    public File generator() throws Exception {
        String basePath = generatorPath();
        Connection connection = getConnection();
        List<String> tables = getTables(connection);
        for (String tableName : tables) {
            List<MetaData> metaDates = TblUtil.getMetaDates(connection, tableName);
            String modelName = VelocityUtil.tableNameConvertModelName(tableName);
            String dockerFile = basePath + "/" + project + "/Dockerfile";
            String servicePath = basePath + "/" + project + "/src/main/java/" + packagePath.replace(".", "/") + "/service/" + modelName + "Service." + getExt();
            String serviceImplPath = basePath + "/" + project + "/src/main/java/" + packagePath.replace(".", "/") + "/service/impl/" + modelName + "ServiceImpl." + getExt();
            String controllerPath = basePath + "/" + project + "/src/main/java/" + packagePath.replace(".", "/") + "/web/" + modelName + "Controller." + getExt();
            String modelPath = basePath + "/" + project + "/src/main/java/" + packagePath.replace(".", "/") + "/model/" + modelName + "." + getExt();
            String daoPath = basePath + "/" + project + "/src/main/java/" + packagePath.replace(".", "/") + "/dao/" + modelName + "Dao." + getExt();
            String mdPath = basePath + "/" + project + "/src/main/resources/md/" + modelName + ".md";
            String applicationYml = basePath + "/" + project + "/src/main/resources/application.yml";
            String destPomPath = basePath + "/" + project + "/pom.xml";
            String destMainPath = basePath + "/" + project + "/src/main/java/" + packagePath.replace(".", "/") + "/" + applicationName + "." + getExt();
            new File(basePath + "/" + project + "/src/test/java").mkdirs();

            logger.info(modelName);
            logger.info(servicePath);
            logger.info(serviceImplPath);
            logger.info(controllerPath);
            logger.info(daoPath);
            logger.info(modelPath);
            logger.info(mdPath);
            logger.info(destPomPath);

            String columnKey = TblUtil.getPrimaryKey(connection, tableName);
            String primaryKey = VelocityUtil.toHump(columnKey);
            String primaryType = TblUtil.getJavaType(connection, tableName, primaryKey);


            String model = genModel(modelPath, project, packagePath, metaDates, primaryKey, primaryType, modelName, tableName);
            String dao = genDao(daoPath, project, packagePath, metaDates, primaryKey, primaryType, modelName);
            String service = genService(servicePath, project, packagePath, metaDates, primaryKey, primaryType, modelName);
            String serviceImpl = genServiceImpl(serviceImplPath, project, packagePath, metaDates, primaryKey, primaryType, modelName);
            String controller = genController(controllerPath, project, packagePath, metaDates, primaryKey, primaryType, modelName);
            writePom(getPomPath(), destPomPath, project, packagePath);
            writeMain(getMain(), destMainPath, primaryKey, packagePath);
            writeDockerfile(getDockerFile(), dockerFile, project, packagePath);
            write(dao, daoPath);
            write(model, modelPath);
            write(service, servicePath);
            write(serviceImpl, serviceImplPath);
            write(controller, controllerPath);
            writeApplicationConfig(getApplicationPath(), applicationYml, project, packagePath);
            new File(basePath + project + "/src/test/java").mkdirs();

        }
        logger.info(basePath);
        connection.close();
        return new File(basePath);
    }


    public abstract String genService(String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName);

    public abstract String genServiceImpl(String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName);

    public abstract String genController(String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName);

    public abstract String genModel(String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String modelName, String primaryType, String tableName);

    public abstract String genDao(String destPath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName);

    public abstract String getPomPath();

    public abstract String getApplicationPath();

    public abstract String getMain();

    public abstract String getName();

    public abstract String getExt();

}
