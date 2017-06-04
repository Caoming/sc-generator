package com.github.sc.gennerator.mybatis;

import com.github.sc.common.utils.TblUtil;
import com.github.sc.common.utils.VelocityUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mybatis.generator.api.*;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.codegen.mybatis3.IntrospectedTableMyBatis3Impl;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.Context;
import org.mybatis.generator.config.MergeConstants;
import org.mybatis.generator.exception.InvalidConfigurationException;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.NullProgressCallback;
import org.mybatis.generator.internal.ObjectFactory;
import org.mybatis.generator.internal.XmlFileMergerJaxp;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mybatis.generator.internal.util.ClassloaderUtility.getCustomClassloader;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

/**
 * @author wuyu
 * @Date 2015/3/03
 */
public class MapperGenerator extends MyBatisGenerator {

    static Logger logger = Logger.getLogger(MyBatisGenerator.class);

    private Connection conn;

    private String baseModel;

    private String baseExample;

    private boolean extendsBase = false;

    private boolean support = false;

    private boolean swagger = true;

    public MapperGenerator(Configuration configuration, ShellCallback shellCallback, List<String> warnings)
            throws InvalidConfigurationException {
        super(configuration, shellCallback, warnings);
    }

    String baseDaoFullName;

    public void setBaseDao(String baseDaoFullName) {
        this.baseDaoFullName = baseDaoFullName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void generate(ProgressCallback callback, Set<String> contextIds, Set<String> fullyQualifiedTableNames)
            throws SQLException, IOException, InterruptedException {
        if (callback == null) {
            callback = new NullProgressCallback();
        }

        List<GeneratedJavaFile> generatedJavaFiles = null;
        List<GeneratedXmlFile> generatedXmlFiles = null;
        Configuration configuration = null;
        Set<String> projects = null;
        ShellCallback shellCallback = null;
        try {

            Field generatedJavaFilesField = MyBatisGenerator.class.getDeclaredField("generatedJavaFiles");
            Field generatedXmlFilesField = MyBatisGenerator.class.getDeclaredField("generatedXmlFiles");
            Field configurationField = MyBatisGenerator.class.getDeclaredField("configuration");
            Field projectsField = MyBatisGenerator.class.getDeclaredField("projects");
            Field shellCallbackField = MyBatisGenerator.class.getDeclaredField("shellCallback");

            generatedJavaFilesField.setAccessible(true);
            generatedXmlFilesField.setAccessible(true);
            configurationField.setAccessible(true);
            projectsField.setAccessible(true);
            shellCallbackField.setAccessible(true);
            generatedJavaFiles = (List<GeneratedJavaFile>) generatedJavaFilesField.get(this);
            generatedXmlFiles = (List<GeneratedXmlFile>) generatedXmlFilesField.get(this);
            configuration = (Configuration) configurationField.get(this);
            projects = (Set<String>) projectsField.get(this);
            shellCallback = (ShellCallback) shellCallbackField.get(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        generatedJavaFiles.clear();
        generatedXmlFiles.clear();

        // calculate the contexts to run
        List<Context> contextsToRun;
        if (contextIds == null || contextIds.size() == 0) {
            contextsToRun = configuration.getContexts();
        } else {
            contextsToRun = new ArrayList<Context>();
            for (Context context : configuration.getContexts()) {
                if (contextIds.contains(context.getId())) {
                    contextsToRun.add(context);
                }
            }
        }

        // setup custom classloader if required
        if (configuration.getClassPathEntries().size() > 0) {
            ClassLoader classLoader = getCustomClassloader(configuration.getClassPathEntries());
            ObjectFactory.addExternalClassLoader(classLoader);
        }

        // now run the introspections...
        int totalSteps = 0;
        for (Context context : contextsToRun) {
            totalSteps += context.getIntrospectionSteps();
        }
        callback.introspectionStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.introspectTables(callback, new ArrayList<String>(), null);
        }

        // now run the generates
        totalSteps = 0;
        List<IntrospectedTableMyBatis3Impl> introspectedTables = null;
        Map<String, Integer> map = new HashMap<>();
        for (Context context : contextsToRun) {
            try {
                Field introspectedTablesFiled = Context.class.getDeclaredField("introspectedTables");
                introspectedTablesFiled.setAccessible(true);
                introspectedTables = (List<IntrospectedTableMyBatis3Impl>) introspectedTablesFiled.get(context);
                int i = 0;
                for (IntrospectedTableMyBatis3Impl ins : introspectedTables) {
                    List<IntrospectedColumn> baseColumns = ins.getBaseColumns();
                    List<IntrospectedColumn> blobColumns = ins.getBLOBColumns();
                    baseColumns.addAll(blobColumns);
                    blobColumns.clear();
                    List<IntrospectedColumn> nonBLOBColumns = ins.getNonBLOBColumns();

                    String tableName = ins.getTableConfiguration().getTableName();
                    map.put(TblUtil.firstUp(tableName), i);
                    i++;

                }
            } catch (NoSuchFieldException | SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            totalSteps += context.getGenerationSteps();
        }
        callback.generationStarted(totalSteps);

        for (Context context : contextsToRun) {
            context.generateFiles(callback, generatedJavaFiles, generatedXmlFiles, new ArrayList<String>());
        }
        List<String> warnings = new ArrayList<String>();
        // now save the files
        callback.saveStarted(generatedXmlFiles.size() + generatedJavaFiles.size());

        for (GeneratedXmlFile gxf : generatedXmlFiles) {

            String fileName = gxf.getFileName().replace("Mapper", "Dao");
            String modelName = fileName.replace("Dao", "").replace(".xml", "");

            IntrospectedTableMyBatis3Impl introspectedTableMyBatis3Impl = introspectedTables.get(map.get(modelName));
            List<IntrospectedColumn> primaryKeyColumns = introspectedTableMyBatis3Impl.getPrimaryKeyColumns();
            String primaryKey = null;
            String primaryKeyClass = null;
            String primaryKeyColumnName = null;
            if (primaryKeyColumns.size() > 0) {
                primaryKey = primaryKeyColumns.get(0).getActualColumnName();
                primaryKeyClass = primaryKeyColumns.get(0).getFullyQualifiedJavaType().getFullyQualifiedName();
                primaryKeyColumnName = primaryKeyColumns.get(0).getActualColumnName();
            }
            logger.info("generator xml:" + fileName);
            try {
                Field FileNameFiled = GeneratedXmlFile.class.getDeclaredField("fileName");
                Field documentField = GeneratedXmlFile.class.getDeclaredField("document");
                FileNameFiled.setAccessible(true);
                documentField.setAccessible(true);
                FileNameFiled.set(gxf, fileName);
                Document document = (Document) documentField.get(gxf);
                XmlElement rootElement = document.getRootElement();
                List<Attribute> rootAttribute = new ArrayList<Attribute>();
                String daoFulleName = "";
                for (Attribute attribute : rootElement.getAttributes()) {
                    daoFulleName = attribute.getValue().replace("Mapper", "Dao");
                    Attribute a = new Attribute(attribute.getName(), daoFulleName);
                    rootAttribute.add(a);
                }
                List<Element> rootElements = rootElement.getElements();
                XmlElement baseCloumnElement = new XmlElement("sql");
                baseCloumnElement.addAttribute(new Attribute("id", "Base_Column_List_Dy"));

                rootElements.add(baseCloumnElement);
                String exampleFullName = daoFulleName.replace("Dao", "Example").replace(".dao.", ".model.query.");
                XmlElement selectByPrimaryKeys = new XmlElement("select");

                XmlElement insertBatch = new XmlElement("insert");
                insertBatch.addAttribute(new Attribute("id", "insertBatch"));
                IntrospectedTableMyBatis3Impl tableMyBatis3Impl = introspectedTables.get(map.get(modelName));
                String tableName = tableMyBatis3Impl.getTableConfiguration().getTableName();
                insertBatch.addAttribute(new Attribute("parameterType", "java.util.List"));
                insertBatch.addElement(new TextElement("insert into " + tableName + "("));
                XmlElement refId = new XmlElement("include");
                refId.addAttribute(new Attribute("refid", "Base_Column_List"));
                ;
                insertBatch.addElement(refId);
                insertBatch.addElement(new TextElement(")"));
                insertBatch.addElement(new TextElement("values"));
                XmlElement insertBatchFor = new XmlElement("foreach");
                insertBatchFor.addAttribute(new Attribute("collection", "list"));
                insertBatchFor.addAttribute(new Attribute("item", "item"));
                insertBatchFor.addAttribute(new Attribute("separator", ","));
                insertBatchFor.addAttribute(new Attribute("index", "index"));
                StringBuffer insertValues = new StringBuffer();
                insertValues.append("(\n\t");
                List<IntrospectedColumn> allColumns = tableMyBatis3Impl.getAllColumns();
                for (int i = 0; i < allColumns.size(); i++) {
                    IntrospectedColumn column = allColumns.get(i);
                    String javaProperty = column.getJavaProperty();
                    insertValues.append("#{item." + javaProperty + "}");
                    if ((i + 1) != allColumns.size()) {
                        insertValues.append(",");
                    }

                    if (i % 2 == 0) {
                        insertValues.append("\n\t");
                    }

                }

                insertBatchFor.addElement(new TextElement(insertValues.toString() + ")"));
                insertBatch.addElement(insertBatchFor);
                rootElements.add(insertBatch);


                XmlElement updateBatch = new XmlElement("update");
                updateBatch.addAttribute(new Attribute("id", "updateBatch"));
                updateBatch.addAttribute(new Attribute("parameterType", "java.util.List"));
                XmlElement updateBatchFor = new XmlElement("foreach");
                updateBatchFor.addAttribute(new Attribute("collection", "list"));
                updateBatchFor.addAttribute(new Attribute("item", "item"));
                updateBatchFor.addAttribute(new Attribute("separator", ";"));
                updateBatchFor.addAttribute(new Attribute("index", "index"));
                StringBuffer updateValues = new StringBuffer();

                updateValues.append("update `" + tableName + "`\r\n\t\t<trim suffix=','><set>\r\n");
                for (int i = 0; i < allColumns.size(); i++) {
                    IntrospectedColumn column = allColumns.get(i);
                    String javaProperty = column.getJavaProperty();
                    String actualColumnName = column.getActualColumnName();
                    updateValues.append("\t\t\t<if test=\"" + javaProperty + "!=null\">\r\n");
                    if (i == allColumns.size() - 1) {
                        updateValues.append("\t\t\t\t" + actualColumnName + " = #{item." + javaProperty + "}\r\n");
                    } else {
                        updateValues.append("\t\t\t\t" + actualColumnName + " = #{item." + javaProperty + "},\r\n");
                    }

                    updateValues.append("\t\t\t</if>\r\n");
                }
                updateValues.append("\t\t" + "</set></trim>\r\n");


                updateValues.append("\r\n\t\twhere \r\n\t\t\t" + primaryKeyColumnName + " = #{item." + primaryKey + "}");


                updateBatchFor.addElement(new TextElement(updateValues.toString()));
                updateBatch.addElement(updateBatchFor);
                rootElements.add(updateBatch);


                XmlElement selectByPrimaryKey = null;
                for (Element element : rootElements) {
                    XmlElement e = (XmlElement) element;

                    List<Attribute> attributes = e.getAttributes();
                    for (int i = attributes.size() - 1; i >= 0; i--) {
                        Attribute attribute = attributes.get(i);
                        if (attribute.getValue()
                                .equals(daoFulleName.replace("Dao", "Example").replace(".dao.", ".model."))) {
                            Attribute a = new Attribute(attributes.get(i).getName(), exampleFullName);
                            attributes.remove(attributes.get(i));
                            attributes.add(a);
                        }

                        if (attribute.getValue().equals("Base_Column_List")) {
                            XmlElement ifNullElement = new XmlElement("if");

                            XmlElement includeBaseColunms = new XmlElement("include");
                            includeBaseColunms.addAttribute(new Attribute("refid", "Base_Column_List"));
                            ifNullElement.addElement(includeBaseColunms);
                            Attribute testNullAttr = new Attribute("test", "fields==null");
                            ifNullElement.addAttribute(testNullAttr);

                            XmlElement ifNotElement = new XmlElement("if");
                            Attribute testNotNull = new Attribute("test", "fields!=null");
                            ifNotElement.addAttribute(testNotNull);

                            XmlElement forEachElement = new XmlElement("foreach");
                            Attribute collectionAttr = new Attribute("collection", "fields");
                            Attribute itemAttr = new Attribute("item", "field");
                            Attribute separatorAttr = new Attribute("separator", ",");
                            forEachElement.addAttribute(collectionAttr);
                            forEachElement.addAttribute(itemAttr);
                            forEachElement.addAttribute(separatorAttr);
                            forEachElement.addElement(new TextElement("${field}"));
                            ifNotElement.addElement(forEachElement);

                            baseCloumnElement.addElement(ifNullElement);
                            baseCloumnElement.addElement(ifNotElement);
                        }

                        if (extendsBase) {
                            if (attribute.getValue().equals("selectByExample")
                                    || attribute.getValue().equals("selectByExampleWithBLOBs")) {
                                List<Element> elements = e.getElements();
                                for (Element element2 : elements) {
                                    if (element2 instanceof XmlElement) {
                                        XmlElement e2 = (XmlElement) element2;
                                        List<Attribute> attributes2 = e2.getAttributes();
                                        for (int j = attributes2.size() - 1; j >= 0; j--) {
                                            if (attributes2.get(j).getValue().equals("Base_Column_List")) {
                                                attributes2.remove(j);
                                                e2.addAttribute(new Attribute("refid", "Base_Column_List_Dy"));
                                            }
                                        }
                                    }

                                }
                                XmlElement ifNotElement = new XmlElement("if");
                                Attribute testNotNull = new Attribute("test", "limit != null");
                                ifNotElement.addAttribute(testNotNull);
                                TextElement limit = new TextElement("limit");
                                ifNotElement.addElement(limit);
                                XmlElement forEachElement = new XmlElement("foreach");
                                Attribute collectionAttr = new Attribute("collection", "limit");
                                Attribute itemAttr = new Attribute("item", "num");
                                Attribute separatorAttr = new Attribute("separator", ",");
                                forEachElement.addAttribute(collectionAttr);
                                forEachElement.addAttribute(itemAttr);
                                forEachElement.addAttribute(separatorAttr);
                                forEachElement.addElement(new TextElement("${num}"));
                                ifNotElement.addElement(forEachElement);
                                elements.add(ifNotElement);
                            }
                        }


                        if (attribute.getValue().equals("selectByPrimaryKey")) {
                            selectByPrimaryKey = e;
                        }

                        if (primaryKey != null
                                && primaryKeyClass != null & !primaryKeyClass.equalsIgnoreCase("java.lang.String")) {
                            if (attribute.getValue().equals("insert")
                                    || attribute.getValue().equals("insertSelective")) {
                                e.addAttribute(new Attribute("useGeneratedKeys", "true"));
                                e.addAttribute(new Attribute("keyProperty", VelocityUtil.toHump(primaryKey)));

                            }
                        }

                    }

                    // List<Attribute> sqlAttr = e.getAttributes();
                    // for (Attribute attribute : sqlAttr) {
                    // if (attribute.getValue().equals("BaseResultMap")) {
                    // for (Element element2 : e.getElements()) {
                    // if (element2 instanceof XmlElement) {
                    // XmlElement x2 = (XmlElement) element2;
                    // List<Attribute> attributes2 = x2.getAttributes();
                    // for (Attribute attribute2 : attributes2) {
                    // String name = attribute2.getName();
                    // String value = attribute2.getValue();
                    // if (name.equals("column")) {
                    // columns.add(value);
                    // }
                    // if (name.equals("property")) {
                    // fields.add(value);
                    // }
                    // }
                    // }
                    //
                    // }
                    // }
                    // }
                }
                Field attributesField = XmlElement.class.getDeclaredField("attributes");
                attributesField.setAccessible(true);
                attributesField.set(rootElement, rootAttribute);
                document.setRootElement(rootElement);

                if (selectByPrimaryKey != null) {

                    XmlElement selectWithPage = new XmlElement("select");
                    Attribute selectWithPageId = new Attribute("id", "selectWithPage");
                    selectWithPage.addAttribute(selectWithPageId);

                    Attribute idAttr = new Attribute("id", "selectByPrimaryKeys");
                    selectByPrimaryKeys.addAttribute(idAttr);
                    for (Attribute selectAttr : selectByPrimaryKey.getAttributes()) {
                        if (!selectAttr.getValue().equals("selectByPrimaryKey")
                                && !selectAttr.getName().equals("parameterType")) {
                            Attribute selectsAttr = new Attribute(selectAttr.getName(), selectAttr.getValue());
                            selectByPrimaryKeys.addAttribute(selectsAttr);
                            selectWithPage.addAttribute(selectAttr);
                        }
                    }

                    rootElement.addElement(selectByPrimaryKeys);
                    rootElement.addElement(selectWithPage);

                    for (Element element2 : selectByPrimaryKey.getElements()) {
                        selectByPrimaryKeys.addElement(element2);
                        selectWithPage.addElement(element2);
                        if (element2 instanceof TextElement) {
                            TextElement e2 = (TextElement) element2;

                            if (e2.getContent().contains("where")) {
                                selectByPrimaryKeys.getElements().remove(e2);
                                selectWithPage.getElements().remove(e2);
                                String[] split = e2.getContent().replace("where ", "").split("=");
                                TextElement textElement = new TextElement("where " + split[0] + " in \n");
                                selectByPrimaryKeys.getElements().remove(element2);
                                selectByPrimaryKeys.addElement(textElement);
                                XmlElement forEachElement = new XmlElement("foreach");
                                Attribute collectionAttr = new Attribute("collection", "list");
                                Attribute itemAttr = new Attribute("item", "id");
                                Attribute separatorAttr = new Attribute("separator", ",");
                                Attribute openAttr = new Attribute("open", "(");
                                Attribute closeAttr = new Attribute("close", ")");
                                forEachElement.addAttribute(collectionAttr);
                                forEachElement.addAttribute(itemAttr);
                                forEachElement.addAttribute(separatorAttr);
                                forEachElement.addAttribute(openAttr);
                                forEachElement.addAttribute(closeAttr);
                                forEachElement.addElement(new TextElement("#{id}"));
                                selectByPrimaryKeys.addElement(forEachElement);
                            }

                        }
                    }
                    TextElement t = new TextElement("limit \n");
                    selectWithPage.addElement(t);

                    XmlElement forEachElement = new XmlElement("foreach");
                    Attribute collectionAttr = new Attribute("collection", "list");
                    Attribute itemAttr = new Attribute("item", "num");
                    Attribute separatorAttr = new Attribute("separator", ",");
                    forEachElement.addAttribute(collectionAttr);
                    forEachElement.addAttribute(itemAttr);
                    forEachElement.addAttribute(separatorAttr);
                    forEachElement.addElement(new TextElement("${num}"));
                    selectWithPage.addElement(forEachElement);

                }

            } catch (Exception e1) {
                e1.printStackTrace();
            }
            projects.add(gxf.getTargetProject());

            File targetFile;
            String source;
            try {
                File directory = shellCallback.getDirectory(gxf.getTargetProject(), gxf.getTargetPackage());
                targetFile = new File(directory, gxf.getFileName());
                if (targetFile.exists()) {
                    if (gxf.isMergeable()) {
                        source = XmlFileMergerJaxp.getMergedSource(gxf, targetFile);
                    } else if (shellCallback.isOverwriteEnabled()) {
                        source = gxf.getFormattedContent();
                        warnings.add(getString("Warning.11", //$NON-NLS-1$
                                targetFile.getAbsolutePath()));
                    } else {
                        source = gxf.getFormattedContent();
                        targetFile = getUniqueFileName(directory, gxf.getFileName());
                        warnings.add(getString("Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                    }
                } else {
                    source = gxf.getFormattedContent();
                }
            } catch (ShellException e) {
                warnings.add(e.getMessage());
                continue;
            }

            callback.checkCancel();
            callback.startTask(getString("Progress.15", targetFile.getName())); //$NON-NLS-1$
            writeFile(targetFile, source, "UTF-8"); //$NON-NLS-1$
        }


        for (

                GeneratedJavaFile gjf : generatedJavaFiles)

        {
            logger.info("gen java:" + gjf.getFileName());
            projects.add(gjf.getTargetProject());

            File targetFile;
            String source;
            try {
                File directory = shellCallback.getDirectory(gjf.getTargetProject(), gjf.getTargetPackage());

                targetFile = new File(directory, gjf.getFileName());
                String packageFullName = gjf.getTargetPackage();
                String DaoName = gjf.getFileName().replace("Mapper", "Dao").replace(".java", "");
                String modelName = DaoName.replace("Dao", "").replace(".java", "");
                if (gjf.getFileName().contains("Mapper")) {
                    targetFile = new File(directory, gjf.getFileName().replace("Mapper", "Dao"));
                }
                if (gjf.getFileName().contains("Example")) {
                    if (!(new File(directory + "/query/")).exists()) {
                        (new File(directory + "/query/")).mkdirs();
                    }
                    targetFile = new File(directory + "/query/", gjf.getFileName());
                    // targetFile = new File(directory, gjf.getFileName());

                }
                if (targetFile.exists()) {
                    if (shellCallback.isMergeSupported()) {
                        source = shellCallback.mergeJavaFile(gjf.getFormattedContent(), targetFile.getAbsolutePath(),
                                MergeConstants.OLD_ELEMENT_TAGS, gjf.getFileEncoding());
                    } else if (shellCallback.isOverwriteEnabled()) {
                        source = gjf.getFormattedContent();
                        warnings.add(getString("Warning.11", //$NON-NLS-1$
                                targetFile.getAbsolutePath()));
                    } else {
                        source = gjf.getFormattedContent();
                        targetFile = getUniqueFileName(directory, gjf.getFileName());
                        warnings.add(getString("Warning.2", targetFile.getAbsolutePath())); //$NON-NLS-1$
                    }
                } else {
                    source = gjf.getFormattedContent();
                }

                Random r = new Random();
                StringBuffer sb = new StringBuffer();

                StringBuffer remark = new StringBuffer();
                remark.append("/**");
                remark.append("\r\n");
                remark.append(" * Created on " + new SimpleDateFormat("yyyy/M/dd HH:mm").format(new Date()) + ".\r\n\r\n");
                remark.append(" */");

                if (gjf.getFileName().contains("Mapper")) {
                    if (extendsBase) {
                        String line = "package " + packageFullName + ";" + "\r\n\r\n" + "import " + baseDaoFullName
                                + ";" + "\r\n" + "import " + packageFullName.replace("dao", "model") + "." + modelName
                                + ";" + "\r\n" + "import " + packageFullName.replace("dao", "model") + ".query."
                                + modelName + "Example;" + "\r\n\r\n" + remark + "\r\npublic interface " + DaoName
                                + " extends BaseDao<" + modelName + ", " + modelName + "Example> {" + "\r\n\r\n" + "}";
                        sb.append(line);

                    } else {
                        String[] lines = source.split("\r\n");
                        if (lines.length == 1) {
                            lines = source.split("\r");
                            if (lines.length == 1) {
                                lines = source.split("\n");
                            }
                        }
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];
                            if (line.contains("Mapper")) {
                                line = line.replace("Mapper", "Dao");
                            }

                            if (line.contains(".model." + modelName + "Example")) {
                                line = line.replace(".model." + modelName + "Example", ".model.query." + modelName + "Example");
                            }
                            sb.append(line + "\r\n");
                        }

                    }
                }

                if (!gjf.getFileName().contains("Example") && !gjf.getFileName().contains("Mapper")) {
                    String[] lines = source.split("\r\n");
                    if (lines.length == 1) {
                        lines = source.split("\r");
                        if (lines.length == 1) {
                            lines = source.split("\n");
                        }
                    }
                    for (int i = 0; i < lines.length; i++) {
                        long nextLong = Math.abs(r.nextLong());
                        String line = lines[i];
                        if (i == 2) {

                            StringBuffer remark2 = new StringBuffer();
                            remark2.append("\r\n/**");
                            remark2.append("\r\n");
                            remark2.append(" * Created on " + new SimpleDateFormat("yyyy/M/dd HH:mm").format(new Date()) + ".\r\n *\r\n");
                            try {
                                for (IntrospectedTableMyBatis3Impl inr : introspectedTables) {
                                    String tableName = inr.getTableConfiguration().getTableName();
                                    if (modelName.equalsIgnoreCase(TblUtil.firstUp2(tableName))) {
                                        List<IntrospectedColumn> baseColumns = inr.getAllColumns();
                                        for (IntrospectedColumn column : baseColumns) {
                                            String jdbcTypeName = column.getJdbcTypeName();
                                            int length = column.getLength();
                                            String javaProperty = column.getJavaProperty();
                                            String remarks = column.getRemarks();
                                            remark2.append(" * " + javaProperty + " \t");
                                            remark2.append(jdbcTypeName + " \t");
                                            remark2.append(length + " \t");
                                            remark2.append(StringUtils.join(StringUtils.join(remarks.split("\r\n"), " ").split("\n"), " ").replace("\"", "") + " ");
                                            remark2.append("\r\n");

                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            String importDate = "import java.util.Date;";
                            remark2.append(" */");
                            if (line.contains(importDate)) {
                                line = line.replace(importDate, "");
                                if (extendsBase) {
                                    line = "import " + baseModel + ";\r\n" + importDate + "\r\n" +
                                            (swagger ? "import io.swagger.annotations.ApiModelProperty;\r\n" : "") + "\r\n\r\n" + remark2.toString() + line;
                                } else {
                                    line = "import java.io.Serializable;\r\n" + importDate + "\r\n" +
                                            (swagger ? "import io.swagger.annotations.ApiModelProperty;\r\n" : "") + "\r\n\r\n" + remark2.toString() + line;
                                }


                            } else {

                                if (extendsBase) {
                                    line = "import " + baseModel + ";\r\n" + (swagger ? "import io.swagger.annotations.ApiModelProperty;\r\n" : "")
                                            + remark2.toString() + "\r\n\r\n" + line;
                                } else {
                                    line = "import java.io.Serializable;\r\n" + (swagger ? "import io.swagger.annotations.ApiModelProperty;\r\n" : "") + remark2.toString() + "\r\n\r\n" + line;
                                }

                            }


                            // +"import java.util.Map;\r\n"
                            // +"import java.util.LinkedHashMap;\r\n";
                        }

                        if (line.contains("public class") && line.contains("{")) {

                            if (extendsBase && !line.contains("extends ")) {

                                line = line.replace("{", "extends BaseModel<" + modelName + "> {")
                                        + "\r\n    private static final long serialVersionUID = " + nextLong + "L;\r\n";
                            } else {
                                line = line.replace("{", "implements Serializable {")
                                        + "\r\n\r\n\tprivate static final long serialVersionUID = " + nextLong + "L;\r\n";
                            }

                        }


                        if (line.contains(" private ") && !line.contains("private static final long serialVersionUID")) {
                            String[] filedLine = line.trim().substring(0, line.trim().length() - 1).split(" ");
                            String field = filedLine[filedLine.length - 1];

                            for (IntrospectedTableMyBatis3Impl inr : introspectedTables) {
                                String tableName = inr.getTableConfiguration().getTableName();
                                if (modelName.equalsIgnoreCase(TblUtil.firstUp2(tableName))) {
                                    List<IntrospectedColumn> baseColumns = inr.getAllColumns();
                                    for (IntrospectedColumn column : baseColumns) {
                                        String javaProperty = column.getJavaProperty();
                                        String remarks = "";
                                        if (column.getRemarks() != null) {
                                            remarks = StringUtils.join(StringUtils.join(column.getRemarks().split("\r\n")).split("\n")).replace("\"", "");
                                        }
                                        if (swagger) {
                                            if (field.equals(javaProperty)) {
                                                if (remarks.equals("")) {
                                                    line = "    @ApiModelProperty(value = \"" + field + "\")\r\n" + line;
                                                } else {
                                                    line = "    @ApiModelProperty(value = \"" + remarks + "\")\r\n" + line;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        if (line.contains("public void set")) {
                            line = line.replace("public void set",
                                    "public " + gjf.getFileName().replace(".java", "") + " set");
                            i++;
                            line = line + "\r\n" + lines[i] + "\r\n\t\treturn this;";
                        }


                        // if (line.equals("}")) {
                        // line = "\r\n public Map<String, Object> toMap(){\r\n"
                        // + " Map<String, Object> map=new
                        // LinkedHashMap<String,Object>();\r\n";
                        // for (String field : fields) {
                        // line = line + " map.put(\"" + field + "\",this." +
                        // field
                        // + ");\r\n";
                        // }
                        // line=line+ " return map;\r\n";
                        // line = line + " }\r\n";
                        //
                        // sb.append(line+"\r\n}");
                        // continue;
                        // }

                        sb.append(line + "\r\n");
                    }
                }

                if (gjf.getFileName().contains("Example")) {

                    String[] lines = source.split("\r\n");
                    if (lines.length == 1) {
                        lines = source.split("\r");
                        if (lines.length == 1) {
                            lines = source.split("\n");
                        }
                    }
                    for (int i = 0; i < lines.length; i++) {
                        long nextLong = Math.abs(r.nextLong());
                        String line = lines[i];
                        if (i == 0) {
                            line = line.replace(gjf.getTargetPackage(), gjf.getTargetPackage() + ".query");
                        }

                        if (i == 2) {
                            if (extendsBase) {
                                line = "import java.io.Serializable;\r\n" + "import " + baseExample + ";\r\n" + line;
                            } else {
                                line = "import java.io.Serializable;\r\n" + line;
                            }
                        }

                        if (line.contains(" class ") && line.contains("{") && extendsBase) {

                            if (line.contains("Example")) {
                                line = line.trim().replace("{", "extends BaseExample {")
                                        + "\r\n\r\n\tprivate static final long serialVersionUID = " + nextLong
                                        + "L;\r\n";
                            }

                            if (line.contains("GeneratedCriteria") || line.contains("Criteria")
                                    || line.contains("Criterion"))
                                line = line.trim().replace("{", "implements Serializable {")
                                        + "\r\n\r\n\tprivate static final long serialVersionUID = " + nextLong
                                        + "L;\r\n";
                        }
                        sb.append(line + "\r\n");
                    }

                }

                callback.checkCancel();
                callback.startTask(getString("Progress.15", targetFile.getName())); //$NON-NLS-1$

                if (!sb.toString().equals("")) {

                    writeFile(targetFile, sb.toString(), "utf-8");
                } else {
                    writeFile(targetFile, source, gjf.getFileEncoding());
                }
            } catch (

                    ShellException e)

            {
                e.printStackTrace();
                warnings.add(e.getMessage());
            }
        }

        for (

                String project : projects)

        {
            shellCallback.refreshProject(project);
        }

        callback.done();

    }

    private File getUniqueFileName(File directory, String fileName) {
        File answer = null;

        // try up to 1000 times to generate a unique file name
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 1000; i++) {
            sb.setLength(0);
            sb.append(fileName);
            sb.append('.');
            sb.append(i);

            File testFile = new File(directory, sb.toString());
            if (!testFile.exists()) {
                answer = testFile;
                break;
            }
        }

        if (answer == null) {
            throw new RuntimeException(getString("RuntimeError.3", directory.getAbsolutePath())); //$NON-NLS-1$
        }

        return answer;
    }

    private void writeFile(File file, String content, String fileEncoding) throws IOException {
        FileOutputStream fos = new FileOutputStream(file, false);
        OutputStreamWriter osw;
        if (fileEncoding == null) {
            osw = new OutputStreamWriter(fos);
        } else {
            osw = new OutputStreamWriter(fos, fileEncoding);
        }

        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.close();
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public String getBaseModel() {
        return baseModel;
    }

    public void setBaseModel(String baseModel) {
        this.baseModel = baseModel;
    }

    public String getBaseExample() {
        return baseExample;
    }

    public void setBaseExample(String baseExample) {
        this.baseExample = baseExample;
    }

    public void setExtendsBase(boolean extendsBase) {
        this.extendsBase = extendsBase;
    }

    public void setSupport(boolean support) {
        this.support = support;
    }

    public void setSwagger(boolean swagger) {
        this.swagger = swagger;
    }
}