<template>
  <div class="container">
    <button @click="exportChartsToPDF" class="export-button">导出为PDF</button>
    <div class="left">
      <!-- 左侧上半部分：地图 -->
      <div class="map-chart chart" ref="mapChart"></div>
      <!-- 左侧下半部分 -->
      <div class="bottom-left">
        <div class="pie-charts">
          <div class="chart" ref="barChartHorizontal"></div>
          <div class="chart" ref="pieChart1"></div>
        </div>
        <div class="line-chart chart" ref="lineChart"></div>
      </div>
    </div>
    <div class="right">
      <!-- 右侧竖向排列的条形图 -->
      <div class="chart" ref="barChartVertical1"></div>
      <div class="chart" ref="pieChart2"></div>
      <div class="chart" ref="barChartVertical"></div>
    </div>
  </div>
</template>

<script>
import * as echarts from "echarts";
import chinaJson from "@/utils/china"; // 引入中国地图数据
import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import axios from "axios";
import { graphic } from 'echarts/core';
export default {
  name: "DataVisualization",
  mounted() {
    this.initCharts();
    this.fetchPieChartData(); // 调用后端接口获取饼图数据
    this.fetchLineChartData();
    this.fetchbarChartData1();
    this.fetchbarChartData2();
    this.fetchbarChartHorizontal();
    this.fetchpieChart2();
  },
  methods: {
    async fetchPieChartData() {
      try {
        // 获取 token
        const token = localStorage.getItem("token");

        // 调用后端接口
        const response = await axios.get(
          "/abc/api/datacontroller/public/exceptionpie",
          {
            headers: {
              Authorization: `Bearer ${token}`, // 在请求头中传入 token
            },
          }
        );

        const pieData = response.data || [];

        // 更新 pieChart1 的数据
        const pieChart1 = echarts.getInstanceByDom(this.$refs.pieChart1);
        pieChart1.setOption({
          series: [
            {
              data: pieData, // 使用后端返回的数据
            },
          ],
        });

        console.log("饼图数据更新成功:", pieData);
      } catch (error) {
        console.error("获取饼图数据失败:", error);
      }
    },
    async fetchLineChartData() {
      try {
        // 获取 token
        const token = localStorage.getItem("token");

        // 调用后端接口
        const response = await axios.get(
          "/abc/api/datacontroller/public/activity/seven-days",
          {
            headers: {
              Authorization: `Bearer ${token}`, // 在请求头中传入 token
            },
          }
        );

        const lineData = response.data || [];

        const lineChart = echarts.getInstanceByDom(this.$refs.lineChart);
        lineChart.setOption({
          series: [
            {
              data: lineData.onlineData, // 使用后端返回的数据
            },
            {
              data: lineData.activeData, // 使用后端返回的数据
            },
          ],
        });

        console.log("折线图数据更新成功:", lineData);
      } catch (error) {
        console.error("获取折线图数据失败:", error);
      }
    },
    async fetchbarChartData1() {
      try {
        // 获取 token
        const token = localStorage.getItem("token");

        // 调用后端接口
        const response = await axios.get(
          "/abc/api/datacontroller/public/activity/online-time-ranking",
          {
            headers: {
              Authorization: `Bearer ${token}`, // 在请求头中传入 token
            },
          }
        );

        const Data = response.data || [];
        const formattedData = Data.map((item) => [
          item.onlineTime,
          item.vehicleId,
        ]);

        // 在开头添加 ["count", "id"]
        formattedData.unshift(["count", "id"]);

        console.log(formattedData);

        const barChartVertical1 = echarts.getInstanceByDom(
          this.$refs.barChartVertical1
        );
        barChartVertical1.setOption({
          dataset: {
            source: formattedData,
          },
        });

        console.log("在线数量图数据更新成功:", Data);
      } catch (error) {
        console.error("获取在线数量图数据失败:", error);
      }
    },

    //活跃数量
    async fetchpieChart2() {
      try {
        // 获取 token
        const token = localStorage.getItem("token");

        // 调用后端接口
        const response = await axios.get(
          "/abc/api/activecontroller/public/vehicle-activity",
          {
            headers: {
              Authorization: `Bearer ${token}`, // 在请求头中传入 token
            },
          }
        );

        const Data = response.data.data || [];
        const formattedData = Data.map((item) => [
          item.amount,
          item.product,
        ]);

        // 在开头添加 ["count", "id"]
        formattedData.unshift(["count", "id"]);

        console.log(formattedData);

        const pieChart2 = echarts.getInstanceByDom(
          this.$refs.pieChart2
        );
        pieChart2.setOption({
          dataset: {
            source: formattedData,
          },
        });

        console.log("活跃数量图数据更新成功:", Data);
      } catch (error) {
        console.error("活跃在线数量图数据失败:", error);
      }
    },

    //车辆异常数据
    async fetchbarChartData2() {
      try {
        // 获取 token
        const token = localStorage.getItem("token");

        // 调用后端接口
        const response = await axios.get(
          "/abc/api/datacontroller/public/exceptionNumber",
          {
            headers: {
              Authorization: `Bearer ${token}`, // 在请求头中传入 token
            },
          }
        );

        const Data = response.data || [];

        const barChartVertical = echarts.getInstanceByDom(
          this.$refs.barChartVertical
        );
        barChartVertical.setOption({
          xAxis: {
            data: Data.map((item) => item.name), // 使用后端返回的数据
          },
          series: {
            data: Data.map((item) => item.value), // 使用后端返回的数据
          },
        });

        console.log("车辆异常数据图数据更新成功:", Data);
      } catch (error) {
        console.error("车辆异常数据图数据失败:", error);
      }
    },
    //机器学习MSE
    async fetchbarChartHorizontal() {
      try {
        // 获取 token
        const token = localStorage.getItem("token");

        // 调用后端接口
        const response = await axios.get(
          "/abc/api/datacontroller/public/getmlexceptiondata",
          {
            headers: {
              Authorization: `Bearer ${token}`, // 在请求头中传入 token
            },
          }
        );

        const Data = response.data.data.data || [];

        const barChartHorizontal = echarts.getInstanceByDom(
          this.$refs.barChartHorizontal
        );
        barChartHorizontal.setOption({
          radiusAxis: {
            data: Data[0], // 使用后端返回的数据
          },
          series: {
            data: Data[1], // 使用后端返回的数据
          },
        });

        console.log("机器学习MSE图数据更新成功:", Data);
      } catch (error) {
        console.error("机器学习MSE图数据失败:", error);
      }
    },

    async exportChartsToPDF() {
      const charts = [
        this.$refs.pieChart1,
        this.$refs.pieChart2,
        this.$refs.mapChart,
        this.$refs.lineChart,
        this.$refs.barChartHorizontal,
        this.$refs.barChartVertical,
        this.$refs.barChartVertical1,
      ];

      const pdf = new jsPDF("p", "mm", "a4"); // 使用A4纸张
      const currentTime = new Date();
      const formattedTime = `${currentTime.getFullYear()}-${String(
        currentTime.getMonth() + 1
      ).padStart(2, "0")}-${String(currentTime.getDate()).padStart(
        2,
        "0"
      )} ${String(currentTime.getHours()).padStart(2, "0")}:${String(
        currentTime.getMinutes()
      ).padStart(2, "0")}`;
      const title = `${formattedTime} `;

      // 添加标题
      pdf.setFontSize(16);
      pdf.setFont("", "bold");
      pdf.text(title, pdf.internal.pageSize.getWidth() / 2, 10, {
        align: "center",
      });

      let position = 20; // 起始位置，标题占用了一部分空间

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

      // 动态命名文件
      const fileName = `${formattedTime} 导出报表.pdf`;
      pdf.save(fileName);
    },
    initCharts() {
      const pieChart1 = echarts.init(this.$refs.pieChart1);
      const pieChart2 = echarts.init(this.$refs.pieChart2);
      const mapChart = echarts.init(this.$refs.mapChart);
      const lineChart = echarts.init(this.$refs.lineChart);
      const barChartHorizontal = echarts.init(this.$refs.barChartHorizontal);
      const barChartVertical = echarts.init(this.$refs.barChartVertical);
      const barChartVertical1 = echarts.init(this.$refs.barChartVertical1);

      // 注册中国地图
      echarts.registerMap("China", chinaJson);

      // 模拟数据

      // 饼图1
      pieChart1.setOption({
        title: {
          text: "异常种类饼图",
          left: "left",
          textStyle: {
            color: "#00FBFF",
          },
        },
        tooltip: {
          trigger: "item",
        },
        legend: {
          top: "75%",
          left: "center",
          textStyle: {
            color: "#00FBFF",
          },
        },
        series: [
          {
            name: "Access From",
            type: "pie",
            radius: ["40%", "70%"],
            avoidLabelOverlap: false,
            label: {
              show: false,
              position: "center",
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 40,
                fontWeight: "bold",
              },
            },
            labelLine: {
              show: false,
            },
            data: [],
          },
        ],
      });

      // 饼图2
      pieChart2.setOption({
        dataset: {
          source: [
          ],
        },
        title: {
          text: "活跃时长",
          left: "center",
          textStyle: {
            color: "#00FBFF",
          },
        },
        grid: { containLabel: true },
        xAxis: {
          name: "amount",
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
          },
        },
        yAxis: {
          type: "category",
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
          },
        },
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

      // 中国地图
      mapChart.setOption({
        title: {
          text: "异常分布",
          left: "center",
          textStyle: {
            color: "#00FBFF",
            fontWeight: "bold",
          },
        },
        tooltip: {
          trigger: "item",
        },
        geo: {
          map: "China",
          roam: false,
          emphasis: {
            label: {
              show: false,
            },
          },
        },
        visualMap: {
          show: true, // 显示视觉映射
          min: 0,
          max: 200,
          realtime: true,
          calculable: true,
          inRange: {
            color: ["#d2e0f5", "#71A9FF", "#FF0000"], // 颜色范围：浅蓝 -> 深蓝 -> 红色
          },
        },
        series: [
          {
            geoIndex: 0,
            name: "地域分布",
            type: "map",
            coordinateSystem: "geo",
            map: "china",
            data: generateRandomProvinceData(), // 调用生成随机数据的函数
          },
        ],
      });
      // 生成随机省份数据的函数
      function generateRandomProvinceData() {
        const provinces = [
          "北京",
          "上海",
          "广东",
          "浙江",
          "江西",
          "山东",
          "广西",
          "河南",
          "青海",
          "黑龙江",
          "新疆",
          "云南",
          "甘肃",
          "山西",
          "陕西",
          "吉林",
          "福建",
          "湖南",
          "湖北",
          "辽宁",
          "四川",
          "贵州",
          "海南",
          "重庆",
          "内蒙古",
          "西藏",
          "宁夏",
          "台湾",
          "香港",
          "澳门",
          "河北",
          "安徽",
          "江苏",
          "天津",
        ];

        return provinces.map((province) => {
          return {
            name: province,
            value: Math.floor(Math.random() * 201), // 随机生成 0~200 的值
          };
        });
      }

      // 折线图
      lineChart.setOption({
        title: {
          text: "历史活跃表",
          left: "left",
          textStyle: {
            color: "#00FBFF",
            fontWeight: "bold",
          },
        },
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: "line",
          },
        },
        legend: {
          data: ["活跃数量", "在线数量"],
          textStyle: {
            color: "#00FBFF",
          },
        },
        xAxis: {
          type: "category",
          data: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
          },
        },
        yAxis: {
          type: "value",
          axisLine: {
            lineStyle: {
              color: "#00FBFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
          },
          min: null,
          max: null,
        },
        series: [
          {
            name: "活跃数量",
            type: "line",
            smooth: true,
            data: [],
          },
          {
            name: "在线数量",
            type: "line",
            smooth: true,
            data: [],
          },
        ],
      });

      // 环形条形图
      barChartHorizontal.setOption({
        title: [
          {
            text: "机器学习MSE",
            textStyle: {
              color: "#00FBFF",
            },
          },
        ],
        polar: {
          radius: [30, "80%"],
        },
        angleAxis: {
          max: 1,
          startAngle: 75,
          axisLabel: {
            color: "#00FBFF", // 设置角度轴标签字体颜色
          },
        },
        radiusAxis: {
          type: "category",
          data: [],
          axisLabel: {
            color: "#00FBFF", // 设置半径轴标签字体颜色
          },
        },
        tooltip: {
          trigger: "item",
        },
        series: {
          type: "bar",
          data: [],
          coordinateSystem: "polar",
          label: {
            show: true,
            position: "middle",
            formatter: "{b}",
            color: "#00FBFF", // 设置标签字体颜色
          },
        },
      });

      // 竖向条形图
      barChartVertical.setOption({
        title: {
          text: "车辆异常",
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
          data: [],
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
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
            color: "#00FBFF",
          },
        },
        series: [
          {
            data: [],
            type: "bar",
          },
        ],
      });
      barChartVertical1.setOption({
        dataset: {
          source: [
          ],
        },
        title: {
          text: "在线时长",
          left: "center",
          textStyle: {
            color: "#00FBFF",
          },
        },
        grid: { containLabel: true },
        xAxis: {
          name: "count",
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
          },
        },
        yAxis: {
          type: "category",
          axisLine: {
            lineStyle: {
              color: "#FFFFFF",
            },
          },
          axisLabel: {
            color: "#00FBFF",
          },
        },
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

.left {
  flex: 3;
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
}

.right {
  flex: 2;
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.5);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
}

.map-chart {
  flex: 1;
  width: 100%;
  height: 50%;
  margin-bottom: 10px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.5);
  animation: fadeIn 1s ease-in-out;
}

.bottom-left {
  flex: 1;
  display: flex;
  flex-direction: row;
}

.pie-charts {
  display: flex;
  flex: 2;
  flex-direction: column;
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

.line-chart {
  flex: 3;
  margin-top: 10px;
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