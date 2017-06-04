package com.github.sc.common.utils;

import com.alibaba.fastjson.JSON;
import com.github.sc.gennerator.MetaData;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class TblUtil {

    public static String getColunmType(Connection conn, String tblName, String colunmName) throws Exception {
        List<Map<String, Object>> infoList = getColumnAndMeta(conn, tblName);
        for (Map<String, Object> map : infoList) {

            Object o = map.get("COLUMN_NAME");
            if (o != null) {
                if (o.toString().equalsIgnoreCase(colunmName)) {
                    return map.get("TYPE_NAME").toString();
                }
            }

        }
        return "Object";
    }

    public static String getJavaType(Connection conn, String tblName, String colunmName) throws Exception {
        String colunmType = getColunmType(conn, tblName, colunmName);
        return getJdbcType(colunmType);
    }

    public static String getCreateTblSql(Connection connection, List<String> tables) throws Exception {
        Statement stmt = connection.createStatement();
        StringBuilder sb = new StringBuilder();

        if (tables == null) {
            tables = getTbls(connection);
        }


        for (String table : tables) {
            ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE  `" + table + "`");
            while (rs.next()) {
                String createDDL = rs.getString(2).replace("CREATE TABLE", "CREATE TABLE IF NOT EXISTS ");
                sb.append(createDDL);
                sb.append(";\r\n\r\n");
            }
            rs.close();
        }

        stmt.close();
        return sb.toString();
    }

    public static String getCreateDatabaseSql(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        StringBuilder sb = new StringBuilder();

        String catalog = connection.getCatalog();
        ResultSet rs = stmt.executeQuery("SHOW CREATE DATABASE  `" + catalog + "`");
        while (rs.next()) {
            String createDDL = rs.getString(2).replace("CREATE DATABASE ", "CREATE DATABASE IF NOT EXISTS ");
            sb.append(createDDL);
            sb.append(";\r\n\r\n");
        }
        rs.close();
        stmt.close();
        return sb.toString();
    }


    public static String limiterInsertSql(Connection connection, List<String> tableNames, Integer size) throws Exception {
        StringBuilder insert = new StringBuilder();
        if (tableNames == null) {
            tableNames = getTbls(connection);
        }
        for (String tableName : tableNames) {
            PreparedStatement statement = connection.prepareStatement("select * from " + tableName + " limit " + size);
            statement.execute();
            ResultSetMetaData metaData = statement.getMetaData();
            ResultSet rsm = statement.getResultSet();

            while (rsm.next()) {
                StringBuilder prefix = new StringBuilder("INSERT INTO `" + tableName + "` (");
                StringBuilder suffix = new StringBuilder(") values (");
                for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
                    prefix.append("`")
                            .append(metaData.getColumnName(i))
                            .append("`")
                            .append(",");
                }
                prefix = new StringBuilder(prefix.substring(0, prefix.length() - 1));

                for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
                    Object object = rsm.getObject(i);
                    if (object == null) {
                        suffix.append("null");
                    } else if (object instanceof Integer ||
                            object instanceof Long ||
                            object instanceof BigDecimal ||
                            object instanceof Short ||
                            object instanceof Double ||
                            object instanceof Byte ||
                            object instanceof Boolean) {
                        suffix.append(object);
                    } else {
                        suffix.append("'")
                                .append(object.toString().replace("'", "\\'")
                                        .replace("\"", "\\\""))
                                .append("'");

                    }
                    suffix.append(",");
                }
                suffix = new StringBuilder(suffix.substring(0, suffix.length() - 1));
                suffix.append(");");
                suffix.append("\r\n");

                insert.append(prefix).append(suffix);
            }

            statement.close();
            insert.append("\r\n");
        }

        return insert.toString();


    }

    public static boolean isNumber(String type) {
        if (type == null) {
            return false;
        }
        if (type.equalsIgnoreCase("INT") || type.equalsIgnoreCase("INT UNSIGNED") || type.equalsIgnoreCase("INTEGER") || type.equalsIgnoreCase("MEDIUMINT UNSIGNED")) {
            return true;
        }

        if (type.equalsIgnoreCase("SMALLINT") || type.equalsIgnoreCase("SMALLINT UNSIGNED")) {
            return true;
        }

        if (type.equalsIgnoreCase("TINYINT") || type.equalsIgnoreCase("TINYINT UNSIGNED")) {
            return true;
        }

        if (type.equalsIgnoreCase("BIGINT")) {
            return true;
        }
        return false;
    }

    public static boolean isDate(String type) {
        return type.equalsIgnoreCase("DATETIME") || type.equalsIgnoreCase("TIMESTAMP")
                || type.equalsIgnoreCase("DATE") || type.equalsIgnoreCase("year") || type.equalsIgnoreCase("time");
    }

    public static String getJdbcType(String type) {
        if (type.equalsIgnoreCase("VARCHAR") || type.equalsIgnoreCase("TEXT") || type.equalsIgnoreCase("LONGTEXT")
                || type.equalsIgnoreCase("LONGVARCHAR") || type.equalsIgnoreCase("VARCHAR2") || type.equalsIgnoreCase("json")
                || type.equalsIgnoreCase("set") || type.equalsIgnoreCase("enum") || type.equalsIgnoreCase("numeric")) {
            return "String";
        }

        if (type.equalsIgnoreCase("INT") || type.equalsIgnoreCase("INT UNSIGNED") || type.equalsIgnoreCase("INTEGER") || type.equalsIgnoreCase("MEDIUMINT UNSIGNED")) {
            return "Integer";
        }

        if (type.equalsIgnoreCase("SMALLINT") || type.equalsIgnoreCase("SMALLINT UNSIGNED")) {
            return "Short";
        }

        if (type.equalsIgnoreCase("TINYINT") || type.equalsIgnoreCase("TINYINT UNSIGNED")) {
            return "Byte";
        }

        if (type.equalsIgnoreCase("DATETIME") || type.equalsIgnoreCase("TIMESTAMP")
                || type.equalsIgnoreCase("DATE") || type.equalsIgnoreCase("year") || type.equalsIgnoreCase("time")) {
            return "Date";
        }

        if (type.equalsIgnoreCase("BIGINT")) {
            return "Long";
        }

        if (type.equalsIgnoreCase("DOUBLE")) {
            return "Double";
        }

        if (type.equalsIgnoreCase("decimal")) {
            return "decimal";
        }

        if (type.equalsIgnoreCase("FLOAT")) {
            return "Float";
        }

        if (type.equalsIgnoreCase("BYTE")) {
            return "Byte";
        }

        if (type.equalsIgnoreCase("binary") || type.equalsIgnoreCase("geometry")) {
            return "Byte[]";
        }

        return "Object";
    }


    public static List<String> getImportedKeys(Connection conn, String tableName) throws SQLException {
        ResultSet importedKeys = conn.getMetaData().getImportedKeys(null, null, tableName);
        List<String> keys = new ArrayList<>();
        while (importedKeys.next()) {
            String key = importedKeys.getString(7);
            keys.add(key);
        }
        return keys;
    }

    public static Map<String, List<Map<String, Object>>> getTblsAndMeta(Connection conn) throws Exception {

        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<String> tbls = getTbls(conn);

        // 3. 提取表的名字。
        for (String tblName : tbls) {
            List<Map<String, Object>> colunmNameMap = getColumnAndMeta(conn, tblName);
            map.put(tblName, colunmNameMap);
        }
        return map;
    }

    public static List<String> getTbls(Connection conn) throws Exception {
        String driver = conn.getMetaData().getDriverName().toLowerCase();

        if (driver.contains("sqlserver")) {
            return getSqlServerTbls(conn);
        } else if (driver.contains("h2")) {
            return getH2Tbls(conn);
        } else if (driver.contains("sqlite")) {
            return getSqliteTbls(conn);
        } else if (driver.contains("postgre")) {
            return getPostgreSqlTbls(conn);
        } else if (driver.contains("derby")) {
            return getDerbyTbls(conn);
        }
        return getMysqlTbls(conn);
    }

    public static List<String> getDerbyTbls(Connection connection) throws SQLException {
        List<String> tblList = new ArrayList<String>();

        Statement statement = connection.createStatement();
        statement.execute("select columnnumber, tablename TABLE_NAME, columnname, columndatatype from sys.systables t, sys.syscolumns, sys.sysschemas s tableid=referenceid and t.schemaid=s.schemaid");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            tblList.add(resultSet.getString("TABLE_NAME"));
        }
        return tblList;
    }

    public static List<String> getH2Tbls(Connection conn) throws SQLException {
        List<String> tblList = new ArrayList<String>();

        Statement statement = conn.createStatement();
        statement.execute("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA !='information_schema'");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            tblList.add(resultSet.getString("TABLE_NAME"));
        }
        return tblList;
    }

    public static List<String> getSqliteTbls(Connection conn) throws SQLException {
        List<String> tblList = new ArrayList<String>();

        Statement statement = conn.createStatement();
        statement.execute("select name TABLE_NAME from sqlite_master where type='table' order by name;");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            tblList.add(resultSet.getString("TABLE_NAME"));
        }
        return tblList;
    }

    public static List<String> getPostgreSqlTbls(Connection conn) throws SQLException {
        List<String> tblList = new ArrayList<String>();

        Statement statement = conn.createStatement();
        statement.execute("SELECT   tablename TABLE_NAME FROM   pg_tables WHERE   tablename   NOT   LIKE   'pg%' AND tablename NOT LIKE 'sql_%' ");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            tblList.add(resultSet.getString("TABLE_NAME"));
        }
        return tblList;
    }

    public static List<String> getMysqlTbls(Connection conn) throws Exception {
        List<String> tblList = new ArrayList<String>();
        ResultSet tbls = conn.getMetaData().getTables(null, null, null, null);


        // 3. 提取表的名字。
        while (tbls.next()) {
            tblList.add(tbls.getString("TABLE_NAME"));
        }
        return tblList;
    }

    public static List<String> getSqlServerTbls(Connection conn) throws Exception {
        Statement statement = conn.createStatement();
        String database = conn.getCatalog();
        statement.execute("Select NAME FROM " + database + "..SysObjects Where XType='U' ORDER BY Name");
        ResultSet tbls = statement.getResultSet();

        List<String> tblList = new ArrayList<String>();

        // 3. 提取表的名字。
        while (tbls.next()) {
            tblList.add(tbls.getString(1));
        }
        return tblList;
    }

    public static String getPrimaryKey(Connection conn, String tblName) throws SQLException {
        ResultSet resultSet = conn.getMetaData().getPrimaryKeys(null, null, tblName);
        String primaryKey = "";
        while (resultSet.next()) {
            primaryKey = resultSet.getString(4);
        }

        return primaryKey;

    }

    public static List<Object> getPrimaryKeys(Connection conn, String tblName) throws SQLException {
        ResultSet resultSet = conn.getMetaData().getPrimaryKeys(null, null, tblName);
        List<Object> ps = new ArrayList<>();
        while (resultSet.next()) {
            ps.add(resultSet.getString(4));

        }

        return ps;
    }


    public static String getAutoIncrementColumn(Connection conn, String tblName) throws SQLException {

        ResultSet colRet = conn.getMetaData().getColumns(null, null, tblName, null);
        while (colRet.next()) {
            String is_autoincrement = colRet.getString("IS_AUTOINCREMENT");
            if (is_autoincrement.equalsIgnoreCase("YES")) {
                return colRet.getString("COLUMN_NAME");
            }
        }
        return null;
    }

    public static List<Map<String, Object>> getColumnAndMeta(Connection conn, String tblName) throws Exception {

        ResultSet colRet = conn.getMetaData().getColumns(null, null, tblName, null);
        List<Map<String, Object>> list = new ArrayList();
        while (colRet.next()) {
            Map<String, Object> map = new HashMap<>();
            // 字段名称
            String columnName = colRet.getString("COLUMN_NAME");
            // 字段类型
            String columnType = colRet.getString("TYPE_NAME");
            // 字段长度
            int datasize = colRet.getInt("COLUMN_SIZE");
            // DECIMAL精度
            int digits = colRet.getInt("DECIMAL_DIGITS");

            // 是否必须
            int nullAble = colRet.getInt("NULLABLE");
            String def = colRet.getString("COLUMN_DEF");

            String remark = colRet.getString("REMARKS");

            int ORDINAL_POSITION = colRet.getInt("ORDINAL_POSITION");
            map.put("COLUMN_NAME", columnName.toLowerCase());
            map.put("TYPE_NAME", columnType);
            map.put("COLUMN_SIZE", datasize);
            map.put("REMARKS", remark);
            map.put("DECIMAL_DIGITS", digits);
            map.put("NULLABLE", nullAble);
            map.put("COLUMN_DEF", def);
            map.put("ORDINAL_POSITION", ORDINAL_POSITION);
            list.add(map);
        }

        return list;
    }

    public static List<Map<String, Object>> getIndex(Connection conn, String tblName) throws SQLException {
        List<Map<String, Object>> list = new ArrayList();
        ResultSet indexInfo = conn.getMetaData().getIndexInfo(null, null, tblName, false, true);

        // 输出索引信息
        while (indexInfo.next()) {
            Map<String, Object> map = new HashMap<>();
            int ORDINAL_POSITION = indexInfo.getInt("ORDINAL_POSITION");
            // 获取索引名
            String indexName = indexInfo.getString("INDEX_NAME");

            String indexType = "";
            // 获取索引类型
            switch (indexInfo.getShort("TYPE")) {
                case 0: {
                    indexType = "";
                    break;
                }
                case 1: {
                    indexType = "聚集索引";
                    break;
                }
                case 2: {
                    indexType = "Hash Index";
                    break;
                }
                case 3: {
                    indexType = "Index";
                    break;
                }
            }
            map.put("ORDINAL_POSITION", ORDINAL_POSITION);
            map.put("INDEX_NAME", indexName);
            map.put("INDEX_TYPE", indexType);
            list.add(map);

        }

        return list;
    }

    private static List<String> drivers = Arrays.asList("net.sourceforge.jtds.jdbc.Driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "com.mysql.jdbc.Driver", "org.mariadb.jdbc.Driver");

    public static String genMd(Connection connection, String driverClass, String dest, String language) throws Exception {
        StringBuffer all = new StringBuffer();
        Map<String, List<Map<String, Object>>> tblsAndMeta = TblUtil.getTblsAndMeta(connection);
        for (String tbl : tblsAndMeta.keySet()) {
            List<Map<String, Object>> maps = tblsAndMeta.get(tbl);

            String modelName = "";
            for (String str : tbl.split("_")) {
                modelName += VelocityUtil.firstUp(str);
            }

            StringBuffer tblDesc = new StringBuffer();
            StringBuffer tblIndex = new StringBuffer();

            // sb.append("#" + modelName + "\n\n");
            if (language != null && "ZH".equalsIgnoreCase(language.toUpperCase())) {
                tblDesc.append("### " + tbl
                        + "表描述\n---\n序号|字段|表名称|类型|表字段类型|长度|精度|是否必填|默认值|备注\n  ---|---|---|---|---|---|---|---|---|---\n");

                tblIndex.append("\r\n###" + tbl + "索引描述e\n---\n" + "索引名称|索引类型\n  ---|---\n");
            } else {
                tblDesc.append("### " + tbl
                        + "Table Describe\n---\nNumber|FieldName|ColumnName|FieldType|ColumnType|Length|Digit|NotNull|Default|Remark\n  ---|---|---|---|---|---|---|---|---|---\n");

                tblIndex.append("\r\n###" + tbl + "Index Describe\n---\n" + "IndexName|IndexType\n  ---|---\n");
            }
            for (Map<String, Object> map : maps) {
                String columnName = (String) map.get("COLUMN_NAME");
                String columnType = (String) map.get("TYPE_NAME");
                int datasize = (int) map.get("COLUMN_SIZE");
                int digits = (int) map.get("DECIMAL_DIGITS");

                int position = (int) map.get("ORDINAL_POSITION");

                String remark = (String) map.get("REMARKS");
                if (remark != null) {
                    String[] split = remark.split("\r\n");
                    remark = "";
                    for (String s : split) {
                        remark += s + " ";
                    }

                    split = remark.split("\n");
                    remark = "";
                    for (String s : split) {
                        remark += s + " ";
                    }

                    split = remark.split("\t");
                    remark = "";
                    for (String s : split) {
                        remark += s + " ";
                    }
                }

                int nullable = (int) map.get("NULLABLE");
                String nullDes = nullable == 0 ? "Y" : "N";
                String def = (String) map.get("COLUMN_DEF");

                String jdbcType = TblUtil.getJdbcType(columnType);
                tblDesc.append(position + "|\t" + TblUtil.toHump(columnName) + "|\t" + columnName + "|\t" + jdbcType
                        + "|\t" + columnType + "|\t" + datasize + "|\t" + digits + "|\t" + nullDes + "|\t" + def + "|\t"
                        + remark + "\n");

            }

            tblDesc.append("\r\n\r\n##### create table sql\r\n```\r\n");
            tblDesc.append(getCreateTblSql(connection, Arrays.asList(tbl)) + "\r\n```\r\n");

            all.append(tblDesc.toString() + "\r\n");

            if (drivers.contains(driverClass)) {
                List<Map<String, Object>> indexs = TblUtil.getIndex(connection, tbl);
                for (Map<String, Object> index : indexs) {
                    String indexName = (String) index.get("INDEX_NAME");
                    String indexType = (String) index.get("INDEX_TYPE");
                    tblIndex.append(indexName + "|\t" + indexType + "\n");
                }

            }
            tblDesc.append(tblIndex).toString();


            if (dest != null) {
                write(tblDesc.toString(), dest + "/docs/model/" + modelName + ".md");
            }

        }
        connection.close();

        if (dest != null) {
            write(all.toString(), dest + "/docs/model/All.md");
        }

        return all.toString();
    }



    public static List<MetaData> getMetaDates(Connection connection, String tableName) throws Exception {
        List<Map<String, Object>> columnAndMeta = TblUtil.getColumnAndMeta(connection, tableName);
        return JSON.parseArray(JSON.toJSONString(columnAndMeta), MetaData.class);
    }

    public static void write(String code, String dest) throws IOException {
        if (org.apache.commons.lang.StringUtils.isBlank(code)) {
            return;
        }
        File file = new File(dest);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(out, "utf-8");
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(code);

        }
    }


    public static List<String> getColunms(Connection conn, String tblName) throws Exception {
        ResultSet colRet = conn.getMetaData().getColumns(null, null, tblName, null);
        List<String> colunmList = new ArrayList<String>();
        while (colRet.next()) {
            colunmList.add(colRet.getString("COLUMN_NAME"));
        }
        return colunmList;
    }


    public static List<String> getColumnInfo(Connection conn, String tblName) throws SQLException {
        ResultSet columns = conn.getMetaData().getColumns(null, null, tblName, null);
        List<String> lists = new ArrayList<String>();
        while (columns.next()) {
            String string = columns.getString("REMARKS");
            lists.add(string);
        }
        return lists;

    }


    public static String toHump(String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        String[] split = str.split("_");
        StringBuilder newStr = new StringBuilder();
        newStr.append(split[0]);
        for (int i = 1; i < split.length; i++) {
            newStr.append(split[i].substring(0, 1).toUpperCase()).append(split[i].substring(1));
        }
        str = newStr.toString();
        return str;
    }

    public static String firstUp(String str) {
        String[] split = str.split("_");
        String rep = "";
        for (String s : split) {
            rep += s.substring(0, 1).toUpperCase() + s.substring(1);
        }
        return rep;
    }

    public static String firstUp2(String str2) {
        String[] split = str2.split("_");
        String rep = "";
        for (String s : split) {
            rep += s.substring(0, 1).toUpperCase() + s.substring(1);
        }

        return rep;
    }

}
