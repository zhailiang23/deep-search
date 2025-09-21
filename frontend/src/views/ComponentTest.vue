<template>
  <div class="min-h-screen bg-background text-foreground p-8">
    <div class="max-w-6xl mx-auto space-y-8">
      <!-- 页面标题和主题切换 -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-3xl font-bold">UI 组件测试</h1>
          <p class="text-muted-foreground mt-2">测试所有 UI 组件的主题切换功能</p>
        </div>

        <div class="flex items-center space-x-4">
          <span class="text-sm text-muted-foreground">
            当前主题: {{ isDark ? '暗色' : '亮色' }}
          </span>
          <Button
            variant="outline"
            size="sm"
            @click="toggleTheme"
            class="flex items-center space-x-2"
          >
            <component :is="isDark ? SunIcon : MoonIcon" class="h-4 w-4" />
            <span>切换主题</span>
          </Button>
        </div>
      </div>

      <!-- 按钮组件测试 -->
      <Card>
        <CardHeader>
          <CardTitle>Button 组件</CardTitle>
          <CardDescription>不同变体和尺寸的按钮</CardDescription>
        </CardHeader>
        <CardContent class="space-y-4">
          <div class="flex flex-wrap gap-2">
            <Button variant="default">默认按钮</Button>
            <Button variant="secondary">次要按钮</Button>
            <Button variant="destructive">危险按钮</Button>
            <Button variant="outline">边框按钮</Button>
            <Button variant="ghost">幽灵按钮</Button>
            <Button variant="link">链接按钮</Button>
          </div>

          <div class="flex flex-wrap gap-2">
            <Button size="xs">超小</Button>
            <Button size="sm">小型</Button>
            <Button size="default">默认</Button>
            <Button size="lg">大型</Button>
            <Button size="xl">超大</Button>
          </div>
        </CardContent>
      </Card>

      <!-- 输入框组件测试 -->
      <Card>
        <CardHeader>
          <CardTitle>Input 组件</CardTitle>
          <CardDescription>不同类型和状态的输入框</CardDescription>
        </CardHeader>
        <CardContent class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input v-model="inputValue" placeholder="基础输入框" />
            <Input
              v-model="emailValue"
              type="email"
              placeholder="邮箱输入框"
              :prefix-icon="MailIcon"
            />
            <Input
              v-model="passwordValue"
              type="password"
              placeholder="密码输入框"
              :suffix-icon="EyeIcon"
            />
            <Input
              v-model="searchValue"
              placeholder="可清空输入框"
              clearable
              :prefix-icon="SearchIcon"
            />
          </div>

          <Input
            v-model="errorValue"
            placeholder="错误状态"
            error="这是一条错误信息"
          />

          <Input
            v-model="disabledValue"
            placeholder="禁用状态"
            disabled
          />
        </CardContent>
      </Card>

      <!-- 卡片组件测试 -->
      <Card>
        <CardHeader>
          <CardTitle>Card 组件变体</CardTitle>
          <CardDescription>不同样式的卡片</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card variant="default" size="sm">
              <CardHeader>
                <CardTitle class="text-lg">默认卡片</CardTitle>
              </CardHeader>
              <CardContent>
                <p class="text-sm text-muted-foreground">默认样式的卡片</p>
              </CardContent>
            </Card>

            <Card variant="elevated" size="sm">
              <CardHeader>
                <CardTitle class="text-lg">阴影卡片</CardTitle>
              </CardHeader>
              <CardContent>
                <p class="text-sm text-muted-foreground">带阴影的卡片</p>
              </CardContent>
            </Card>

            <Card variant="outlined" size="sm">
              <CardHeader>
                <CardTitle class="text-lg">边框卡片</CardTitle>
              </CardHeader>
              <CardContent>
                <p class="text-sm text-muted-foreground">加粗边框的卡片</p>
              </CardContent>
            </Card>

            <Card variant="ghost" size="sm">
              <CardHeader>
                <CardTitle class="text-lg">幽灵卡片</CardTitle>
              </CardHeader>
              <CardContent>
                <p class="text-sm text-muted-foreground">无边框的卡片</p>
              </CardContent>
            </Card>
          </div>
        </CardContent>
      </Card>

      <!-- 徽章和头像 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Badge 组件</CardTitle>
            <CardDescription>不同类型的徽章</CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="flex flex-wrap gap-2">
              <Badge variant="default">默认</Badge>
              <Badge variant="secondary">次要</Badge>
              <Badge variant="destructive">危险</Badge>
              <Badge variant="outline">边框</Badge>
              <Badge variant="success">成功</Badge>
              <Badge variant="warning">警告</Badge>
              <Badge variant="info">信息</Badge>
            </div>

            <div class="flex flex-wrap gap-2">
              <Badge size="sm">小型</Badge>
              <Badge size="default">默认</Badge>
              <Badge size="lg">大型</Badge>
              <Badge size="xl">超大</Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Avatar 组件</CardTitle>
            <CardDescription>不同状态的头像</CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="flex items-center space-x-4">
              <Avatar src="https://api.dicebear.com/7.x/avataaars/svg?seed=1" name="张三" />
              <Avatar src="https://api.dicebear.com/7.x/avataaars/svg?seed=2" name="李四" status="online" />
              <Avatar name="王五" status="busy" />
              <Avatar name="赵六" status="away" shape="square" />
            </div>

            <div class="flex items-center space-x-4">
              <Avatar size="xs" name="A" />
              <Avatar size="sm" name="B" />
              <Avatar size="default" name="C" />
              <Avatar size="lg" name="D" />
              <Avatar size="xl" name="E" />
              <Avatar size="2xl" name="F" />
            </div>
          </CardContent>
        </Card>
      </div>

      <!-- 下拉菜单和对话框 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>DropdownMenu 组件</CardTitle>
            <CardDescription>下拉菜单功能</CardDescription>
          </CardHeader>
          <CardContent>
            <DropdownMenu>
              <template #trigger>
                <Button variant="outline">
                  更多操作
                  <ChevronDownIcon class="ml-2 h-4 w-4" />
                </Button>
              </template>
              <template #content>
                <DropdownMenuLabel>账户管理</DropdownMenuLabel>
                <DropdownMenuItem>
                  <template #icon>
                    <UserIcon class="h-4 w-4" />
                  </template>
                  个人资料
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <template #icon>
                    <SettingsIcon class="h-4 w-4" />
                  </template>
                  设置
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem destructive>
                  <template #icon>
                    <LogOutIcon class="h-4 w-4" />
                  </template>
                  退出登录
                </DropdownMenuItem>
              </template>
            </DropdownMenu>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Dialog 组件</CardTitle>
            <CardDescription>对话框功能</CardDescription>
          </CardHeader>
          <CardContent>
            <Button @click="showDialog = true">打开对话框</Button>
          </CardContent>
        </Card>
      </div>

      <!-- 表格组件 -->
      <Card>
        <CardHeader>
          <CardTitle>Table 组件</CardTitle>
          <CardDescription>数据表格</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>姓名</TableHead>
                <TableHead>角色</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="user in tableData" :key="user.id">
                <TableCell>
                  <div class="flex items-center space-x-3">
                    <Avatar :name="user.name" size="sm" />
                    <span>{{ user.name }}</span>
                  </div>
                </TableCell>
                <TableCell>{{ user.role }}</TableCell>
                <TableCell>
                  <Badge :variant="user.status === '在线' ? 'success' : 'secondary'">
                    {{ user.status }}
                  </Badge>
                </TableCell>
                <TableCell>
                  <Button variant="ghost" size="sm">编辑</Button>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <!-- Tooltip 和 Loading -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Tooltip 组件</CardTitle>
            <CardDescription>提示功能</CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="flex space-x-4">
              <Tooltip content="这是一个顶部提示" placement="top">
                <Button variant="outline">顶部提示</Button>
              </Tooltip>
              <Tooltip content="这是一个底部提示" placement="bottom">
                <Button variant="outline">底部提示</Button>
              </Tooltip>
              <Tooltip content="这是一个左侧提示" placement="left">
                <Button variant="outline">左侧提示</Button>
              </Tooltip>
              <Tooltip content="这是一个右侧提示" placement="right">
                <Button variant="outline">右侧提示</Button>
              </Tooltip>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Loading 组件</CardTitle>
            <CardDescription>加载状态</CardDescription>
          </CardHeader>
          <CardContent class="space-y-4">
            <div class="flex flex-wrap gap-4">
              <Loading variant="spinner" size="sm" text="旋转加载" />
              <Loading variant="dots" size="sm" text="点状加载" />
              <Loading variant="pulse" size="sm" text="脉冲加载" />
              <Loading variant="bars" size="sm" text="条形加载" />
              <Loading variant="wave" size="sm" text="波浪加载" />
            </div>
          </CardContent>
        </Card>
      </div>
    </div>

    <!-- 对话框 -->
    <Dialog v-model:open="showDialog">
      <DialogHeader>
        <DialogTitle>确认操作</DialogTitle>
        <DialogDescription>
          这是一个示例对话框，用于测试主题切换功能。
        </DialogDescription>
      </DialogHeader>
      <DialogContent>
        <p>确定要执行此操作吗？此操作无法撤销。</p>
      </DialogContent>
      <DialogFooter>
        <Button variant="outline" @click="showDialog = false">取消</Button>
        <Button variant="destructive" @click="showDialog = false">确认</Button>
      </DialogFooter>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSimpleTheme } from '@/composables/useTheme'
import {
  Button,
  Input,
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  Badge,
  Avatar,
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  Dialog,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogContent,
  DialogFooter,
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Tooltip,
  // Loading, // 暂时注释掉
} from '@/components/ui'

import {
  SunIcon,
  MoonIcon,
  MailIcon,
  EyeIcon,
  SearchIcon,
  ChevronDownIcon,
  UserIcon,
  SettingsIcon,
  LogOutIcon,
} from 'lucide-vue-next'

// 主题控制
const { isDark, toggleTheme } = useSimpleTheme()

// 表单数据
const inputValue = ref('')
const emailValue = ref('')
const passwordValue = ref('')
const searchValue = ref('')
const errorValue = ref('')
const disabledValue = ref('禁用文本')

// 对话框状态
const showDialog = ref(false)

// 表格数据
const tableData = [
  { id: 1, name: '张三', role: '管理员', status: '在线' },
  { id: 2, name: '李四', role: '编辑', status: '离线' },
  { id: 3, name: '王五', role: '查看者', status: '在线' },
  { id: 4, name: '赵六', role: '编辑', status: '离线' },
]
</script>