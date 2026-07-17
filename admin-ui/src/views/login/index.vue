<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 品牌区域 -->
      <div class="brand-area">
        <div class="logo-icon">
          <svg viewBox="0 0 48 48" width="48" height="48">
            <rect x="8" y="20" width="32" height="8" rx="2" fill="#409eff" opacity="0.3"/>
            <rect x="12" y="12" width="24" height="24" rx="3" fill="#409eff" opacity="0.6"/>
            <rect x="16" y="8" width="16" height="32" rx="4" fill="#409eff"/>
          </svg>
        </div>
        <h2 class="brand-title">期货交易管理后台</h2>
        <p class="brand-subtitle">专业期货交易管理系统</p>
      </div>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <div
          :class="['tab-item', { active: mode === 'login' }]"
          @click="mode = 'login'"
        >登 录</div>
        <div
          :class="['tab-item', { active: mode === 'register' }]"
          @click="mode = 'register'"
        >注 册</div>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-if="mode === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        size="large"
        class="auth-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loginLoading"
            style="width: 100%"
            @click="handleLogin"
          >登 录</el-button>
        </el-form-item>

        <!-- 第三方登录 -->
        <div class="oauth-divider">
          <span class="oauth-divider-text">其他登录方式</span>
        </div>
        <div class="oauth-buttons">
          <el-button class="oauth-btn google-btn" @click="handleGoogleOauth">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            <span style="margin-left: 6px;">Google 账号</span>
          </el-button>
        </div>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-if="mode === 'register'"
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        size="large"
        class="auth-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱地址"
            :prefix-icon="Message"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码（至少6位）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="registerLoading"
            style="width: 100%"
            @click="handleRegister"
          >注 册</el-button>
        </el-form-item>
        <div class="register-tip">
          已有账号？
          <span class="link-text" @click="mode = 'login'">立即登录</span>
        </div>
      </el-form>

      <!-- 底部版权 -->
      <div class="footer-text">
        Futures Terminal v1.0.0
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { loginApi, registerApi, googleOauthApi } from '@/api/login'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const mode = ref<'login' | 'register'>('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

// 登录表单
const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// 注册表单
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  inviteCode: '',
})

const validatePass = (_rule: any, value: string, callback: any) => {
  if (value === '') callback(new Error('请再次输入密码'))
  else if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度 3-32 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass, trigger: 'blur' },
  ],
}

// 登录
async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loginLoading.value = true
  try {
    const res = await loginApi(loginForm)
    if (res.code === 200) {
      userStore.setToken(res.data.accessToken)
      userStore.setRefreshToken(res.data.refreshToken)
      userStore.setUserInfo(res.data.user)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '网络错误')
  } finally {
    loginLoading.value = false
  }
}

// 注册
async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    const res = await registerApi({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
    })
    if (res.code === 200) {
      userStore.setToken(res.data.accessToken)
      userStore.setRefreshToken(res.data.refreshToken)
      userStore.setUserInfo(res.data.user)
      ElMessage.success('注册成功，欢迎使用！')
      router.push('/')
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || e.message || '网络错误')
  } finally {
    registerLoading.value = false
  }
}

// Google OAuth
async function handleGoogleOauth() {
  ElMessage.info('正在跳转至 Google 授权...')
  // 模拟 Google OAuth — 生产环境使用真实 OAuth 流程
  // 这里用 prompt 模拟输入 Google 邮箱
  try {
    const email = window.prompt('请输入您的 Google 邮箱（演示模式）')
    if (!email) return
    const res = await googleOauthApi('mock-id-token', email, email.split('@')[0])
    if (res.code === 200) {
      userStore.setToken(res.data.accessToken)
      userStore.setRefreshToken(res.data.refreshToken)
      userStore.setUserInfo(res.data.user)
      ElMessage.success('Google 登录/注册成功')
      router.push('/')
    } else {
      ElMessage.error(res.msg || 'Google OAuth 失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.msg || e.message || 'OAuth 网络错误')
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a0a1a 0%, #1a1a3e 30%, #0f3460 70%, #0a0a1a 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.08), transparent 70%);
  top: -200px;
  right: -200px;
  pointer-events: none;
}

.login-container::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.05), transparent 70%);
  bottom: -100px;
  left: -100px;
  pointer-events: none;
}

.login-card {
  width: 420px;
  padding: 36px 40px 28px;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 12px;
  box-shadow: 0 8px 48px rgba(0, 0, 0, 0.25);
  position: relative;
  z-index: 1;
}

.brand-area {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  margin-bottom: 8px;
}

.brand-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  letter-spacing: 1px;
}

.brand-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #999;
}

.tab-bar {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
}

.tab-item.active {
  color: #409eff;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 20%;
  width: 60%;
  height: 2px;
  background: #409eff;
  border-radius: 1px;
}

.auth-form {
  margin-top: 4px;
}

.auth-form :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e4e7ed inset;
  border-radius: 8px;
  transition: box-shadow 0.25s;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.auth-form :deep(.el-input__inner) {
  height: 42px;
}

.oauth-divider {
  display: flex;
  align-items: center;
  margin: 16px 0 12px;
}

.oauth-divider::before,
.oauth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e4e7ed;
}

.oauth-divider-text {
  padding: 0 12px;
  font-size: 12px;
  color: #999;
}

.oauth-buttons {
  display: flex;
  justify-content: center;
}

.oauth-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 40px;
  border-radius: 8px;
  font-size: 14px;
}

.google-btn {
  background: #fff;
  border: 1px solid #dadce0;
  color: #3c4043;
}

.google-btn:hover {
  background: #f8f9fa;
  border-color: #babec2;
}

.register-tip {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.link-text {
  color: #409eff;
  cursor: pointer;
  text-decoration: none;
}

.link-text:hover {
  text-decoration: underline;
}

.footer-text {
  text-align: center;
  font-size: 11px;
  color: #bbb;
  margin-top: 24px;
}

@media (max-width: 480px) {
  .login-card {
    width: 92%;
    padding: 28px 20px 20px;
  }
}
</style>
