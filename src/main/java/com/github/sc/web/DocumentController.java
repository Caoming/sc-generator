package com.github.sc.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.sc.common.utils.FileUtil;
import com.github.sc.model.Document;
import com.github.sc.service.DocumentService;
import com.github.sc.zuul.SwaggerDocDiscovery;
import io.github.swagger2markup.*;
import io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.swagger.generator.online.Generator;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.asciidoctor.*;
import org.jruby.RubyInstanceConfig;
import org.jruby.javasupport.JavaEmbedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

import static org.asciidoctor.Asciidoctor.Factory.create;

/**
 * Created by wuyu on 2017/4/3.
 */
@RestController
@RequestMapping(value = "/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ObjectMapper objectMapper;

    private ObjectMapper yamlMapper = new YAMLMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private JavaPropsMapper javaPropsMapper = new JavaPropsMapper();


    private static String folder = System.getProperty("java.io.tmpdir");

    @Autowired
    private Asciidoctor asciidoctor;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Value("${zuul.servlet-path}")
    private String prefix = "";


    @RequestMapping(value = "/swagger/{language}/{id}.{extension}", method = RequestMethod.GET)
    public Object swagger(HttpServletResponse response,
                          @PathVariable(value = "id") Integer id,
                          @PathVariable(value = "extension") String extension,
                          @RequestParam(value = "convert",defaultValue = "true") boolean convert,
                          @PathVariable(value = "language") String language) throws Exception {
        Document document = documentService.findOne(id);
        if (document == null || document.getContent() == null) {
            return "";
        }
        Swagger swagger = new SwaggerParser()
                .parse(document.getContent());
        if(convert){
            swagger = Generator.convertSwagger(swagger);
        }
        if (extension.equalsIgnoreCase("json")) {
            return swagger;
        } else if (extension.equalsIgnoreCase("yml")) {
            return yamlMapper.writeValueAsString(swagger);
        } else if (extension.equalsIgnoreCase("properties")) {
            return javaPropsMapper.writeValueAsString(swagger);
        } else if (extension.equalsIgnoreCase("pdf")) {
            try (InputStream inputStream = generatorDoc(swagger, extension, document.getTitle(), language)) {
                FileUtil.down(response, inputStream, document.getTitle() + ".pdf");
            }
        } else if (extension.equalsIgnoreCase("doc")) {
            try (InputStream inputStream = generatorDoc(swagger, "html", document.getTitle(), language)) {
                FileUtil.down(response, inputStream, document.getTitle() + ".doc");
            }
        } else {
            try (InputStream inputStream = generatorDoc(swagger, extension, document.getTitle(), language); StringWriter writer = new StringWriter()) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
                IOUtils.copy(inputStreamReader, writer);
                if (language.equalsIgnoreCase("ZH")) {
                    return writer.toString().replaceAll("消耗</h5>", "请求类型</h5>")
                            .replaceAll("生成</h5>", "响应类型</h5>")
                            .replaceAll("##### 消耗", "##### 请求类型")
                            .replaceAll("##### 生成", "##### 响应类型")
                            .replaceAll("===== 消耗", "===== 请求类型")
                            .replaceAll("===== 生成", "===== 响应类型");
                }
                inputStreamReader.close();
                return writer.toString();
            }
        }
        return null;
    }


    @RequestMapping(value = "/", method = RequestMethod.POST)
    public void save(@RequestBody Document document) throws Exception {
        Swagger swagger = new SwaggerParser()
                .read(document.getUrl());
        String content = objectMapper.writeValueAsString(swagger);
        if(StringUtils.isBlank(document.getTitle())){
            document.setTitle(swagger.getInfo().getTitle());
        }
        documentService.save(document.setContent(content));
        applicationEventPublisher.publishEvent(new InstanceRegisteredEvent<>(SwaggerDocDiscovery.class, swagger));
    }

    @RequestMapping(value = "/edit", method = RequestMethod.PUT)
    public void edit(@RequestParam(value = "data") String data, @RequestParam(value = "id", required = false) Integer id) throws IOException {
        Swagger swagger = new SwaggerParser().parse(data);
        if (id != null) {
            documentService.update(id, objectMapper.writeValueAsString(swagger), new Date());
        } else {
            documentService.save(new Document()
                    .setTitle(swagger.getInfo().getTitle())
                    .setCreatedTime(new Date())
                    .setContent(objectMapper.writeValueAsString(swagger)));
        }
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public void upload(MultipartFile file, HttpServletResponse response) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(file.getInputStream(), out);
        Swagger swagger = new SwaggerParser()
                .parse(out.toString("utf-8"));
        out.close();
        String content = objectMapper.writeValueAsString(swagger);
        documentService.save(new Document()
                .setContent(content)
                .setTitle(file.getOriginalFilename()));
        applicationEventPublisher.publishEvent(new InstanceRegisteredEvent<>(SwaggerDocDiscovery.class, swagger));
        response.sendRedirect("/#/document.html");
    }

    @RequestMapping(value = "/swaggerDocProxy/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Swagger swaggerDocProxy(@PathVariable(value = "id") Integer id,
                                   @RequestParam(value = "trimSystemController", defaultValue = "true") boolean trimSystemController,
                                   HttpServletRequest request) {
        Document document = documentService.findOne(id);
        if (document != null) {
            Swagger swagger = new SwaggerParser()
                    .parse(document.getContent());
            if (trimSystemController) {
                swagger = Generator.convertSwagger(swagger);
            }
            swagger.setHost(request.getServerName() + ":" + request.getServerPort() + prefix + "/" + id);
            return swagger;
        }
        return null;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public void updateDocument(@PathVariable(value = "id") Integer id) throws JsonProcessingException {
        Document document = documentService.findOne(id);
        if (document.getUrl() == null) {
            return;
        }
        Swagger swagger = new SwaggerParser()
                .read(document.getUrl());
        String content = objectMapper.writeValueAsString(swagger);
        documentService.update(id, content, new Date());
    }

    public InputStream generatorDoc(Swagger swagger, String extension, String title, String language) throws FileNotFoundException {

        String filePath = folder + "/" + new Date().getTime() + "/";

        Swagger2MarkupConfigBuilder builder = new Swagger2MarkupConfigBuilder();
        if (extension.equalsIgnoreCase("md")) {
            builder.withMarkupLanguage(MarkupLanguage.MARKDOWN);
        } else {
            builder.withMarkupLanguage(MarkupLanguage.ASCIIDOC);
        }


        Swagger2MarkupConfig config = builder.withBasePathPrefix()
                .withInterDocumentCrossReferences()
                .withPathsGroupedBy(GroupBy.TAGS)
                .withTagOrdering(OrderBy.AS_IS)
                .withoutPathSecuritySection()
                .withOutputLanguage(Language.valueOf(language.toUpperCase()))
                .withGeneratedExamples()
                .withFlatBody()
                .build();


        Swagger2MarkupConverter.from(swagger)
                .withConfig(config)
                .build()
                .toFile(Paths.get(filePath + "index"));

        if (extension.equalsIgnoreCase("html") || extension.equalsIgnoreCase("pdf") || extension.equalsIgnoreCase("epub3")) {
            Attributes toc = new Attributes("toc");
            toc.setTitle(title);
            //auto, left, right, macro, preamble
            toc.setTableOfContents(Placement.LEFT);
            //html5, docbook5, docbook45
            toc.setDocType("docbook5");
            toc.setSectNumLevels(4);
            OptionsBuilder optionsBuilder = OptionsBuilder.options()
                    .headerFooter(true)
                    .inPlace(true)
                    .toFile(true)
                    .attributes(toc)
                    .safe(SafeMode.SAFE);


            if (extension.equalsIgnoreCase("pdf")) {
                optionsBuilder.backend("pdf");
                asciidoctor.convertDirectory(new AsciiDocDirectoryWalker(filePath), optionsBuilder);
                return new FileInputStream(filePath + "index.pdf");
            } else if (extension.equalsIgnoreCase("epub3")) {
                optionsBuilder.backend("epub3");
                asciidoctor.convertDirectory(new AsciiDocDirectoryWalker(filePath), optionsBuilder);
                return new FileInputStream(filePath + "index.epub3");
            } else {
                optionsBuilder.backend("html5");
                asciidoctor.convertDirectory(new AsciiDocDirectoryWalker(filePath), optionsBuilder);
                return new FileInputStream(filePath + "index.html");
            }
        } else if (extension.equalsIgnoreCase("md")) {
            return new FileInputStream(filePath + "index.md");
        } else if (extension.equalsIgnoreCase("adoc")) {
            return new FileInputStream(filePath + "index.adoc");
        } else {
            return new FileInputStream(filePath);
        }
    }


    @RequestMapping(value = "findAll", method = RequestMethod.GET)
    public Iterable<Document> findAll() {
        return documentService.findAll();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable(value = "id") Integer id) {
        documentService.delete(id);
        applicationEventPublisher.publishEvent(new InstanceRegisteredEvent<>(SwaggerDocDiscovery.class, -1));
    }


    @Bean
    public Asciidoctor asciidoctor() {
        RubyInstanceConfig rubyInstanceConfig = new RubyInstanceConfig();
        rubyInstanceConfig.setLoader(this.getClass().getClassLoader());
        JavaEmbedUtils.initialize(Arrays.asList("META-INF/jruby.home/lib/ruby/2.0", "classpath:/gems/asciidoctor-1.5.4/lib"), rubyInstanceConfig);
        return create(this.getClass().getClassLoader());
    }


}
