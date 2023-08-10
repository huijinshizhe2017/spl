package com.spl.geo.vector;

import com.spl.geo.common.GeoFileUtils;
import com.spl.geo.exception.ShpException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 矢量文件操作类
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/15
 */
public class ShpFileUtils {
    public static final String SHP_SUFFIX = ".shp";
    public static final String SHP_SUFFIX_REG = "(\\.shp|\\.dbf|\\.prj|\\.sbn|\\.sbx|\\.shx|\\.xml)";

    private ShpFileUtils() {
    }

    /**
     * 获取shp文件，通过文件夹或者shp后缀
     *
     * @param path 查找的路径
     * @return 返回以.shp结尾的文件
     */
    public static File getShpFile(String path) {
        File shpFile = new File(path);
        if (shpFile.isFile() && path.endsWith(SHP_SUFFIX)) {
            return shpFile;
        }
        //如果是文件，则获取父级文件夹
        if (shpFile.isFile()) {
            shpFile = shpFile.getParentFile();
        }
        Optional<File> shpFileOptional = Arrays.stream(Objects.requireNonNull(shpFile.listFiles()))
                .filter(file -> file.getName().endsWith(SHP_SUFFIX)).findFirst();

        if (shpFileOptional.isPresent()) {
            return shpFileOptional.get();
        }
        throw new ShpException("提供路径不符合shp要求");
    }

    public static File getEmptyShpFromPath(String shpPath) {
        //如果传递的为shp后缀，则以此作为shp文件，如果文件夹的路径，则以此文件夹名称作为shp名称
        File shpFile = new File(shpPath);
        if (shpPath.endsWith(SHP_SUFFIX)) {
            File parentFile = shpFile.getParentFile();
            GeoFileUtils.clearFileContent(parentFile);
        } else {
            GeoFileUtils.clearFileContent(shpFile);
            String shpFileName = shpFile.getName();
            shpFile = new File(shpFile, shpFileName + SHP_SUFFIX);
        }
        return shpFile;
    }

    /**
     * 拷贝Shp文件
     *
     * @param srcPath  需要拷贝的shp文件按路径,必须以.shp结尾
     * @param descPath shp拷贝的目标文件或者文件夹，
     *                 如果以".shp"结尾，则为文件，否则为文件夹
     */
    public static void copyShp(String srcPath, String descPath) {
        //源Shp文件夹处理
        if (!srcPath.endsWith(SHP_SUFFIX)) {
            throw new ShpException("srcPath必须以.shp结尾");
        }
        File srcFile = new File(srcPath);
        if (!srcFile.exists() || srcFile.isDirectory()) {
            throw new ShpException("srcPath不存在或者不是文件...");
        }
        File srcShpFolder = srcFile.getParentFile();
        String srcShpName = srcFile.getName();
        srcShpName = srcShpName.substring(0, srcShpName.length() - 4);


        //目标文件夹处理
        File descFile = new File(descPath);
        String descShpName;
        File descShpFolder;
        //如果不以.shp结尾，则按照文件夹处理,shp名称按照源文件名称
        if (descPath.endsWith(SHP_SUFFIX)) {
            String shpName = descFile.getName();
            descShpName = shpName.substring(0, shpName.length() - 4);
            descShpFolder = descFile.getParentFile();
        } else {
            GeoFileUtils.clearFileContent(descFile);
            descShpFolder = descFile;
            descShpName = srcShpName;
        }

        String shpReg = "^" + srcShpName + "\\.(cpg|dbf|prj|shp|shx)$";
        Stream.of(Objects.requireNonNull(srcShpFolder.listFiles())).forEach(file -> {
            String name = file.getName();
            if (file.getName().matches(shpReg)) {
                String desc = name.replaceAll(shpReg, descShpName + ".$1");
                File descTempFile = new File(descShpFolder, desc);
                try {
                    FileUtils.copyFile(file, descTempFile);
                } catch (IOException e) {
                    throw new ShpException(e.getMessage(), e);
                }
            }
        });
    }

}
