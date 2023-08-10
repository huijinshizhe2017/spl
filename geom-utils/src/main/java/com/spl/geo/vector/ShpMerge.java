package com.spl.geo.vector;

import com.spl.geo.exception.ShpException;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import java.util.List;

/**
 * shp合并
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/6/4 9:26
 */
public class ShpMerge {

    private ShpMerge() {
    }

    private static final String MULTIPOLYGON = "MultiPolygon";
    private static final String POLYGON = "Polygon";


    public static void mergerShp(List<String> shpPathList, String outShpPath) {
        if (shpPathList == null || shpPathList.size() <= 1) {
            throw new ShpException("shpPath集合为空或者小于等于,无必要合并Shp图层");
        }
        Pair<SimpleFeatureCollection, SimpleFeatureType> shpInfo = ReadShpUtils.getShpInfo(shpPathList.get(0));
        SimpleFeatureType type = shpInfo.getRight();
        List<SimpleFeature> descFeatureList = GeometryUtils.collection2List(shpInfo.getKey());

        for (int i = 1; i < shpPathList.size(); i++) {
            List<SimpleFeature> tempFeatureList = ReadShpUtils.getFeatureListFromShp(shpPathList.get(i));
            mergerShp(tempFeatureList, descFeatureList);
        }

        //输出结果集，即将shp2输出结果集
        WriteShpUtils.buildShpByFeatureList(descFeatureList, type, outShpPath);
    }

    /**
     * 合并shp图层
     *
     * @param shpPath1   要合并的shp1
     * @param shpPath2   要合并的shp2
     * @param outShpPath 输出shp路径
     */
    public static void mergeShp(String shpPath1, String shpPath2, String outShpPath) {
        Pair<SimpleFeatureCollection, SimpleFeatureType> shpInfo = ReadShpUtils.getShpInfo(shpPath1);
        SimpleFeatureType type = shpInfo.getRight();

        //获取shp1的要素集合
        List<SimpleFeature> featureList1 = ReadShpUtils.getFeatureListFromShp(shpPath1);
        List<SimpleFeature> featureList2 = ReadShpUtils.getFeatureListFromShp(shpPath2);
        mergerShp(featureList1, featureList2);

        //输出结果集，即将shp2输出结果集
        WriteShpUtils.buildShpByFeatureList(featureList2, type, outShpPath);
    }


    private static void mergerShp(List<SimpleFeature> srcFeatureList, List<SimpleFeature> descFeatureList) {
        //featureList2 为输出结果
        for (SimpleFeature feature : srcFeatureList) {
            Geometry geometry1 = (Geometry) feature.getDefaultGeometry();
            if (geometry1 == null) {
                continue;
            }
            for (int i = descFeatureList.size() - 1; i >= 0; i--) {
                SimpleFeature feature2 = descFeatureList.get(i);
                Geometry geometry2 = (Geometry) feature2.getDefaultGeometry();
                if (geometry1.intersects(geometry2)) {
                    Geometry union = geometry1.union(geometry2);
                    geometry1 = parseGeom(union);
                    geometry1.normalize();
                    descFeatureList.remove(i);
                }
            }
            feature.setDefaultGeometry(geometry1);
            descFeatureList.add(feature);
        }
    }

    /**
     * 将多面或者面要素转换为多面要素
     *
     * @param geometry 需要转换的要素
     * @return 转换结果，如果传入的未非面或者多面，则返回null。
     */
    private static MultiPolygon parseGeom(Geometry geometry) {

        //防止自相交
        geometry = geometry.intersection(geometry);

        String geometryType = geometry.getGeometryType();
        if (MULTIPOLYGON.equals(geometryType)) {
            return (MultiPolygon) geometry;
        }
        //将polygon转换为MultiPolygon
        if (POLYGON.equals(geometryType)) {
            GeometryFactory gf = new GeometryFactory();
            Polygon[] polygons = new Polygon[1];
            polygons[0] = (Polygon) geometry;
            return gf.createMultiPolygon(polygons);
        }

        throw new ShpException("转换的地理要素不是面状要素");
    }
}
