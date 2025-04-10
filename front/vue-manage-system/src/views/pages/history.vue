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
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%;"
          />
        </el-col>
        <el-col :span="6">
            <el-select v-model="filters.exceptionType" placeholder="请选择异常类型" clearable style="width: 100%;">
            <el-option label="全部类型" value="all" />
            <el-option label="类型1" value="类型1" />
            <el-option label="类型2" value="类型2" />
            <el-option label="类型3" value="类型3" />
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
      <el-table :data="paginatedData" border style="width: 100%">
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

// 过滤条件
const filters = ref({
  vehicleId: '',
  dateRange: [],
  exceptionType: ''
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
    const matchesDateRange =
      !filters.value.dateRange.length ||
      (new Date(item.time) >= new Date(filters.value.dateRange[0]) &&
        new Date(item.time) <= new Date(filters.value.dateRange[1]));
    const matchesExceptionType =
    !filters.value.exceptionType || filters.value.exceptionType === "all" || item.exceptionType === filters.value.exceptionType;
    return matchesVehicleId && matchesDateRange && matchesExceptionType;
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
    dateRange: [],
    exceptionType: ''
  };
  currentPage.value = 1;
};

const exportData = () => {
  // 导出数据逻辑
  console.log('导出数据:', filters.value);
  // 这里可以调用后端接口进行数据导出
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