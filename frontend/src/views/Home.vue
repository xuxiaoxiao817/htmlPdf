<template>
  <el-row :gutter="24" class="home-row">
    <el-col :xs="24" :md="11">
      <el-card shadow="never" class="box-card">
        <template #header>
          <span class="card-title">新建协议</span>
        </template>

        <el-form :model="form" label-position="top" @submit.prevent>
          <el-form-item label="语种" required>
            <LanguageSelect v-model="form.language" :options="languages" />
          </el-form-item>

          <el-form-item label="标题（可选）">
            <el-input v-model="form.title" placeholder="例如：服务协议" clearable />
          </el-form-item>

          <el-form-item label="协议内容（富文本）" required>
            <RichTextEditor v-model="form.content" />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="submitting"
              :icon="Plus"
              @click="onSubmit"
            >保存协议</el-button>
            <el-button :icon="Refresh" @click="onReset">清空</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :xs="24" :md="13">
      <el-card shadow="never" class="box-card">
        <template #header>
          <div class="list-header">
            <span class="card-title">协议列表（{{ rows.length }}）</span>
            <el-button :icon="Refresh" size="small" @click="loadList">刷新</el-button>
          </div>
        </template>
        <AgreementList
          :rows="rows"
          :languages="languages"
          :loading="listLoading"
          @refresh="loadList"
        />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import LanguageSelect from '../components/LanguageSelect.vue'
import RichTextEditor from '../components/RichTextEditor.vue'
import AgreementList from '../components/AgreementList.vue'

import { createAgreement, listAgreements, listLanguages } from '../api/agreement.js'

const languages = ref([])
const rows = ref([])
const listLoading = ref(false)
const submitting = ref(false)

const form = reactive({
  language: 'zh-CN',
  title: '',
  content: ''
})

async function loadLanguages() {
  try {
    languages.value = await listLanguages()
  } catch (e) {
    ElMessage.error('加载语种失败：' + (e?.message || e))
  }
}

async function loadList() {
  listLoading.value = true
  try {
    rows.value = await listAgreements()
  } catch (e) {
    ElMessage.error('加载列表失败：' + (e?.message || e))
  } finally {
    listLoading.value = false
  }
}

async function onSubmit() {
  if (!form.content || !form.content.trim()) {
    ElMessage.warning('请填写协议内容')
    return
  }
  submitting.value = true
  try {
    const created = await createAgreement({
      language: form.language,
      title: form.title || null,
      content: form.content
    })
    ElMessage.success(`已保存，ID = ${created.id}`)
    form.content = ''
    form.title = ''
    await loadList()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || e))
  } finally {
    submitting.value = false
  }
}

function onReset() {
  form.content = ''
  form.title = ''
}

onMounted(async () => {
  await loadLanguages()
  await loadList()
})
</script>

<style scoped>
.home-row {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 0;
}
.box-card { border-radius: 8px; }
.card-title { font-weight: 600; color: #1f2937; }
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
