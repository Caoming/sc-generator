package com.github.sc.gennerator.jpa;

import com.github.sc.gennerator.MetaData;
import com.github.sc.common.utils.VelocityUtil;

import java.util.List;

/**
 * Created by wuyu on 2015/3/23.
 */

public class KotlinGeneratorJpa extends AbstractGeneratorJpa {


    public KotlinGeneratorJpa( String url,String username, String password, String driverClass, String project, String packagePath,List<String> tables) {
        super( url, username, password,driverClass, project, packagePath,tables);
    }

    @Override
    public String genService(String basePath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType, String modelName) {
        StringBuilder sb = new StringBuilder();
        String javaType = getJavaType(metaDates, primaryKey);
        String javaName = VelocityUtil.toHump(primaryKey);
        sb.append("package " + packagePath + ".service")
                .append("\r\n\r\n")
                .append("import " + packagePath + ".model." + modelName)
                .append("\r\n\r\n")
                .append("interface " + modelName + "Service {")
                .append("\r\n\r\n")
                .append("\tfun findOne(" + javaName + ": " + javaType + "): " + modelName)
                .append("\r\n\r\n")
//                .append("\tfun findAll(): Iterable<" + modelName + ">")
//                .append("\r\n\r\n")
                .append("\tfun save(" + VelocityUtil.firstLow(modelName) + ": " + modelName + ")")
                .append("\r\n\r\n")
                .append("\tfun delete(" + javaName + ": " + javaType + ")")
                .append("\r\n}");


        return sb.toString();
    }

    @Override
    public String genServiceImpl(String basePath, String project, String packagePath, List<MetaData> metaDates, String primaryKey,String primaryType, String modelName) {
        StringBuilder sb = new StringBuilder();
        String javaType = getJavaType(metaDates, primaryKey);
        String javaName = VelocityUtil.toHump(primaryKey);
        String daoNameLow = VelocityUtil.firstLow(modelName) + "Dao";
        sb.append("package " + packagePath + ".service.impl")
                .append("\r\n\r\n")
                .append("import " + packagePath + ".model." + modelName + "\r\n")
                .append("import " + packagePath + ".dao." + modelName + "Dao\r\n")
                .append("import " + packagePath + ".service." + modelName + "Service\r\n")
                .append("import org.springframework.beans.factory.annotation.Autowired\r\n")
                .append("import org.springframework.stereotype.Service\r\n")
                .append("\r\n\r\n")
                .append("@Service\r\n")
                .append("class " + modelName + "ServiceImpl : " + modelName + "Service{")
                .append("\r\n\r\n")
                .append("\t@Autowired")
                .append("\r\n")
                .append("\tlateinit var " + daoNameLow + ": " + modelName + "Dao")
                .append("\r\n\r\n")
                .append("\toverride fun findOne(" + javaName + ": " + javaType + "): " + modelName + " {")
                .append("\r\n\t\t")
                .append("return " + daoNameLow + ".findOne(" + javaName + ")")
                .append("\r\n")
                .append("\t}")
                .append("\r\n\r\n")
//                .append("\toverride fun findAll(): kotlin.collections.Iterable<" + modelName + "> {")
//                .append("\r\n\t\t")
//                .append("return " + daoNameLow + ".findAll()")
//                .append("\r\n")
//                .append("\t}")
//                .append("\r\n\r\n")
                .append("\toverride fun save(" + VelocityUtil.firstLow(modelName) + ": " + modelName + ") {")
                .append("\r\n\t\t")
                .append(daoNameLow + ".save(" + VelocityUtil.firstLow(modelName) + ")")
                .append("\r\n")
                .append("\t}")
                .append("\r\n\r\n")
                .append("\toverride fun delete(" + javaName + ": " + javaType + ") {")
                .append("\r\n\t\t")
                .append(daoNameLow + ".delete(" + javaName + ")")
                .append("\r\n")
                .append("\t}")
                .append("\r\n\r\n")
                .append("}");
        return sb.toString();
    }

    @Override
    public String genController(String basePath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType,String modelName) {
        StringBuilder sb = new StringBuilder();
        String javaType = getJavaType(metaDates, primaryKey);
        String javaName = VelocityUtil.toHump(primaryKey);
        String serviceName = VelocityUtil.firstLow(modelName) + "Service";
        sb.append("package " + packagePath + ".web")
                .append("\r\n\r\n")
                .append("import " + packagePath + ".model." + modelName + "\r\n")
                .append("import " + packagePath + ".service." + modelName + "Service\r\n")
                .append("import org.springframework.beans.factory.annotation.Autowired\r\n")
                .append("import org.springframework.web.bind.annotation.PathVariable\r\n")
                .append("import org.springframework.web.bind.annotation.RequestBody\r\n")
                .append("import org.springframework.web.bind.annotation.RequestMapping\r\n")
                .append("import org.springframework.web.bind.annotation.RestController\r\n")
                .append("import org.springframework.web.bind.annotation.RequestMethod\r\n")
                .append("\r\n\r\n")
                .append("@RestController\r\n")
                .append("@RequestMapping(value =  \"/" + VelocityUtil.firstLow(modelName) + "/\")\r\n")
                .append("class " + modelName + "Controller {")
                .append("\r\n\r\n")
                .append("\t@Autowired")
                .append("\r\n")
                .append("\tlateinit var " + serviceName + ": " + modelName + "Service")
                .append("\r\n\r\n")
                .append("\t@RequestMapping(value = \"{id}\", method = arrayOf(RequestMethod.GET))\r\n")
                .append("\tfun findOne(@PathVariable(\"id\") " + javaName + ": " + javaType + "): " + modelName + " {")
                .append("\r\n\t\t")
                .append("return " + serviceName + ".findOne(" + javaName + ")")
                .append("\r\n")
                .append("\t}")
                .append("\r\n\r\n")
//                .append("\t@RequestMapping(value = \"findAll\", method = arrayOf(RequestMethod.GET))\r\n")
//                .append("\tfun findAll(): Iterable<" + modelName + "> {")
//                .append("\r\n\t\t")
//                .append("return " + serviceName + ".findAll()")
//                .append("\r\n")
//                .append("\t}")
//                .append("\r\n\r\n")
                .append("\t@RequestMapping(value = \"{id}\", method = arrayOf(RequestMethod.DELETE))\r\n")
                .append("\tfun delete(@PathVariable(\"id\") " + javaName + ": " + javaType + ") {")
                .append("\r\n\t\t")
                .append(serviceName + ".delete(" + javaName + ")")
                .append("\r\n")
                .append("\t}")
                .append("\r\n\r\n")
                .append("\t@RequestMapping(method = arrayOf(RequestMethod.POST))\r\n")
                .append("\tfun save(@RequestBody " + VelocityUtil.firstLow(modelName) + ": " + modelName + ") {")
                .append("\r\n\t\t")
                .append(serviceName + ".save(" + VelocityUtil.firstLow(modelName) + ")")
                .append("\r\n")
                .append("\t}")
                .append("\r\n\r\n")
                .append("}");
        return sb.toString();
    }

    @Override
    public String genModel(String basePath, String project, String packagePath, List<MetaData> metaDates, String primaryKey, String primaryType,String modelName, String tableName) {

        StringBuilder sb = new StringBuilder();
        sb.append("package " + packagePath + ".model");
        sb.append("\r\n\r\n");
        sb.append("import javax.persistence.Entity\r\n")
                .append(("import javax.persistence.GeneratedValue\r\n"))
                .append(("import javax.persistence.Id\r\n"))
                .append(("import javax.persistence.Table\r\n"))
                .append(("import javax.persistence.Column\r\n"))
//                .append(("import javax.validation.constraints.NotNull\r\n"))
                .append("\r\n")
                .append("import io.swagger.annotations.ApiModelProperty")
                .append("\r\n\r\n")
                .append("@Entity")
                .append("\r\n")
                .append("@Table(name = \"" + tableName + "\")")
                .append("\r\n")
                .append("class " + modelName + " {")
                .append("\r\n")
                .append("\r\n");

        for (MetaData metaDate : metaDates) {
            String columnName = VelocityUtil.toHump(metaDate.getColumnName());
            String columnType = metaDate.getTypeName();
            String remarks = metaDate.getRemarks();
            if (columnType.equalsIgnoreCase("date")) {
                columnType = "java.util.Date";
            }

            if (columnName.equalsIgnoreCase(primaryKey)) {
                if(columnType.equalsIgnoreCase("long")||columnType.equalsIgnoreCase("Integer")){
                    sb.append("\t")
                            .append("@Id")
                            .append("\r\n")
                            .append("\t@GeneratedValue")
                            .append("\r\n");
                }else{
                    sb.append("\t")
                            .append("@Id")
                            .append("\r\n");
                }
            } else {
                boolean nullAble = !(metaDate.getNullAble() == 0);
                sb.append("\t")
                        .append("@Column(name = \"" + metaDate.getColumnName() + "\",nullable = " + nullAble + ",length = "+metaDate.getColumnSize()+", precision = "+metaDate.getDecimalDigits()+")")
                        .append("\r\n");

//                if (!nullAble) {
//                    sb.append("\t@NotNull\r\n");
//                }
            }


            sb.append("\t");
            if (remarks.equalsIgnoreCase("")) {
                sb.append("@ApiModelProperty(value=\"" + columnName + "\")");
            } else {
                sb.append("@ApiModelProperty(value=\"" + remarks + "\")");
            }

            sb.append("\r\n")
                    .append("\t")
                    .append("var " + columnName + ": " + columnType + "? = null")
                    .append("\r\n\r\n");

        }

        sb.append("}");

        return sb.toString();
    }

    @Override
    public String genDao(String basePath, String project, String packagePath, List<MetaData> metaDates, String primaryKey,String primaryType, String modelName) {
        StringBuilder sb = new StringBuilder();
        String javaType = getJavaType(metaDates, primaryKey);

        sb.append("package " + packagePath + ".dao")
                .append("\r\n\r\n")
                .append("import org.springframework.data.repository.CrudRepository\r\n")
                .append("import " + packagePath + ".model." + modelName)
                .append("\r\n\r\n")
                .append("interface " + modelName + "Dao : CrudRepository<" + modelName + ", " + javaType + ">");
        return sb.toString();
    }


    @Override
    public String getPomPath() {
        return "/templates/pom/springboot/kotlin/jpa/pom.vm";
    }

    @Override
    public String getApplicationPath() {
        return "/templates/pom/springboot/kotlin/jpa/application.yml";
    }

    @Override
    public String getMain() {
        return "/templates/pom/springboot/kotlin/jpa/Main.vm";
    }

    @Override
    public String getName() {
        return "kotlin";
    }

    @Override
    public String getExt() {
        return "kt";
    }


}
