import { createPinia } from 'pinia'
import type { App } from 'vue'

const pinia = createPinia()

// 注册Pinia插件
export function setupStore(app: App) {
  app.use(pinia)
}

export { pinia }
export default pinia