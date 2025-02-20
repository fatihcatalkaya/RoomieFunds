import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import Icons from 'unplugin-icons/vite';

export default defineConfig({
    plugins: [Icons({ compiler: 'svelte', autoInstall: true }), sveltekit(), tailwindcss()],
    server: {
        proxy: {
            '/realms': {
                target: 'http://127.0.0.1:9090',
                changeOrigin: true
            },
            '/resources': {
                target: 'http://127.0.0.1:9090',
                changeOrigin: true
            },
            '/api': {
                target: 'http://127.0.0.1:8080',
                changeOrigin: true,
            },
        }
    }
});
