package com.spl.geo.vector;

import com.spl.geo.exception.RasterException;
import org.locationtech.jts.geom.Geometry;

/**
 * 拓扑检查
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/17
 */
public class TopologyUtils {

    private TopologyUtils() {
    }

    /**
     * ；啦啦啦啦
     *
     * @param geometry1 要素1
     * @param geometry2 要素2
     * @return 相交部分
     */
    public static Geometry calIntersection(Geometry geometry1, Geometry geometry2) {
        try {
            return geometry1.intersection(geometry2);
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }
    }
}
