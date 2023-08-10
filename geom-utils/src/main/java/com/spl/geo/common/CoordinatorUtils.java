package com.spl.geo.common;

import com.spl.geo.exception.CoordinatorException;
import org.geotools.data.FileDataStore;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * 坐标工具类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/15 coordinator
 */
public class CoordinatorUtils {

    private CoordinatorUtils() {
    }

    /**
     * 地理信息转换
     *
     * @param srcGeometry 原始地理对象
     * @param targetSrid  目标坐标信息
     * @return 目标地理对象
     */
    public static Geometry transform(Geometry srcGeometry, Integer targetSrid) {
        try {
            int srcEpsg = srcGeometry.getSRID();
            CoordinateReferenceSystem srcCrs = CRS.decode("EPSG:" + srcEpsg, true);
            CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:" + targetSrid, true);
            MathTransform mathTransform = CRS.findMathTransform(srcCrs, targetCrs, true);
            return JTS.transform(srcGeometry, mathTransform);
        } catch (Exception e) {
            throw new CoordinatorException(e.getMessage(), e);
        }
    }


    /**
     * 获取srid代码
     *
     * @param store 文件存储
     * @return srid
     */
    public static Integer getSrId(FileDataStore store) {
        try {
            String wkt = store.getSchema().getCoordinateReferenceSystem().toWKT();
            CoordinateReferenceSystem crsTarget = CRS.parseWKT(wkt);
            return CRS.lookupEpsgCode(crsTarget, true);
        } catch (Exception e) {
            throw new CoordinatorException(e.getMessage(), e);
        }
    }
}
