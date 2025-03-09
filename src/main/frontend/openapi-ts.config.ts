import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
//  input: 'http://100.124.21.155:8080/q/openapi',
  input: 'http://localhost:8080/q/openapi',
  output: 'src/lib/client',
  plugins: ['@hey-api/client-fetch'],
});
