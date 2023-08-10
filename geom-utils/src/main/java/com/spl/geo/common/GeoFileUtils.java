package com.spl.geo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 文件处理类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/15
 */
public class GeoFileUtils {

    private GeoFileUtils() {
    }

    private static Logger logger = LoggerFactory.getLogger(GeoFileUtils.class);

    /**
     * 清空文件夹中内容
     *
     * @param file 需要清空的文件夹
     */
    public static void clearFileContent(File file) {
        if (file.exists() && file.isDirectory() && file.delete()) {
            logger.debug("文件夹{}删除后重建", file.getAbsolutePath());
        }
        if (file.mkdirs()) {
            logger.debug("文件夹{}被创建", file.getAbsolutePath());
        }
    }
}
