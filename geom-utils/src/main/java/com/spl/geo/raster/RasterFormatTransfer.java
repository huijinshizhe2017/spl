package com.spl.geo.raster;

import com.spl.geo.exception.RasterException;
import com.spl.geo.raster.enums.ImgType;
import com.spl.geo.vector.ReadShpUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.Style;
import org.geotools.util.factory.Hints;
import org.opengis.geometry.Envelope;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 栅格图像格式转换
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/18
 */
public class RasterFormatTransfer {
    private RasterFormatTransfer() {
    }

    public static void scaleTifToImg(String tifPath, String imgPath, ImgType imgType, int pix) {
        TifExportImgUtils.exportPng(tifPath, null, pix, imgPath, imgType);
    }


    /**
     * tif转图像
     *
     * @param tifPath tif路径
     * @param imgPath 目标图像路径
     * @param imgType 目标图像格式
     */
    public static void tifToImg(String tifPath, String imgPath, ImgType imgType) {
        try {
            RenderedImage renderedImage = getRenderedImage(tifPath);
            File file = new File(imgPath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            ImageIO.write(renderedImage, imgType.getName(), new File(imgPath));
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }
    }

    /**
     * tif转png
     *
     * @param tifPath tif路径
     * @param pngPath png路径
     * @throws IOException
     */
    public static void tifToPng(String tifPath, String pngPath) {
        tifToImg(tifPath, pngPath, ImgType.IMG_PNG);
    }

    /**
     * Tif转base64
     *
     * @param tifPath tif路径
     * @return base64字符串
     */
    public static String tifToBase64(String tifPath) {
        RenderedImage renderedImage = getRenderedImage(tifPath);
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(renderedImage, "png", Base64.getEncoder().wrap(os));
            return os.toString(StandardCharsets.ISO_8859_1.name());
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }
    }

    private static RenderedImage getRenderedImage(String tifPath) {
        return getImageAndEnv(tifPath).getKey();
    }

    private static Pair<RenderedImage,Envelope> getImageAndEnv(String tifPath) {
        if (!new File(tifPath).exists()) {
            throw new RasterException("文件名称为:" + tifPath + "不存在");
        }
        AbstractGridFormat format = GridFormatFinder.findFormat(tifPath);
        Hints hints = new Hints();
        if (format instanceof GeoTiffFormat) {
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        }
        AbstractGridCoverage2DReader reader = format.getReader(tifPath, hints);
        try {
            GridCoverage2D cov = reader.read(null);
            RenderedImage renderedImage = cov.getRenderedImage();
            Envelope envelope = cov.getEnvelope();
            return Pair.of(renderedImage,envelope);
        } catch (Exception e) {
            throw new RasterException(e.getMessage(), e);
        }
    }

    /**
     * 云量叠加图
     * @param basePath 底图路径。
     * @param cloudPath 云量tif路径
     * @param shpPath 行政区shp(面)
     * @param pngPath 输出图片路径
     * @throws IOException
     */
    public static void tifSingleBand2Png(String basePath, String cloudPath, String shpPath,String pngPath) throws IOException {
        //处理云图
        Pair<RenderedImage, Envelope> imageAndEnv = getImageAndEnv(cloudPath);
        Raster data = imageAndEnv.getKey().getData();
        int width = data.getWidth() * 4;
        int height = data.getHeight() * 4;
        BufferedImage bi = handlerCloud(data);
        BufferedImage cloudBi = Thumbnails.of(bi).scale(4f).asBufferedImage();
//        Image scaledInstance = bi.getScaledInstance(width, height, Image.SCALE_REPLICATE);
//        BufferedImage cloudBi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        cloudBi.getGraphics().drawImage(scaledInstance,0,0,width,height,null);


        //处理shp矢量
        BufferedImage shpImg = handlerShp(shpPath,imageAndEnv.getValue(),width,height);
        BufferedImage baseImg = ImageIO.read(new File(basePath));
        BufferedImage distBf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = distBf.getGraphics();
        graphics.drawImage(baseImg, 0, 0, width, height, null);
        graphics.drawImage(cloudBi, 0, 0, width, height, null);
        graphics.drawImage(shpImg, 0, 0, width, height, null);
        ImageIO.write(distBf, ImgType.IMG_PNG.getName(), new File(pngPath));
    }

    private static BufferedImage handlerShp(String shpPath,Envelope envelope,Integer width,Integer height){
        MapContent map = new MapContent();
        SimpleFeatureCollection featureCollection = ReadShpUtils.getFeatureCollectionFromShp(shpPath);
        Style vectorStyle = TifExportImgUtils.createDefaultStyle(Color.YELLOW,0.2);
        Layer layer = new FeatureLayer(featureCollection, vectorStyle);
        map.addLayer(layer);

        double west = envelope.getMinimum(0);
        double south = envelope.getMinimum(1);
        double east = envelope.getMaximum(0);
        double north = envelope.getMaximum(1);
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(west,east,south,north,envelope.getCoordinateReferenceSystem());
        return TifExportImgUtils.getMapContent(referencedEnvelope,width,height,map);
    }

    private static BufferedImage handlerCloud(Raster data){
        int width = data.getWidth();
        int height = data.getHeight();
        int[] dataPix = new int[data.getNumBands()];
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                data.getPixel(i, j, dataPix);
                int a = dataPix[0] * 250 / 100;
                int rgb = (a << 24) | (0x00ffffff);
                bi.setRGB(i, j, rgb);
            }
        }
        return bi;
    }



}
