package com.spl.geo.vector;

import com.spl.geo.common.CoordinatorUtils;
import com.spl.geo.exception.GeometryException;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 地理要素处理类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/15geometry
 */
public class GeometryUtils {

    private GeometryUtils() {
    }

    /**
     * wkt转feature
     *
     * @param wktList wkt集合
     * @param srid    目标srid
     * @return feature集合
     */
    public static SimpleFeatureCollection wktList2Feature(List<String> wktList, Integer srid) {
        //封装集合
        try {
            WKTReader reader = new WKTReader();
            List<SimpleFeature> features = new ArrayList<>();
            final SimpleFeatureType type = DataUtilities.createType("Location", "*the_geom:MultiPolygon:srid=" + srid);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
            for (String wkt : wktList) {
                Geometry read = reader.read(wkt);
                Geometry reverse = read.reverse();
                featureBuilder.add(reverse);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }
            return new ListFeatureCollection(type, features);
        } catch (Exception e) {
            throw new GeometryException(e.getMessage(), e);
        }
    }


    /**
     * geom转wkt
     *
     * @param geometry 地理要素
     * @return wkt字符串
     */
    public static String geometry2Wkt(Geometry geometry) {
        WKTWriter wktWriter = new WKTWriter();
        return wktWriter.write(geometry);
    }

    public static List<SimpleFeature> collection2List(SimpleFeatureCollection simpleFeatureCollection) {
        List<SimpleFeature> featureList = new ArrayList<>(simpleFeatureCollection.size());
        SimpleFeature[] featureArray = new SimpleFeature[simpleFeatureCollection.size()];
        featureList.addAll(Arrays.asList(simpleFeatureCollection.toArray(featureArray)));
        return featureList;
    }

    /**
     * wkt转地理要素
     *
     * @param wkt wkt字符串
     * @return 地理要素
     */
    public static Geometry wkt2Geometry(String wkt) {
        try {
            WKTReader reader = new WKTReader();
            return reader.read(wkt);
        } catch (Exception e) {
            throw new GeometryException(e.getMessage(), e);
        }
    }

    /**
     * 按照度带计算面积，支持4326与4490的转换
     *
     * @param geometry 地理要素
     * @return 面积数值，单位m2
     */
    public static Double calAreaByGs(Geometry geometry) {
        int srid = geometry.getSRID();
        //计算属于哪一度带
        Point centroid = geometry.getCentroid();
        Double code = Math.floor((centroid.getX() + 1.5) / 3);
        Geometry targetGeometry;
        if (srid == 4326 || srid == 4490) {
            Integer baseCode = srid == 4326 ? 2324 : 4488;
            targetGeometry = CoordinatorUtils.transform(geometry, baseCode + code.intValue());
        } else {
            targetGeometry = geometry;
        }
        return targetGeometry.getArea();
    }

    public static Double calArea(Geometry geometry) {
        return geometry.getArea();
    }

    /**
     * 查询srid
     *
     * @param schema 约束
     * @return srid值
     */
    public static Integer getSrId(SimpleFeatureType schema) {
        try {
            String wkt = schema.getCoordinateReferenceSystem().toWKT();
            CoordinateReferenceSystem crsTarget = CRS.parseWKT(wkt);
            return CRS.lookupEpsgCode(crsTarget, true);
        } catch (Exception e) {
            throw new GeometryException(e.getMessage(), e);
        }
    }
}
