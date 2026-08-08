import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import Icons from 'unplugin-icons/vite';

export default defineConfig({
	esbuild: {
		supported: {
			'top-level-await': true
		}
	},
	// The oidc-spa Vite plugin does this for us in other stacks, but it only supports projects with a
	// root index.html, which SvelteKit does not have. Pre-bundling oidc-spa breaks its early init.
	optimizeDeps: {
		exclude: ['oidc-spa', 'oidc-spa/core', 'oidc-spa/entrypoint']
	},
	plugins: [
		Icons({
			compiler: 'svelte',
			autoInstall: true
		}),
		sveltekit(),
		tailwindcss()
	],
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
				changeOrigin: true
			}
		}
	}
});
