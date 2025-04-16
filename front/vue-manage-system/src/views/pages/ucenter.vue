<template>
    <div>
        <div class="user-container">
            <el-card class="user-profile" shadow="hover" :body-style="{ padding: '0px' }">
                <div class="user-profile-bg"></div>
                <div class="user-avatar-wrap">
                    <el-avatar class="user-avatar" :size="120" :src="avatarImg" />
                </div>
                <div class="user-info">
                    <div class="info-name">{{ name }}</div>
                    <div class="info-desc">
                        <el-divider direction="vertical" />
                        <el-link href="https://github.com/Painnb/jiulongpo-slayer" target="_blank">jiulongpo-slayer
                            .github</el-link>
                    </div>
                    <div class="info-icon">
                        <a href="https://github.com/Painnb/jiulongpo-slayer" target="_blank"> <i class="el-icon-lx-github-fill"></i></a>
                 
                    </div>
                </div>
            </el-card>
            <el-card
                class="user-content"
                shadow="hover"
                :body-style="{ padding: '20px 50px', height: '100%', boxSizing: 'border-box' }"
            >
                <el-tabs tab-position="left" v-model="activeName">
                    <el-tab-pane name="label5" label="更改订阅" class="user-tabpane">
                        <el-form class="w500" label-position="top">
                            <el-form-item label="Broker URL：">
                                <el-input v-model="mqttConfig.brokerUrl" placeholder="tcp://192.168.31.250:1887"></el-input>
                            </el-form-item>
                            <el-form-item label="用户名：">
                                <el-input v-model="mqttConfig.username" placeholder="smqtt"></el-input>
                            </el-form-item>
                            <el-form-item label="密码：">
                                <el-input type="password" v-model="mqttConfig.password" placeholder="smqtt"></el-input>
                            </el-form-item> 
                            <el-form-item label="客户端 ID：">
                                <el-input v-model="mqttConfig.clientId" placeholder="myclient"></el-input>
                            </el-form-item>
                            <el-form-item label="订阅主题：">
                                <el-input v-model="mqttConfig.subTopics" placeholder="#"></el-input>
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="saveMqttConfig">保存</el-button>
                                <el-button type="primary" @click="subMqttConfig">订阅</el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>
                        
                    <el-tab-pane name="label2" label="个人信息" class="user-tabpane">
                        <el-form class="w500" label-position="top">
                            <el-form-item label="用户名：">
                                <el-input v-model="userInfo.username" placeholder="请输入用户名"></el-input>
                            </el-form-item>
                            <el-form-item label="邮箱：">
                                <el-input v-model="userInfo.email" placeholder="请输入邮箱"></el-input>
                            </el-form-item>
                            <el-form-item label="手机号：">
                                <el-input v-model="userInfo.phone" placeholder="请输入手机号"></el-input>
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="saveUserInfo">保存</el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>
                    <el-tab-pane name="label3" label="修改密码" class="user-tabpane">
                        <el-form class="w500" label-position="top">
                            <el-form-item label="旧密码：">
                                <el-input type="password" v-model="form.old"></el-input>
                            </el-form-item>
                            <el-form-item label="新密码：">
                                <el-input type="password" v-model="form.new"></el-input>
                            </el-form-item>
                            <el-form-item label="确认新密码：">
                                <el-input type="password" v-model="form.new1"></el-input>
                            </el-form-item>
                            <el-form-item>
                                <el-button type="primary" @click="onSubmit">保存</el-button>
                            </el-form-item>
                        </el-form>
                    </el-tab-pane>
                    
                </el-tabs>
            </el-card>
        </div>
    </div>
</template>

<script setup lang="ts" name="ucenter">
import { ElMessage } from 'element-plus';
import { reactive, ref, onMounted } from 'vue';
import { VueCropper } from 'vue-cropper';
import 'vue-cropper/dist/index.css';
import avatar from '@/assets/img/img.jpg';
import TabsComp from '../element/tabs.vue';
import axios from 'axios';

const name = localStorage.getItem('vuems_name');
const form = reactive({
    new1: '',
    new: '',
    old: '',
});
const onSubmit = () => {};

const activeName = ref('label5');

const avatarImg = ref(avatar);
const imgSrc = ref(avatar);
const cropImg = ref('');
const cropper: any = ref();

const userInfo = reactive({
    username: '默认用户名',
    email: 'example@example.com',
    phone: '1234567890',
});

const saveUserInfo = () => {
    console.log('保存用户信息：', userInfo);
    // TODO: 调用后端接口保存用户信息
    // 示例：
    // axios.post('/api/user/info', userInfo).then(response => {
    //     console.log('保存成功', response);
    // }).catch(error => {
    //     console.error('保存失败', error);
    // });
};

const setImage = (e: any) => {
    const file = e.target.files[0];
    if (!file.type.includes('image/')) {
        return;
    }
    const reader = new FileReader();
    reader.onload = (event: any) => {
        imgSrc.value = event.target.result;
        cropper.value && cropper.value.replace(event.target.result);
    };
    reader.readAsDataURL(file);
};

const cropImage = () => {
    cropImg.value = cropper.value?.getCroppedCanvas().toDataURL();
};

const saveAvatar = () => {
    avatarImg.value = cropImg.value;
};

const mqttConfig = reactive({
    brokerUrl: localStorage.getItem('mqtt_brokerUrl') || 'tcp://192.168.31.250:1887',
    username: localStorage.getItem('mqtt_username') || 'smqtt',
    password: localStorage.getItem('mqtt_password') || 'smqtt',
    clientId: localStorage.getItem('mqtt_clientId') || 'myclient',
    subTopics: localStorage.getItem('mqtt_subTopics') || '#',
});
// 组件挂载时从localStorage加载MQTT配置
onMounted(() => {
    const savedConfig = localStorage.getItem('mqttConfig');
    if (savedConfig) {
        Object.assign(mqttConfig, JSON.parse(savedConfig));
    }
});
const saveMqttConfig = () => {
    // 保存到localStorage
    localStorage.setItem('mqttConfig', JSON.stringify(mqttConfig));
    // 同时单独存储每个字段以便其他页面使用
    localStorage.setItem('mqtt_brokerUrl', mqttConfig.brokerUrl);
    localStorage.setItem('mqtt_username', mqttConfig.username);
    localStorage.setItem('mqtt_password', mqttConfig.password);
    localStorage.setItem('mqtt_clientId', mqttConfig.clientId);
    localStorage.setItem('mqtt_subTopics', mqttConfig.subTopics);
    
    console.log('MQTT配置已保存到本地存储:', mqttConfig);
    // 添加成功提示
    ElMessage.success('MQTT配置已保存');
    
    // 如果需要同时保存到后端
    const token = localStorage.getItem('token');
    if (token) {
        axios.post('/abc/mqtt/config', mqttConfig, {
            headers: {
                'Authorization': `Bearer ${token}`
            },
        })
        .then(response => {
            console.log('保存到服务器成功', response);
            ElMessage.success('配置已同步到服务器');
        })
        .catch(error => {
            console.error('保存到服务器失败', error);
            ElMessage.error('服务器保存失败，但本地已保存');
        });
    }
};

const subMqttConfig = () => {
    console.log('保存 MQTT 配置：', mqttConfig);
    const token = localStorage.getItem('token');
    if (token) {
        axios.post('/abc/mqtt/config',  mqttConfig, {
            headers: {
                'Authorization': `Bearer ${token}`
            },

        })
        .then(response => {
            console.log('保存成功', response);
            // 可以在这里添加保存成功的提示信息
        })
        .catch(error => {
            console.error('保存失败', error);
            // 可以在这里添加保存失败的提示信息
        });
    } else {
        console.error('Token 未找到');
        // 可以在这里添加 Token 未找到的提示信息
    }
};
</script>

<style scoped>
.user-container {
    display: flex;
    height: 80vh;
}

.user-profile {
    position: relative;
    height: 80vh;
    width: 500px;
    margin-right: 20px;
    flex: 0 0 auto;
    align-self: flex-start;
}

.user-profile-bg {
    width: 100%;
    height: 80vh;
    background-image: url('../../assets/img/ucenter-bg.jpg');
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    z-index: 1;
    position: absolute;
    top: 0;
    left: 0;
}

.user-avatar-wrap {
    position: absolute;
    top: 135px;
    width: 100%;
    text-align: center;
    z-index: 2;
}

.user-avatar {
    border: 5px solid #fff;
    border-radius: 50%;
    overflow: hidden;
    box-shadow: 0 7px 12px 0 rgba(62, 57, 107, 0.16);
}

.user-info {
    position: relative;
    z-index: 2;
    margin-top: 260px; 
    text-align: center;
    padding: 20px;
    background-color: rgba(255, 255, 255, 0.675);
    border-radius: 10px; /* 圆角 */
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); /* 添加阴影 */
}

.info-name {
    margin: 0 0 10px;
    font-size: 26px;
    font-weight: 600;
    color: #333;
}

.info-desc {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
}

.info-desc,
.info-desc a {
    font-size: 18px;
    color: #555;
}

.info-icon {
    margin-top: 10px;
}

.info-icon i {
    font-size: 30px;
    margin: 0 10px;
    color: #333; 
    cursor: pointer;
    transition: color 0.3s;
}

.info-icon i:hover {
    color: #0056b3; /* 鼠标悬停时的颜色 */
}

.user-content {
    flex: 1;
    background-image: url('../../assets/img/ucenter-bg1.jpg');
}
</style>

<style>
.el-tabs.el-tabs--left {
    height: 100%;
}
</style>