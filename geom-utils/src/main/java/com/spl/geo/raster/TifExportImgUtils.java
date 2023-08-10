package com.spl.geo.raster;

import com.spl.geo.exception.RasterException;
import com.spl.geo.raster.enums.ImgType;
import com.spl.geo.vector.GeometryUtils;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Stroke;
import org.geotools.styling.*;
import org.geotools.util.factory.Hints;
import org.opengis.filter.FilterFactory2;
import org.opengis.style.ContrastMethod;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 栅格矢量套图输出
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/9/20
 */
public class TifExportImgUtils {

    private TifExportImgUtils() {
    }

    private static final StyleFactory STYLE_FACTORY = CommonFactoryFinder.getStyleFactory();
    private static final FilterFactory2 FILTER_FACTORY_2 = CommonFactoryFinder.getFilterFactory2();

    private static final int BAND_COUNT = 3;


    /**
     * tif和wkt套和导出png
     *
     * @param tifPath     tif文件路径
     * @param wktList     wkt集合，不包含坐标系统
     * @param pix         导出png的像素
     * @param vectorStyle 矢量的样式
     * @return png图片流
     */
    public static BufferedImage exportPng(String tifPath, List<String> wktList, Integer pix, Style vectorStyle) {
        MapContent map = null;
        try {
            map = new MapContent();
            AbstractGridCoverage2DReader reader = readTif(tifPath);
            Style rgbStyle = createRgbStyle(reader);
            GridCoverage2D read = reader.read(null);
            GridCoverageLayer gridCoverageLayer = new GridCoverageLayer(read, rgbStyle);
            ReferencedEnvelope bounds = gridCoverageLayer.getBounds();
            map.addLayer(gridCoverageLayer);
            if(wktList!= null && !wktList.isEmpty()) {
                List<String> xyChangeWktList = wktList.stream()
                        .map(wkt -> wkt.replaceAll("(\\d+\\.\\d*) (\\d+\\.\\d*)([,)])", "$2 $1$3"))
                        .collect(Collectors.toList());
                SimpleFeatureCollection featureCollection = GeometryUtils.wktList2Feature(xyChangeWktList, 4490);
                Layer layer = new FeatureLayer(featureCollection, vectorStyle);
                map.addLayer(layer);
            }
            return getMapContent(bounds, pix, map);
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }finally {
            if(map != null){
                map.dispose();
            }
        }
    }

    /**
     * tif和wkt套和导出png
     *
     * @param tifPath tif文件路径
     * @param wktList wkt集合，不包含坐标系统
     * @param pix     导出png的像素
     * @return png图片流
     */
    public static BufferedImage exportPng(String tifPath, List<String> wktList, Integer pix) {
        Style style = createDefaultStyle();
        return exportPng(tifPath, wktList, pix, style);
    }



    /**
     * 输出png图
     *
     * @param tifPath     tif路径
     * @param wktList    wkt串
     * @param pix         像素
     * @param outPath     输出路径
     * @param vectorStyle 矢量样式
     */
    public static void exportPng(String tifPath, List<String> wktList, Integer pix, String outPath, Style vectorStyle, ImgType imgType) {
        BufferedImage bufferedImage = exportPng(tifPath, wktList, pix, vectorStyle);
        try {
            ImageIO.write(bufferedImage, imgType.getName(), new File(outPath));
        } catch (Exception e) {
            throw new RasterException(e.getMessage());
        }
    }

    /**
     * tif与wkt套图输出png
     *
     * @param tifPath  tif路径
     * @param wktList 范围界限
     * @param pix      输出像素
     * @param outPath  输出路径
     */
    public static void exportPng(String tifPath, List<String> wktList, Integer pix, String outPath, ImgType imgType) {
        Style style = createDefaultStyle();
        exportPng(tifPath, wktList, pix, outPath, style,imgType);

    }

    /**
     * tif与wkt套图输出png
     *
     * @param tifPath  tif路径
     * @param wktList 范围界限
     * @param pix      输出像素
     * @param outPath  输出路径
     */
    public static void exportPng(String tifPath, List<String> wktList, Integer pix, String outPath) {
        exportPng(tifPath,wktList,pix,outPath,ImgType.IMG_PNG);
    }

    private static AbstractGridCoverage2DReader readTif(String tifPath) {
        AbstractGridFormat format = GridFormatFinder.findFormat(tifPath);
        Hints hints = new Hints();
        if (format instanceof GeoTiffFormat) {
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        }
        return format.getReader(tifPath, hints);
    }

    private static Style createRgbStyle(AbstractGridCoverage2DReader reader) {
        GridCoverage2D cov;
        try {
            cov = reader.read(null);
        } catch (IOException giveUp) {
            throw new RasterException(giveUp);
        }
        //我们需要至少三个波段的样式
        int numBands = cov.getNumSampleDimensions();
        if (numBands < BAND_COUNT) {
            return null;
        }
        //获取各个波段的名称
        String[] sampleDimensionNames = new String[numBands];
        for (int i = 0; i < numBands; i++) {
            GridSampleDimension dim = cov.getSampleDimension(i);
            sampleDimensionNames[i] = dim.getDescription().toString();
        }
        final int red = 0, green = 1, blue = 2;
        int[] channelNum = {-1, -1, -1};
        for (int i = 0; i < numBands; i++) {
            String name = sampleDimensionNames[i].toLowerCase();
            if (name.matches("red.*")) {
                channelNum[red] = i + 1;
            } else if (name.matches("green.*")) {
                channelNum[green] = i + 1;
            } else if (name.matches("blue.*")) {
                channelNum[blue] = i + 1;
            }
        }
        if (channelNum[red] < 0 || channelNum[green] < 0 || channelNum[blue] < 0) {
            channelNum[red] = 1;
            channelNum[green] = 2;
            channelNum[blue] = 3;
        }
        //我们使用selected通道创建RasterSymbolize样式
        SelectedChannelType[] sct = new SelectedChannelType[cov.getNumSampleDimensions()];
        ContrastEnhancement ce = STYLE_FACTORY.contrastEnhancement(FILTER_FACTORY_2.literal(1.0), ContrastMethod.NORMALIZE);
        for (int i = 0; i < BAND_COUNT; i++) {
            sct[i] = STYLE_FACTORY.createSelectedChannelType(String.valueOf(channelNum[i]), ce);
        }
        RasterSymbolizer sym = STYLE_FACTORY.getDefaultRasterSymbolizer();
        ChannelSelection sel = STYLE_FACTORY.channelSelection(sct[red], sct[green], sct[blue]);
        sym.setChannelSelection(sel);
        return SLD.wrapSymbolizers(sym);
    }

    public static BufferedImage getMapContent(ReferencedEnvelope mapArea, Integer width,Integer height, MapContent map){
        try {
            // 设置输出范围
            StreamingRenderer sr = new StreamingRenderer();
            sr.setMapContent(map);
            // 初始化输出图像
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.getGraphics();
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle rect = new Rectangle(0, 0, width, height);
            // 绘制地图
            sr.paint((Graphics2D) g, rect, mapArea);
            return bi;
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }
    }


    private static BufferedImage getMapContent(ReferencedEnvelope mapArea, Integer pix, MapContent map) {
        return getMapContent(mapArea,pix,pix,map);
    }

    public static Style createDefaultStyle() {
        return createDefaultStyle(Color.red,5.0);
    }

    public static Style createDefaultStyle(Color color,Double lineWidth) {
        Stroke stroke = STYLE_FACTORY.createStroke(FILTER_FACTORY_2.literal(color), FILTER_FACTORY_2.literal(lineWidth));
        LineSymbolizer symbolize = STYLE_FACTORY.createLineSymbolizer(stroke, "the_geom");
        Rule rule = STYLE_FACTORY.createRule();
        rule.symbolizers().add(symbolize);
        FeatureTypeStyle fts = STYLE_FACTORY.createFeatureTypeStyle();
        fts.rules().add(rule);
        Style style = STYLE_FACTORY.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }
}
