package io.swagger.generator.online;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.codegen.*;
import io.swagger.generator.exception.ApiException;
import io.swagger.generator.exception.BadRequestException;
import io.swagger.generator.model.GeneratorInput;
import io.swagger.generator.model.InputOption;
import io.swagger.generator.util.ZipUtil;
import io.swagger.models.*;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class Generator {
    static Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    public static Map<String, CliOption> getOptions(String language) throws ApiException {
        CodegenConfig config = null;
        try {
            config = CodegenConfigLoader.forName(language);
        } catch (Exception e) {
            throw new BadRequestException(String.format("Unsupported target %s supplied. %s", language, e));
        }
        Map<String, CliOption> map = new LinkedHashMap<String, CliOption>();
        for (CliOption option : config.cliOptions()) {
            map.put(option.getOpt(), option);
        }
        return map;
    }

    public enum Type {
        CLIENT("client"),
        SERVER("server");

        private String name;

        Type(String name) {
            this.name = name;
        }

        String getTypeName() {
            return name;
        }
    }

    public static String generateClient(String language, GeneratorInput opts) throws ApiException {
        return generate(language, null, opts, Type.CLIENT);
    }

    public static String generateServer(String language, GeneratorInput opts) throws ApiException {
        return generate(language, null, opts, Type.SERVER);
    }

    public static String generateClient(String language, Swagger swagger, GeneratorInput opts) throws ApiException {
        return generate(language, swagger, opts, Type.CLIENT);
    }


    public static String generateServer(String language, Swagger swagger, GeneratorInput opts) throws ApiException {
        return generate(language, swagger, opts, Type.SERVER);
    }

    private static String generate(String language, Swagger swagger, GeneratorInput opts, Type type) throws ApiException {
        LOGGER.debug(String.format("generate %s for %s", type.getTypeName(), language));
        if (opts == null) {
            throw new BadRequestException("No options were supplied");
        }
        JsonNode node = opts.getSpec();
        if (node != null && "{}".equals(node.toString())) {
            LOGGER.debug("ignoring empty spec");
            node = null;
        }
        if (swagger == null) {
            if (node == null) {
                if (opts.getSwaggerUrl() != null) {
                    if (opts.getAuthorizationValue() != null) {
                        List<AuthorizationValue> authorizationValues = new ArrayList<AuthorizationValue>();
                        authorizationValues.add(opts.getAuthorizationValue());

                        swagger = new SwaggerParser().read(opts.getSwaggerUrl(), authorizationValues, true);
                    } else {
                        swagger = new SwaggerParser().read(opts.getSwaggerUrl());
                    }
                } else {
                    throw new BadRequestException("No swagger specification was supplied");
                }
            } else if (opts.getAuthorizationValue() != null) {
                List<AuthorizationValue> authorizationValues = new ArrayList<AuthorizationValue>();
                authorizationValues.add(opts.getAuthorizationValue());
                swagger = new SwaggerParser().read(node, authorizationValues, true);
            } else {
                swagger = new SwaggerParser().read(node, true);
            }
            if (swagger == null) {
                throw new BadRequestException("The swagger specification supplied was not valid");
            }
        }

        String destPath = null;

        if (opts.getOptions() != null) {
            destPath = opts.getOptions().get("outputFolder");
        }
        if (destPath == null) {
            destPath = language + "-"
                    + type.getTypeName();
        }

        convertSwagger(swagger);

        ClientOptInput clientOptInput = new ClientOptInput();
        ClientOpts clientOpts = new ClientOpts();
        String outputFolder = getTmpFolder().getAbsolutePath() + File.separator + destPath;
        String outputFilename = outputFolder + "-bundle.zip";

        clientOptInput
                .opts(clientOpts)
                .swagger(swagger);

        CodegenConfig codegenConfig = null;
        try {
            if (language.equals("java")) {
                codegenConfig = new JavaClientCodegenImpl();
            } else if (language.equalsIgnoreCase("spring")) {
                codegenConfig = new SpringServerCodeGenImpl();
            } else {
                codegenConfig = CodegenConfigLoader.forName(language);
            }
        } catch (RuntimeException e) {
            throw new BadRequestException("Unsupported target " + language + " supplied");
        }

        if (opts.getOptions() != null) {
            codegenConfig.additionalProperties().putAll(opts.getOptions());
            codegenConfig.additionalProperties().put("swagger", swagger);
        }

        codegenConfig.setOutputDir(outputFolder);

        LOGGER.debug(Json.pretty(clientOpts));

        clientOptInput.setConfig(codegenConfig);

        try {
            List<File> files = new DefaultGenerator().opts(clientOptInput).generate();
            if (files.size() > 0) {
                List<File> filesToAdd = new ArrayList<File>();
                LOGGER.debug("adding to " + outputFolder);
                filesToAdd.add(new File(outputFolder));
                ZipUtil zip = new ZipUtil();
                zip.compressFiles(filesToAdd, outputFilename);
            } else {
                throw new BadRequestException("A target generation was attempted, but no files were created!");
            }
            for (File file : files) {
                try {
                    file.delete();
                } catch (Exception e) {
                    LOGGER.error("unable to delete file " + file.getAbsolutePath());
                }
            }
            try {
                new File(outputFolder).delete();
            } catch (Exception e) {
                LOGGER.error("unable to delete output folder " + outputFolder);
            }
        } catch (Exception e) {
            throw new BadRequestException("Unable to build target: " + e.getMessage());
        }
        return outputFilename;
    }

    public static InputOption clientOptions(@SuppressWarnings("unused") String language) {
        return null;
    }

    public static InputOption serverOptions(@SuppressWarnings("unused") String language) {
        return null;
    }

    protected static File getTmpFolder() {
        try {
            File outputFolder = File.createTempFile("codegen-", "-tmp");
            outputFolder.delete();
            outputFolder.mkdir();
            outputFolder.deleteOnExit();
            return outputFolder;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static Set<String> ignoreService = new HashSet<>(Arrays.asList("basic-error-controller",
            "endpoint-mvc-adapter",
            "environment-manager-mvc-endpoint",
            "environment-mvc-endpoint",
            "generic-postable-mvc-endpoint",
            "heapdump-mvc-endpoint",
            "health-mvc-endpoint",
            "jolokia-mvc-endpoint",
            "metrics-mvc-endpoint",
            "restart-mvc-endpoint",
            "log-file-mvc-endpoint",
            "loggers-mvc-endpoint",
            "hystrix-stream-endpoint",
            "audit-events-mvc-endpoint",
            "routes-mvc-endpoint",
            "environment-manager-mvc-endpoint",
            "restart-mvc-endpoint",
            "generic-postable-mvc-endpoint",
            "audit-events-mvc-endpoint"));

    private static Set<String> ignoreModel = new HashSet<>(Arrays.asList("Type", "ModelAndView", "View", "ResponseEntity", "JSONObject"));

    public static Swagger convertSwagger(Swagger swagger) {

        List<Tag> tags = swagger.getTags();
        for (int i = tags.size() - 1; i >= 0; i--) {
            Tag tag = tags.get(i);
            if (ignoreService.contains(tag.getName())) {
                tags.remove(i);
            } else {
                String name = trimImpl(tag.getName());
                tag.setName(name);
            }
        }
        swagger.setTags(tags);


        Map<String, Path> paths = swagger.getPaths();
        List<String> removeServiceKeys = new ArrayList<>();
        for (String key : paths.keySet()) {
            Path path = paths.get(key);
            if (convertPath(path) == null) {
                removeServiceKeys.add(key);
            }
        }
        for (String removeKey : removeServiceKeys) {
            paths.remove(removeKey);
        }

        Map<String, Model> definitions = swagger.getDefinitions();
        List<String> removeModelKeys = new ArrayList<>();
        for (String key : definitions.keySet()) {
            if (ignoreModel.contains(key)) {
                removeModelKeys.add(key);
            }
        }
        for (String removeModelKey : removeModelKeys) {
            definitions.remove(removeModelKey);
        }
        swagger.setPaths(paths);
        return swagger;
    }


    private static String trimImpl(String name) {
        String[] split = name.split("-");
        String suffix = split[split.length - 1];
        if (suffix.equalsIgnoreCase("impl") ||
                suffix.equalsIgnoreCase("controller") ||
                suffix.equalsIgnoreCase("api") ||
                suffix.equalsIgnoreCase("endpoint") ||
                suffix.equalsIgnoreCase("action") ||
                suffix.equalsIgnoreCase("mapping")) {
            String str = "";
            int index = 1;
            if (split[split.length - 2].equalsIgnoreCase("service") || split[split.length - 2].equalsIgnoreCase("api")) {
                index = index + 1;
            }
            for (int i = 0; i < split.length - index; i++) {
                if (split[i].length() <= 1) {
                    str = str + split[i].substring(0, 1).toUpperCase();
                } else {
                    str = str + split[i].substring(0, 1).toUpperCase() + split[i].substring(1);
                }
            }
            return str;
        }
        return name;
    }

    private static String trimUsing(String name) {
        if (name.contains("UsingPUT") || name.contains("UsingDELETE") ||
                name.contains("UsingGET") || name.contains("UsingPATCH") ||
                name.contains("UsingHEAD") || name.contains("UsingPOST")) {
            int lastIndexOf = name.lastIndexOf("Using");
            return name.substring(0, lastIndexOf);
        }
        return name;
    }

    public static void setTags(Operation operation) {
        List<String> tagNames = new ArrayList<>();
        for (String tagName : operation.getTags()) {
            tagNames.add(trimImpl(tagName));
        }
        operation.setTags(tagNames);
    }

    public static void setOperationId(Operation operation) {
        String operationId = operation.getOperationId();
        operation.setOperationId(trimUsing(operationId));
    }

    private static List<Path> convertPath(Path path) {

        List<Path> paths = new ArrayList<>();
        Operation get = path.getGet();
        Operation post = path.getPost();
        Operation put = path.getPut();
        Operation delete = path.getDelete();
        Operation head = path.getHead();
        Operation options = path.getOptions();
        boolean containsIgnoreService = containsIgnoreService(Arrays.asList(get, post, put, delete, head));
        if (!containsIgnoreService) {
            return null;
        }
        if (get != null) {
            setTags(get);
            String operationId = trimUsing(get.getOperationId());
            get.setOperationId(operationId);
//            get.setSummary(operationId);
            Path p = new Path();
            p.setGet(get);
            p.setParameters(path.getParameters());
            paths.add(p);

//            setOperationId(get);
//            path.setDelete(null);
//            path.setHead(null);
//            path.setPatch(null);
//            path.setPut(null);
//            path.setPost(null);
//            path.setOptions(null);
        }

        if (post != null) {
            setTags(post);
            String operationId = trimUsing(post.getOperationId());
            post.setOperationId(operationId);
//            get.setSummary(operationId);
            Path p = new Path();
            p.setPost(post);
            p.setParameters(path.getParameters());
            paths.add(p);

//            setOperationId(post);
//            path.setDelete(null);
//            path.setHead(null);
//            path.setPatch(null);
//            path.setPut(null);
//            path.setGet(null);
//            path.setOptions(null);
        }

        if (put != null) {
            setTags(put);
            String operationId = trimUsing(put.getOperationId());
            put.setOperationId(operationId);
            //            put.setSummary(operationId);
            Path p = new Path();
            p.setPut(put);
            p.setParameters(path.getParameters());
            paths.add(p);
//            setOperationId(put);
//            path.setDelete(null);
//            path.setHead(null);
//            path.setPatch(null);
//            path.setGet(null);
//            path.setPost(null);
        }
        if (delete != null) {
            setTags(delete);
            String operationId = trimUsing(delete.getOperationId());
            delete.setOperationId(operationId);
            //            delete.setSummary(operationId);
            Path p = new Path();
            p.setDelete(delete);
            p.setParameters(path.getParameters());
            paths.add(p);

//            setOperationId(delete);
//            path.setGet(null);
//            path.setHead(null);
//            path.setPatch(null);
//            path.setPut(null);
//            path.setPost(null);
//            path.setOptions(null);
        }

        if (head != null) {
            setTags(head);
            String operationId = trimUsing(head.getOperationId());
            head.setOperationId(operationId);
            //            head.setSummary(operationId);
            Path p = new Path();
            p.setHead(head);
            p.setParameters(path.getParameters());
            paths.add(p);
//            setOperationId(head);
//            path.setDelete(null);
//            path.setGet(null);
//            path.setPatch(null);
//            path.setPut(null);
//            path.setPost(null);
//            path.setOptions(null);
        }

        if (options != null) {
            setTags(options);
            String operationId = trimUsing(options.getOperationId());
            options.setOperationId(operationId);
//            options.setSummary(operationId);
            Path p = new Path();
            p.setOptions(options);
            p.setParameters(options.getParameters());
            paths.add(p);
//            setOperationId(head);
//            path.setDelete(null);
//            path.setGet(null);
//            path.setPatch(null);
//            path.setPut(null);
//            path.setPost(null);
//            path.setOptions(null);
        }
        return paths;
    }

    private static boolean containsIgnoreService(List<Operation> operations) {
        for (Operation operation : operations) {
            if (operation == null || operation.getTags() == null) {
                continue;
            }
            for (String tag : operation.getTags()) {
                if (ignoreService.contains(tag)) {
                    return false;
                }
            }
        }
        return true;
    }

}
