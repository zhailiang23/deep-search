<template>
  <div
    :class="cn(avatarVariants({ size, shape }), $attrs.class as string)"
    v-bind="$attrs"
  >
    <!-- 图片 -->
    <img
      v-if="src && !imageError"
      :src="src"
      :alt="alt"
      :class="cn('object-cover w-full h-full', shape === 'circle' ? 'rounded-full' : 'rounded-md')"
      @error="handleImageError"
      @load="handleImageLoad"
    />

    <!-- 回退内容：首字母或图标 -->
    <div
      v-else
      :class="cn(
        'flex items-center justify-center w-full h-full text-muted-foreground bg-muted',
        shape === 'circle' ? 'rounded-full' : 'rounded-md'
      )"
    >
      <!-- 自定义回退内容 -->
      <slot name="fallback">
        <!-- 显示首字母 -->
        <span
          v-if="initials"
          :class="cn('font-medium', sizeTextMap[size as keyof typeof sizeTextMap])"
        >
          {{ initials }}
        </span>

        <!-- 默认用户图标 -->
        <UserIcon
          v-else
          :class="cn('text-muted-foreground', sizeIconMap[size as keyof typeof sizeIconMap])"
        />
      </slot>
    </div>

    <!-- 状态指示器 -->
    <div
      v-if="status"
      :class="cn(
        'absolute border-2 border-background rounded-full',
        statusVariants({ status, size })
      )"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import { UserIcon } from 'lucide-vue-next'

// 头像变体配置
const avatarVariants = cva(
  'relative inline-flex items-center justify-center overflow-hidden bg-muted',
  {
    variants: {
      size: {
        xs: 'h-6 w-6',
        sm: 'h-8 w-8',
        default: 'h-10 w-10',
        lg: 'h-12 w-12',
        xl: 'h-16 w-16',
        '2xl': 'h-20 w-20',
        '3xl': 'h-24 w-24',
      },
      shape: {
        circle: 'rounded-full',
        square: 'rounded-md',
      },
    },
    defaultVariants: {
      size: 'default',
      shape: 'circle',
    },
  }
)

// 状态指示器变体配置
const statusVariants = cva(
  '',
  {
    variants: {
      status: {
        online: 'bg-green-500',
        offline: 'bg-gray-500',
        busy: 'bg-red-500',
        away: 'bg-yellow-500',
      },
      size: {
        xs: 'h-1.5 w-1.5 -bottom-0 -right-0',
        sm: 'h-2 w-2 -bottom-0 -right-0',
        default: 'h-2.5 w-2.5 -bottom-0.5 -right-0.5',
        lg: 'h-3 w-3 -bottom-0.5 -right-0.5',
        xl: 'h-3.5 w-3.5 -bottom-1 -right-1',
        '2xl': 'h-4 w-4 -bottom-1 -right-1',
        '3xl': 'h-5 w-5 -bottom-1.5 -right-1.5',
      },
    },
  }
)

// 文字大小映射
const sizeTextMap = {
  xs: 'text-xs',
  sm: 'text-xs',
  default: 'text-sm',
  lg: 'text-base',
  xl: 'text-lg',
  '2xl': 'text-xl',
  '3xl': 'text-2xl',
}

// 图标大小映射
const sizeIconMap = {
  xs: 'h-3 w-3',
  sm: 'h-4 w-4',
  default: 'h-5 w-5',
  lg: 'h-6 w-6',
  xl: 'h-8 w-8',
  '2xl': 'h-10 w-10',
  '3xl': 'h-12 w-12',
}

export interface AvatarProps extends /* @vue-ignore */ VariantProps<typeof avatarVariants> {
  src?: string
  alt?: string
  name?: string
  status?: 'online' | 'offline' | 'busy' | 'away'
}

// 组件属性
const props = withDefaults(defineProps<AvatarProps>(), {
  alt: 'Avatar',
  size: 'default',
  shape: 'circle'
})

// 禁用继承的 class 属性
defineOptions({
  inheritAttrs: false
})

// 内部状态
const imageError = ref(false)
const imageLoaded = ref(false)

// 计算首字母
const initials = computed(() => {
  if (!props.name) return ''

  const words = props.name.trim().split(/\s+/)
  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase()
  }

  return words
    .slice(0, 2)
    .map(word => word.charAt(0).toUpperCase())
    .join('')
})

// 事件处理
const handleImageError = () => {
  imageError.value = true
}

const handleImageLoad = () => {
  imageError.value = false
  imageLoaded.value = true
}
</script>