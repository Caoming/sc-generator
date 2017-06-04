package io.swagger.generator.online;

import io.swagger.codegen.CodegenConstants;
import io.swagger.codegen.languages.SpringCodegen;

/**
 * Created by wuyu on 2017/4/4.
 */
public class SpringServerCodeGenImpl extends SpringCodegen {

    public SpringServerCodeGenImpl() {
        super();
        additionalProperties.put(FULL_JAVA_UTIL,"true");
        additionalProperties.put(CodegenConstants.SERIALIZABLE_MODEL, serializableModel);
        typeMapping.put("Map","java.util.Map");
        typeMapping.put("HashMap","java.util.Map");
        typeMapping.put("hashMap","java.util.Map");
        typeMapping.put("jSONObject","java.util.Map");
        typeMapping.put("JSONObject","java.util.Map");
        typeMapping.put("JSONArray","java.util.List");
        typeMapping.put("jSONArray","java.util.List");
        typeMapping.put("Iterable","java.util.List");
        typeMapping.put("iterable","java.util.List");
        typeMapping.put("iterable","java.util.List");
        typeMapping.put("Set","java.util.Set");
        typeMapping.put("set","java.util.Set");
    }
}
