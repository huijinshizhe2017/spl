package com.spl.geo.vector;

import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import com.spl.geo.exception.DbfException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * dbf工具类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/9/1
 */
class DbfUtils {

    private DbfUtils() {
    }

    public static final String DBF_SUFFIX = ".dbf";


    public static Map<String, DbfFieldDef> parseDbfField(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new DbfException("输入路径不能为为空");
        }
        File dbfFile = new File(path);
        if (path.endsWith(ShpFileUtils.SHP_SUFFIX)) {
            String name = dbfFile.getName();
            name = name.substring(0, name.length() - 4) + DBF_SUFFIX;
            dbfFile = new File(dbfFile.getParentFile(), name);
            if (!dbfFile.exists()) {
                throw new DbfException("输入的shp文件");
            }
        }

        //读取文件
        try (InputStream fis = new FileInputStream(dbfFile);
             DBFReader reader = new DBFReader(fis, Charset.forName("GBK"))) {

            int fieldCount = reader.getFieldCount();
            Map<String, DbfFieldDef> dbfFieldDefMap = new HashMap<>(fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                DBFField field = reader.getField(i);
                dbfFieldDefMap.put(field.getName(), new DbfFieldDef(field.getName(), field.getType().name(), field.getLength(), field.getDecimalCount()));
            }
            return dbfFieldDefMap;
        } catch (Exception e) {
            throw new DbfException(e.getMessage(), e);
        }
    }
}
