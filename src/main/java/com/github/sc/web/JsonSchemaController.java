package com.github.sc.web;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by wuyu on 2017/5/27.
 */
@RequestMapping(value = "/jsonschema")
@RestController
public class JsonSchemaController {

    private static String folder = System.getProperty("java.io.tmpdir");


    @RequestMapping(value = "/generator/preview", method = RequestMethod.POST)
    public String preview(@RequestParam(value = "schema") String schema,
                          @RequestParam(value = "targetpackage") String targetpackage,
                          @RequestParam(value = "sourcetype", required = false) final String sourcetype,
                          @RequestParam(value = "annotationstyle", required = false) final String annotationstyle,
                          @RequestParam(value = "usedoublenumbers", required = false) final boolean usedoublenumbers,
                          @RequestParam(value = "includeaccessors", required = false) final boolean includeaccessors,
                          @RequestParam(value = "includeadditionalproperties", required = false) final boolean includeadditionalproperties,
                          @RequestParam(value = "propertyworddelimiters", required = false) final String propertyworddelimiters,
                          @RequestParam(value = "classname") String classname) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        JCodeModel codegenModel = getCodegenModel(schema, targetpackage, sourcetype, annotationstyle, usedoublenumbers, includeaccessors, includeadditionalproperties, propertyworddelimiters, classname);
        codegenModel.build(new CodeWriter() {
            @Override
            public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
                return byteArrayOutputStream;
            }

            @Override
            public void close() throws IOException {
                byteArrayOutputStream.close();
            }
        });
        return byteArrayOutputStream.toString("utf-8");
    }

    protected JCodeModel getCodegenModel(String schema,
                                         String targetpackage,
                                         final String sourcetype,
                                         final String annotationstyle,
                                         final boolean usedoublenumbers,
                                         final boolean includeaccessors,
                                         final boolean includeadditionalproperties,
                                         final String propertyworddelimiters,
                                         String classname) throws IOException {

        JCodeModel codeModel = new JCodeModel();

        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() { // set config option by overriding method
                return true;
            }

            @Override
            public SourceType getSourceType() {
                return SourceType.valueOf(sourcetype.toUpperCase());
            }

            @Override
            public AnnotationStyle getAnnotationStyle() {
                return AnnotationStyle.valueOf(annotationstyle.toUpperCase());
            }

            @Override
            public boolean isUseDoubleNumbers() {
                return usedoublenumbers;
            }

            @Override
            public boolean isIncludeAccessors() {
                return includeaccessors;
            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return includeadditionalproperties;
            }

            @Override
            public char[] getPropertyWordDelimiters() {
                return propertyworddelimiters.replace(" ", "").toCharArray();
            }


        };


        AnnotatorFactory factory = new AnnotatorFactory(config);
        CompositeAnnotator annotator = factory.getAnnotator(factory.getAnnotator(config.getAnnotationStyle()), factory.getAnnotator(config.getCustomAnnotator()));
        SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, annotator, new SchemaStore()), new SchemaGenerator());
        mapper.generate(codeModel, classname, targetpackage, schema);
        return codeModel;
    }
}
