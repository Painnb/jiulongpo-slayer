<template>
  <div class="history-container">
    <!-- 顶部输入框区域 -->
    <div class="filter-container">
      <el-row :gutter="20">
        <el-col :span="5">
          <el-input v-model="filters.vehicleId" placeholder="请输入车辆ID" clearable />
        </el-col>
        <el-col :span="5">
          <el-date-picker
            v-model="filters.startTime"
            type="datetime"
            placeholder="开始时间"
            style="width: 100%;"
          />
        </el-col>
        <el-col :span="5">
          <el-date-picker
            v-model="filters.endTime"
            type="datetime"
            placeholder="结束时间"
            style="width: 100%;"
          />
        </el-col>
        <el-col :span="5">
          <el-select
            v-model="filters.selectedTables"
            multiple
            placeholder="请选择异常"
            style="width: 100%;"
          >
            <el-option label="发动机异常" value="engine_exp" />
            <el-option label="速度异常" value="speed_exp" />
            <el-option label="加速度异常" value="acceleration_exp" />
            <el-option label="刹车异常" value="brake_exp" />
            <el-option label="经纬度异常" value="geo_location_exp" />
            <el-option label="机器学习异常" value="ml_exp" />
            <el-option label="转向异常" value="steering_exp" />
            <el-option label="时间戳异常" value="timestamp_exp" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="success" @click="exportData">导出</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 中间列表区域 -->
    <div class="list-container">
      <el-table :data="paginatedData" border >
        <el-table-column prop="vehicleId" label="车辆ID" width="150" />
        <el-table-column prop="timestamp" label="时间" />
        <el-table-column prop="exceptionType" label="异常类型" />
      </el-table>
    </div>

    <!-- 底部分页区域 -->
    <div class="pagination-container">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="filteredData.length"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { requestManager } from "@/utils/requestManager"; // 引入请求管理器

const makeRequest = (url: string, options = {}) => {
  const controller = new AbortController();
  // 将该控制器添加到全局管理器中
  requestManager.add(controller);

  return axios({
    url,
    ...options,
    signal: controller.signal, // 将 AbortSignal 传递给 Axios
  });
};

// 过滤条件
const filters = ref({
  vehicleId: '',
  startTime: null,
  endTime: null,
  selectedTables: []
});

// 模拟数据
const data = ref([

  // 更多数据...
]);
onMounted(() => {
  initCharts(); // 组件挂载时自动查询数据
});
// 分页相关
const currentPage = ref(1);
const pageSize = ref(15);

// 过滤后的数据
const exceptionTypeMap = {
  engine_exp: '发动机异常',
  speed_exp: '速度异常',
  acceleration_exp: '加速度异常',
  brake_exp: '刹车异常',
  geo_location_exp: '经纬度异常',
  ml_exp: '机器学习异常',
  steering_exp: '转向异常',
  timestamp_exp: '时间戳异常'
};

const filteredData = computed(() => {
  return data.value.filter((item) => {
    const matchesVehicleId = !filters.value.vehicleId || item.vehicleId.includes(filters.value.vehicleId);
    const matchesStartTime = !filters.value.startTime || new Date(item.timestamp) >= new Date(filters.value.startTime);
    const matchesEndTime = !filters.value.endTime || new Date(item.timestamp) <= new Date(filters.value.endTime);
    const matchesSelectedTables = !filters.value.selectedTables.length || filters.value.selectedTables.some(table => item.exceptionType.includes(exceptionTypeMap[table]));
    return matchesVehicleId && matchesStartTime && matchesEndTime && matchesSelectedTables;
  });
});

// 当前页的数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredData.value.slice(start, end);
});

const initCharts = async () => {
  try {
    currentPage.value = 1; // 查询时重置到第一页

    // 构造请求参数
    const params = {
      startTime: "2024-04-01 00:00:00",
      endTime: "2035-04-01 00:00:00",
      selectedTables: ['engine_exp', 'speed_exp','acceleration_exp', 'brake_exp','geo_location_exp','ml_exp','steering_exp','timestamp_exp'],
      selectedColumns: {}
    };

    console.log('请求参数:', params);
    const token = localStorage.getItem('token');
    const apiUrl = '/abc/api/query/business/tables/all-vehicles-exceptions-query';

    // 调用后端接口
    const response = await makeRequest(apiUrl,  {
      METHOD: 'POST',
      headers: {
        Authorization: `Bearer ${token}`, // 添加 token
      },
      params: params,
    });

    // 格式化返回的数据
    const rawData = response.data || [];
    const formattedData = rawData.map((item) => {
      const exceptionTypes = [];
      if (item.speed_exp === 1) exceptionTypes.push('速度异常');
      if (item.engine_exp === 1) exceptionTypes.push('发动机异常');
      if (item.acceleration_exp === 1) exceptionTypes.push('加速度异常');
      if (item.brake_exp === 1) exceptionTypes.push('刹车异常');
      if (item.geo_location_exp === 1) exceptionTypes.push('经纬度异常');
      if (item.ml_exp === 1) exceptionTypes.push('机器学习异常');
      if (item.steering_exp === 1) exceptionTypes.push('转向异常');
      if (item.timestamp_exp === 1) exceptionTypes.push('时间戳异常');
      return {
        vehicleId: item.vehicleId,
        timestamp: item.timestamp,
        exceptionType: exceptionTypes.join('，'), // 多个异常用逗号分隔
      };
    });

    // 将格式化后的数据赋值给 data
    data.value = formattedData;
    console.log('格式化后的查询结果:', data.value);
  } catch (error) {
    console.error('获取数据失败:', error);
  }
};


// 重置按钮点击事件
const resetFilters = () => {
  filters.value = {
    vehicleId: '',
    startTime: null,
    endTime: null,
    selectedTables: [],
    
  };
  currentPage.value = 1;
};

const exportData = async () => {
  try {
    // 检查用户权限
    const userRole = localStorage.getItem("auth"); // 假设用户权限存储在 localStorage 中
    if (userRole === "USER") {
      ElMessage.error("无权限导出数据"); // 使用 ElMessage 弹出提示
      return; // 停止后续操作
    }
    const formatDateTime = (date) => {
      if (!date) return null;
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    };

    // 构造请求参数
    const params = {
      startTime: formatDateTime(filters.value.startTime),
      endTime: formatDateTime(filters.value.endTime),
      selectedTables: filters.value.selectedTables,
      selectedColumns: {}
    };

    if (filters.value.vehicleId) {
      params.vehicleId = filters.value.vehicleId;
    }

    console.log('导出数据请求参数:', params);
    const token = localStorage.getItem('token');

    // 确定调用哪个接口
    const apiUrl = filters.value.vehicleId ? '/abc/api/dataprocess/business/tables/combined-export' : '/abc/api/dataprocess/business/tables/all-vehicles-exceptions-export';

    // 调用后端接口
    const response = await makeRequest(apiUrl,  {
      method: 'POST',
      responseType: 'blob', // 确保接收的是文件流
      headers: {
        Authorization: `Bearer ${token}`, // 在请求头中加入 token
      },
      params: params
    });

    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = 'export.xlsx';
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename="(.+)"/);
      if (fileNameMatch.length === 2) {
        fileName = fileNameMatch[1];
      }
    }

    // 创建下载链接
    const blob = new Blob([response.data], { type: 'application/octet-stream' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName; // 设置下载文件名
    link.click();
    window.URL.revokeObjectURL(url); // 释放 URL 对象

    console.log('数据导出成功');
  } catch (error) {
    console.error('导出数据失败:', error);
  }
};

// 分页切换事件
const handlePageChange = (page) => {
  currentPage.value = page;
};
</script>

<style scoped>
.history-container {
  display: flex;
  flex-direction: column;
  height: 80vh;
  padding: 20px;
  background-color: #f5f5f5;
  background-image: url('@/assets/img/his_bg.jpg');
  background-size: cover;
  font-size: 16px; /* 调整整体字体大小 */
}

.filter-container {
  margin-bottom: 20px;
  font-size: 16px; /* 调整过滤区域字体大小 */
}

.list-container {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
  font-size: 16px; /* 调整列表区域字体大小 */
}

.pagination-container {
  text-align: center;
  font-size: 16px; /* 调整分页区域字体大小 */
}
</style>