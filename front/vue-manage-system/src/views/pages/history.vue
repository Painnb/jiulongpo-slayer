<template>
  <div class="history-container">
    <!-- 顶部输入框区域 -->
    <div class="filter-container">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input v-model="filters.vehicleId" placeholder="请输入车辆ID" clearable />
        </el-col>
        <el-col :span="6">
          <el-date-picker
            v-model="filters.startTime"
            type="datetime"
            placeholder="开始时间"
            style="width: 100%;"
          />
        </el-col>
        <el-col :span="6">
          <el-date-picker
            v-model="filters.endTime"
            type="datetime"
            placeholder="结束时间"
            style="width: 100%;"
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="filters.selectedTables"
            multiple
            placeholder="请选择表名"
            style="width: 100%;"
          >
            <el-option label="发动机异常" value="engine_exp" />
            <el-option label="速度异常" value="speed_exp" />
            <el-option label="其他异常" value="other_exp" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="success" @click="exportData">导出</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 中间列表区域 -->
    <div class="list-container">
      <el-table :data="paginatedData" border >
        <el-table-column prop="vehicleId" label="车辆ID" width="150" />
        <el-table-column prop="time" label="时间" />
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

<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';

// 过滤条件
const filters = ref({
  vehicleId: '',
  startTime: null,
  endTime: null,
  selectedTables: []
});

// 模拟数据
const data = ref([
  { vehicleId: '001', time: '2025-04-01 10:00:00', exceptionType: '类型1' },
  { vehicleId: '002', time: '2025-04-02 11:00:00', exceptionType: '类型2' },
  { vehicleId: '003', time: '2025-04-03 12:00:00', exceptionType: '类型3' },
  { vehicleId: '001', time: '2025-04-04 13:00:00', exceptionType: '类型1' },
  { vehicleId: '002', time: '2025-04-05 14:00:00', exceptionType: '类型2' },
  { vehicleId: '003', time: '2025-04-06 15:00:00', exceptionType: '类型3' },
  
  
  // 更多数据...
]);

// 分页相关
const currentPage = ref(1);
const pageSize = ref(15);

// 过滤后的数据
const filteredData = computed(() => {
  return data.value.filter((item) => {
    const matchesVehicleId = !filters.value.vehicleId || item.vehicleId.includes(filters.value.vehicleId);
    const matchesStartTime = !filters.value.startTime || new Date(item.time) >= new Date(filters.value.startTime);
    const matchesEndTime = !filters.value.endTime || new Date(item.time) <= new Date(filters.value.endTime);
    const matchesSelectedTables = !filters.value.selectedTables.length || filters.value.selectedTables.includes(item.exceptionType);
    return matchesVehicleId && matchesStartTime && matchesEndTime && matchesSelectedTables;
  });
});

// 当前页的数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredData.value.slice(start, end);
});

// 查询按钮点击事件
const search = () => {
  currentPage.value = 1; // 查询时重置到第一页
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
      vehicleId: filters.value.vehicleId,
      startTime: formatDateTime(filters.value.startTime),
      endTime: formatDateTime(filters.value.endTime),
      selectedTables: filters.value.selectedTables,
      selectedColumns: {}
    };
    console.log('导出数据请求参数:', params);
    const token = localStorage.getItem('token');

    // 调用后端接口
    const response = await axios.post('/abc/api/dataprocess/business/tables/combined-export', params, {
      responseType: 'blob', // 确保接收的是文件流
      headers: {
        Authorization: `Bearer ${token}`, // 在请求头中加入 token
      },
    });

    console.log('导出数据请求参数:', response);

      // 3. 从响应头获取文件名
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
  background-size: cover
}



.filter-container {
  margin-bottom: 20px;
}

.list-container {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 20px;
  
}

.pagination-container {
  text-align: center;
}

</style>