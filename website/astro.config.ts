import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://just-read.web.app',
  integrations: [
    starlight({
      title: 'Just Read',
      description: 'Official documentation and privacy compliance site for Just Read on Android.',
    }),
  ],
});
