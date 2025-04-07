<template>
    <div class="container">
        <el-card class="mgb20" shadow="hover">
            <template #header>
                <div class="content-title">折线图</div>
            </template>
            <v-chart class="schart" :option="lineOptions" />
        </el-card>
    </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers'; // 导入 Canvas 渲染器
import { LineChart } from 'echarts/charts'; // 导入折线图类型
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent  } from 'echarts/components'; // 导入组件
import { lineOptions } from './lineOptions'; // 引入折线图配置


// 注册 ECharts 模块
use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent,TitleComponent ]);
</script>

<style scoped>
.container {
    padding: 20px;
}

.schart {
    width: 1500px;
    height: 400px;
}

.content-title {
    font-weight: 400;
    font-size: 22px;
    color: #1f2f3d;
}
</style>
