<template>
  <div class="container">
    <button @click="exportChartsToPDF" class="export-button">导出为PDF</button>
    <div class="left">
      <div class="chart" ref="pieChart1"></div>
      <div class="chart" ref="pieChart2"></div>
      <div class="chart" ref="ringChart"></div>
    </div>
    <div class="center">
      <div class="chart" ref="mapChart"></div>
      <div class="chart" ref="lineChart"></div>
    </div>
    <div class="right">
      <div class="chart" ref="barChartHorizontal"></div>
      <div class="chart" ref="barChartVertical1"></div>
      <div class="chart" ref="barChartVertical"></div>
    </div>
  </div>
</template>

<script>
import * as echarts from "echarts";
import chinaJson from "@/utils/china"; // 引入中国地图数据
import html2canvas from "html2canvas";
import jsPDF from "jspdf";
export default {
  name: "DataVisualization",
  mounted() {
    this.initCharts();
  },
  methods: {
    async exportChartsToPDF() {
      const charts = [
        this.$refs.pieChart1,
        this.$refs.pieChart2,
        this.$refs.ringChart,
        this.$refs.mapChart,
        this.$refs.lineChart,
        this.$refs.barChartHorizontal,
        this.$refs.barChartVertical,
        this.$refs.barChartVertical1,
      ];

      const pdf = new jsPDF("p", "mm", "a4"); // 使用A4纸张
      let position = 10; // 起始位置

      for (let chart of charts) {
        const chartInstance = echarts.getInstanceByDom(chart);
        const width = chart.offsetWidth;
        const height = chart.offsetHeight;
        const scale = 2; // 缩放比例，可以根据需要调整

        const canvas = await chartInstance.renderToCanvas({
          width: width * scale,
          height: height * scale,
        });

        const imgData = canvas.toDataURL("image/png");
        const imgProps = pdf.getImageProperties(imgData);
        const pdfWidth = pdf.internal.pageSize.getWidth() - 20; // 减去左右边距
        const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;

        if (position + pdfHeight > pdf.internal.pageSize.getHeight() - 10) {
          pdf.addPage();
          position = 10; // 新页面的起始位置
        }

        pdf.addImage(imgData, "PNG", 10, position, pdfWidth, pdfHeight);
        position += pdfHeight + 10; // 添加间距
      }

      pdf.save("charts.pdf");
    },
    initCharts() {
      const pieChart1 = echarts.init(this.$refs.pieChart1);
      const pieChart2 = echarts.init(this.$refs.pieChart2);
      const ringChart = echarts.init(this.$refs.ringChart);
      const mapChart = echarts.init(this.$refs.mapChart);
      const lineChart = echarts.init(this.$refs.lineChart);
      const barChartHorizontal = echarts.init(this.$refs.barChartHorizontal);
      const barChartVertical = echarts.init(this.$refs.barChartVertical);
      const barChartVertical1 = echarts.init(this.$refs.barChartVertical1);

      // 注册中国地图
      echarts.registerMap("China", chinaJson);

      // 深色科技风配色
      const colors = ["#00E5FF", "#00C853", "#FFEA00", "#FF3D00", "#6200EA"];

      // 模拟数据
      const pieData1 = [
        { value: 335, name: "直接访问" },
        { value: 310, name: "邮件营销" },
        { value: 234, name: "联盟广告" },
        { value: 135, name: "视频广告" },
        { value: 1548, name: "搜索引擎" },
      ];
      const pieData2 = [
        { value: 234, name: "直接访问" },
        { value: 135, name: "邮件营销" },
        { value: 1548, name: "联盟广告" },
        { value: 310, name: "视频广告" },
        { value: 335, name: "搜索引擎" },
      ];
      const ringData = [
        { value: 1048, name: "搜索引擎" },
        { value: 735, name: "直接访问" },
        { value: 580, name: "邮件营销" },
        { value: 484, name: "联盟广告" },
        { value: 300, name: "视频广告" },
      ];
      const mapData = [
        { name: "重庆市", value: 120 }, // 修改为中国的省份或城市名称
        { name: "北京市", value: 200 },
        { name: "上海市", value: 150 },
        { name: "广东省", value: 180 },
        { name: "江苏省", value: 220 },
      ];
      const lineData = [
        { name: "周一", value: 120 },
        { name: "周二", value: 200 },
        { name: "周三", value: 150 },
        { name: "周四", value: 180 },
        { name: "周五", value: 220 },
        { name: "周六", value: 190 },
        { name: "周日", value: 210 },
      ];
      const barDataHorizontal = [
        { name: "类别A", value: 120 },
        { name: "类别B", value: 200 },
        { name: "类别C", value: 150 },
        { name: "类别D", value: 180 },
        { name: "类别E", value: 220 },
      ];
      const barDataVertical = [
        { name: "类别1", value: 120 },
        { name: "类别2", value: 200 },
        { name: "类别3", value: 150 },
        { name: "类别4", value: 180 },
        { name: "类别5", value: 220 },
      ];

      // 饼图1
      pieChart1.setOption({
        title: {
          text: "饼图1",
          left: "center",
          textStyle: {
            color: "#00FBFF",
            fontWeight: "bold",
          },
        },
        tooltip: {
          trigger: "item",
        },
        textStyle: {
          color: "#E4E1E1",
          fontWeight: "bold",
        },
        series: [
          {
            name: "访问来源",
            type: "pie",
            radius: "50%",
            data: pieData1,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: "rgba(0, 0, 0, 0.5)",
              },
            },
          },
        ],
      });

      // 饼图2
      pieChart2.setOption({
        title: {
          text: "雷达图",
            
          textStyle: {
            color: "#00FBFF",
          },
        },
        legend: {
          top: "5%",
          data: ["Allocated Budget", "Actual Spending"],
          textStyle: {
            color: "#E4E1E1",
            fontWeight: "bold",
          },
        },
        radar: {
          // shape: 'circle',
          indicator: [
            { name: "Sales", max: 6500 },
            { name: "Administration", max: 16000 },
            { name: "Information Technology", max: 30000 },
            { name: "Customer Support", max: 38000 },
            { name: "Development", max: 52000 },
            { name: "Marketing", max: 25000 },
          ],
        },
        series: [
          {
            name: "Budget vs spending",
            type: "radar",
            data: [
              {
                value: [4200, 3000, 20000, 35000, 50000, 18000],
                name: "Allocated Budget",
              },
              {
                value: [5000, 14000, 28000, 26000, 42000, 21000],
                name: "Actual Spending",
              },
            ],
          },
        ],
      });

      // 环形图
      ringChart.setOption({
        title: {
          text: "环形图",
          left: "center",
          textStyle: {
            color: "#00FBFF",
            fontWeight: "bold",
          },
        },
        tooltip: {
          trigger: "item",
        },
        legend: {
          top: "90%",
          left: "center",
          textStyle: {
            color: "#E4E1E1",
            fontWeight: "bold",
          },
        },
        series: [
          {
            name: "访问来源",
            type: "pie",
            radius: ["40%", "70%"],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderWidth: 2,
            },
            label: {
              show: false,
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 20,
                fontWeight: "bold",
              },
            },
            labelLine: {
              show: false,
            },
            data: ringData,
          },
        ],
      });

      // 中国地图
      mapChart.setOption({
        title: {
          text: "中国地图",
          left: "center",
          textStyle: {
            color: "#00FBFF",
          },
        },
        tooltip: {
          trigger: "item",
        },

        series: [
          {
            name: "中国",
            type: "map",
            mapType: "China",
            roam: true,
            label: {
              show: false,
            },
            data: mapData,
          },
        ],
      });

      // 折线图
      lineChart.setOption({
        title: {
          text: "折线图",
          left: "center",
          textStyle: {
            color: "#00FBFF",
            fontWeight: "bold",
          },
        },
        tooltip: {
          trigger: "axis",
        },
        xAxis: {
          type: "category",
          data: lineData.map((item) => item.name),
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#FFFFFF",
          },
        },
        yAxis: {
          type: "value",
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#FFFFFF",
          },
        },
        series: [
          {
            data: lineData.map((item) => item.value),
            type: "line",
            lineStyle: {
              color: "#00E5FF",
            },
            itemStyle: {
              color: "#00E5FF",
            },
          },
        ],
      });

      // 环形条形图
      barChartHorizontal.setOption({
        title: [
          {
            text: "环形条形图",
            textStyle: {
              color: "#00FBFF",
            },
          },
        ],
        polar: {
          radius: [30, "80%"],
        },
        angleAxis: {
          max: 4,
          startAngle: 75,
        },
        radiusAxis: {
          type: "category",
          data: ["a", "b", "c", "d"],
        },
        tooltip: {},
        series: {
          type: "bar",
          data: [2, 1.2, 2.4, 3.6],
          coordinateSystem: "polar",
          label: {
            show: true,
            position: "middle",
            formatter: "{b}: {c}",
          },
        },
      });

      // 竖向条形图
      barChartVertical.setOption({
        title: {
          text: "竖向条形图",
          left: "center",
          textStyle: {
            color: "#00FBFF",
            fontWeight: "bold",
          },
        },
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: "shadow",
          },
        },
        xAxis: {
          type: "category",
          data: barDataVertical.map((item) => item.name),
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#FFFFFF",
          },
        },
        yAxis: {
          type: "value",
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#FFFFFF",
          },
        },
        series: [
          {
            data: barDataVertical.map((item) => item.value),
            type: "bar",
          },
        ],
      });
      barChartVertical1.setOption({
        dataset: {
          source: [
            ["score", "amount", "product"],
            [89.3, 58212, "Matcha Latte"],
            [57.1, 78254, "Milk Tea"],
            [74.4, 41032, "Cheese Cocoa"],
            [50.1, 12755, "Cheese Brownie"],
            [89.7, 20145, "Matcha Cocoa"],
            [68.1, 79146, "Tea"],
            [19.6, 91852, "Orange Juice"],
            [10.6, 101852, "Lemon Juice"],
            [32.7, 20112, "Walnut Brownie"],
          ],
        },
        title: {
          text: "横向条形图",
          left: "center",
          textStyle: {
            color: "#00FBFF",
          },
        },
        grid: { containLabel: true },
        xAxis: { name: "amount" },
        yAxis: { type: "category" },
        series: [
          {
            type: "bar",
            encode: {
              // Map the "amount" column to X axis.
              x: "amount",
              // Map the "product" column to Y axis
              y: "product",
            },
          },
        ],
      });
    },
  },
};
</script>

<style scoped>
.export-button {
  position: absolute;
  top: 52px;
  right: 22px;
  z-index: 1000;
  padding: 10px 20px;
  background-color: #30399f;
  color: #fff;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

.export-button:hover {
  background-color: #00bfff;
}

.container {
  display: flex;
  height: 85vh;
  background: url("@/assets/img/bg1.png") no-repeat center center;
  background-size: cover;
  color: #fff;
  font-family: "Microsoft YaHei", Arial, sans-serif;
}

.left,
.right {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
}

.center {
  flex: 2;
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
}

.chart {
  flex: 1;
  width: 100%;
  height: 100%;
  margin: 10px 0;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
  animation: fadeIn 1s ease-in-out;
}

.chart-title {
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  color: #00eaff;
  margin-bottom: 10px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>