<template>
    <div class="login-bg">
        <div class="login-container">
            <div class="login-header">
                <img class="logo mr10" src="../../assets/img/logo.png" alt="" />
                <div class="login-title">车云数据解析系统</div>
            </div>
            <el-form :model="param" :rules="rules" ref="register" size="large">
                <el-form-item prop="username">
                    <el-input v-model="param.username" placeholder="用户名">
                        <template #prepend>
                            <el-icon>
                                <User />
                            </el-icon>
                        </template>
                    </el-input>
                </el-form-item>
                
                <el-form-item prop="password">
                    <el-input
                        type="password"
                        placeholder="密码"
                        v-model="param.password"
                        @keyup.enter="submitForm(register)"
                    >
                        <template #prepend>
                            <el-icon>
                                <Lock />
                            </el-icon>
                        </template>
                    </el-input>
                </el-form-item>

                <el-form-item prop="email">
                    <el-input v-model="param.email" placeholder="邮箱">
                        <template #prepend>
                            <el-icon>
                                <Message />
                            </el-icon>
                        </template>
                    </el-input>
                </el-form-item>

                <el-button class="login-btn" type="primary" size="large" @click="submitForm(register)">注册</el-button>
                <p class="login-text">
                    已有账号，<el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
                </p>
            </el-form>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import axios from 'axios'; // 引入 axios
import { Register } from '@/types/user';

const router = useRouter();
const param = reactive<Register>({
    username: '',
    password: '',
    email: '',
});

const rules: FormRules = {
    username: [
        {
            required: true,
            message: '请输入用户名',
            trigger: 'blur',
        },
    ],
    password: [{ required: true, message: '密码应至少8位，且包含大小写字母和数字', trigger: 'blur' }],
    email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
};
const register = ref<FormInstance>();

const submitForm = async (formEl: FormInstance | undefined) => {
    if (!formEl) return;
    formEl.validate(async (valid: boolean) => {
        if (valid) {
            try {
                // 调用后端 API
                const response = await axios.post('http://111.231.191.2:8080/api/usermanage/public/register', {
                    username: param.username,
                    password: param.password,
                    email: param.email,
                });

                // 假设后端返回的数据结构为 { success: true, message: '注册成功' }
                if (response.status===200) {
                    ElMessage.success(response.data.message || '注册成功，请登录');
                    router.push('/login'); // 跳转到登录页面
                } else {
                    ElMessage.error(response.data.message || '注册失败');
                }
            } catch (error: any) {
                if (error.response) {
                    const errorData = error.response.data;
                    if (errorData.message) {
                        // 处理不同的错误类型
                        if (errorData.message.includes('用户名已存在')) {
                            ElMessage.error('用户名已存在，请更换其他用户名');
                        } else if (errorData.message.includes('密码长度至少为8位')) {
                            ElMessage.error('密码长度至少为8位');
                        } else if (errorData.message.includes('密码必须包含至少一个大写字母')) {
                            ElMessage.error('密码必须包含至少一个大写字母、一个数字和一个特殊字符');
                        } else if (errorData.message.includes('Duplicate entry') && errorData.message.includes('email')) {
                            ElMessage.error('该邮箱已被注册，请使用其他邮箱');
                        } else {
                            // 其他服务器错误
                            ElMessage.error(errorData.message || '注册失败，请检查输入信息');
                        }
                    } else {
                        ElMessage.error('注册失败，请检查网络或联系管理员');
                    }
                } else {
                    ElMessage.error('网络错误，请检查网络连接');
                }
                console.error(error);
            }
        } else {
            ElMessage.error('表单验证失败');
            return false;
        }
    });
};

</script>

<style scoped>
.login-bg {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100vh;
    background: url(../../assets/img/bg.png) center/cover no-repeat;
}

.login-header {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 40px;
}

.logo {
    width: 70px;
}

.login-title {
    font-size: 22px;
    color: #C9E9FF;
    font-weight: bold;
}

.login-container {
    width: 450px;
    border-radius: 5px;
    background: rgba(5, 23, 47, 0.8); /* 设置背景为白色并添加透明度 */
    padding: 40px 50px 50px;
    box-sizing: border-box;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2); /* 添加阴影以增强视觉效果 */
}

.login-btn {
    display: block;
    width: 100%;
}

.login-text {
    display: flex;
    align-items: center;
    margin-top: 20px;
    font-size: 14px;
    color: #787878;
}
</style>
