package org.swu.vehiclecloud.dto;

/**
 * 异常统计数据传输对象
 * 用于封装每个异常类型的统计信息
 */
public class AnomalyStat {
    private String title;   // 异常标题（如：方向盘异常）
    private int value;      // 异常发生次数
    private int percent;    // 占比百分比（0-100）
    private String color;   // 前端展示用的颜色编码

    public AnomalyStat(String title, int value, String color) {
        this.title = title;
        this.value = value;
        this.color = color;
    }

    // Getters（Setter仅给percent使用）
    public String getTitle() { return title; }
    public int getValue() { return value; }
    public int getPercent() { return percent; }
    public String getColor() { return color; }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}
