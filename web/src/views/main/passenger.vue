<template>
  <p>
    <a-space>
      <a-button type="primary" @click="handleQuery()">刷新</a-button>
      <a-button type="primary" @click="onAdd">新增</a-button>
    </a-space>
  </p>
  <a-table :dataSource="passengers"
           :columns="columns"
           :pagination="pagination"
           @change="handleTableChange"
           :loading="loading">
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'operation'">
        <a-space>
          <a-popconfirm
              title="删除后不可恢复，确认删除?"
              @confirm="onDelete(record)"
              ok-text="确认" cancel-text="取消">
            <a style="color: red">删除</a>
          </a-popconfirm>
          <a @click="onEdit(record)">编辑</a>
        </a-space>
      </template>
      <template v-else-if="column.dataIndex === 'type'">
        <span v-for="item in PASSENGER_TYPE_ARRAY" :key="item.value">
          <span v-if="item.value === record.type">
            {{item.label}}
          </span>
        </span>
      </template>
    </template>
  </a-table>
  <a-modal v-model:visible="visible" title="乘车人" @ok="handleOk"
           ok-text="确认" cancel-text="取消">
    <a-form :model="passenger" :label-col="{span: 4}" :wrapper-col="{ span: 20 }">
      <a-form-item label="姓名">
        <a-input v-model:value="passenger.name" />
      </a-form-item>
      <a-form-item label="身份证">
        <a-input v-model:value="passenger.idCard" />
      </a-form-item>
      <a-form-item label="旅客类型">
        <a-select v-model:value="passenger.type">
          <a-select-option
            v-for="item in PASSENGER_TYPE_ARRAY"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { notification } from 'ant-design-vue'
import axios from 'axios'
import { PASSENGER_TYPE_ARRAY } from '@/config/passengerType'

// 弹窗显示
const visible = ref(false)

// 当前编辑/新增的乘客
const passenger = ref({
  id: undefined,
  memberId: undefined,
  name: undefined,
  idCard: undefined,
  type: undefined,
  createTime: undefined,
  updateTime: undefined,
})

// 乘客列表
const passengers = ref([])

// 分页
const pagination = ref({
  total: 0,
  current: 1,
  pageSize: 10,
})

// 表格 loading
const loading = ref(false)

// 表格列定义
const columns = [
  { title: '会员id', dataIndex: 'memberId', key: 'memberId' },
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '身份证', dataIndex: 'idCard', key: 'idCard' },
  { title: '旅客类型', dataIndex: 'type', key: 'type' },
  { title: '操作', dataIndex: 'operation', key: 'operation' },
]

// 新增
const onAdd = () => {
  passenger.value = {}
  visible.value = true
}

// 编辑
const onEdit = (record) => {
  passenger.value = { ...record } // 简单深拷贝
  visible.value = true
}

// 删除
const onDelete = (record) => {
  axios.delete('/member/passenger/delete/' + record.id).then((res) => {
    const data = res.data
    if (data.success) {
      notification.success({ description: '删除成功！' })
      handleQuery({
        page: pagination.value.current,
        size: pagination.value.pageSize,
      })
    } else {
      notification.error({ description: data.message })
    }
  })
}

// 弹窗确认
const handleOk = () => {
  axios.post('/member/passenger/save', passenger.value).then((res) => {
    const data = res.data
    if (data.success) {
      notification.success({ description: '保存成功！' })
      visible.value = false
      handleQuery({
        page: pagination.value.current,
        size: pagination.value.pageSize,
      })
    } else {
      notification.error({ description: data.message })
    }
  })
}

// 查询列表
const handleQuery = (param) => {
  if (!param) {
    param = { page: 1, size: pagination.value.pageSize }
  }
  loading.value = true
  axios
    .get('/member/passenger/query-list', {
      params: { page: param.page, size: param.size },
    })
    .then((res) => {
      loading.value = false
      const data = res.data
      if (data.success) {
        passengers.value = data.content.list.map(item => ({
          ...item,
          type: Number(item.type)
        }))
        pagination.value.current = param.page
        pagination.value.total = data.content.total
      } else {
        notification.error({ description: data.message })
      }
    })
}

// 表格分页变化
const handleTableChange = (page) => {
  handleQuery({ page: page.current, size: page.pageSize })
}

// 页面初始化查询
onMounted(() => {
  handleQuery({ page: 1, size: pagination.value.pageSize })
})
</script>

