<template>
  <el-table
    :data="rows"
    v-loading="loading"
    stripe
    border
    style="width: 100%"
    empty-text="暂无协议"
  >
    <el-table-column prop="id" label="ID" width="70" />
    <el-table-column label="语种" width="120">
      <template #default="{ row }">
        <el-tag>{{ languageLabel(row.language) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
    <el-table-column label="内容预览" min-width="200" show-overflow-tooltip>
      <template #default="{ row }">
        <span class="preview" v-html="stripTags(row.content)" />
      </template>
    </el-table-column>
    <el-table-column prop="createdAt" label="创建时间" width="180" />
    <el-table-column label="操作" width="200" fixed="right">
      <template #default="{ row }">
        <el-button
          type="primary"
          size="small"
          :icon="Download"
          :loading="downloadingId === row.id"
          @click="onDownload(row)"
        >
          下载 PDF
        </el-button>
        <el-button
          type="danger"
          size="small"
          :icon="Delete"
          @click="onDelete(row)"
        />
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { Download, Delete } from '@element-plus/icons-vue'
import { deleteAgreement, downloadAgreementPdf } from '../api/agreement.js'

const props = defineProps({
  rows: { type: Array, required: true },
  languages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['refresh'])

const downloadingId = ref(0)

function languageLabel(code) {
  const m = props.languages.find(l => l.code === code)
  return m ? m.nativeName : code
}

function stripTags(html) {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return (div.textContent || div.innerText || '').slice(0, 80)
}

async function onDownload(row) {
  downloadingId.value = row.id
  try {
    await downloadAgreementPdf(row.id)
    ElMessage.success(`PDF 已下载：agreement_${row.id}_${row.language}.pdf`)
  } catch (e) {
    ElMessage.error('下载失败：' + (e?.message || e))
  } finally {
    downloadingId.value = 0
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除协议 #${row.id}？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await deleteAgreement(row.id)
    ElMessage.success('已删除')
    emit('refresh')
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.message || e))
  }
}

import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
</script>

<style scoped>
.preview {
  color: #606266;
  font-size: 13px;
}
</style>
