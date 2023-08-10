package com.spl.geo.vector;

import com.spl.geo.common.CoordinatorUtils;
import com.spl.geo.exception.ShpException;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * shp导入数据库
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/7/14
 */
public class Shp2Db {

    private Shp2Db() {
    }

    private static final String GEOM_FIELD_REG = "^(the_geom|shape)$";
    private static final String GEOM_FINAL_FIELD = "geom";

    private static final Logger log = LoggerFactory.getLogger(Shp2Db.class);


    public static void shp2Db(String tableName, String shpPath, JdbcTemplate jdbcTemplate){
        shp2Db(tableName,shpPath,jdbcTemplate,null);
    }

    public static void shp2Db(String tableName, String shpPath, JdbcTemplate jdbcTemplate,String seqName) {
        File shpFile = new File(shpPath);
        String encode = getCharSetByCpg(shpFile.getAbsolutePath());
        String checkCharSet = null;
        if(StringUtils.isEmpty(seqName)){
            seqName = tableName + "_seq";
        }
        //读取shp文件
        ShapefileDataStore fds = null;
        SimpleFeatureIterator features = null;
        try {
            fds = new ShapefileDataStore(shpFile.toURI().toURL());
            Integer crsCode = CoordinatorUtils.getSrId(fds);
            //获取坐标系统epsg
            String geomSql;
            if (crsCode == null || crsCode == 4490) {
                geomSql = ",ST_GeomFromText(?,4490)";
            } else {
                geomSql = ",st_transform(ST_GeomFromText(?," + crsCode + "),4490)";
            }
            log.debug("读取到shp数据属性：{}", fds);
            if (StringUtils.isNotEmpty(encode)) {
                fds.setCharset(Charset.forName(encode));
            }
            SimpleFeatureSource sfs = fds.getFeatureSource();
            SimpleFeatureCollection featureCollection = sfs.getFeatures();

            //获取约束
            Map<String, DbfFieldDef> dbfFieldDefMap = DbfUtils.parseDbfField(shpPath);
            //查询并创建表
            LinkedList<String> fieldList = checkAndCreateTable(tableName, dbfFieldDefMap, featureCollection.getSchema(), jdbcTemplate,seqName);

            //构建插入数据的字段集合
            StringBuilder placeholderSb = new StringBuilder();
            fieldList.forEach(field -> {
                if (field.matches(GEOM_FIELD_REG)) {
                    placeholderSb.append(geomSql);
                } else {
                    placeholderSb.append(",?");
                }
            });

            String fields = fieldList.stream().map(field->"\"" + field + "\"").collect(Collectors.joining(","));
            fields = fields.replaceAll("(the_geom|shape)", GEOM_FINAL_FIELD);
            //组装sql语句
            String insertSql = "insert into \"" + tableName + "\"(" + fields + ") values(" + placeholderSb.substring(1) + ")";
            log.debug("插入要素的sql==>{}", insertSql);

            //批量插入要素的集合，1000条批量插入一次
            List<Object[]> batchValueList = new ArrayList<>();
            features = sfs.getFeatures().features();
            int featureCount = 0;
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Object[] valueObjArr = new Object[fieldList.size()];
                int index = 0;
                for (String field : fieldList) {
                    Object attribute = feature.getAttribute(field);
                    //如果是地理要素字段，必须用toString()方法
                    if (field.matches(GEOM_FIELD_REG)) {
                        valueObjArr[index++] = attribute.toString();
                    } else if ((attribute instanceof String) && StringUtils.isEmpty(encode)) {
                        //防止乱码
                        String attrStr = attribute.toString();
                        if (StringUtils.isEmpty(checkCharSet)) {
                            checkCharSet = checkCharSet(attrStr, checkCharSet);
                        }
                        if (StringUtils.isNotEmpty(checkCharSet)) {
                            attrStr = new String(attrStr.getBytes(StandardCharsets.ISO_8859_1), checkCharSet);
                        }
                        valueObjArr[index++] = attrStr;
                    } else {
                        valueObjArr[index++] = attribute == null ? "" : attribute;
                    }
                }
                batchValueList.add(valueObjArr);
                featureCount++;
                //如果大于1000，要批量插入
                if (batchValueList.size() == 1000) {
                    jdbcTemplate.batchUpdate(insertSql, batchValueList);
                    batchValueList.clear();
                }
            }

            //执行剩余记录
            if (!batchValueList.isEmpty()) {
                jdbcTemplate.batchUpdate(insertSql, batchValueList);
            }
            log.info("共导入矢量要素{}条",featureCount);
        } catch (Exception e) {
            throw new ShpException(e.getMessage(), e);
        } finally {
            if (features != null) {
                features.close();
            }
            if (fds != null) {
                fds.dispose();
            }
        }
    }


    /**
     * 判断字符串的编码格式
     *
     * @param str     需要判断的字符串
     * @param charSet 默认编码格式，如果不为空，直接返回此编码格式
     * @return 判断的编码格式，如果返回null,则不能判断编码
     */
    private static String checkCharSet(String str, String charSet) {
        if (charSet != null && !"".equals(charSet)) {
            return charSet;
        }
        try {
            int lenUtf8 = new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).length();
            int lenGbk = new String(str.getBytes(StandardCharsets.ISO_8859_1), "GBK").length();
            //字符串不包含中文，不能判断是哪一种编码集
            if (lenGbk == lenUtf8) {
                return null;
            }
            return lenUtf8 > lenGbk ? "GBK" : "UTF-8";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "UTF-8";
        }
    }


    /**
     * 检查是否存在表，不存在，则创建，同时返回shp字段的列表
     *
     * @param tableName    创建的表名
     * @param schema       需shp约束
     * @param jdbcTemplate 需要插入数据的数据连接
     * @return 返回字段的名称
     */
    private static LinkedList<String> checkAndCreateTable(String tableName, Map<String, DbfFieldDef> dbfFieldDefMap,
                                                          SimpleFeatureType schema, JdbcTemplate jdbcTemplate,String seqName) {

        LinkedList<String> fieldList = new LinkedList<>();

        //获取shp属性
        Collection<PropertyDescriptor> descriptors = schema.getDescriptors();

        //提取Sql字段
        StringBuilder createTableSqlSb = new StringBuilder();
        for (PropertyDescriptor descriptor : descriptors) {
            String fieldName = descriptor.getName().toString();
            fieldList.add(fieldName);

            if (fieldName.matches(GEOM_FIELD_REG)) {
                appendField(createTableSqlSb, GEOM_FINAL_FIELD, "\"public\".\"geometry\"");
            } else {
                DbfFieldDef dbfFieldDef = dbfFieldDefMap.get(fieldName);
                appendField(createTableSqlSb, fieldName, dbfFieldDef.pgFieldDef());
            }
        }

        //查询数据库名称是否存在
        //查询序列是否存在

        String existTableSql = "select count(*) from pg_class where relname = ?";
        Integer seqCount = jdbcTemplate.queryForObject(existTableSql, Integer.class, seqName);
        if (seqCount == 0) {
            //创建序列
            String seqSql = "create sequence \"" + seqName + "\" increment by 1 minvalue 1 no maxvalue start with 1;";
            log.debug("执行创建序列的sql==>{}", seqSql);
            jdbcTemplate.update(seqSql);
        }

        //查询或创建表
        int tableCount = jdbcTemplate.queryForObject(existTableSql, Integer.class, tableName);
        if (tableCount == 0) {
            String createTableSql = String.format("CREATE TABLE IF NOT EXISTS \"" + tableName + "\"(" +
                            "f_id int8 not null primary key DEFAULT nextval('%s'::regclass),%s);",
                    seqName, createTableSqlSb.substring(1));
            log.debug("执行创建表的sql==>{}", createTableSql);
            //执行sql
            jdbcTemplate.update(createTableSql);
        }
        return fieldList;
    }

    private static void appendField(StringBuilder createTableSqlSb, String fieldName, String type) {
        createTableSqlSb.append(",\"").append(fieldName).append("\" ").append(type);
    }

    /**
     * 判断shp文件的编码格式
     *
     * @param shpPath shp文件路径
     * @return 编码格式
     */
    private static String getCharSetByCpg(String shpPath) {
        String cpgPath = shpPath.substring(0, shpPath.length() - 3) + "cpg";
        File file = new File(cpgPath);
        if (!file.exists()) {
            return null;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine();

        } catch (Exception e) {
            throw new ShpException(e.getMessage(), e);
        }
    }
}
