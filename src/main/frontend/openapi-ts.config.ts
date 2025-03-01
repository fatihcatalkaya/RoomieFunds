import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: 'http://100.124.17.197:8080/q/openapi',
  output: 'src/lib/client',
  plugins: ['@hey-api/client-fetch'],
});
