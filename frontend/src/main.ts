import { createApp } from 'vue'
import { createHead } from '@vueuse/head'
import App from './App.vue'
import router from './router'
import { setupStore } from './stores'

// 全局样式
import './styles/globals.css'
import './styles/mobile.css'

// 创建应用实例
const app = createApp(App)

// 创建head管理器
const head = createHead()

// 安装插件
app.use(head)
app.use(router)
setupStore(app)

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err)
  console.error('Component:', vm)
  console.error('Info:', info)

  // 发送错误到监控服务
  if (import.meta.env.PROD) {
    // TODO: 集成错误监控服务 (如 Sentry)
  }
}

// 全局警告处理
app.config.warnHandler = (msg, vm, trace) => {
  if (import.meta.env.DEV) {
    console.warn('Vue Warning:', msg)
    console.warn('Component:', vm)
    console.warn('Trace:', trace)
  }
}

// 性能监控
app.config.performance = import.meta.env.DEV

// 挂载应用
app.mount('#app')