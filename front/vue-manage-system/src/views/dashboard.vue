<template>
    <div>
        <el-row :gutter="20" class="mgb20">
            <el-col :span="6">
                <el-card shadow="hover" body-class="card-body">
                    <el-icon class="card-icon bg1">
                        <User />
                    </el-icon>
                    <div class="card-content">
                        <countup class="card-num color1" :end="6666" />
                        <div>用户访问量</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" body-class="card-body">
                    <el-icon class="card-icon bg2">
                        <ChatDotRound />
                    </el-icon>
                    <div class="card-content">
                        <countup class="card-num color2" :end="168" />
                        <div>系统消息</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" body-class="card-body">
                    <el-icon class="card-icon bg3">
                        <DataAnalysis />
                    </el-icon>
                    <div class="card-content">
                        <countup class="card-num color3" :end="999" />
                        <div>在线数量</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover" body-class="card-body">
                    <el-icon class="card-icon bg4">
                        <MostlyCloudy />
                    </el-icon>
                    <div class="card-content">
                        <countup class="card-num color4" :end="888" />
                        <div>离线数量</div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <el-row :gutter="20" class="mgb20">
            <el-col :span="18">
                <el-card shadow="hover">
                    <div class="card-header">
                        <p class="card-header-title">动态数据</p>
                        <p class="card-header-desc">实时监测的车辆数据</p>
                    </div>
                    <v-chart class="chart" :option="dashOpt1" />
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card shadow="hover">
                    <div class="card-header">
                        <div class="card-header-left">
                            <p class="card-header-title">车辆状态</p>
                            <p class="card-header-desc">实时监测的车辆状态</p>
                        </div>
                        <!-- 添加按钮 -->
                        <el-button size="mini" type="primary" @click="showList = true">选择车辆</el-button>
                    </div>
                    <!-- 图表区域 -->
                    <div v-if="!showList">
                        <v-chart class="chart" :option="dashOpt2" />
                    </div>
                    <!-- 列表区域 -->
                    <div v-else class="list-container">
                        <el-checkbox-group v-model="selectedOptions" class="scrollable-list">
                            <el-checkbox
                                v-for="(option, index) in options"
                                :key="index"
                                :label="option"
                                class="checkbox-item"
                            >
                                {{ option }}
                            </el-checkbox>
                        </el-checkbox-group>
                        <div class="list-buttons">
                            <el-button size="mini" type="primary" @click="showList = false">返回</el-button>
                            <el-button size="mini" type="success" @click="printSelections">打印</el-button>
                        </div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
        <el-row :gutter="20">
            <el-col :span="7">
                <el-card shadow="hover" :body-style="{ height: '400px' }">
                    <div class="card-header">
                        <p class="card-header-title">时间线</p>
                        <p class="card-header-desc"></p>
                    </div>
                    <el-timeline>
                        <el-timeline-item v-for="(activity, index) in activities" :key="index" :color="activity.color">
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
            <el-col :span="10">
                <el-card shadow="hover" :body-style="{ height: '400px' }">
                    <div class="card-header">
                        <p class="card-header-title">异常分布</p>
                        <p class="card-header-desc">最近一个月全国各地的异常分布</p>
                    </div>
                    <v-chart class="map-chart" :option="mapOptions" />
                </el-card>
            </el-col>
            <el-col :span="7">
                <el-card shadow="hover" :body-style="{ height: '400px' }">
                    <div class="card-header">
                        <p class="card-header-title">异常统计</p>
                        <p class="card-header-desc"></p>
                    </div>
                    <div>
                        <div class="rank-item" v-for="(rank, index) in ranks">
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
import countup from '@/components/countup.vue';
import { use, registerMap } from 'echarts/core';
import { BarChart, LineChart, PieChart, MapChart } from 'echarts/charts';
import {
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent,
    VisualMapComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';
import { dashOpt1, dashOpt2, mapOptions } from './chart/options';
import chinaMap from '@/utils/china';
import { ref } from 'vue';

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
registerMap('china', chinaMap);
const activities = [
    {
        content: '车辆行驶',
        description: 'xxx车辆正在行驶，去查看车辆状态',
        timestamp: '30分钟前',
        color: '#00bcd4',
    },
    {
        content: '异常处理',
        description: 'xxx异常已被处理',
        timestamp: '55分钟前',
        color: '#1ABC9C',
    },
    {
        content: '异常捕捉',
        description: '捕捉到xxx异常，请处理',
        timestamp: '1小时前',
        color: '#3f51b5',
    }
];

const ranks = [
    {
        title: '方向盘异常',
        value: 10000,
        percent: 80,
        color: '#f25e43',
    },
    {
        title: '车速异常',
        value: 8000,
        percent: 70,
        color: '#00bcd4',
    },
    {
        title: '加速度异常',
        value: 6000,
        percent: 60,
        color: '#64d572',
    },
    {
        title: '油门异常',
        value: 5000,
        percent: 55,
        color: '#e9a745',
    },
    {
        title: '发动机异常',
        value: 4000,
        percent: 50,
        color: '#009688',
    },
];

// 控制显示列表还是图表
const showList = ref(false);

// 列表选项
const options = ref(['选项1', '选项2', '选项3', '选项4', '选项5', '选项6', '选项7', '选项8', '选项9', '选项10']);

// 当前选中的选项（多选）
const selectedOptions = ref([]);

// 打印选中的选项
const printSelections = () => {
    if (selectedOptions.value.length > 0) {
        console.log('选中的选项是：', selectedOptions.value);
        alert(`选中的选项是：${selectedOptions.value.join(', ')}`);
    } else {
        alert('请先选择一个或多个选项！');
    }
};
</script>

<style>
.card-body {
    display: flex;
    align-items: center;
    height: 100px;
    padding: 0;
}
</style>
<style scoped>
.card-content {
    flex: 1;
    text-align: center;
    font-size: 14px;
    color: #999;
    padding: 0 20px;
}

.card-num {
    font-size: 30px;
}

.card-icon {
    font-size: 50px;
    width: 100px;
    height: 100px;
    text-align: center;
    line-height: 100px;
    color: #fff;
}

.bg1 {
    background: #2d8cf0;
}

.bg2 {
    background: #64d572;
}

.bg3 {
    background: #f25e43;
}

.bg4 {
    background: #e9a745;
}

.color1 {
    color: #2d8cf0;
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
    justify-content: space-between; /* 将标题和按钮分开对齐 */
    padding-left: 10px;
    margin-bottom: 20px;
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
    font-size: 16px;
    color: #000;
}

.timeline-time,
.timeline-desc {
    font-size: 12px;
    color: #787878;
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
    height: 350px; /* 与图表高度一致 */
    padding: 10px;
}

.scrollable-list {
    flex: 1; /* 占据剩余空间 */
    width: 100%;
    overflow-y: auto; /* 启用垂直滚动 */
    padding-right: 10px; /* 防止滚动条遮挡内容 */
    margin-bottom: 10px;
    border: 1px solid #e0e0e0; /* 添加边框以区分列表 */
    border-radius: 4px;
}

.checkbox-item {
    display: block; /* 强制每个选项占据一行 */
    margin-top: 5px; /* 添加选项之间的间距 */
    margin-left: 10px;
}

.scrollable-list ::-webkit-scrollbar {
    width: 6px; /* 滚动条宽度 */
}

.scrollable-list ::-webkit-scrollbar-thumb {
    background-color: #c1c1c1; /* 滚动条颜色 */
    border-radius: 3px;
}

.scrollable-list ::-webkit-scrollbar-track {
    background-color: #f5f5f5; /* 滚动条轨道颜色 */
}

.list-buttons {
    display: flex;
    justify-content: space-between;
    width: 100%;
    margin-top: 10px;
}
</style>
