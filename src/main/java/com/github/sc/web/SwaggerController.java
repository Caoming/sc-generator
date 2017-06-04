package com.github.sc.web;

import com.github.sc.model.Document;
import com.github.sc.service.DocumentService;
import io.swagger.codegen.*;
import io.swagger.codegen.languages.JavaClientCodegen;
import io.swagger.codegen.languages.SpringCodegen;
import io.swagger.generator.exception.BadRequestException;
import io.swagger.generator.model.GeneratorInput;
import io.swagger.generator.online.Generator;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@RequestMapping("/swagger")
@RestController
public class SwaggerController {
    static List<String> clients = new ArrayList<String>();
    static List<String> servers = new ArrayList<String>();

    private static String folder = System.getProperty("java.io.tmpdir");

    @Autowired
    private DocumentService documentService;

    static {
        List<CodegenConfig> extensions = Codegen.getExtensions();
        for (CodegenConfig config : extensions) {
            if (config.getTag().equals(CodegenType.CLIENT) || config.getTag().equals(CodegenType.DOCUMENTATION)) {
                clients.add(config.getName());

            } else if (config.getTag().equals(CodegenType.SERVER)) {
                servers.add(config.getName());
            }
        }

        Collections.sort(clients, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(servers, String.CASE_INSENSITIVE_ORDER);
    }

    public ResponseEntity<byte[]> downloadFile(File file, String name) throws Exception {
        byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(file);
        return ResponseEntity.ok().header("Content-Type", "application/zip")
                .header("Content-Disposition", "attachment; filename=\"" + name + ".zip\"")
                .header("Accept-Range", "bytes").header("Content-Length", bytes.length + "").body(bytes);

    }

    @RequestMapping(value = "/clients", method = RequestMethod.POST)
    public ResponseEntity<byte[]> generateClient(HttpServletRequest request, @RequestParam("clientLanguage") String clientLanguage,
                                                 GeneratorInput opts,
                                                 @RequestParam(value = "apiPackage", required = false) String apiPackage,
                                                 @RequestParam(value = "artifactId") String artifactId,
                                                 @RequestParam(value = "id") Integer id,
                                                 @RequestParam(value = "groupId") String groupId,
                                                 @RequestParam(value = "convert", defaultValue = "false") boolean convert) throws Exception {


        Map<String, String> options = opts.getOptions();
        if (options == null) {
            options = new HashMap<>();
            opts.setOptions(options);
        }

        if (StringUtils.isNotBlank(apiPackage)) {
            options.put(CodegenConstants.MODEL_PACKAGE, apiPackage + ".model");
            options.put(CodegenConstants.API_PACKAGE, apiPackage + ".api");
        } else {
            options.put(CodegenConstants.MODEL_PACKAGE, "model");
            options.put(CodegenConstants.API_PACKAGE, "api");
        }

        options.put(CodegenConstants.INVOKER_PACKAGE, apiPackage);
        options.put(CodegenConstants.ARTIFACT_ID, artifactId);
        options.put(CodegenConstants.GROUP_ID, groupId);
        options.put(SpringCodegen.BASE_PACKAGE, apiPackage);


        if (clientLanguage.equals("java")) {
            options.put(CodegenConstants.LIBRARY, JavaClientCodegen.RETROFIT_2);
            options.put(JavaClientCodegen.USE_RX_JAVA, "true");
            options.put(CodegenConstants.SOURCE_FOLDER, "src/main/java/");
            //使用java 自带的时间
            options.put(JavaClientCodegen.DATE_LIBRARY, "legacy");

        }

        if (clientLanguage.equals("go")) {
            options.put(CodegenConstants.PACKAGE_NAME, apiPackage);

        }

        Document document = documentService.findOne(id);
        Swagger swagger = new SwaggerParser()
                .parse(document.getContent());
        if (swagger == null) {
            throw new BadRequestException("The swagger specification supplied was not valid");
        }
        String filename = Generator.generateClient(clientLanguage, swagger, opts);

        if (filename != null) {
            return downloadFile(new File(filename), artifactId);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/clients/{language}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, CliOption>> getClientOptions(@PathVariable("language") String language) throws Exception {

        Map<String, CliOption> opts = Generator.getOptions(language);

        if (opts != null) {
            return ResponseEntity.ok().body(opts);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/servers/{framework}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, CliOption>> getServerOptions(@RequestParam("framework") String framework) throws Exception {

        Map<String, CliOption> opts = Generator.getOptions(framework);

        if (opts != null) {
            return ResponseEntity.ok().body(opts);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/clients", method = RequestMethod.GET)
    public ResponseEntity<String[]> clientOptions() {
        String[] languages = new String[clients.size()];
        languages = clients.toArray(languages);
        return ResponseEntity.ok().body(languages);
    }

    @RequestMapping(value = "/servers", method = RequestMethod.GET)
    public ResponseEntity<String[]> serverOptions() {
        String[] languages = new String[servers.size()];
        languages = servers.toArray(languages);
        return ResponseEntity.ok().body(languages);
    }

    @RequestMapping(value = "/servers", method = RequestMethod.POST)
    public ResponseEntity<byte[]> generateServerForLanguage(@RequestParam("serverLanguage") String serverLanguage,
                                                            GeneratorInput opts,
                                                            @RequestParam(value = "id") Integer id,
                                                            @RequestParam(value = "apiPackage", required = false) String apiPackage,
                                                            @RequestParam(value = "artifactId") String artifactId,
                                                            @RequestParam(value = "groupId") String groupId,
                                                            @RequestParam(value = "convert", defaultValue = "false") boolean convert) throws Exception {


        Map<String, String> options = opts.getOptions();
        if (options == null) {
            options = new HashMap<>();
            opts.setOptions(options);
        }

        if (StringUtils.isNotBlank(apiPackage)) {
            options.put(CodegenConstants.MODEL_PACKAGE, apiPackage + ".model");
            options.put(CodegenConstants.API_PACKAGE, apiPackage + ".api");
        } else {
            options.put(CodegenConstants.MODEL_PACKAGE, "model");
            options.put(CodegenConstants.API_PACKAGE, "api");
        }

        options.put(CodegenConstants.INVOKER_PACKAGE, apiPackage);
        options.put(CodegenConstants.ARTIFACT_ID, artifactId);
        options.put(CodegenConstants.GROUP_ID, groupId);
        options.put(SpringCodegen.BASE_PACKAGE, apiPackage);


        if (serverLanguage.equalsIgnoreCase("springcloud") ||
                serverLanguage.equalsIgnoreCase("springmvc") ||
                serverLanguage.equalsIgnoreCase("springboot")) {
            if (serverLanguage.equalsIgnoreCase("springcloud")) {
                options.put(CodegenConstants.LIBRARY, SpringCodegen.SPRING_CLOUD_LIBRARY);
            } else if (serverLanguage.equalsIgnoreCase("springmvc")) {
                options.put(CodegenConstants.LIBRARY, SpringCodegen.SPRING_MVC_LIBRARY);
            } else {
                options.put(CodegenConstants.LIBRARY, SpringCodegen.DEFAULT_LIBRARY);
            }

            options.put(JavaClientCodegen.USE_RX_JAVA, "true");
            options.put(SpringCodegen.CONFIG_PACKAGE, apiPackage + "/config");
            options.put(CodegenConstants.INVOKER_PACKAGE, apiPackage);
            options.put(CodegenConstants.SOURCE_FOLDER, "src/main/java/");
            //使用java 自带的时间
            options.put(JavaClientCodegen.DATE_LIBRARY, "legacy");
            serverLanguage = "spring";
        }
        options.put(SpringCodegen.BASE_PACKAGE, apiPackage);

        if (serverLanguage.equals("go")) {
            options.put(CodegenConstants.PACKAGE_NAME, apiPackage);
        }
        Document document = documentService.findOne(id);
        Swagger swagger = new SwaggerParser()
                .parse(document.getContent());
        if (swagger == null) {
            throw new BadRequestException("The swagger specification supplied was not valid");
        }
        swagger = Generator.convertSwagger(swagger);
        String filename = Generator.generateServer(serverLanguage, swagger, opts);

        if (filename != null) {
            return downloadFile(new File(filename), artifactId);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
