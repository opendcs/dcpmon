import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path';

export default defineConfig(({ mode }) => {
  const isDev = mode === 'development';

  return {
    plugins: [react(), tailwindcss(),],
    resolve: {
      alias: isDev
        && {
            'dds-api': path.resolve(__dirname, '../api-client/generated/dist'),
          }
    },
    server: {
      watch: {
        ignored: ['!**/api-client/generated/dist/**']
      }
    }
  };
});