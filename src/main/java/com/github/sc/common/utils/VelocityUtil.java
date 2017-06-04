package com.github.sc.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VelocityUtil {

	public static String writerGeneratorConfig(String url, String username, String password, String driverClass,
			String packagePath, String targetPath, List<String> tables) throws IOException, SQLException {
		VelocityContext ctx = new VelocityContext();
		Template template = getTempate("templates/generatorConfig.vm");
		ctx.put("url", url);
		ctx.put("username", username);
		ctx.put("password", password);
		ctx.put("driverClass", driverClass);
		ctx.put("tables", tables);

		Map<String, String> primaryKeys = new HashMap<>();

		Connection connection = DriverManager.getConnection(url, username, password);
		for (String tableName : tables) {
			String primaryKey = TblUtil.getPrimaryKey(connection, tableName);
			String key = toHump(primaryKey);
			primaryKeys.put(tableName, key);
		}
		ctx.put("primaryKeys", primaryKeys);
		List<String> models = new ArrayList<String>();
		for (String table : tables) {
			String modelName = tableNameConvertModelName(table);
			models.add(modelName);
		}
		ctx.put("models", models);
		ctx.put("packagePath", packagePath);
		ctx.put("targetPath", targetPath);
		StringWriter writer = new StringWriter();
		template.merge(ctx, writer);
		writer.flush();
		writer.close();
		return writer.toString();
	}

	public static Template getTempate(String path) {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		Template t = ve.getTemplate(path, "utf-8");
		return t;
	}

	public static String tableNameConvertModelName(String tablesName) {
		String[] split = tablesName.split("_");
		String modelName = "";
		for (String string : split) {
			modelName = modelName + firstUp(string);
		}
		return modelName;
	}

	public static String toHump(String str) {
		String fieldName = null;
		if (StringUtils.isNotBlank(fieldName)) {
			return fieldName;
		}
		String[] split = str.split("_");
		StringBuilder newStr = new StringBuilder();
		newStr.append(split[0]);
		for (int i = 1; i < split.length; i++) {
			newStr.append(split[i].substring(0, 1).toUpperCase()).append(split[i].substring(1));
		}
		fieldName = newStr.toString();
		return fieldName;
	}

	public static String firstUp(String str) {

		if (str == null) {
			return null;
		}
		if (str.equals("")) {
			return "";
		}

		if (str.length() == 1) {
			return str.toUpperCase();
		}

		String first = str.substring(0, 1).toUpperCase();
		String last = str.substring(1).toLowerCase();

		return first + last;
	}

    public static String firstLow(String str) {

        if (str == null) {
            return null;
        }
        if (str.equals("")) {
            return "";
        }

        if (str.length() == 1) {
            return str.toUpperCase();
        }

        String first = str.substring(0, 1).toLowerCase();
        String last = str.substring(1);

        return first + last;
    }
}
