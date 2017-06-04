package com.github.sc.db;

import com.github.sc.common.utils.TblUtil;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2015/1/12.
 */
public class MysqlExport {

    private String url;

    private String username;

    private String password;

    private List<String> tableNames = new ArrayList<>();

    private static Logger logger = LoggerFactory.getLogger(MysqlExport.class);

    public MysqlExport(String url, String username, String password, List<String> tableNames) {
        this.username = username;
        this.password = password;
        this.url = url;
        if (tableNames != null) this.tableNames.addAll(tableNames);
    }

    public void dump(OutputStream out) throws Exception {
        Connection connection = DriverManager.getConnection(url, username, password);
        if (tableNames.size() == 0) tableNames = TblUtil.getTbls(connection);

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        statement.execute("SHOW MASTER STATUS");
        ResultSet resultSet = statement.getResultSet();
        while (resultSet.next()) {
            String binlogFile = resultSet.getString(1);
            long position = resultSet.getLong(2);
            out.write(("/* binlogFile: " + binlogFile).getBytes("utf-8"));
            out.write((" position: " + position + "*/\r\n").getBytes("utf-8"));
        }

        String createTblSql = TblUtil.getCreateDatabaseSql(connection)+TblUtil.getCreateTblSql(connection, tableNames);
        out.write(createTblSql.getBytes("utf-8"));

        for (String tableName : tableNames) {
            Statement countStatement = connection.createStatement();
            countStatement.execute("select count(*) from " + tableName);
            ResultSet countResultSet = countStatement.getResultSet();
            countResultSet.next();
            long count = countResultSet.getLong(1);

            int pageCount = 1000;
            int taskNumber = (int) (count / pageCount);
            int remain = (int) (count % pageCount);

            String autoIncrementColumn;
            String columnType;
            try {
                autoIncrementColumn = TblUtil.getAutoIncrementColumn(connection, tableName);
                columnType = TblUtil.getColunmType(connection, tableName, autoIncrementColumn);
            } catch (Exception e) {
                logger.warn(e.getMessage());
                autoIncrementColumn = TblUtil.getPrimaryKey(connection, tableName);
                columnType = TblUtil.getColunmType(connection, tableName, autoIncrementColumn);
            }

            if (TblUtil.isNumber(columnType)) {
                long index = 0;
                for (int j = 0; j < taskNumber; j++) {
                    String sql = "select * from " + tableName + " where " + autoIncrementColumn + " >" + index + " order by " + autoIncrementColumn + " asc limit " + pageCount;
                    index = generatorSql(connection, sql, tableName, autoIncrementColumn, out);
                }

                if (remain != 0) {
                    String sql = "select * from " + tableName + " where " + autoIncrementColumn + " >" + index + " order by " + autoIncrementColumn + " asc limit " + remain;
                    generatorSql(connection, sql, tableName, autoIncrementColumn, out);
                }

            } else {
                for (int j = 0; j < taskNumber; j++) {
                    int startNum = j * pageCount;
                    String sql = "select * from " + tableName + " limit " + startNum + ", " + pageCount;
                    generatorSql(connection, sql, tableName, null, out);
                }

                if (remain != 0) {
                    String sql = "select * from " + tableName + " limit " + taskNumber * pageCount + ", " + remain;
                    generatorSql(connection, sql, tableName, null, out);
                }
            }
        }
        connection.commit();
        connection.setAutoCommit(true);
        connection.close();
    }


    public InputStream dump(String binlogFile, Long position, OutputStream out) throws Exception {
        Connection connection = DriverManager.getConnection(this.url, this.username, this.password);

        String host = url.substring(0, url.lastIndexOf("/")).substring(url.substring(0, url.lastIndexOf("/")).lastIndexOf("/") + 1);
        int port = 3306;
        String database = url.substring(url.lastIndexOf("/") + 1);
        if (host.contains(":")) {
            String[] split = host.split(":");
            host = split[0];
            port = Integer.parseInt(split[1]);
        }

        final Map<Long, String> tableMap = new ConcurrentHashMap<>();

        BinaryLogClient client = new BinaryLogClient(host, port, username, password);
        if (binlogFile != null && position != null) {
            client.setBinlogFilename(binlogFile);
            client.setBinlogPosition(position);
        }

        BinaryLogClient.EventListener listener = new BinaryLogClient.EventListener() {
            @Override
            public void onEvent(Event event) {
                EventData data = event.getData();
                if (data == null) {
                    return;
                }
                if (data instanceof QueryEventData) {
                    QueryEventData queryEventData = (QueryEventData) data;
                    String sql = queryEventData.getSql();
                } else if (data instanceof TableMapEventData) {
                    TableMapEventData tableMapEventData = (TableMapEventData) data;
                    String table = tableMapEventData.getTable();
                    long tableId = tableMapEventData.getTableId();
                    tableMap.put(tableId, table);
                } else if (data instanceof WriteRowsEventData) {
                    WriteRowsEventData writeRowsEventData = (WriteRowsEventData) data;
                    List<Serializable[]> rows = writeRowsEventData.getRows();
                    BitSet includedColumns = writeRowsEventData.getIncludedColumns();
                    String tableName = tableMap.get(writeRowsEventData.getTableId());
                    StringBuilder sb = new StringBuilder();
                    sb.append("INSERT INTO `")
                            .append(tableName)
                            .append("` ");
                    for (Serializable[] row : rows) {
                        for (Serializable serializable : row) {
                            if (serializable instanceof byte[]) {
                                System.err.println(new java.lang.String((byte[]) serializable));
                            }
                        }
                    }
                } else if (data instanceof UpdateRowsEventData) {
                    UpdateRowsEventData updateRowsEventData = (UpdateRowsEventData) data;
                    List<Map.Entry<Serializable[], Serializable[]>> rows = updateRowsEventData.getRows();
                    String tableName = tableMap.get(updateRowsEventData.getTableId());

                } else if (data instanceof DeleteRowsEventData) {
                    DeleteRowsEventData deleteRowsEventData = (DeleteRowsEventData) data;
                    List<Serializable[]> rows = deleteRowsEventData.getRows();
                    String tableName = tableMap.get(deleteRowsEventData.getTableId());

                }
            }
        };
        client.registerEventListener(listener);
        client.connect();
        return null;
    }

    public static void main(String[] args) throws Exception {
//        MysqlExport mysqlExport = new MysqlExport("jdbc:mysql://rm-2zezf1dl45yqvh4zzo.mysql.rds.aliyuncs.com/hbase_tool", "visualchina", "visualchina123", null);
//        mysqlExport.dump(null, null, new FileOutputStream("g:/dump.sql"));
        MysqlExport mysqlExport = new MysqlExport("jdbc:mysql://localhost:3306/xiaoniu_user", "root", "root", null);
        mysqlExport.dump(null, null, new FileOutputStream("g:/dump.sql"));
    }

    private long generatorSql(Connection connection, String sql, String tableName, String primaryKey, OutputStream out) throws SQLException, IOException {
        logger.info(sql);
        StringBuilder insert = new StringBuilder();
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.execute();
        ResultSetMetaData metaData = statement.getMetaData();
        ResultSet rsm = statement.getResultSet();

        long index = 0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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
                } else if (object instanceof byte[]) {
                    suffix.append("unhex('")
                            .append(Hex.encodeHexString((byte[]) object))
                            .append("')");
                } else if (object instanceof java.util.Date) {
                    suffix.append("'")
                            .append(simpleDateFormat.format(object))
                            .append("'");

                } else {
                    suffix.append("'")
                            .append(String.valueOf(object)
                                    .replace("'", "\\'")
                                    .replace("\"", "\\\""))
                            .append("'");

                }

                if (StringUtils.isNotBlank(primaryKey)) {
                    Object primaryResult = rsm.getObject(primaryKey);
                    if (primaryResult instanceof Long) {
                        index = (long) primaryResult;
                    } else {
                        index = Long.parseLong(String.valueOf(primaryResult));
                    }

                }
                suffix.append(",");
            }
            suffix = new StringBuilder(suffix.substring(0, suffix.length() - 1));
            suffix.append(");");
            suffix.append("\r\n");

            insert.append(prefix).append(suffix);
        }
        rsm.close();
        statement.close();
        out.write(insert.toString().getBytes("utf-8"));
        out.flush();
        return index;
    }
}