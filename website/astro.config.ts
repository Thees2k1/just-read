import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://just-read.web.app',
  integrations: [
    starlight({
      title: 'Just Read',
      description: 'Official documentation and privacy compliance site for Just Read on Android.',
      customCss: ['./src/styles/custom.css'],
      sidebar: [
        {
          label: 'Legal & Privacy',
          items: [
            { label: 'Privacy Policy', slug: 'privacy' },
            { label: 'Account & Data Deletion', slug: 'delete-account' },
          ],
        },
        {
          label: 'Guides',
          items: [{ autogenerate: { directory: 'guides' } }],
        },
      ],
    }),
  ],
});
