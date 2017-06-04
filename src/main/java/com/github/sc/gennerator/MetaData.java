package com.github.sc.gennerator;

import com.alibaba.fastjson.annotation.JSONField;
import com.github.sc.common.utils.TblUtil;
import org.apache.commons.lang.StringUtils;

/**
 * Created by wuyu on 2015/3/23.
 */

public class MetaData {

    //字段名称
    @JSONField(name = "COLUMN_NAME")
    private String columnName;

    //字段类型
    @JSONField(name = "TYPE_NAME")
    private String typeName;

    //字段大小
    @JSONField(name = "COLUMN_SIZE")
    private String columnSize;

    //备注
    @JSONField(name = "REMARKS")
    private String remarks;

    //是否允许为空
    @JSONField(name = "NULLABLE")
    private int nullAble;

    @JSONField(name = "DECIMAL_DIGITS")
    private int decimalDigits;


    public MetaData() {
    }

    public MetaData(String columnName, String typeName, String columnSize, String remarks, int nullAble, int decimalDigits) {
        this.columnName = columnName;
        this.typeName = typeName;
        this.columnSize = columnSize;
        this.remarks = remarks;
        this.nullAble = nullAble;
        this.decimalDigits = decimalDigits;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = TblUtil.getJdbcType(typeName);
    }

    public String getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(String columnSize) {
        this.columnSize = columnSize;
    }

    public String getRemarks() {
        if (remarks != null) {
            return StringUtils.join(StringUtils.join(remarks.split("\r\n")," ").split("\n")," ").replace("\"", "");
        }
        return "";
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public int getNullAble() {
        return nullAble;
    }

    public void setNullAble(int nullAble) {
        this.nullAble = nullAble;
    }

    public int getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

}
