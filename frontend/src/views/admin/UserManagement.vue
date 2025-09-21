<template>
  <div class="user-management-page">
    <div class="space-y-6">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold text-foreground">用户管理</h1>
          <p class="text-muted-foreground mt-2">管理系统用户和权限</p>
        </div>
        <Button @click="showCreateDialog = true">
          新增用户
        </Button>
      </div>

      <!-- 搜索和筛选 -->
      <Card>
        <CardContent class="pt-6">
          <div class="flex flex-col md:flex-row gap-4">
            <div class="flex-1">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索用户姓名、邮箱..."
                class="w-full px-3 py-2 border border-border rounded-md bg-background"
                @input="searchUsers"
              />
            </div>
            <div class="flex gap-2">
              <select
                v-model="filterRole"
                @change="filterUsers"
                class="px-3 py-2 border border-border rounded-md bg-background"
              >
                <option value="">所有角色</option>
                <option value="admin">管理员</option>
                <option value="editor">编辑者</option>
                <option value="viewer">查看者</option>
              </select>
              <select
                v-model="filterStatus"
                @change="filterUsers"
                class="px-3 py-2 border border-border rounded-md bg-background"
              >
                <option value="">所有状态</option>
                <option value="active">激活</option>
                <option value="inactive">未激活</option>
                <option value="suspended">已暂停</option>
              </select>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 用户列表 -->
      <Card>
        <CardHeader>
          <CardTitle>用户列表</CardTitle>
          <CardDescription>共 {{ filteredUsers.length }} 个用户</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-border">
                  <th class="text-left py-3">用户信息</th>
                  <th class="text-left py-3">角色</th>
                  <th class="text-left py-3">状态</th>
                  <th class="text-left py-3">最后登录</th>
                  <th class="text-left py-3">创建时间</th>
                  <th class="text-left py-3">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="user in filteredUsers"
                  :key="user.id"
                  class="border-b border-border hover:bg-muted/50"
                >
                  <td class="py-3">
                    <div class="flex items-center space-x-3">
                      <div class="w-8 h-8 bg-primary rounded-full flex items-center justify-center text-primary-foreground text-sm font-medium">
                        {{ user.name.charAt(0).toUpperCase() }}
                      </div>
                      <div>
                        <div class="font-medium">{{ user.name }}</div>
                        <div class="text-xs text-muted-foreground">{{ user.email }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="py-3">
                    <Badge :variant="getRoleVariant(user.role)">
                      {{ getRoleText(user.role) }}
                    </Badge>
                  </td>
                  <td class="py-3">
                    <Badge :variant="getStatusVariant(user.status)">
                      {{ getStatusText(user.status) }}
                    </Badge>
                  </td>
                  <td class="py-3 text-muted-foreground">
                    {{ formatDateTime(user.lastLogin) }}
                  </td>
                  <td class="py-3 text-muted-foreground">
                    {{ formatDateTime(user.createdAt) }}
                  </td>
                  <td class="py-3">
                    <div class="flex items-center space-x-2">
                      <Button size="sm" variant="outline" @click="editUser(user)">
                        编辑
                      </Button>
                      <Button
                        size="sm"
                        :variant="user.status === 'active' ? 'outline' : 'default'"
                        @click="toggleUserStatus(user)"
                      >
                        {{ user.status === 'active' ? '暂停' : '激活' }}
                      </Button>
                      <Button size="sm" variant="outline" @click="deleteUser(user)">
                        删除
                      </Button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 创建/编辑用户对话框 -->
    <Dialog :open="showCreateDialog" @update:open="showCreateDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {{ editingUser ? '编辑用户' : '新增用户' }}
          </DialogTitle>
          <DialogDescription>
            {{ editingUser ? '修改用户信息和权限' : '创建新的系统用户' }}
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <div>
            <label class="text-sm font-medium mb-2 block">姓名</label>
            <input
              v-model="userForm.name"
              type="text"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            />
          </div>
          <div>
            <label class="text-sm font-medium mb-2 block">邮箱</label>
            <input
              v-model="userForm.email"
              type="email"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            />
          </div>
          <div v-if="!editingUser">
            <label class="text-sm font-medium mb-2 block">密码</label>
            <input
              v-model="userForm.password"
              type="password"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            />
          </div>
          <div>
            <label class="text-sm font-medium mb-2 block">角色</label>
            <select
              v-model="userForm.role"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            >
              <option value="viewer">查看者</option>
              <option value="editor">编辑者</option>
              <option value="admin">管理员</option>
            </select>
          </div>
          <div>
            <label class="text-sm font-medium mb-2 block">状态</label>
            <select
              v-model="userForm.status"
              class="w-full px-3 py-2 border border-border rounded-md bg-background"
            >
              <option value="active">激活</option>
              <option value="inactive">未激活</option>
              <option value="suspended">已暂停</option>
            </select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showCreateDialog = false">
            取消
          </Button>
          <Button @click="saveUser">
            {{ editingUser ? '更新' : '创建' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui'
import { Button } from '@/components/ui'
import { Badge } from '@/components/ui'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui'

interface User {
  id: number
  name: string
  email: string
  role: 'admin' | 'editor' | 'viewer'
  status: 'active' | 'inactive' | 'suspended'
  lastLogin: Date
  createdAt: Date
}

// 响应式数据
const searchQuery = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const showCreateDialog = ref(false)
const editingUser = ref<User | null>(null)

// 用户表单
const userForm = ref<{
  name: string
  email: string
  password: string
  role: 'admin' | 'editor' | 'viewer'
  status: 'active' | 'inactive' | 'suspended'
}>({
  name: '',
  email: '',
  password: '',
  role: 'viewer',
  status: 'active'
})

// 模拟用户数据
const users = ref<User[]>([
  {
    id: 1,
    name: '张三',
    email: 'zhang@example.com',
    role: 'admin',
    status: 'active',
    lastLogin: new Date(Date.now() - 2 * 60 * 60 * 1000),
    createdAt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
  },
  {
    id: 2,
    name: '李四',
    email: 'li@example.com',
    role: 'editor',
    status: 'active',
    lastLogin: new Date(Date.now() - 5 * 60 * 60 * 1000),
    createdAt: new Date(Date.now() - 25 * 24 * 60 * 60 * 1000)
  },
  {
    id: 3,
    name: '王五',
    email: 'wang@example.com',
    role: 'viewer',
    status: 'inactive',
    lastLogin: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
    createdAt: new Date(Date.now() - 20 * 24 * 60 * 60 * 1000)
  }
])

// 筛选后的用户列表
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchesSearch = !searchQuery.value ||
      user.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesRole = !filterRole.value || user.role === filterRole.value
    const matchesStatus = !filterStatus.value || user.status === filterStatus.value

    return matchesSearch && matchesRole && matchesStatus
  })
})

// 工具函数
const getRoleVariant = (role: string) => {
  switch (role) {
    case 'admin': return 'default'
    case 'editor': return 'secondary'
    case 'viewer': return 'outline'
    default: return 'outline'
  }
}

const getRoleText = (role: string) => {
  switch (role) {
    case 'admin': return '管理员'
    case 'editor': return '编辑者'
    case 'viewer': return '查看者'
    default: return role
  }
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'active': return 'default'
    case 'inactive': return 'secondary'
    case 'suspended': return 'destructive'
    default: return 'outline'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'active': return '激活'
    case 'inactive': return '未激活'
    case 'suspended': return '已暂停'
    default: return status
  }
}

const formatDateTime = (date: Date) => {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 操作方法
const searchUsers = () => {
  // 搜索功能已在computed中实现
}

const filterUsers = () => {
  // 筛选功能已在computed中实现
}

const editUser = (user: User) => {
  editingUser.value = user
  userForm.value = {
    name: user.name,
    email: user.email,
    password: '',
    role: user.role,
    status: user.status
  }
  showCreateDialog.value = true
}

const toggleUserStatus = (user: User) => {
  const newStatus = user.status === 'active' ? 'suspended' : 'active'
  user.status = newStatus
  console.log(`User ${user.name} status changed to ${newStatus}`)
}

const deleteUser = (user: User) => {
  if (confirm(`确定要删除用户 ${user.name} 吗？`)) {
    const index = users.value.findIndex(u => u.id === user.id)
    if (index > -1) {
      users.value.splice(index, 1)
    }
  }
}

const saveUser = () => {
  if (editingUser.value) {
    // 更新用户
    Object.assign(editingUser.value, userForm.value)
    console.log('User updated:', editingUser.value)
  } else {
    // 创建新用户
    const newUser: User = {
      id: Math.max(...users.value.map(u => u.id)) + 1,
      name: userForm.value.name,
      email: userForm.value.email,
      role: userForm.value.role,
      status: userForm.value.status,
      lastLogin: new Date(),
      createdAt: new Date()
    }
    users.value.push(newUser)
    console.log('User created:', newUser)
  }

  // 重置表单
  userForm.value = {
    name: '',
    email: '',
    password: '',
    role: 'viewer',
    status: 'active'
  }
  editingUser.value = null
  showCreateDialog.value = false
}

onMounted(() => {
  console.log('User management page mounted')
})
</script>

<style scoped>
.user-management-page {
  /* 自定义样式 */
}
</style>