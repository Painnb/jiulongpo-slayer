<template>
    <div class="container">
        <!-- 地图区域 -->
        <div class="map-container">
            <!-- 查询按钮 -->
            <div class="query-panel">
                <button @mouseenter="showMarkerList = true" @mouseleave="showMarkerList = false">查询车辆</button>
                <div class="marker-list" v-show="showMarkerList" @mouseenter="showMarkerList = true" @mouseleave="showMarkerList = false">
                    <h3>车辆列表</h3>
                    <ul>
                        <li v-for="(point, index) in markers" :key="index" @click="showVehicleInfo(point, index)">
                            车辆{{ index + 1 }}: {{ point.lng.toFixed(4) }}, {{ point.lat.toFixed(4) }}
                        </li>
                    </ul>
                </div>
            </div>

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
                :key="index" 
                :position="point"
                @click="showVehicleInfo(point, index)"
              >
                <bm-label
                  :content="`车辆${index + 1}`"
                  :labelStyle="{color: '#fff', fontSize: '12px', backgroundColor: '#81c784', padding: '2px 6px', borderRadius: '10px'}"
                  :offset="{width: 0, height: 20}"
                />
              </bm-marker>
            </baidu-map>

            <!-- 车辆信息面板 -->
            <div v-if="infoPanelVisible" class="info-panel" :style="infoPanelStyle">
                <div class="info-header">
                    <h3>车辆监控信息</h3>
                    <span class="current-time">{{ currentTime }}</span>
                    <button @click="closeInfoPanel" class="close-btn">×</button>
                </div>
                <div class="info-content">
                    <div class="vehicle-image">
                        <img src="@/assets/img/car.png" alt="车辆图片" />
                    </div>
                    <div class="info-row">
                        <span class="info-label">坐标：</span>
                        <span class="info-value">{{ selectedPoint.lng.toFixed(6) }}, {{ selectedPoint.lat.toFixed(6) }}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">编号：</span>
                        <span class="info-value">V-{{ hoveredMarkerIndex + 1 }}</span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">状态：</span>
                        <span :class="['status-badge', vehicleStatus.class]">{{ vehicleStatus.text }}</span>
                    </div>
                    <div class="info-divider"></div>
                    <div class="status-grid">
                        <div class="status-item">
                            <span class="status-icon" :class="getStatusClass('steering')">{{ getStatusIcon('steering') }}</span>
                            <span class="status-name">方向盘</span>
                        </div>
                        <div class="status-item">
                            <span class="status-icon" :class="getStatusClass('acceleration')">{{ getStatusIcon('acceleration') }}</span>
                            <span class="status-name">加速度</span>
                        </div>
                        <div class="status-item">
                            <span class="status-icon" :class="getStatusClass('brake')">{{ getStatusIcon('brake') }}</span>
                            <span class="status-name">制动</span>
                        </div>
                        <div class="status-item">
                            <span class="status-icon" :class="getStatusClass('tire')">{{ getStatusIcon('tire') }}</span>
                            <span class="status-name">轮胎</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右键菜单 -->
            <div v-if="contextMenuVisible" class="context-menu" :style="contextMenuStyle">
                <div class="menu-item" @click="addMarkerAtContextMenu">
                    <span class="menu-icon">➕</span> 添加车辆
                </div>
                <div v-if="hoveredMarkerIndex !== null" class="menu-item delete-item" @click="removeMarkerAtContextMenu">
                    <span class="menu-icon">❌</span> 删除车辆
                </div>
            </div>

            <!-- 左侧按钮区域 -->
            <div class="button-panel">
                <button @click="toggleChart('chart1')">图表1</button>
                <button @click="toggleChart('chart2')">图表2</button>
                <button @click="toggleChart('chart3')">图表3</button>
            </div>

            <!-- 右侧图表区域 -->
            <div class="chart-panel">
                <div 
                  v-for="chart in visibleCharts" 
                  :key="chart" 
                  :id="chart" 
                  class="chart-item"
                  :class="{ 'chart-expanded': expandedChart === chart }"
                  @mouseenter="expandChart(chart)"
                  @mouseleave="shrinkChart(chart)"
                ></div>
            </div>
        </div>
    </div>
</template>

<script setup>
import * as echarts from 'echarts';
import { ref, computed, onMounted, onUnmounted } from 'vue';

// 地图中心点和标点
const center = ref({ lng: 106.552, lat: 29.562 });
const markers = ref([]);
const showMarkerList = ref(false);

// 信息面板相关
const infoPanelVisible = ref(false);
const selectedPoint = ref({});
const infoPanelStyle = ref({});
const hoveredMarkerIndex = ref(null);
const currentTime = ref('');

// 右键菜单相关
const contextMenuVisible = ref(false);
const contextMenuPosition = ref({ x: 0, y: 0, lng: 0, lat: 0 });
const contextMenuStyle = computed(() => ({
    left: `${contextMenuPosition.value.x + 10}px`,
    top: `${contextMenuPosition.value.y + 10}px`
}));

// 图表相关
const visibleCharts = ref([]);
const expandedChart = ref(null);
const chartInstances = ref({});

// 车辆状态数据
const statuses = ref({
    steering: true,
    acceleration: false,
    brake: true,
    tire: true,
});

// 更新时间
const updateTime = () => {
    const now = new Date();
    currentTime.value = now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    }).replace(/\//g, '-');
};

// 计算车辆整体状态
const vehicleStatus = computed(() => {
    const allOk = Object.values(statuses.value).every(v => v);
    return {
        class: allOk ? 'status-ok' : 'status-warning',
        text: allOk ? '正常' : '警告'
    };
});

// 地图加载完成的回调
const handleMapReady = ({ BMap, map }) => {
    console.log("地图已加载", map);
    map.setMapStyleV2({ styleId: '65d44bc71123817a008a3285df684c69' });
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
    const index = markers.value.findIndex(marker => 
        Math.abs(marker.lng - point.lng) < 0.0001 && 
        Math.abs(marker.lat - point.lat) < 0.0001
    );
    
    hoveredMarkerIndex.value = index !== -1 ? index : null;
    contextMenuPosition.value = {
        x: event.domEvent.clientX,
        y: event.domEvent.clientY,
        lng: point.lng,
        lat: point.lat
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
        lat: contextMenuPosition.value.lat
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
    selectedPoint.value = point;
    hoveredMarkerIndex.value = index;
    infoPanelVisible.value = true;
    infoPanelStyle.value = {
        left: '20px',
        top: '80px'
    };
    
    // 随机生成车辆状态（演示用）
    statuses.value = {
        steering: Math.random() > 0.3,
        acceleration: Math.random() > 0.3,
        brake: Math.random() > 0.3,
        tire: Math.random() > 0.3
    };
    
    // 更新当前时间
    updateTime();
    showMarkerList.value = false;
};

// 关闭信息面板
const closeInfoPanel = () => {
    infoPanelVisible.value = false;
    hoveredMarkerIndex.value = null;
};

// 状态样式和图标
const getStatusClass = (type) => {
    return statuses.value[type] ? 'status-ok' : 'status-error';
};

const getStatusIcon = (type) => {
    return statuses.value[type] ? '✓' : '✗';
};

// 图表相关方法
const toggleChart = (chartType) => {
    const index = visibleCharts.value.indexOf(chartType);
    if (index === -1) {
        visibleCharts.value.push(chartType);
    } else {
        visibleCharts.value.splice(index, 1);
        if (expandedChart.value === chartType) {
            expandedChart.value = null;
        }
    }
    setTimeout(() => {
        initCharts();
    }, 0);
};

const initCharts = () => {
    visibleCharts.value.forEach((chartType) => {
        const chartDom = document.getElementById(chartType);
        if (!chartDom) return;

        if (chartInstances.value[chartType]) {
            chartInstances.value[chartType].dispose();
        }

        const myChart = echarts.init(chartDom);
        chartInstances.value[chartType] = myChart;

        let option;
        if (chartType === 'chart1') {
            option = {
                backgroundColor: 'transparent',
                title: { 
                    text: '车辆速度统计', 
                    textStyle: { color: '#666' }
                },
                tooltip: {},
                xAxis: { 
                    data: ['周一', '周二', '周三', '周四', '周五'],
                    axisLine: { lineStyle: { color: '#ddd' } },
                    axisLabel: { color: '#666' }
                },
                yAxis: {
                    axisLine: { lineStyle: { color: '#ddd' } },
                    axisLabel: { color: '#666' },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                series: [{ 
                    type: 'bar', 
                    data: [80, 120, 90, 150, 110],
                    itemStyle: { color: '#a5d6a7' }
                }],
                textStyle: { color: '#666' }
            };
        } else if (chartType === 'chart2') {
            option = {
                backgroundColor: 'transparent',
                title: { 
                    text: '行驶里程趋势', 
                    textStyle: { color: '#666' }
                },
                tooltip: {},
                xAxis: { 
                    data: ['1月', '2月', '3月', '4月', '5月'],
                    axisLine: { lineStyle: { color: '#ddd' } },
                    axisLabel: { color: '#666' }
                },
                yAxis: {
                    axisLine: { lineStyle: { color: '#ddd' } },
                    axisLabel: { color: '#666' },
                    splitLine: { lineStyle: { color: '#eee' } }
                },
                series: [{ 
                    type: 'line', 
                    data: [150, 230, 180, 280, 210],
                    itemStyle: { color: '#a5d6a7' },
                    lineStyle: { color: '#a5d6a7' }
                }],
                textStyle: { color: '#666' }
            };
        } else if (chartType === 'chart3') {
            option = {
                backgroundColor: 'transparent',
                title: { 
                    text: '故障类型分布', 
                    textStyle: { color: '#666' }
                },
                tooltip: {},
                series: [{
                    type: 'pie',
                    radius: '50%',
                    data: [
                        { value: 10, name: '发动机' },
                        { value: 20, name: '制动系统' },
                        { value: 30, name: '电子设备' },
                        { value: 40, name: '轮胎' },
                        { value: 50, name: '其他' },
                    ],
                    itemStyle: {
                        color: function(params) {
                            const colorList = ['#a5d6a7', '#81c784', '#66bb6a', '#4caf50', '#43a047'];
                            return colorList[params.dataIndex];
                        }
                    },
                    label: { color: '#666' },
                    labelLine: { lineStyle: { color: '#ddd' } }
                }],
                textStyle: { color: '#666' }
            };
        }

        myChart.setOption(option);
    });
};

const expandChart = (chartType) => {
    expandedChart.value = chartType;
    if (chartInstances.value[chartType]) {
        setTimeout(() => {
            chartInstances.value[chartType].resize();
        }, 10);
    }
};

const shrinkChart = (chartType) => {
    if (expandedChart.value === chartType) {
        expandedChart.value = null;
        if (chartInstances.value[chartType]) {
            setTimeout(() => {
                chartInstances.value[chartType].resize();
            }, 10);
        }
    }
};

// 定时更新时间
let timeInterval;
onMounted(() => {
    updateTime();
    timeInterval = setInterval(updateTime, 1000);
    
    // 初始化时加载一个默认图表
    visibleCharts.value.push('chart1');
    setTimeout(initCharts, 100);
});

onUnmounted(() => {
    clearInterval(timeInterval);
});
</script>

<style scoped>
/* 基础样式 */
html, body, .container, .map-container, .map {
    margin: 0;
    padding: 0;
    width: 100%;
    height: 100%;
    font-family: 'Arial', sans-serif;
}

.container {
    display: flex;
    height: 100vh;
    background-color: #f5f5f5;
}

.map-container {
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
    margin-top: 5px;
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
    box-shadow: 0 0 20px rgba(0,0,0,0.1);
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
