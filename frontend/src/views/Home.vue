<template>
  <div class="home-page">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <h1 class="hero-title">
            智能搜索平台
            <span class="gradient-text">为银行业务而生</span>
          </h1>
          <p class="hero-description">
            基于先进的语义搜索技术，为银行业务场景提供精准、快速的智能搜索服务。
            支持产品查询、服务咨询、政策解读等多种搜索需求。
          </p>
          <div class="hero-actions">
            <router-link to="/search" class="primary-button">
              <SearchIcon class="w-5 h-5" />
              开始搜索
            </router-link>
            <a href="#features" class="secondary-button">
              了解更多
            </a>
          </div>
        </div>
        <div class="hero-image">
          <div class="search-demo">
            <div class="demo-search-box">
              <SearchIcon class="w-5 h-5 text-gray-400" />
              <span class="demo-text">搜索银行产品和服务...</span>
            </div>
            <div class="demo-suggestions">
              <div class="demo-suggestion">理财产品</div>
              <div class="demo-suggestion">贷款申请</div>
              <div class="demo-suggestion">开户流程</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能特性 -->
    <section id="features" class="features-section">
      <div class="features-container">
        <div class="section-header">
          <h2>核心功能</h2>
          <p>为银行业务量身定制的智能搜索解决方案</p>
        </div>

        <div class="features-grid">
          <div v-for="feature in features" :key="feature.title" class="feature-card">
            <div class="feature-icon">
              <component :is="feature.icon" class="w-8 h-8" />
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 搜索统计 -->
    <section class="stats-section">
      <div class="stats-container">
        <div class="stats-grid">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <div class="stat-number">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 快速开始 -->
    <section class="quick-start-section">
      <div class="quick-start-container">
        <div class="section-header">
          <h2>快速开始</h2>
          <p>三步即可开始使用智能搜索</p>
        </div>

        <div class="steps-grid">
          <div v-for="(step, index) in steps" :key="step.title" class="step-card">
            <div class="step-number">{{ index + 1 }}</div>
            <h3 class="step-title">{{ step.title }}</h3>
            <p class="step-description">{{ step.description }}</p>
          </div>
        </div>

        <div class="quick-start-action">
          <router-link to="/search" class="primary-button large">
            立即体验
            <ArrowRightIcon class="w-5 h-5" />
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import {
  SearchIcon,
  ArrowRightIcon,
  BrainIcon,
  ZapIcon,
  ShieldIcon,
  BarChart3Icon,
  ClockIcon,
  UsersIcon
} from 'lucide-vue-next'

const features = [
  {
    icon: BrainIcon,
    title: '语义理解',
    description: '先进的自然语言处理技术，准确理解用户搜索意图，支持同义词和语义匹配。'
  },
  {
    icon: ZapIcon,
    title: '快速响应',
    description: '毫秒级搜索响应，实时自动补全和搜索建议，提供流畅的搜索体验。'
  },
  {
    icon: ShieldIcon,
    title: '安全可靠',
    description: '银行级安全保障，完善的权限管理和数据加密，确保信息安全。'
  },
  {
    icon: BarChart3Icon,
    title: '智能分析',
    description: '详细的搜索分析报告，了解用户行为，优化搜索策略和内容。'
  },
  {
    icon: ClockIcon,
    title: '实时更新',
    description: '支持实时数据同步，确保搜索结果始终反映最新的业务信息。'
  },
  {
    icon: UsersIcon,
    title: '多端适配',
    description: '响应式设计，完美适配桌面端、移动端和平板设备。'
  }
]

const stats = [
  { value: '10万+', label: '日均搜索次数' },
  { value: '95%', label: '搜索准确率' },
  { value: '<100ms', label: '平均响应时间' },
  { value: '24/7', label: '服务可用性' }
]

const steps = [
  {
    title: '输入查询',
    description: '在搜索框中输入您要查找的内容，支持自然语言查询。'
  },
  {
    title: '智能匹配',
    description: '系统自动分析查询意图，匹配最相关的内容和服务。'
  },
  {
    title: '获取结果',
    description: '查看搜索结果，点击相关条目获取详细信息。'
  }
]
</script>

<style scoped>
.home-page {
  min-height: 100vh;
}

/* 英雄区域 */
.hero-section {
  background: linear-gradient(135deg, hsl(var(--primary) / 0.05) 0%, hsl(var(--secondary) / 0.05) 100%);
  padding: 80px 0 120px;
  overflow: hidden;
}

.hero-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
}

@media (max-width: 768px) {
  .hero-container {
    grid-template-columns: 1fr;
    gap: 40px;
    text-align: center;
  }
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 24px;
  color: hsl(var(--foreground));
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
}

.gradient-text {
  background: linear-gradient(135deg, hsl(var(--primary)), hsl(var(--primary) / 0.7));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 1.25rem;
  line-height: 1.6;
  color: hsl(var(--muted-foreground));
  margin-bottom: 32px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

@media (max-width: 640px) {
  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  text-decoration: none;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.primary-button:hover {
  background: hsl(var(--primary) / 0.9);
  transform: translateY(-1px);
  box-shadow: 0 6px 8px -1px rgba(0, 0, 0, 0.15);
}

.primary-button.large {
  padding: 16px 32px;
  font-size: 1.125rem;
}

.secondary-button {
  padding: 14px 28px;
  color: hsl(var(--foreground));
  text-decoration: none;
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.2s;
}

.secondary-button:hover {
  background: hsl(var(--accent));
  border-color: hsl(var(--primary));
}

/* 搜索演示 */
.hero-image {
  display: flex;
  justify-content: center;
  align-items: center;
}

.search-demo {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.demo-search-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  margin-bottom: 16px;
}

.demo-text {
  color: hsl(var(--muted-foreground));
  font-size: 0.95rem;
}

.demo-suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.demo-suggestion {
  padding: 10px 12px;
  background: hsl(var(--accent));
  border-radius: 6px;
  color: hsl(var(--accent-foreground));
  font-size: 0.9rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.demo-suggestion:hover {
  background: hsl(var(--primary) / 0.1);
}

/* 功能特性 */
.features-section {
  padding: 80px 0;
  background: hsl(var(--background));
}

.features-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 16px;
  color: hsl(var(--foreground));
}

.section-header p {
  font-size: 1.125rem;
  color: hsl(var(--muted-foreground));
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 32px;
}

.feature-card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 12px;
  padding: 32px 24px;
  text-align: center;
  transition: all 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 12px -1px rgba(0, 0, 0, 0.1);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: hsl(var(--primary) / 0.1);
  border-radius: 50%;
  color: hsl(var(--primary));
  margin-bottom: 20px;
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 12px;
  color: hsl(var(--foreground));
}

.feature-description {
  color: hsl(var(--muted-foreground));
  line-height: 1.6;
}

/* 统计数据 */
.stats-section {
  padding: 60px 0;
  background: hsl(var(--primary) / 0.05);
}

.stats-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 32px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 700;
  color: hsl(var(--primary));
  margin-bottom: 8px;
}

.stat-label {
  font-size: 1rem;
  color: hsl(var(--muted-foreground));
}

/* 快速开始 */
.quick-start-section {
  padding: 80px 0;
  background: hsl(var(--card));
}

.quick-start-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

.steps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 32px;
  margin-bottom: 48px;
}

.step-card {
  text-align: center;
  padding: 24px;
}

.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border-radius: 50%;
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 16px;
}

.step-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 12px;
  color: hsl(var(--foreground));
}

.step-description {
  color: hsl(var(--muted-foreground));
  line-height: 1.6;
}

.quick-start-action {
  text-align: center;
}
</style>