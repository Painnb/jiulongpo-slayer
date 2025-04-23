<template>
  <div class="dashboard">
    <el-row :gutter="20" class="mgb20">
      <el-col :span="6">
        <el-card shadow="hover" body-class="card-body">
          <img
            src="@/assets/img/card1.png"
            alt="在线数量"
            class="card-icon bg-blue"
          />
          <div class="card-content">
            <div class="card-num color1">{{ onlineCount }}</div>
            <div>在线数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-class="card-body">
          <img
            src="@/assets/img/card2.png"
            alt="活跃数量"
            class="card-icon bg-green"
          />
          <div class="card-content">
            <div class="card-num color2">{{ activeCount }}</div>
            <div>活跃数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-class="card-body">
          <img
            src="@/assets/img/card3.png"
            alt="异常数量"
            class="card-icon bg-red"
          />
          <div class="card-content">
            <div class="card-num color3">{{ exceptionCount }}</div>
            <div>异常数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" body-class="card-body">
          <img
            src="@/assets/img/card4.png"
            alt="时间"
            class="card-icon bg-orange"
          />
          <div class="card-content">
            <div class="card-num color4">{{ currentTime }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mgb20">
      <el-col :span="12">
        <el-card
          shadow="hover"
          :body-style="{ height: '420px', backgroundColor: '#eef5ff' }"
        >
          <div class="card-header">
            <p class="card-header-title">历史数据</p>
            <p class="card-header-desc">监测到的车辆活跃与在线数据</p>
          </div>
          <v-chart class="chart" :option="dashOpt1" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card
              shadow="hover"
              :body-style="{ height: '420px', backgroundColor: '#c0dbf8' }"
            >
              <div class="card-header">
                <div class="card-header-left">
                  <p class="card-header-title">数据解析</p>
                  <p class="card-header-desc">输入数据并解析为JSON</p>
                </div>
              </div>
              <div class="data-parser" v-if="showInitialParser">
                <el-input
                  v-model="inputData"
                  type="textarea"
                  placeholder="输入待解析数据"
                  size="small"
                  class="parser-input"
                />
                <el-button
                  type="primary"
                  size="small"
                  @click="parseData"
                  class="parser-button"
                >
                  解析
                </el-button>
              </div>

              <!-- 修改后的解析结果区域 -->
              <div v-else class="parser-result-container">
                <div class="result-header">
                  <span>解析结果：</span>
                  <div>
                    <el-button
                      type="text"
                      size="small"
                      @click="copyParsedData"
                      class="copy-button"
                    >
                      <el-icon><DocumentCopy /></el-icon> 一键复制
                    </el-button>
                    <el-button
                      type="primary"
                      size="small"
                      @click="resetParser"
                      class="parser-button"
                    >
                      返回
                    </el-button>
                  </div>
                </div>
                <pre class="json-display">{{ parsedData }}</pre>
              </div>

              <div class="card-header">
                <div class="card-header-left">
                  <p class="card-header-title">机器学习检测</p>
                  <p class="card-header-desc">输入车辆信息并检测是否存在异常</p>
                </div>
              </div>
              <div class="data-parser" v-if="!showDetectionResult">
                <el-input
                  v-model="vehicleInfo"
                  type="textarea"
                  placeholder="输入车辆信息（JSON格式）"
                  size="small"
                  class="parser-input"
                />
                <el-button
                  type="primary"
                  size="small"
                  @click="detectAnomalies"
                  class="parser-button"
                >
                  检测
                </el-button>
              </div>
              <div v-else class="parser-result-container">
                <div class="result-header">
                  <span>检测结果：</span>
                  <el-button
                    type="primary"
                    size="small"
                    @click="resetDetection"
                    class="parser-button"
                  >
                    返回
                  </el-button>
                </div>
                <pre class="json-display">{{ detectionResult }}</pre>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card
              shadow="hover"
              :body-style="{ height: '420px', backgroundColor: '#76A9F7' }"
            >
              <div class="card-header">
                <p class="card-header-title">通知</p>
              </div>
              <div class="notification-input">
                <el-input
                  v-model="newNotification"
                  placeholder="输入通知内容"
                  size="small"
                  class="notification-textbox"
                />
                <el-button
                  type="primary"
                  size="small"
                  @click="addNotification"
                  class="notification-button"
                >
                  发布通知
                </el-button>
              </div>
              <el-timeline>
                <el-timeline-item
                  v-for="(activity, index) in activities"
                  :key="index"
                  :color="activity.color"
                >
                  <div class="timeline-item">
                    <div>
                      <p>{{ activity.content }}</p>
                      <p class="timeline-desc">{{ activity.description }}</p>
                    </div>
                    <div class="timeline-time">{{ activity.timestamp }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card
          shadow="hover"
          :body-style="{ height: '390px', backgroundColor: '#B1CFFF' }"
        >
          <div class="card-header">
            <p class="card-header-title">异常分布</p>
            <p class="card-header-desc">最近一个月全国各地的异常分布</p>
          </div>
          <v-chart class="map-chart" :option="mapOptions" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card
          shadow="hover"
          :body-style="{ height: '390px', backgroundColor: '#BFD5F8' }"
        >
          <div class="card-header">
            <p class="card-header-title">历史异常统计</p>
          </div>
          <div>
            <div class="rank-item" v-for="(rank, index) in ranks" :key="index">
              <div class="rank-item-avatar">{{ index + 1 }}</div>
              <div class="rank-item-content">
                <div class="rank-item-top">
                  <div class="rank-item-title">{{ rank.title }}</div>
                  <div class="rank-item-desc">数量：{{ rank.value }}</div>
                </div>
                <el-progress
                  :show-text="false"
                  striped
                  :stroke-width="10"
                  :percentage="rank.percent"
                  :color="rank.color"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="dashboard">
import { use, registerMap } from "echarts/core";
import { BarChart, LineChart, PieChart, MapChart } from "echarts/charts";
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  VisualMapComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import VChart from "vue-echarts";
import { dashOpt2, mapOptions } from "./chart/options";
import chinaMap from "@/utils/china";
import { ref, onMounted, onUnmounted } from "vue";
import axios from "axios";
import { createSSEConnection } from "../utils/sse";
import { graphic } from "echarts/core";
import { DocumentCopy } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { onActivated, onDeactivated } from 'vue';

const activeCount = ref<string>("");
const onlineCount = ref<string>("");
const exceptionCount = ref<string>("");
let sseConnection: { close: () => void } | null = null;
let sseConnection1: { close: () => void } | null = null;
let sseConnection2: { close: () => void } | null = null;

const token = localStorage.getItem("token") || "";

onActivated(() => {
  sseConnection = createSSEConnection(
    "/abc/api/datacontroller/public/ssestream/10",
    token,
    {
      onOpen: () => {
        console.log("SSE连接已建立");
      },
      onMessage: (data) => {
        console.log("收到SSE在线消息:", data);

        onlineCount.value = data.numOfOnline;
      },
      onError: (error) => {
        console.error("SSE连接错误:", error);
      },
    }
  );
  sseConnection1 = createSSEConnection(
    "/abc/api/datacontroller/public/ssestream/2",
    token,
    {
      onOpen: () => {
        console.log("SSE连接已建立");
      },
      onMessage: (data) => {
        console.log("收到SSE异常消息:", data);
        exceptionCount.value = data.numOfExp;
      },
      onError: (error) => {
        console.error("SSE连接错误:", error);
      },
    }
  );
  sseConnection2 = createSSEConnection(
    "/abc/api/datacontroller/public/ssestream/11",
    token,
    {
      onOpen: () => {
        console.log("SSE连接已建立");
      },
      onMessage: (data) => {
        console.log("收到SSE活跃消息:", data);
        activeCount.value = data.numOfActivity;
      },
      onError: (error) => {
        console.error("SSE连接错误:", error);
      },
    }
  );
});

onDeactivated(() => {
  sseConnection?.close();
  sseConnection1?.close();
  sseConnection2?.close();
  console.log("所有 SSE 连接已关闭");
});

const dashOpt1 = ref({
  xAxis: {
    type: "category",
    boundaryGap: false,
    data: [],
  },
  yAxis: {
    type: "value",
  },
  legend: {
    data: ["活跃数量", "在线数量"],
  },
  grid: {
    top: "2%",
    left: "2%",
    right: "3%",
    bottom: "2%",
    containLabel: true,
  },
  color: ["#009688", "#f44336"],
  series: [
    {
      name: "在线数量",
      type: "line",
      areaStyle: {
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "rgba(0, 150, 136, 0.8)" },
          { offset: 1, color: "rgba(0, 150, 136, 0.2)" },
        ]),
      },
      smooth: true,
      data: [],
    },
    {
      name: "活跃数量",
      type: "line",
      areaStyle: {
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "rgba(244, 67, 54, 0.8)" },
          { offset: 1, color: "rgba(244, 67, 54, 0.2)" },
        ]),
      },
      smooth: true,
      data: [],
    },
  ],
});

const fetchChartData = async () => {
  try {
    const response = await axios.get(
      "/abc/api/datacontroller/public/activity/seven-days",
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    const lineData = response.data.data;
    // 提取日期作为 xAxis 数据
    const xAxisData = Object.keys(lineData);

    // 提取 onlineCount 和 activityCount 作为 series 数据
    const onlineData = Object.values(lineData).map((item: { onlineCount: number }) => item.onlineCount);
    const activityData = Object.values(lineData).map(
      (item: { activityCount: number }) => item.activityCount
    );

    dashOpt1.value.xAxis.data =xAxisData;
    dashOpt1.value.series[0].data = onlineData;
    dashOpt1.value.series[1].data = activityData;
    console.log("图表数据更新成功:", lineData);
  } catch (error) {
    console.error("获取图表数据失败:", error);
    return null;
  }
};

onMounted(() => {
  fetchChartData(); // 初次加载时获取图表数据
  fetchExpChartData(); // 初次加载时获取异常数据

  // 每五分钟更新一次图表
  const updateInterval = setInterval(() => {
    fetchChartData();
    fetchExpChartData();
  }, 5 * 60 * 1000); // 5 分钟 = 5 * 60 * 1000 毫秒

  // 在组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(updateInterval);
  });
});

use([
  CanvasRenderer,
  BarChart,
  GridComponent,
  LineChart,
  PieChart,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  VisualMapComponent,
  MapChart,
]);
registerMap("china", chinaMap);

const currentTime = ref("");

setInterval(() => {
  const now = new Date();
  currentTime.value = now.toLocaleString("zh-CN", {
    hour12: false,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}, 1000);

const activities = ref([
  {
    content: "车辆行驶",
    description: "xxx车辆正在行驶，去查看车辆状态",
    timestamp: "30分钟前",
    color: "#00bcd4",
  },
  {
    content: "异常处理",
    description: "xxx异常已被处理",
    timestamp: "55分钟前",
    color: "#1ABC9C",
  },
  {
    content: "异常捕捉",
    description: "捕捉到xxx异常，请处理",
    timestamp: "1小时前",
    color: "#3f51b5",
  },
]);

const newNotification = ref("");
const addNotification = () => {
  if (newNotification.value.trim()) {
    activities.value.unshift({
      content: "通知",
      description: newNotification.value,
      timestamp: "刚刚",
      color: "#f39c12",
    });
    newNotification.value = "";
  } else {
    alert("请输入通知内容！");
  }
};

const ranks = ref([]);

const fetchExpChartData = async () => {
  try {
    const response = await axios.get(
      "/abc/api/datacontroller/public/exceptiondata",
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    const chartData = response.data;
    ranks.value = chartData;
    console.log("图表数据更新成功:", chartData);
  } catch (error) {
    console.error("获取图表数据失败:", error);
    return null;
  }
};

const showList = ref(false);
const options = ref([
  "选项1",
  "选项2",
  "选项3",
  "选项4",
  "选项5",
  "选项6",
  "选项7",
  "选项8",
  "选项9",
  "选项10",
]);
const selectedOptions = ref([]);

const printSelections = () => {
  if (selectedOptions.value.length > 0) {
    console.log("选中的选项是：", selectedOptions.value);
    alert(`选中的选项是：${selectedOptions.value.join(", ")}`);
  } else {
    alert("请先选择一个或多个选项！");
  }
};

const inputData = ref("");
const parsedData = ref("");
const parsedTableData = ref([]);
const showInitialParser = ref(true);

const resetParser = () => {
  parsedData.value = "";
  showInitialParser.value = true;
};

const parseData = async () => {
  if (!inputData.value.trim()) {
    alert("请输入数据！");
    return;
  }
  try {
    const token = localStorage.getItem("token") || "";
    const response = await fetch("/abc/api/mqtt/analysis", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: inputData.value,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! 状态码: ${response.status}`);
    }

    const result = await response.json();
    parsedData.value = JSON.stringify(result, null, 2);
    showInitialParser.value = false;

    parsedTableData.value = [
      { key: "prefix", value: result.prefix },
      { key: "dataLen", value: result.dataLen },
      { key: "dataCategory", value: result.dataCategory },
      { key: "ver", value: result.ver },
      { key: "timestamp", value: new Date(result.timestamp).toLocaleString() },
      { key: "ctl", value: result.ctl },
      { key: "vehicleId", value: result.dataContent.vehicleId },
      { key: "messageId", value: result.dataContent.messageId },
      {
        key: "timestampGNSS",
        value: new Date(result.dataContent.timestampGNSS).toLocaleString(),
      },
      { key: "velocityGNSS", value: result.dataContent.velocityGNSS },
      {
        key: "position",
        value: `经度: ${result.dataContent.position.longitude}, 纬度: ${result.dataContent.position.latitude}, 海拔: ${result.dataContent.position.elevation}`,
      },
      { key: "heading", value: result.dataContent.heading },
      { key: "tapPos", value: result.dataContent.tapPos },
      { key: "steeringAngle", value: result.dataContent.steeringAngle },
      { key: "engineTorque", value: result.dataContent.engineTorque },
      {
        key: "destLocation",
        value: `经度: ${result.dataContent.destLocation.longitude}, 纬度: ${result.dataContent.destLocation.latitude}`,
      },
      { key: "passPointsNum", value: result.dataContent.passPointsNum },
    ];
  } catch (error) {
    console.error("解析失败:", error);
  }
};

const vehicleInfo = ref("");
const detectionResult = ref("");
const showDetectionResult = ref(false);

const resetDetection = () => {
  detectionResult.value = "";
  showDetectionResult.value = false;
};

const detectAnomalies = async () => {
  if (!vehicleInfo.value.trim()) {
    alert("请输入车辆信息！");
    return;
  }
  try {
    const token = localStorage.getItem("token") || "";
    const response = await fetch("/stu/detect-anomaly/", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: vehicleInfo.value,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! 状态码: ${response.status}`);
    }

    const result = await response.json();
    detectionResult.value = JSON.stringify(result, null, 2);
    showDetectionResult.value = true;
    console.log("检测结果:", result);
  } catch (error) {
    console.error("检测失败:", error);
    alert("检测失败，请检查输入或稍后重试！");
  }
};

// 新增的复制功能方法
const copyParsedData = () => {
  if (!parsedData.value) return;
  navigator.clipboard
    .writeText(parsedData.value)
    .then(() => {
      ElMessage.success("解析结果已复制到剪贴板");
    })
    .catch((err) => {
      console.error("复制失败:", err);
      ElMessage.error("复制失败，请手动复制");
    });
};

const copyDetectionResult = () => {
  if (!detectionResult.value) return;
  navigator.clipboard
    .writeText(detectionResult.value)
    .then(() => {
      ElMessage.success("检测结果已复制到剪贴板");
    })
    .catch((err) => {
      console.error("复制失败:", err);
      ElMessage.error("复制失败，请手动复制");
    });
};
</script>

<style>
.dashboard {
  background-color: #4575bd;
}

.card-body {
  display: flex;
  align-items: center;
  height: 100px;
  padding: 0;
}

.card-content {
  flex: 1;
  text-align: center;
  font-size: 14px;
  color: #362f2f;
  padding: 0 20px;
}

.card-num {
  font-size: 30px;
}

.card-icon {
  width: 80px;
  height: 80px;
  object-fit: contain;
  padding: 10px;
}

.bg-blue {
  background-color: #007bff;
}

.bg-green {
  background-color: #28a745;
}

.bg-red {
  background-color: #dc3545;
}

.bg-orange {
  background-color: #fd7e14;
}

.color1 {
  color: #b2d2f5;
}

.color2 {
  color: #64d572;
}

.color3 {
  color: #f25e43;
}

.color4 {
  color: #e9a745;
}

.chart {
  width: 100%;
  height: 400px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 10px;
  margin-bottom: 5px;
}

.card-header-left {
  display: flex;
  flex-direction: column;
}

.card-header-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.card-header-desc {
  font-size: 14px;
  color: #999;
}

.timeline-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  color: #fbffeacb;
}

.tiline-time,
.timeline-desc {
  font-size: 12px;
  color: #dcdcdc;
}

.rank-item {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.rank-item-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f2f2f2;
  text-align: center;
  line-height: 40px;
  margin-right: 10px;
}

.rank-item-content {
  flex: 1;
}

.rank-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #343434;
  margin-bottom: 10px;
}

.rank-item-desc {
  font-size: 14px;
  color: #999;
}

.map-chart {
  width: 100%;
  height: 350px;
}

.list-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  height: 350px;
  padding: 10px;
}

.scrollable-list {
  flex: 1;
  width: 100%;
  overflow-y: auto;
  padding-right: 10px;
  margin-bottom: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.checkbox-item {
  display: block;
  margin-top: 5px;
  margin-left: 10px;
}

.scrollable-list ::-webkit-scrollbar {
  width: 6px;
}

.scrollable-list ::-webkit-scrollbar-thumb {
  background-color: #c1c1c1;
  border-radius: 3px;
}

.scrollable-list ::-webkit-scrollbar-track {
  background-color: #f5f5f5;
}

.list-buttons {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-top: 10px;
}

.notification-input {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.notification-textbox {
  flex: 1;
  margin-right: 10px;
  height: 30px;
  font-size: 16px;
}

.notification-button {
  flex-shrink: 0;
  height: 30px;
  font-size: 16px;
}

.data-parser {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
}

.parser-input {
  flex: 1;
  margin-right: 10px;
  height: 100px;
}

.parser-button {
  flex-shrink: 0;
  height: 40px;
}

/* 新增的解析结果容器样式 */
.parser-result-container {
  background-color: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 20px;
  padding: 10px;
  max-height: 250px;
  overflow-y: auto;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: bold;
}

.copy-button {
  padding: 0;
  margin-left: 10px;
}

.json-display {
  margin: 0px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
}
</style>
