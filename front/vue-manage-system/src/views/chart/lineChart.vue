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
import { lineOptions } from './lineOptions'; // 引入折线图配置
</script>

<style scoped>
.container {
    padding: 20px;
}

.schart {
    width: 100%;
    height: 400px;
}

.content-title {
    font-weight: 400;
    font-size: 22px;
    color: #1f2f3d;
}
</style>