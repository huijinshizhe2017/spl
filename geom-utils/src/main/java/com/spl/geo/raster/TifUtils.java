package com.spl.geo.raster;

import com.spl.geo.entity.TifInfo;
import com.spl.geo.exception.RasterException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.util.factory.Hints;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Iterator;

/**
 * Tif文件工具类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/7/14
 */
public class TifUtils {

    private TifUtils() {
    }

    /**
     * 获取tif基本信息
     *
     * @param tifPath tif路径
     * @return tif基本信息
     */
    public static TifInfo parseTifInfo(String tifPath) {
        AbstractGridFormat format = GridFormatFinder.findFormat(tifPath);
        Hints hints = new Hints();
        if (format instanceof GeoTiffFormat) {
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        }
        AbstractGridCoverage2DReader reader = format.getReader(tifPath, hints);
        try {
            TifInfo tifInfo = new TifInfo();
            GridCoverage2D cov = reader.read(null);
            //四至坐标
            Envelope envelope = cov.getEnvelope();
            tifInfo.setWest(envelope.getMinimum(0));
            tifInfo.setSouth(envelope.getMinimum(1));
            tifInfo.setEast(envelope.getMaximum(0));
            tifInfo.setNorth(envelope.getMaximum(1));

            RenderedImage renderedImage = cov.getRenderedImage();
            Raster raster = renderedImage.getTile(0, 0);
            tifInfo.setSizeX(raster.getWidth());
            tifInfo.setSizeY(raster.getHeight());
            tifInfo.setBits(16);

            //坐标系fillCoordinate
            fillCoordinate(tifInfo, cov.getCoordinateReferenceSystem());

            //波段数量
            int numSampleDimensions = cov.getNumSampleDimensions();
            tifInfo.setBandCount(numSampleDimensions);
            reader.dispose();
            return tifInfo;
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }
    }


    private static void fillCoordinate(TifInfo tifInfo, CoordinateReferenceSystem coordinateReferenceSystem) {
        if (coordinateReferenceSystem == null) {
            return;
        }
        String name = coordinateReferenceSystem.getName().getCode();
        tifInfo.setCoordinateName(name);
        Iterator<ReferenceIdentifier> iterator = coordinateReferenceSystem.getIdentifiers().iterator();
        if (iterator.hasNext()) {
            String code = iterator.next().getCode();
            tifInfo.setSrid(Integer.valueOf(code));
        }
    }
}
