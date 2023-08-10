package com.spl.geo.vector;

import com.spl.geo.exception.ShpException;
import net.postgis.jdbc.PGgeometry;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 写矢量工具类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/17
 */
public class WriteShpUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteShpUtils.class);

    private WriteShpUtils() {
    }

    public static void buildShpByFeatureCollection(SimpleFeatureCollection collection, SimpleFeatureType type, String outShpPath,String charSet) {
        File outShpFile = ShpFileUtils.getEmptyShpFromPath(outShpPath);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore newDataStore = null;
        try (Transaction transaction = new DefaultTransaction("create")) {
            Map<String, Serializable> params = new HashMap<>(2);
            params.put("url", outShpFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);

            newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(type);
            newDataStore.setCharset(Charset.forName(charSet));

            String typeName = newDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

            if (!(featureSource instanceof SimpleFeatureStore)) {
                throw new ShpException(typeName + " does not support read/write access");
            }
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            featureStore.setTransaction(transaction);
            featureStore.addFeatures(collection);
            transaction.commit();
        } catch (Exception e) {
            throw new ShpException(e.getMessage(), e);
        } finally {
            if (newDataStore != null) {
                newDataStore.dispose();
            }
        }
    }



    /**
     * @param collection 需要创建shp的要素对象
     * @param type       要素类型
     * @param outShpPath 输出shp的文件路径
     */
    public static void buildShpByFeatureCollection(SimpleFeatureCollection collection, SimpleFeatureType type, String outShpPath) {
        buildShpByFeatureCollection(collection,type,outShpPath,"UTF-8");
    }

    /**
     * list转shp
     *
     * @param featureList 要素集合
     * @param shpPath     shp输出路径
     */
    public static void list2Shp(List<Map<String, Object>> featureList, String shpPath) {
        if (featureList == null || featureList.isEmpty()) {
            LOGGER.info("数据表没有相关数据...");
            return;
        }
        //
        List<SimpleFeature> features = new ArrayList<>();

        //构建第一个要素以及构建schema
        Map<String, Object> firstKv = featureList.get(0);

        List<String> keyList = new ArrayList<>();
        StringBuilder typeBuilder = new StringBuilder();
        for (Map.Entry<String, Object> firstEntity : firstKv.entrySet()) {
            String key = firstEntity.getKey();
            if (key.matches("(geom|shape|wkt)")) {
                typeBuilder.append(",").append("the_geom:MultiPolygon:srid=4490");
                keyList.add(key);
                continue;
            }
            typeBuilder.append(",").append(key).append(":").append(mapType(firstEntity.getValue()));
            keyList.add(key);
        }
        try {
            //获取type
            SimpleFeatureType type = DataUtilities.createType("Location", typeBuilder.substring(1));
            //组装featureList
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
            WKTReader reader = new WKTReader();
            for (Map<String, Object> kv : featureList) {
                for (String key : keyList) {
                    if ("wkt".equalsIgnoreCase(key)) {
                        featureBuilder.add(reader.read(kv.get(key).toString()));
                    } else if (key.matches("(geom|shape)")) {
                        PGgeometry pGgeometry = new PGgeometry(kv.get(key).toString());
                        String s = pGgeometry.toString();
                        featureBuilder.add(reader.read(s.substring(s.indexOf(";") + 1)));
                    } else {
                        featureBuilder.add(kv.get(key));
                    }
                }
                features.add(featureBuilder.buildFeature(null));
            }
            WriteShpUtils.buildShpByFeatureList(features, type, shpPath);
        } catch (Exception e) {
            throw new ShpException(e.getMessage(), e);
        }
    }

    /**
     * 构建Shp文件
     *
     * @param featureList 构建shp文件的要素集合
     * @param outShpPath  shp的输出路径
     */
    public static void buildShpByFeatureList(List<SimpleFeature> featureList, SimpleFeatureType type, String outShpPath) {
        SimpleFeatureCollection collection = new ListFeatureCollection(type, featureList);
        buildShpByFeatureCollection(collection, type, outShpPath);
    }

    /**
     * wkt集合转shp
     *
     * @param wktList wkt集合
     * @param shpPath 输出shp的路径
     */
    public static void wktList2Shp(List<String> wktList, String shpPath) {
        //获取shp名称
        File shpFile = new File(shpPath);
        if (!shpPath.endsWith(ShpFileUtils.SHP_SUFFIX)) {
            shpFile = new File(shpFile, shpFile.getName() + ShpFileUtils.SHP_SUFFIX);
        }
        //封装集合
        try {
            SimpleFeatureCollection featureCollection = GeometryUtils.wktList2Feature(wktList, 4490);
            WriteShpUtils.buildShpByFeatureCollection(featureCollection, featureCollection.getSchema(), shpFile.getAbsolutePath());
        } catch (Exception e) {
            throw new ShpException(e.getMessage(), e);
        }
    }


    private static String mapType(Object obj) {
        if (obj instanceof Double) {
            return "Double";
        } else if (obj instanceof Float) {
            return "Double";
        } else if (obj instanceof Timestamp) {
            return "Date";
        } else if (obj instanceof Integer) {
            return "Integer";
        } else if (obj instanceof Long) {
            return "Integer";
        } else {
            return "String";
        }
    }
}
