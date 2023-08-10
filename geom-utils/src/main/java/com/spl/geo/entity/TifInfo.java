package com.spl.geo.entity;

/**
 * tif基本信息
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/7/14
 */
public class TifInfo {

    private static final String WKT_FORMAT = "POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))";

    private String coordinateName;

    private Integer srid;

    private Double west;

    private Double east;

    private Double north;

    private Double south;

    private Integer bandCount;

    private Integer sizeX;

    private Integer sizeY;

    private Integer bits;


    /**
     * 打印范围界限
     * @return
     */
    public String printEWkt(){
        return String.format("SRID=%d;%s",srid,printWkt());
    }

    /**
     * 计算分辨率
     * @return
     */
    public double calResolution(){
        return (east - west) / sizeX;
    }


    public String printWkt(){
        return String.format(WKT_FORMAT,west,north,east,north,east,south,west,south,west,north);
    }

    public String getCoordinateName() {
        return coordinateName;
    }

    public void setCoordinateName(String coordinateName) {
        this.coordinateName = coordinateName;
    }

    public Integer getSrid() {
        return srid;
    }

    public void setSrid(Integer srid) {
        this.srid = srid;
    }

    public Double getWest() {
        return west;
    }

    public void setWest(Double west) {
        this.west = west;
    }

    public Double getEast() {
        return east;
    }

    public void setEast(Double east) {
        this.east = east;
    }

    public Double getNorth() {
        return north;
    }

    public void setNorth(Double north) {
        this.north = north;
    }

    public Double getSouth() {
        return south;
    }

    public void setSouth(Double south) {
        this.south = south;
    }

    public Integer getBandCount() {
        return bandCount;
    }

    public void setBandCount(Integer bandCount) {
        this.bandCount = bandCount;
    }

    public static String getWktFormat() {
        return WKT_FORMAT;
    }

    public Integer getSizeX() {
        return sizeX;
    }

    public void setSizeX(Integer sizeX) {
        this.sizeX = sizeX;
    }

    public Integer getSizeY() {
        return sizeY;
    }

    public void setSizeY(Integer sizeY) {
        this.sizeY = sizeY;
    }

    public Integer getBits() {
        return bits;
    }

    public void setBits(Integer bits) {
        this.bits = bits;
    }

    @Override
    public String toString() {
        return "TifInfo{" +
                "coordinateName='" + coordinateName + '\'' +
                ", srid=" + srid +
                ", west=" + west +
                ", east=" + east +
                ", north=" + north +
                ", south=" + south +
                ", bandCount=" + bandCount +
                '}';
    }
}
