<template>
  <div class="container">
    <!-- 地图区域 -->
    <div class="map-container">
      <baidu-map
        class="map"
        :center="center"
        :zoom="15"
        @ready="handleMapReady"
        @click="handleMapClick"
        @rightclick="handleMapRightClick"
      >
        <bm-navigation anchor="BMAP_ANCHOR_TOP_RIGHT"></bm-navigation>
        <bm-marker
          v-for="(point, index) in markers"
          :key="point.vehicleId"
          :position="{ lng: point.longitude, lat: point.latitude }"
          :icon="customIcon"
          @click="showVehicleInfo(point, index)"
        >
          <bm-label
            :content="`${point.vehicleId}`"
            :labelStyle="{
              color: '#fff',
              fontSize: '12px',
              backgroundColor: '#81c784',
              padding: '2px 6px',
              borderRadius: '10px',
            }"
            :offset="{ width: 0, height: 20 }"
          />
        </bm-marker>
      </baidu-map>
      <!-- 右键菜单 -->
      <div
        v-if="contextMenuVisible"
        class="context-menu"
        :style="contextMenuStyle"
      >
        <div class="menu-item" @click="addMarkerAtContextMenu">
          <span class="menu-icon">➕</span> 添加车辆
        </div>
        <div
          v-if="hoveredMarkerIndex !== null"
          class="menu-item delete-item"
          @click="removeMarkerAtContextMenu"
        >
          <span class="menu-icon">❌</span> 删除车辆
        </div>
      </div>
    </div>

    <!-- 车辆列表区域 -->
    <div class="list-container">
      <h3 class="list-title">车辆列表</h3>
      <el-table
        :data="paginatedMarkers"
        border
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="vehicleId" label="车辆ID" />
        <el-table-column prop="longitude" label="经度" />
        <el-table-column prop="latitude" label="纬度" />
      </el-table>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="markers.length"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
    <!-- 车辆信息面板 -->
    <div
      v-if="infoPanelVisible"
      class="info-panel"
      :style="infoPanelStyle"
      @mousedown="startDrag"
    >
      <div class="info-header">
        <h3>车辆监控信息</h3>
        <span class="current-time">{{ currentTime }}</span>
        <button @click="closeInfoPanel" class="close-btn">×</button>
      </div>
      <div class="info-content">
        <div class="vehicle-image">
          <img :src="selectedCarImage" alt="车辆图片" />
        </div>
        <div class="info-row">
          <span class="info-label">坐标：</span>
          <span class="info-value"
            >{{ selectedPoint.longitude }},
            {{ selectedPoint.latitude }}</span
          >
        </div>
        <div class="info-row">
          <span class="info-label">编号：</span>
          <span class="info-value">{{ selectedPoint.vehicleId }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">状态：</span>
          <span :class="['status-badge', vehicleStatus.class]">{{
            vehicleStatus.text
          }}</span>
        </div>
        <div class="info-divider"></div>
        <div class="status-grid">
          <div class="status-item">
            <span class="status-icon" :class="getStatusClass('steering')">{{
              getStatusIcon("steering")
            }}</span>
            <span class="status-name">转向</span>
          </div>
          <div class="status-item">
            <span class="status-icon" :class="getStatusClass('timestamp')">{{
              getStatusIcon("timestamp")
            }}</span>
            <span class="status-name">时间戳</span>
          </div>
          <div class="status-item">
            <span class="status-icon" :class="getStatusClass('geoLocation')">{{
              getStatusIcon("geoLocation")
            }}</span>
            <span class="status-name">经纬度</span>
          </div>
          <div class="status-item">
            <span class="status-icon" :class="getStatusClass('speed')">{{
              getStatusIcon("speed")
            }}</span>
            <span class="status-name">速度</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import * as echarts from "echarts";
import { ref, computed, onMounted, onUnmounted } from "vue";
import { createSSEConnection } from '../utils/sse'; 

let sseConnection: { close: () => void } | null = null;
let sseConnection1: { close: () => void } | null = null;
let sseConnection2: { close: () => void } | null = null;
let sseConnection3: { close: () => void } | null = null;
let sseConnection4: { close: () => void } | null = null;

const vehicleMap = new Map(); // 存储车辆数据
const token = localStorage.getItem('token') || ''; // 假设 token 存储在 localStorage 中

onMounted(() => {
  sseConnection = createSSEConnection('/abc/api/datacontroller/public/ssestream/1', token, {
    onOpen: () => {
      console.log('SSE连接已建立');
    },
    onMessage: (data) => {
      try {
        console.log('收到SSE数据:', data);
        const { vehicleId, longitude, latitude } = data;
  
        if (!vehicleId || !longitude || !latitude) {
          console.warn('收到无效数据:', data);
          return;
        }
  
        // 更新或添加车辆数据
        if (vehicleMap.has(vehicleId)) {
          // 更新经纬度
          const marker = vehicleMap.get(vehicleId);
          marker.longitude = longitude;
          marker.latitude = latitude;
        } else {
          // 添加新车辆
          vehicleMap.set(vehicleId, { vehicleId, longitude, latitude });
        }
  
        // 更新地图上的标记
        updateMarkersOnMap();
      } catch (error) {
        console.error('解析 SSE 数据失败:', error);
      }
    },
    onError: (error) => {
      console.error('SSE连接错误:', error);
    },
  });
  // 创建转向异常数据SSE连接
  sseConnection1 = createSSEConnection('/abc/api/datacontroller/public/ssestream/3', token, {
    onOpen: () => {
      console.log('SSE连接1已建立');
    },
    onMessage: (data) => {
      try {
        console.log('收到SSE转向异常数据:', data);
        const { vehicleId, steeringExp } = data;
  
        if (!vehicleId || steeringExp === undefined) {
          console.warn('收到无效数据:', data);
          return;
        }
  
        // 更新或添加车辆数据
        if (vehicleMap.has(vehicleId)) {
          const marker = vehicleMap.get(vehicleId);
          marker.steeringExp = steeringExp;
        } else {
          vehicleMap.set(vehicleId, { vehicleId, steeringExp });
        }
      } catch (error) {
        console.error('解析 SSE 数据失败:', error);
      }
    },
    onError: (error) => {
      console.error('SSE连接错误:', error);
    },
  });
  // 创建时间戳异常数据 SSE 连接
  sseConnection2 = createSSEConnection('/abc/api/datacontroller/public/ssestream/4', token, {
    onOpen: () => {
      console.log('SSE连接2已建立');
    },
    onMessage: (data) => {
      try {
        console.log('收到SSE时间戳异常数据:', data);
        const { vehicleId, timestampExp } = data;
  
        if (!vehicleId || timestampExp === undefined) {
          console.warn('收到无效数据:', data);
          return;
        }
  
        // 更新或添加车辆数据
        if (vehicleMap.has(vehicleId)) {
          const marker = vehicleMap.get(vehicleId);
          marker.timestampExp = timestampExp;
        } else {
          vehicleMap.set(vehicleId, { vehicleId, timestampExp });
        }
      } catch (error) {
        console.error('解析 SSE 数据失败:', error);
      }
    },
    onError: (error) => {
      console.error('SSE连接错误:', error);
    },
  });
  // 创建经纬度异常数据 SSE 连接
  sseConnection3 = createSSEConnection('/abc/api/datacontroller/public/ssestream/5', token, {
    onOpen: () => {
      console.log('SSE连接3已建立');
    },
    onMessage: (data) => {
      try {
        console.log('收到SSE经纬度异常数据:', data);
        const { vehicleId, geoLocationExp } = data;
  
        if (!vehicleId || geoLocationExp === undefined) {
          console.warn('收到无效数据:', data);
          return;
        }
  
        // 更新或添加车辆数据
        if (vehicleMap.has(vehicleId)) {
          const marker = vehicleMap.get(vehicleId);
          marker.geoLocationExp = geoLocationExp;
        } else {
          vehicleMap.set(vehicleId, { vehicleId, geoLocationExp });
        }
      } catch (error) {
        console.error('解析 SSE 数据失败:', error);
      }
    },
    onError: (error) => {
      console.error('SSE连接错误:', error);
    },
  });
  // 创建经纬度异常数据 SSE 连接
  sseConnection4 = createSSEConnection('/abc/api/datacontroller/public/ssestream/6', token, {
    onOpen: () => {
      console.log('SSE连接4已建立');
    },
    onMessage: (data) => {
      try {
        console.log('收到SSE速度异常数据:', data);
        const { vehicleId, speedExp } = data;
  
        if (!vehicleId || speedExp === undefined) {
          console.warn('收到无效数据:', data);
          return;
        }
  
        // 更新或添加车辆数据
        if (vehicleMap.has(vehicleId)) {
          const marker = vehicleMap.get(vehicleId);
          marker.speedExp = speedExp;
        } else {
          vehicleMap.set(vehicleId, { vehicleId, speedExp });
        }
      } catch (error) {
        console.error('解析 SSE 数据失败:', error);
      }
    },
    onError: (error) => {
      console.error('SSE连接错误:', error);
    },
  });
});
onUnmounted(() => {
  sseConnection?.close();
  sseConnection1?.close();
  sseConnection2?.close();
  sseConnection3?.close();
  sseConnection4?.close();
});

const updateMarkersOnMap = () => {
  // 清空现有的 markers
  markers.value = [];

  // 遍历 vehicleMap，将数据添加到 markers
  for (const [vehicleId, { longitude, latitude }] of vehicleMap.entries()) {
    markers.value.push({ vehicleId, longitude, latitude });
  }

  //console.log('地图标记已更新:', markers.value);
};
// 地图中心点和标点
const center = ref({ lng: 106.3852, lat: 29.5384 });
const markers = ref([
  // 添加更多车辆数据
]);
const showMarkerList = ref(false);

// 信息面板相关
const infoPanelVisible = ref(false);
const selectedPoint = ref({});
const infoPanelStyle = ref({});
const hoveredMarkerIndex = ref(null);
const currentTime = ref("");

// 右键菜单相关
const contextMenuVisible = ref(false);
const contextMenuPosition = ref({ x: 0, y: 0, lng: 0, lat: 0 });
const contextMenuStyle = computed(() => ({
  left: `${contextMenuPosition.value.x}px`,
  top: `${contextMenuPosition.value.y}px`,
}));

// 车辆状态数据
const statuses = ref({
  steering: false, // 默认正常
  timestamp: false, // 默认正常
  geoLocation: false, // 默认正常
  speed: false, // 默认正常
});

const carImages = ref([
  "../assets/img/car1.png",
  "../assets/img/car2.png",
  "../assets/img/car3.png",
  "../assets/img/car4.png",
  "../assets/img/car5.png",
]);

const selectedCarImage = ref("");

const getRandomCarImage = () => {
  const randomIndex = Math.floor(Math.random() * carImages.value.length);
  selectedCarImage.value = new URL(
    carImages.value[randomIndex],
    import.meta.url
  ).href;
};

// 自定义图标
const customIcon = ref({
  url: new URL("@/assets/img/car_icon.png", import.meta.url).href, // 使用 new URL 替代 require
  size: { width: 72, height: 32 }, // 图标的大小
  opts: {
    imageOffset: { width: 0, height: 0 }, // 图标在图片中的位置
    imageSize: { width: 48, height: 32 }, // 图标的大小
  },
});

// 更新时间
const updateTime = () => {
  const now = new Date();
  currentTime.value = now
    .toLocaleString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    })
    .replace(/\//g, "-");
};

// 计算车辆整体状态
const vehicleStatus = computed(() => {
  const allOk = Object.values(statuses.value).every((v) => v);
  return {
    class: allOk ? "status-warning" : "status-ok",
    text: allOk ? "警告" : "正常",
  };
});

// 地图加载完成的回调
const handleMapReady = ({ BMap, map }) => {
  console.log("地图已加载", map);
  map.setMapStyleV2({ styleId: "65d44bc71123817a008a3285df684c69" });
  map.enableScrollWheelZoom(true);
};

// 左键点击处理
const handleMapClick = (event) => {
  // 点击地图空白处关闭信息面板
  if (hoveredMarkerIndex.value === null) {
    infoPanelVisible.value = false;
  }
  closeContextMenu();
};

// 右键点击处理
const handleMapRightClick = (event) => {
  const point = event.point;
  const index = markers.value.findIndex(
    (marker) =>
      Math.abs(marker.lng - point.lng) < 0.0001 &&
      Math.abs(marker.lat - point.lat) < 0.0001
  );

  hoveredMarkerIndex.value = index !== -1 ? index : null;
  contextMenuPosition.value = {
    x: event.domEvent.clientX - 250,
    y: event.domEvent.clientY - 170,
    lng: point.lng,
    lat: point.lat,
  };
  contextMenuVisible.value = true;

  // 阻止默认右键菜单
  event.domEvent.preventDefault();
};

// 关闭右键菜单
const closeContextMenu = () => {
  contextMenuVisible.value = false;
};

// 在右键菜单位置添加标记
const addMarkerAtContextMenu = () => {
  const point = {
    lng: contextMenuPosition.value.lng,
    lat: contextMenuPosition.value.lat,
  };
  markers.value.push(point);
  closeContextMenu();
};

// 删除右键菜单选中的标记
const removeMarkerAtContextMenu = () => {
  if (hoveredMarkerIndex.value !== null) {
    markers.value.splice(hoveredMarkerIndex.value, 1);
    infoPanelVisible.value = false;
  }
  closeContextMenu();
};

// 显示车辆信息
const showVehicleInfo = (point, index) => {
  selectedPoint.value = vehicleMap.get(point.vehicleId) || point;
  hoveredMarkerIndex.value = index;
  infoPanelVisible.value = true;
  infoPanelStyle.value = {
    left: "20px",
    top: "80px",
  };

  // 随机生成车辆状态（演示用）
  statuses.value = {
    steering: selectedPoint.value.steeringExp ?? false, // 默认为正常
    timestamp: selectedPoint.value.timestampExp ?? false, // 默认为正常
    geoLocation: selectedPoint.value.geoLocationExp ?? false, // 默认为正常
    speed: selectedPoint.value.speedExp ?? false, // 默认为正常
  };

  // 更新当前时间
  updateTime();
  showMarkerList.value = false;

  // 更新车辆图片
  getRandomCarImage();
};
// 关闭信息面板
const closeInfoPanel = () => {
  infoPanelVisible.value = false;
  hoveredMarkerIndex.value = null;
};

// 状态样式和图标
const getStatusClass = (type) => {
  return statuses.value[type] ? "status-error" : "status-ok"; // true 表示异常
};

const getStatusIcon = (type) => {
  return statuses.value[type] ? "✗" : "✓"; // true 表示异常
};

// 定时更新时间
let timeInterval;
onMounted(() => {
  updateTime();
  timeInterval = setInterval(updateTime, 1000);
});

onUnmounted(() => {
  clearInterval(timeInterval);
});

// 拖动相关
let isDragging = false;
let dragStartX = 0;
let dragStartY = 0;

const startDrag = (event) => {
  isDragging = true;
  dragStartX = event.clientX - parseInt(infoPanelStyle.value.left || 0, 10);
  dragStartY = event.clientY - parseInt(infoPanelStyle.value.top || 0, 10);

  document.addEventListener("mousemove", handleDrag);
  document.addEventListener("mouseup", stopDrag);
};

const handleDrag = (event) => {
  if (isDragging) {
    infoPanelStyle.value.left = `${event.clientX - dragStartX}px`;
    infoPanelStyle.value.top = `${event.clientY - dragStartY}px`;
  }
};

const stopDrag = () => {
  isDragging = false;
  document.removeEventListener("mousemove", handleDrag);
  document.removeEventListener("mouseup", stopDrag);
};

// 分页相关
const currentPage = ref(1);
const pageSize = ref(15);
const paginatedMarkers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return markers.value.slice(start, end).map((marker, index) => ({
    ...marker,
    index: start + index,
  }));
});

const handlePageChange = (page) => {
  currentPage.value = page;
};

const handleRowClick = (row) => {
  center.value = { lng: row.longitude, lat: row.latitude };
};
</script>

<style scoped>
/* 基础样式 */
html,
body,
.container,
.map-container,
.map {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  font-family: "Arial", sans-serif;
}

.container {
  display: flex;
  height: 85vh;
  background-color: #f5f5f5;
}

.map-container {
  flex: 3;
  position: relative;
  width: 100%;
  height: 100%;
}
.map {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.list-container {
  flex: 1;
  background-color: #ffffff;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow-y: auto;
}

.list-title {
  font-size: 16px;
  font-weight: bold;
  color: #424242;
  margin-bottom: 16px;
}

.el-table {
  flex: 1;
  margin-bottom: 16px;
}

.el-pagination {
  margin-top: 16px;
  text-align: center;
}

/* 查询面板 */
.query-panel {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.query-panel button {
  padding: 8px 16px;
  background-color: #81c784;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.query-panel button:hover {
  background-color: #66bb6a;
}

.marker-list {
  position: absolute;
  top: 100%;
  left: 0;
  width: 200px;
  background-color: white;
  border-radius: 0 0 6px 6px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  z-index: 11;
  padding: 10px;
  margin-top: 0;
  border: 1px solid #e0e0e0;
}

.marker-list h3 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
  padding-bottom: 5px;
  border-bottom: 1px solid #eee;
}

.marker-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
}

.marker-list li {
  padding: 8px 10px;
  cursor: pointer;
  font-size: 13px;
  color: #424242;
  border-bottom: 1px solid #f5f5f5;
  transition: all 0.2s;
}

.marker-list li:hover {
  background-color: #f5f5f5;
  color: #2e7d32;
}

.marker-list li:last-child {
  border-bottom: none;
}

/* 信息面板 */
.info-panel {
  position: absolute;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  width: 300px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  cursor: grab;
}

.info-panel:active {
  cursor: grabbing;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f5f5f5;
  color: #424242;
  border-bottom: 1px solid #e0e0e0;
}

.info-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.current-time {
  font-size: 12px;
  color: #757575;
  margin-left: 10px;
}

.close-btn {
  background: none;
  border: none;
  color: #757575;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: all 0.2s;
}

.close-btn:hover {
  color: #424242;
  transform: scale(1.1);
}

.info-content {
  padding: 16px;
}

.vehicle-image {
  width: 100%;
  height: 140px;
  margin-bottom: 16px;
  overflow: hidden;
  border-radius: 4px;
  background-color: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e0e0e0;
}

.vehicle-image img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
}

.info-row {
  display: flex;
  margin-bottom: 12px;
  align-items: center;
}

.info-label {
  font-weight: 500;
  color: #757575;
  width: 60px;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #424242;
  font-size: 14px;
  word-break: break-all;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-ok {
  background-color: rgba(129, 199, 132, 0.2);
  color: #2e7d32;
  border: 1px solid #81c784;
}

.status-warning {
  background-color: rgba(255, 183, 77, 0.2);
  color: #ff8f00;
  border: 1px solid #ffb74d;
}

.info-divider {
  height: 1px;
  background-color: #e0e0e0;
  margin: 16px 0;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  background-color: #fafafa;
  border: 1px solid #e0e0e0;
  transition: all 0.2s;
}

.status-item:hover {
  background-color: #f5f5f5;
  transform: translateY(-2px);
}

.status-icon {
  font-size: 24px;
  margin-bottom: 6px;
}

.status-name {
  font-size: 12px;
  color: #757575;
}

.status-ok {
  color: #2e7d32;
}

.status-error {
  color: #d32f2f;
}

/* 右键菜单 */
.context-menu {
  position: absolute;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  width: 160px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}

.menu-item {
  padding: 10px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #424242;
  transition: all 0.2s;
  font-size: 14px;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-icon {
  margin-right: 10px;
  font-size: 16px;
}

.delete-item {
  color: #d32f2f;
}

/* 按钮面板 */
.button-panel {
  position: absolute;
  top: 70px;
  left: 10px;
  background-color: rgba(255, 255, 255, 0.95);
  padding: 10px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid #e0e0e0;
}

.button-panel button {
  padding: 8px 12px;
  background-color: #e0e0e0;
  color: #424242;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  min-width: 80px;
}

.button-panel button:hover {
  background-color: #bdbdbd;
}

/* 图表面板 */
.chart-panel {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 320px;
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border: 1px solid #e0e0e0;
}

.chart-item {
  width: 100%;
  height: 200px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  transform-origin: top right;
  border: 1px solid #e0e0e0;
}

.chart-item.chart-expanded {
  transform: scale(1.5);
  z-index: 100;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  margin-right: 50px;
  margin-bottom: 50px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .query-panel {
    width: 90%;
    left: 5%;
    transform: none;
  }

  .info-panel {
    width: 90%;
    left: 5% !important;
    top: 100px !important;
  }

  .chart-panel {
    width: 90%;
    left: 5%;
    right: auto;
  }
}
</style>
