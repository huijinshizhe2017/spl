package com.spl.geo.raster.enums;

/**
 * 枚举类型
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/18
 */
public enum  ImgType {
    /**
     * jpg格式
     */
    IMG_JPG("JPG"),

    /**
     * png格式
     */
    IMG_PNG("png"),

    /**
     * webp格式
     */
    IMG_WEBP("webp");

    private String name;

    ImgType(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
