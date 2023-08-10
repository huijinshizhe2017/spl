package com.spl.geo.vector;

/**
 * dbf字段定义
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/9/1
 */
class DbfFieldDef {


    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 类型，包括NUMERIC、CHARACTER、DATE
     */
    private String typeName;

    /**
     * 长度
     */
    private Integer length;

    /**
     * 小数精度
     */
    private Integer decimal;

    public DbfFieldDef(String fieldName, String typeName, Integer length, Integer decimal) {
        this.fieldName = fieldName;
        this.typeName = typeName;
        this.length = length;
        this.decimal = length > decimal ? decimal : length - 1;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getDecimal() {
        return decimal;
    }

    public void setDecimal(Integer decimal) {
        this.decimal = decimal;
    }


    public String pgFieldDef() {
        //NUMERIC、CHARACTER、DATE
        if ("NUMERIC".equalsIgnoreCase(typeName)) {
            int len = this.decimal == 0 ? getLength() : getLength() - 1;
            return "numeric(" + len + "," + decimal + ")";
        } else if ("CHARACTER".equalsIgnoreCase(typeName)) {
            return "varchar(" + getLength() + ")";
        } else if ("DATE".equalsIgnoreCase(typeName)) {
            return "date";
        } else {
            return "varchar(255)";
        }
    }
}
