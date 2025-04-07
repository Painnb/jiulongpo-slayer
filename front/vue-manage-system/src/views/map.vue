<template>
    <div class="container">
        <!-- 地图区域 -->
        <div class="map-container">
            <baidu-map 
              class="map" 
              :center="{ lng: 106.552, lat: 29.562 }" 
              :zoom="15"
              @ready="handleMapReady"
            >
              <bm-navigation anchor="BMAP_ANCHOR_TOP_RIGHT"></bm-navigation>
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
                ></div>
            </div>
        </div>
    </div>
</template>

<script setup>
import * as echarts from 'echarts';
import { ref, watch } from 'vue';

const visibleCharts = ref([]); // 当前显示的图表列表

// 切换图表显示/隐藏
const toggleChart = (chartType) => {
    const index = visibleCharts.value.indexOf(chartType);
    if (index === -1) {
        visibleCharts.value.push(chartType); // 添加图表
    } else {
        visibleCharts.value.splice(index, 1); // 移除图表
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

        const myChart = echarts.init(chartDom);

        // 根据图表类型加载不同的数据和配置
        let option;
        if (chartType === 'chart1') {
            // 图表1：条形图
            option = {
                title: { text: '图表1 - 条形图' },
                tooltip: {},
                xAxis: { data: ['A', 'B', 'C', 'D', 'E'] },
                yAxis: {},
                series: [
                    {
                        type: 'bar',
                        data: [5, 20, 36, 10, 10],
                    },
                ],
            };
        } else if (chartType === 'chart2') {
            // 图表2：折线图
            option = {
                title: { text: '图表2 - 折线图' },
                tooltip: {},
                xAxis: { data: ['A', 'B', 'C', 'D', 'E'] },
                yAxis: {},
                series: [
                    {
                        type: 'line',
                        data: [15, 25, 16, 20, 30],
                    },
                ],
            };
        } else if (chartType === 'chart3') {
            // 图表3：饼图
            option = {
                title: { text: '图表3 - 饼图' },
                tooltip: {},
                series: [
                    {
                        type: 'pie',
                        radius: '50%',
                        data: [
                            { value: 10, name: 'A' },
                            { value: 20, name: 'B' },
                            { value: 30, name: 'C' },
                            { value: 40, name: 'D' },
                            { value: 50, name: 'E' },
                        ],
                    },
                ],
            };
        }

        myChart.setOption(option);
    });
};

const handleMapReady = ({ BMap, map }) => {
    console.log("地图已加载", map);
    map.setMapStyleV2({     
        styleId: '65d44bc71123817a008a3285df684c69'
    });
    map.enableScrollWheelZoom(true);   
};
</script>

<style>
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

.button-panel {
    position: absolute;
    top: 10px;
    left: 10px;
    background-color: rgba(255, 255, 255, 0.9);
    padding: 10px;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
    z-index: 10;
    display: flex;
    flex-direction: column; /* 按钮竖向排列 */
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
    flex-direction: column; /* 图表竖向排列 */
    gap: 10px; /* 图表之间的间距 */
}

.chart-item {
    width: 100%;
    height: 200px; /* 每个图表的高度 */
    background-color: #f5f5f5;
    border-radius: 5px;
    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}
</style>