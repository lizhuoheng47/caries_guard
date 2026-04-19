<template>
  <div class="shell">
    <aside class="nav glass-card">
      <div class="brand">
        <small>CariesGuard</small>
        <h1>RAG Console</h1>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/kb/overview">知识库总览</el-menu-item>
        <el-menu-item index="/kb/documents">文档管理</el-menu-item>
        <el-menu-item index="/kb/upload">文档导入</el-menu-item>
        <el-menu-item index="/kb/review">审核发布</el-menu-item>
        <el-menu-item index="/kb/publish-history">发布回滚记录</el-menu-item>
        <el-menu-item index="/kb/ingest-jobs">Ingest Job</el-menu-item>
        <el-menu-item index="/kb/rebuild-jobs">Rebuild Job</el-menu-item>
        <el-menu-item index="/kb/graph-stats">图谱统计</el-menu-item>
        <el-menu-item index="/rag/doctor-qa">Doctor QA</el-menu-item>
        <el-menu-item index="/rag/patient-explanation">Patient Explain</el-menu-item>
        <el-menu-item index="/rag/retrieval">检索明细</el-menu-item>
        <el-menu-item index="/rag/graph">图谱证据</el-menu-item>
        <el-menu-item index="/rag/logs">RAG 日志</el-menu-item>
        <el-menu-item index="/rag/eval">评估</el-menu-item>
      </el-menu>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 100vh;
  gap: 20px;
  padding: 20px;
}

.nav {
  padding: 20px;
  position: sticky;
  top: 20px;
  height: calc(100vh - 40px);
}

.brand small {
  color: var(--cg-accent-2);
  text-transform: uppercase;
  letter-spacing: 0.2em;
}

.brand h1 {
  margin: 6px 0 24px;
  font-size: 32px;
}

.menu {
  border-right: none;
  background: transparent;
}

.content {
  min-width: 0;
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .nav {
    position: static;
    height: auto;
  }
}
</style>
