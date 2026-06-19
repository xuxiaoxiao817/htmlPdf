<template>
  <el-select
    v-model="modelValue"
    placeholder="选择语种"
    filterable
    style="width: 100%"
  >
    <el-option
      v-for="opt in options"
      :key="opt.code"
      :label="`${opt.nativeName} (${opt.code})`"
      :value="opt.code"
    />
  </el-select>
</template>

<script setup>
defineProps({
  options: { type: Array, required: true }
})
const modelValue = defineModel({ type: String, required: true })
</script>
