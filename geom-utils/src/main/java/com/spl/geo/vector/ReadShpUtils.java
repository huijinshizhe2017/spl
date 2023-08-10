package com.spl.geo.vector;

import com.spl.geo.exception.ShpException;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 读取shp工具类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/17
 */
public class ReadShpUtils {

    private ReadShpUtils() {
    }

    /**
     * 通过shp路径获取矢量要素集合
     *
     * @param shpPath shp路径，可以以shp结尾，也可以文件夹
     * @return 要素集合
     */
    public static SimpleFeatureCollection getFeatureCollectionFromShp(String shpPath) {
        File shpFile = ShpFileUtils.getShpFile(shpPath);
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(shpFile);
            return store.getFeatureSource().getFeatures();
        } catch (IOException e) {
            throw new ShpException(e.getMessage(), e);
        }
    }

    /**
     * 读取shp到集合
     *
     * @param shpPath shp文件路径
     * @return 要素集合
     */
    public static List<SimpleFeature> getFeatureListFromShp(String shpPath) {
        SimpleFeatureCollection featureCollectionFromShp = getFeatureCollectionFromShp(shpPath);
        return GeometryUtils.collection2List(featureCollectionFromShp);
    }

    /**
     * 获取shpInfo信息
     *
     * @param shpPath shp路径
     * @return shp信息
     */
    public static Pair<SimpleFeatureCollection, SimpleFeatureType> getShpInfo(String shpPath) {
        File shpFile = ShpFileUtils.getShpFile(shpPath);
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(shpFile);
            SimpleFeatureType schema = store.getSchema();
            return Pair.of(store.getFeatureSource().getFeatures(), schema);
        } catch (IOException e) {
            throw new ShpException(e.getMessage(), e);
        }
    }


}
