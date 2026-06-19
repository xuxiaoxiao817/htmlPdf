<template>
  <div class="rt-editor">
    <Toolbar
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
      style="border-bottom: 1px solid #ccc"
    />
    <Editor
      v-model="valueHtml"
      :defaultConfig="editorConfig"
      :mode="mode"
      style="height: 280px; overflow-y: auto"
      @onCreated="handleCreated"
    />
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const props = defineProps({
  modelValue: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef(null)
const mode = 'default'
const valueHtml = ref(props.modelValue)

watch(valueHtml, (v) => emit('update:modelValue', v))
watch(() => props.modelValue, (v) => { if (v !== valueHtml.value) valueHtml.value = v })

const toolbarConfig = {
  toolbarKeys: [
    'bold', 'italic', 'underline', '|',
    'fontSize', 'color', 'bgColor', '|',
    'bulletedList', 'numberedList', '|',
    'justifyLeft', 'justifyCenter', 'justifyRight', '|',
    'insertLink', '|',
    'undo', 'redo', '|',
    'clearStyle'
  ]
}
const editorConfig = {
  placeholder: '请输入协议内容（支持富文本：粗体、列表、链接等）'
}

function handleCreated(editor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})
</script>

<style scoped>
.rt-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}
</style>
