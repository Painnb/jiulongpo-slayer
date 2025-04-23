import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import VueSetupExtend from 'vite-plugin-vue-setup-extend';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
export default defineConfig({
	server: {
		port: 3000, // 设置开发服务器的端口号
    	host: "0.0.0.0", // 可选：设置为 0.0.0.0 允许外部访问
    	open: true, // 可选：启动项目时自动打开浏览器
		proxy: {
			'/abc': {
				//target: 'http://127.0.0.1:8080',
				//target: 'http://111.231.191.2:8080',

				target: 'http://10.135.249.135:8080',

				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/abc/, '')
			},
			'/stu': {
				target: 'http://10.135.249.135:8081',
				//target: 'http://111.231.191.2:8081',
				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/stu/, '')
			}
		}
	},
	base: './',
	plugins: [
		vue(),
		VueSetupExtend(),
		AutoImport({
			resolvers: [ElementPlusResolver()]
		}),
		Components({
			resolvers: [ElementPlusResolver()]
		})
	],
	optimizeDeps: {
		include: ['schart.js']
	},
	resolve: {
		alias: {
			'@': '/src',
			'~': '/src/assets'
		}
	},
	define: {
		__VUE_PROD_HYDRATION_MISMATCH_DETAILS__: "true",
	},
	
});
