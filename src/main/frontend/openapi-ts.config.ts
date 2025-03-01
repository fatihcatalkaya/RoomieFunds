import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: 'http://localhost:8080/q/openapi',
  output: 'src/lib/client',
  plugins: ['@hey-api/client-fetch'],
});
