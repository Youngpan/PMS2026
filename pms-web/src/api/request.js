import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import router from '@/router';
const instance = axios.create({
    baseURL: '/api/v1',
    timeout: 15000,
});
instance.interceptors.request.use((config) => {
    const auth = useAuthStore();
    if (auth.token) {
        config.headers.Authorization = `Bearer ${auth.token}`;
    }
    // 写操作自动附带幂等键（避免重复提交）
    const method = (config.method || '').toLowerCase();
    if ((method === 'post' || method === 'put') && !config.headers['Idempotency-Key']) {
        config.headers['Idempotency-Key'] = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    }
    return config;
});
instance.interceptors.response.use((resp) => {
    const data = resp.data;
    if (data && typeof data === 'object' && 'code' in data) {
        if (data.code === 0)
            return data.data;
        ElMessage.error(data.message || '请求失败');
        return Promise.reject(data);
    }
    return data;
}, (error) => {
    const status = error.response?.status;
    const msg = error.response?.data?.message || error.message;
    if (status === 401) {
        const auth = useAuthStore();
        auth.clear();
        router.push('/login');
    }
    ElMessage.error(msg || '网络异常');
    return Promise.reject(error);
});
// 响应拦截器已解包返回 data.data，故包装方法直接声明为 Promise<T>，
// 避免调用方拿到 AxiosResponse 类型。
const request = {
    get: (url, config) => instance.get(url, config),
    post: (url, data, config) => instance.post(url, data, config),
    put: (url, data, config) => instance.put(url, data, config),
    delete: (url, config) => instance.delete(url, config),
};
export default request;
