// VehicleExceptionCount.java
package org.swu.vehiclecloud.dto;

/**
 * 车辆异常数量统计DTO
 */
public class VehicleExceptionCount {
    private String name;  // 车辆ID
    private int value;    // 异常数量

    public VehicleExceptionCount() {
    }

    public VehicleExceptionCount(String name, int value) {
        this.name = name;
        this.value = value;
    }

    // getter和setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
