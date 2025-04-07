<template>
    <div class="container">
        <!-- 地图区域 -->
        <div class="map-container">
            <!-- 输入框和按钮 -->
            <div class="input-panel">
                <input v-model="lng" type="text" placeholder="输入经度" />
                <input v-model="lat" type="text" placeholder="输入纬度" />
                <button @click="setMarker">查询</button>
            </div>

            <baidu-map 
              class="map" 
              :center="{ lng: center.lng, lat: center.lat }" 
              :zoom="15"
              @ready="handleMapReady"
            >
              <bm-navigation anchor="BMAP_ANCHOR_TOP_RIGHT"></bm-navigation>
              <bm-marker :position="{ lng: marker.lng, lat: marker.lat }" />
            </baidu-map>

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
import { ref } from 'vue';

const visibleCharts = ref([]); // 当前显示的图表列表
const expandedChart = ref(null); // 当前放大的图表
const chartInstances = ref({}); // 存储图表实例

// 地图中心点和标点
const center = ref({ lng: 106.552, lat: 29.562 }); // 默认地图中心
const marker = ref({ lng: 106.552, lat: 29.562 }); // 默认标点位置

// 输入框绑定的经纬度
const lng = ref('');
const lat = ref('');

// 设置标点并更新地图中心
const setMarker = () => {
    const longitude = parseFloat(lng.value);
    const latitude = parseFloat(lat.value);

    if (isNaN(longitude) || isNaN(latitude)) {
        alert('请输入有效的经纬度！');
        return;
    }

    marker.value = { lng: longitude, lat: latitude };
    center.value = { lng: longitude, lat: latitude };
};

// 地图加载完成的回调
const handleMapReady = ({ BMap, map }) => {
    console.log("地图已加载", map);
    map.setMapStyleV2({ styleId: '65d44bc71123817a008a3285df684c69' });
    map.enableScrollWheelZoom(true);   
};

// 切换图表显示/隐藏
const toggleChart = (chartType) => {
    const index = visibleCharts.value.indexOf(chartType);
    if (index === -1) {
        visibleCharts.value.push(chartType); // 添加图表
    } else {
        visibleCharts.value.splice(index, 1); // 移除图表
        if (expandedChart.value === chartType) {
            expandedChart.value = null;
        }
    }
    setTimeout(() => {
        initCharts(); // 初始化所有可见图表
    }, 0);
};

// 初始化所有可见图表
const initCharts = () => {
    visibleCharts.value.forEach((chartType) => {
        const chartDom = document.getElementById(chartType);
        if (!chartDom) return;

        // 如果已有实例则先销毁
        if (chartInstances.value[chartType]) {
            chartInstances.value[chartType].dispose();
        }

        const myChart = echarts.init(chartDom);
        chartInstances.value[chartType] = myChart;

        // 根据图表类型加载不同的数据和配置
        let option;
        if (chartType === 'chart1') {
            option = {
                title: { text: '图表1 - 条形图' },
                tooltip: {},
                xAxis: { data: ['A', 'B', 'C', 'D', 'E'] },
                yAxis: {},
                series: [{ type: 'bar', data: [5, 20, 36, 10, 10] }],
            };
        } else if (chartType === 'chart2') {
            option = {
                title: { text: '图表2 - 折线图' },
                tooltip: {},
                xAxis: { data: ['A', 'B', 'C', 'D', 'E'] },
                yAxis: {},
                series: [{ type: 'line', data: [15, 25, 16, 20, 30] }],
            };
        } else if (chartType === 'chart3') {
            option = {
                title: { text: '图表3 - 饼图' },
                tooltip: {},
                series: [{
                    type: 'pie',
                    radius: '50%',
                    data: [
                        { value: 10, name: 'A' },
                        { value: 20, name: 'B' },
                        { value: 30, name: 'C' },
                        { value: 40, name: 'D' },
                        { value: 50, name: 'E' },
                    ],
                }],
            };
        }

        myChart.setOption(option);
    });
};

// 放大图表
const expandChart = (chartType) => {
    expandedChart.value = chartType;
    if (chartInstances.value[chartType]) {
        setTimeout(() => {
            chartInstances.value[chartType].resize();
        }, 10);
    }
};

// 缩小图表
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
</script>

<style scoped>
html, body {
    margin: 0;
    padding: 0;
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

.container {
    display: flex;
    height: 100%;
    width: 100%;
}

.map-container {
    flex: 1;
    position: relative;
}

.input-panel {
    position: absolute;
    top: 10px;
    left: 50%;
    transform: translateX(-50%);
    background-color: rgba(255, 255, 255, 0.9);
    padding: 10px;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
    z-index: 10;
    display: flex;
    gap: 10px;
}

.input-panel input {
    padding: 5px;
    border: 1px solid #ccc;
    border-radius: 3px;
    width: 120px;
}

.input-panel button {
    padding: 5px 10px;
    background-color: #007bff;
    color: white;
    border: none;
    border-radius: 3px;
    cursor: pointer;
}

.input-panel button:hover {
    background-color: #0056b3;
}

.button-panel {
    position: absolute;
    top: 60px;
    left: 10px;
    background-color: rgba(255, 255, 255, 0.9);
    padding: 10px;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
    z-index: 10;
    display: flex;
    flex-direction: column;
}

.button-panel button {
    margin: 5px 0;
    padding: 10px;
    width: 100px;
    cursor: pointer;
}

.chart-panel {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 300px;
    background-color: rgba(255, 255, 255, 0.9);
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
    z-index: 10;
    padding: 10px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.chart-item {
    width: 100%;
    height: 200px;
    background-color: #f5f5f5;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    transform-origin: top right; /* 设置变换原点为右上角 */
}

/* 放大时的样式 */
.chart-item.chart-expanded {
    transform: scale(1.5); /* 放大1.5倍 */
    z-index: 100;
    box-shadow: 0 0 20px rgba(0,0,0,0.3);
    margin-right: 50px; /* 防止放大后超出容器 */
    margin-bottom: 50px; /* 防止放大后超出容器 */
}
</style>
