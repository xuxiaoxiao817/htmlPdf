<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="title">多语言协议签署 POC</div>
      <div class="subtitle">iText 7 + Noto 字体，HTML → PDF</div>
    </el-header>
    <el-main>
      <Home />
    </el-main>
  </el-container>
</template>

<script setup>
import Home from './views/Home.vue'
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  font-family: 'Helvetica Neue', Arial, 'Microsoft YaHei', sans-serif;
}
.app-shell {
  min-height: 100vh;
  background: #f5f7fa;
}
.app-header {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 32px;
}
.app-header .title { font-size: 20px; font-weight: 700; }
.app-header .subtitle { margin-left: 16px; opacity: 0.85; font-size: 13px; }
</style>
